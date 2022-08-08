/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.manifests;

import static io.harness.delegate.beans.DelegateFile.Builder.aDelegateFile;
import static io.harness.delegate.task.helm.CustomManifestFetchTaskHelper.cleanup;
import static io.harness.delegate.task.helm.CustomManifestFetchTaskHelper.zipManifestDirectory;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.helm.CustomManifestFetchTaskHelper;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.manifests.request.CustomManifestValuesFetchParams;
import io.harness.delegate.task.manifests.response.CustomManifestValuesFetchResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.manifest.CustomManifestService;
import io.harness.manifest.CustomManifestSource;

import software.wings.exception.ShellScriptException;

import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class CustomManifestFetchTask extends AbstractDelegateRunnableTask {
  private static final String ZIPPED_CUSTOM_MANIFEST_FILE_NAME = "zippedCustomManifestFiles";
  @Inject private CustomManifestService customManifestService;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private DelegateFileManagerBase delegateFileManagerBase;
  @Inject private CustomManifestFetchTaskHelper customManifestFetchTaskHelper;

  public CustomManifestFetchTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    CustomManifestValuesFetchParams fetchParams = (CustomManifestValuesFetchParams) parameters;
    LogCallback logCallback = getLogStreamingTaskClient().obtainLogCallback(fetchParams.getCommandUnitName());

    String defaultSourceWorkingDirectory = null;
    DelegateFile delegateFile = null;
    CustomManifestValuesFetchResponse valuesFetchResponse = null;

    CustomManifestSource customManifestSource = fetchParams.getCustomManifestSource();
    if (customManifestSource != null && customManifestSource.getScript() == null) {
      throw new InvalidRequestException("Script can not be null for custom manifest source", USER);
    }

    if (customManifestSource != null) {
      try {
        defaultSourceWorkingDirectory = customManifestService.executeCustomSourceScript(
            fetchParams.getActivityId(), logCallback, customManifestSource, true);

        logCallback.saveExecutionLog(color("Successfully fetched following files:", White, Bold));
        logCallback.saveExecutionLog(k8sTaskHelperBase.getManifestFileNamesInLogFormat(defaultSourceWorkingDirectory));

        if (!isManifestsFilesSizeAllowed(logCallback, defaultSourceWorkingDirectory)) {
          return CustomManifestValuesFetchResponse.builder()
              .errorMessage("Custom Manifest File size exceeds allowed max size of 25Mb")
              .commandExecutionStatus(FAILURE)
              .build();
        }

      } catch (ShellScriptException e) {
        cleanup(defaultSourceWorkingDirectory);
        log.error("Failed to execute shell script", e);
        logCallback.saveExecutionLog(
            "Failed to execute custom manifest script. " + getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
        return CustomManifestValuesFetchResponse.builder()
            .commandExecutionStatus(FAILURE)
            .errorMessage(getMessage(e))
            .build();
      } catch (Exception e) {
        cleanup(defaultSourceWorkingDirectory);
        log.error("Failed to process custom manifest", e);
        logCallback.saveExecutionLog("Custom source script execution task failed. " + getMessage(e), ERROR, FAILURE);
        return CustomManifestValuesFetchResponse.builder()
            .commandExecutionStatus(FAILURE)
            .errorMessage(getMessage(e))
            .build();
      }

      try {
        delegateFile = zipAndUploadManifestFiles(fetchParams, logCallback, defaultSourceWorkingDirectory);
      } catch (IOException e) {
        cleanup(defaultSourceWorkingDirectory);
        log.error("Failed to get files from manifest directory", e);
        logCallback.saveExecutionLog(
            "Failed to get manifest files from custom source. " + getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
        return CustomManifestValuesFetchResponse.builder()
            .commandExecutionStatus(FAILURE)
            .errorMessage(getMessage(e))
            .build();
      } catch (Exception e) {
        cleanup(defaultSourceWorkingDirectory);
        logCallback.saveExecutionLog("Failed to process custom manifest files." + getMessage(e), ERROR, FAILURE);
        return CustomManifestValuesFetchResponse.builder()
            .commandExecutionStatus(FAILURE)
            .errorMessage(getMessage(e))
            .build();
      }
    }

    try {
      valuesFetchResponse =
          customManifestFetchTaskHelper.fetchValuesTask(fetchParams, logCallback, defaultSourceWorkingDirectory, true);
      if (valuesFetchResponse.getCommandExecutionStatus() == FAILURE) {
        return valuesFetchResponse;
      }
    } catch (Exception e) {
      log.error("Fetch values from custom manifest failed", e);
      logCallback.saveExecutionLog("Unknown error while trying to fetch values from custom manifest. " + e.getMessage(),
          LogLevel.ERROR, FAILURE);
      return CustomManifestValuesFetchResponse.builder()
          .commandExecutionStatus(FAILURE)
          .errorMessage(getMessage(e))
          .build();
    } finally {
      cleanup(defaultSourceWorkingDirectory);
    }

    logCallback.saveExecutionLog("Done.", LogLevel.INFO, SUCCESS);
    return CustomManifestValuesFetchResponse.builder()
        .commandExecutionStatus(SUCCESS)
        .valuesFilesContentMap(valuesFetchResponse.getValuesFilesContentMap())
        .zippedManifestFileId(delegateFile == null ? null : delegateFile.getFileId())
        .build();
  }

  private boolean isManifestsFilesSizeAllowed(LogCallback logCallback, String defaultSourceWorkingDirectory) {
    long sizeOfManifestDirectory = FileUtils.sizeOfDirectory(new File(defaultSourceWorkingDirectory));
    // added 25Mb cap on manifest files
    if (sizeOfManifestDirectory / (1024 * 1024) > 25) {
      logCallback.saveExecutionLog(
          format("Custom Manifest File size: %s, exceeds cap 25Mb", sizeOfManifestDirectory / (1024 * 1024) > 25),
          ERROR, FAILURE);
      return false;
    }
    return true;
  }

  @NotNull
  private DelegateFile zipAndUploadManifestFiles(CustomManifestValuesFetchParams fetchParams, LogCallback logCallback,
      String manifestFilesDirectory) throws IOException {
    CustomManifestSource customManifestSource = fetchParams.getCustomManifestSource();
    final String destZippedManifestDirectory = customManifestService.getWorkingDirectory();
    final Path destZippedManifestFile = Paths.get(destZippedManifestDirectory, "destZipManifestFile.zip");
    final Path pathToManifestFiles =
        Paths.get(manifestFilesDirectory, customManifestSource.getFilePaths().get(0)).normalize();

    zipManifestFiles(pathToManifestFiles.toString(), destZippedManifestFile.toString());

    final DelegateFile delegateFile = aDelegateFile()
                                          .withAccountId(fetchParams.getAccountId())
                                          .withDelegateId(getDelegateId())
                                          .withTaskId(getTaskId())
                                          .withEntityId(fetchParams.getActivityId())
                                          .withBucket(FileBucket.CUSTOM_MANIFEST)
                                          .withFileName(ZIPPED_CUSTOM_MANIFEST_FILE_NAME + fetchParams.getActivityId())
                                          .build();

    File zippedManifestFile = new File(destZippedManifestFile.toString());
    delegateFileManagerBase.uploadAsFile(delegateFile, zippedManifestFile);

    cleanup(destZippedManifestDirectory);
    return delegateFile;
  }

  private void zipManifestFiles(String manifestFilesDirectory, String destZippedManifestFile) throws IOException {
    zipManifestDirectory(manifestFilesDirectory, destZippedManifestFile);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
