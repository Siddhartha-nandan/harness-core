/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.shell;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_EXECUTION_ID;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.shell.ssh.SshClientManager;
import io.harness.shell.ssh.connection.ExecRequest;
import io.harness.shell.ssh.connection.ExecResponse;
import io.harness.shell.ssh.sftp.SftpRequest;
import io.harness.shell.ssh.sftp.SftpResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Created by anubhaw on 2/10/16.
 */

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_AMI_ASG})
@ValidateOnExecution
@Slf4j
public class ScriptSshExecutor extends AbstractScriptExecutor {
  public static final String CHANNEL_IS_NOT_OPENED = "channel is not opened.";

  protected SshSessionConfig config;

  /**
   * Instantiates a new abstract ssh executor.
   * @param logCallback          the log service
   */
  @Inject
  public ScriptSshExecutor(LogCallback logCallback, boolean shouldSaveExecutionLogs, ScriptExecutionContext config) {
    super(logCallback, shouldSaveExecutionLogs);
    if (isEmpty(((SshSessionConfig) config).getExecutionId())) {
      throw new WingsException(INVALID_EXECUTION_ID);
    }
    this.config = (SshSessionConfig) config;
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command, StringBuffer output, boolean displayCommand) {
    try {
      ExecResponse response = SshClientManager.exec(
          ExecRequest.builder().command(command).displayCommand(displayCommand).build(), config, logCallback);
      if (output != null && isNotEmpty(response.getOutput())) {
        output.append(response.getOutput());
      }
      if (response.getStatus() == SUCCESS) {
        saveExecutionLog("Command finished with status " + SUCCESS);
      }
      return response.getStatus();
    } catch (Exception ex) {
      log.error("Failed to exec due to: ", ex);
      throw ex;
    }
  }

  @Override
  public ExecuteCommandResponse executeCommandString(String command, List<String> envVariablesToCollect) {
    return executeCommandString(command, envVariablesToCollect, Collections.emptyList(), null);
  }

  @Override
  public ExecuteCommandResponse executeCommandString(String command, List<String> envVariablesToCollect,
      List<String> secretEnvVariablesToCollect, Long timeoutInMillis) {
    secretEnvVariablesToCollect =
        secretEnvVariablesToCollect == null ? Collections.emptyList() : secretEnvVariablesToCollect;
    try {
      return executeCommandStringWithSshClient(command, envVariablesToCollect, secretEnvVariablesToCollect);
    } catch (Exception ex) {
      log.error("Failed to execute command: ", ex);
      throw ex;
    } finally {
      logCallback.dispatchLogs();
    }
  }

  public ExecuteCommandResponse executeCommandString(String command, List<String> envVariablesToCollect,
      List<String> secretEnvVariablesToCollect, Long timeoutInMillis, String directoryPath) {
    secretEnvVariablesToCollect =
        secretEnvVariablesToCollect == null ? Collections.emptyList() : secretEnvVariablesToCollect;
    try {
      return executeCommandStringWithSshClient(
          command, envVariablesToCollect, secretEnvVariablesToCollect, directoryPath);
    } catch (Exception ex) {
      log.error("Failed to execute command: ", ex);
      throw ex;
    } finally {
      logCallback.dispatchLogs();
    }
  }

  private ExecuteCommandResponse executeCommandStringWithSshClient(
      String command, List<String> envVariablesToCollect, List<String> secretEnvVariablesToCollect) {
    return executeCommandStringWithSshClient(command, envVariablesToCollect, secretEnvVariablesToCollect, null);
  }

  private ExecuteCommandResponse executeCommandStringWithSshClient(String command, List<String> envVariablesToCollect,
      List<String> secretEnvVariablesToCollect, String directoryPath) {
    command = setupBashEnvironment(command, config, envVariablesToCollect, secretEnvVariablesToCollect, directoryPath);
    ExecResponse response = SshClientManager.exec(
        ExecRequest.builder().command(command).displayCommand(false).build(), config, logCallback);
    Map<String, String> envVariablesMap = new HashMap<>();
    ExecuteCommandResponse result =
        ExecuteCommandResponse.builder()
            .status(response.getStatus())
            .commandExecutionData(ShellExecutionData.builder().sweepingOutputEnvVariables(envVariablesMap).build())
            .build();
    if (response.getStatus() == SUCCESS
        && isNotEmpty(getVariables(envVariablesToCollect, secretEnvVariablesToCollect))) {
      SftpResponse sftpResponse =
          SshClientManager.sftpDownload(SftpRequest.builder()
                                            .fileName(getEnvVariablesFilename(config))
                                            .directory(resolveEnvVarsInPath(config.getWorkingDirectory() + "/"))
                                            .cleanup(true)
                                            .build(),
              config, logCallback);
      String content = sftpResponse.getContent();
      BufferedReader reader = null;
      try {
        Reader inputString = new StringReader(content);
        reader = new BufferedReader(inputString);
        processScriptOutputFile(envVariablesMap, reader, secretEnvVariablesToCollect);
      } catch (IOException ex) {
        log.error("Failed to generate output for variables", ex);
      } finally {
        try {
          reader.close();
        } catch (IOException ex2) {
          log.error("Failed to close reader", ex2);
        }
      }

      validateExportedVariables(envVariablesMap);
    }
    saveExecutionLog("Command finished with status " + response.getStatus());
    return result;
  }

  private String setupBashEnvironment(String command, SshSessionConfig sshSessionConfig,
      List<String> envVariablesToCollect, List<String> secretEnvVariablesToCollect, String directoryPath) {
    if (EmptyPredicate.isEmpty(directoryPath)) {
      directoryPath = resolveEnvVarsInPath(sshSessionConfig.getWorkingDirectory() + "/");
    }

    if (isNotEmpty(sshSessionConfig.getEnvVariables())) {
      String exportCommand = buildExportForEnvironmentVariables(sshSessionConfig.getEnvVariables());
      command = exportCommand + "\n" + command;
    }

    String envVariablesFilename = null;
    command = "cd \"" + directoryPath + "\"\n" + command;

    // combine both variable types
    List<String> allVariablesToCollect = getVariables(envVariablesToCollect, secretEnvVariablesToCollect);

    if (!allVariablesToCollect.isEmpty()) {
      envVariablesFilename = getEnvVariablesFilename(sshSessionConfig);
      command = addEnvVariablesCollector(
          command, allVariablesToCollect, "\"" + directoryPath + envVariablesFilename + "\"", ScriptType.BASH);
    }

    return command;
  }

  @NotNull
  private static String getEnvVariablesFilename(SshSessionConfig sshSessionConfig) {
    return "harness-" + sshSessionConfig.getExecutionId() + ".out";
  }

  @NotNull
  private static List<String> getVariables(
      List<String> envVariablesToCollect, List<String> secretEnvVariablesToCollect) {
    if (null == envVariablesToCollect) {
      envVariablesToCollect = new ArrayList<>();
    }
    if (null == secretEnvVariablesToCollect) {
      secretEnvVariablesToCollect = new ArrayList<>();
    }
    return Stream.concat(envVariablesToCollect.stream(), secretEnvVariablesToCollect.stream())
        .filter(EmptyPredicate::isNotEmpty)
        .collect(Collectors.toList());
  }

  protected String buildExportForEnvironmentVariables(Map<String, String> envVariables) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : envVariables.entrySet()) {
      sb.append(String.format("export %s=\"%s\"\n", entry.getKey(), entry.getValue()));
    }
    return sb.toString();
  }

  @Override
  public String getAccountId() {
    return config.getAccountId();
  }

  @Override
  public String getCommandUnitName() {
    return config.getCommandUnitName();
  }

  @Override
  public String getExecutionId() {
    return config.getExecutionId();
  }

  @Override
  public String getHost() {
    return config.getHost();
  }

  @VisibleForTesting
  public String resolveEnvVarsInPath(String directoryPath) {
    String regex = "(\\$[A-Za-z_-])\\w+";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(directoryPath);
    List<String> envVars = new ArrayList<>();
    while (matcher.find()) {
      envVars.add(matcher.group());
    }
    for (String envVar : envVars) {
      int index = directoryPath.indexOf(envVar);
      if (index > 0 && directoryPath.charAt(index - 1) == '/') {
        directoryPath = directoryPath.replace("/" + envVar, getEnvVarValue(envVar.substring(1)));
      } else {
        directoryPath = directoryPath.replace(envVar, getEnvVarValue(envVar.substring(1)));
      }
    }
    return directoryPath;
  }

  private String getEnvVarValue(String envVar) {
    return System.getenv(envVar);
  }
}
