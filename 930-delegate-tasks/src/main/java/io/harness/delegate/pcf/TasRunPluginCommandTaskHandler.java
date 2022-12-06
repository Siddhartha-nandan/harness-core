package io.harness.delegate.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.model.PcfConstants.PCF_ARTIFACT_DOWNLOAD_DIR_PATH;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static com.google.common.base.Charsets.UTF_8;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FileData;
import io.harness.connector.task.tas.TasNgConfigMapper;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.cf.PcfCommandTaskBaseHelper;
import io.harness.delegate.task.cf.CfCommandTaskHelperNG;
import io.harness.delegate.task.pcf.request.CfCommandRequestNG;
import io.harness.delegate.task.pcf.request.CfRunPluginCommandRequestNG;
import io.harness.delegate.task.pcf.response.CfCommandResponseNG;
import io.harness.delegate.task.pcf.response.TasInfraConfig;
import io.harness.delegate.task.pcf.response.TasRunPluginResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.Misc;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.CfRunPluginScriptRequestData;
import io.harness.pcf.model.CloudFoundryConfig;
import io.harness.pcf.model.PcfConstants;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@NoArgsConstructor
@Singleton
@Slf4j
@OwnedBy(CDP)
public class TasRunPluginCommandTaskHandler extends CfCommandTaskNGHandler {
  @Inject TasNgConfigMapper tasNgConfigMapper;
  @Inject protected CfCommandTaskHelperNG cfCommandTaskHelperNG;
  @Inject PcfCommandTaskBaseHelper pcfCommandTaskBaseHelper;
  @Inject CfDeploymentManager cfDeploymentManager;

  @Override
  protected CfCommandResponseNG executeTaskInternal(CfCommandRequestNG cfCommandRequestNG,
      ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(cfCommandRequestNG instanceof CfRunPluginCommandRequestNG)) {
      throw new InvalidArgumentsException(
          Pair.of("cfCommandRequestNG", "Must be instance of CfRunPluginCommandRequestNG"));
    }

    LogCallback executionLogCallback = iLogStreamingTaskClient.obtainLogCallback(cfCommandRequestNG.getCommandName());
    CfRunPluginCommandRequestNG pluginCommandRequest = (CfRunPluginCommandRequestNG) cfCommandRequestNG;

    executionLogCallback.saveExecutionLog(color("---------- Starting PCF Run Plugin Command Execution", White, Bold));

    TasInfraConfig tasInfraConfig = pluginCommandRequest.getTasInfraConfig();
    CloudFoundryConfig cfConfig = tasNgConfigMapper.mapTasConfigWithDecryption(
        tasInfraConfig.getTasConnectorDTO(), tasInfraConfig.getEncryptionDataDetails());
    CfRequestConfig cfRequestConfig = getCfRequestConfig(pluginCommandRequest, cfConfig);
    File workingDirectory = null;

    try {
      workingDirectory = createWorkingDirectory();
      final String workingDirCanonicalPath = dirCanonicalPath(workingDirectory);

      // save the files in the directory
      if (EmptyPredicate.isNotEmpty(pluginCommandRequest.getFileDataList())) {
        saveFilesInWorkingDirectoryStringContent(pluginCommandRequest.getFileDataList(), workingDirCanonicalPath);
      }

      //  insert working directory in script path
      final String finalScriptString = prepareFinalScript(pluginCommandRequest.getRenderedScriptString(),
          workingDirCanonicalPath, StringUtils.defaultIfEmpty(pluginCommandRequest.getRepoRoot(), "/"));

      // log all the files being saved and files being resolved in the script
      executionLogCallback.saveExecutionLog("\n #  File paths identified in the Script :");
      pcfCommandTaskBaseHelper.printFileNamesInExecutionLogs(
          pluginCommandRequest.getFilePathsInScript(), executionLogCallback);

      executionLogCallback.saveExecutionLog(
          "\n #  Files saved in working directory [" + workingDirCanonicalPath + "]:");
      pcfCommandTaskBaseHelper.printFileNamesInExecutionLogs(
          CollectionUtils.emptyIfNull(pluginCommandRequest.getFileDataList())
              .stream()
              .map(FileData::getFilePath)
              .collect(Collectors.toList()),
          executionLogCallback);

      final CfRunPluginScriptRequestData cfRunPluginScriptRequestData = CfRunPluginScriptRequestData.builder()
                                                                            .workingDirectory(workingDirCanonicalPath)
                                                                            .finalScriptString(finalScriptString)
                                                                            .cfRequestConfig(cfRequestConfig)
                                                                            .build();

      cfDeploymentManager.runPcfPluginScript(cfRunPluginScriptRequestData, executionLogCallback);

      executionLogCallback.saveExecutionLog(
          "\n ----------  PCF Run Plugin Command completed successfully", INFO, CommandExecutionStatus.SUCCESS);
      return TasRunPluginResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();

    } catch (Exception e) {
      return handleError(executionLogCallback, pluginCommandRequest, e);
    } finally {
      removeTempFilesCreated(executionLogCallback, workingDirectory);
    }
  }
  private TasRunPluginResponse handleError(
      LogCallback executionLogCallback, CfRunPluginCommandRequestNG pluginCommandRequest, Exception e) {
    log.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Run Plugin Command task [{}]",
        pluginCommandRequest);
    log.error("Exception is ", e);
    executionLogCallback.saveExecutionLog("\n\n ----------  PCF Run Plugin Command failed to complete successfully",
        ERROR, CommandExecutionStatus.FAILURE);
    Misc.logAllMessages(e, executionLogCallback);
    return TasRunPluginResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .errorMessage(ExceptionUtils.getMessage(e))
        .build();
  }
  private void removeTempFilesCreated(LogCallback executionLogCallback, File workingDirectory) {
    try {
      executionLogCallback.saveExecutionLog("# Deleting any temporary files created");
      if (workingDirectory != null) {
        FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
      }
    } catch (Exception e) {
      log.warn("Failed to remove temp files created", e);
    }
  }

  private CfRequestConfig getCfRequestConfig(
      CfRunPluginCommandRequestNG pluginCommandRequest, CloudFoundryConfig cfConfig) {
    return CfRequestConfig.builder()
        .userName(String.valueOf(cfConfig.getUserName()))
        .password(String.valueOf(cfConfig.getPassword()))
        .endpointUrl(cfConfig.getEndpointUrl())
        .orgName(pluginCommandRequest.getTasInfraConfig().getOrganization())
        .spaceName(pluginCommandRequest.getTasInfraConfig().getSpace())
        .timeOutIntervalInMins(pluginCommandRequest.getTimeoutIntervalInMin())
        .useCFCLI(pluginCommandRequest.isUseCfCLI())
        .cfCliPath(cfCommandTaskHelperNG.getCfCliPathOnDelegate(
            pluginCommandRequest.isUseCfCLI(), pluginCommandRequest.getCfCliVersion()))
        .cfCliVersion(pluginCommandRequest.getCfCliVersion())
        .build();
  }

  private File createWorkingDirectory() throws IOException {
    // This value is set to CF_HOME env variable when process executor is created.
    String randomToken = UUIDGenerator.generateUuid();
    return generateWorkingDirectoryForScript(randomToken);
  }

  private File generateWorkingDirectoryForScript(String workingDirecotry) throws IOException {
    String workingDir = PCF_ARTIFACT_DOWNLOAD_DIR_PATH + "/" + workingDirecotry;
    FileIo.createDirectoryIfDoesNotExist(workingDir);
    return new File(workingDir);
  }

  private String dirCanonicalPath(File workingDirectory) throws IOException {
    String canonicalPath = workingDirectory.getCanonicalPath().trim();
    if (canonicalPath.endsWith("/")) {
      canonicalPath = canonicalPath.substring(0, canonicalPath.length() - 1);
    }
    return canonicalPath;
  }
  private void saveFilesInWorkingDirectoryStringContent(
      final List<FileData> fileDataList, final String workingDirectoryCanonicalPath) throws IOException {
    if (EmptyPredicate.isEmpty(fileDataList)) {
      return;
    }
    for (FileData file : fileDataList) {
      final Path filePath = Paths.get(workingDirectoryCanonicalPath, canonicalise(file.getFilePath()));
      FileIo.createDirectoryIfDoesNotExist(filePath.getParent());
      Files.deleteIfExists(filePath);
      final Path createdFile = Files.createFile(filePath);
      String fileContent = file.getFileContent();
      FileUtils.writeStringToFile(createdFile.toFile(), fileContent, UTF_8);
    }
  }

  private String canonicalise(String filePath) {
    String result = filePath.trim();
    if (result.isEmpty() || result.charAt(0) != '/') {
      result = "/" + result;
    }
    return result;
  }

  @VisibleForTesting
  String prepareFinalScript(String renderedScriptString, String workingDirCanonicalPathStr, String repoRoot) {
    // replace the path identifier with actual working directory path
    String finalScript =
        renderedScriptString.replaceAll(PcfConstants.FILE_START_REPO_ROOT_REGEX, workingDirCanonicalPathStr);
    final String dirPathWithRepoRoot = workingDirCanonicalPathStr + ("/".equals(repoRoot) ? "" : repoRoot);
    finalScript = finalScript.replaceAll(PcfConstants.FILE_START_SERVICE_MANIFEST_REGEX, dirPathWithRepoRoot);

    return finalScript;
  }
}
