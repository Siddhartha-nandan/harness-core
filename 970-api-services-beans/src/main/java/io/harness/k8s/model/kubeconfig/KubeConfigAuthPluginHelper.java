/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.model.kubeconfig;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@OwnedBy(HarnessTeam.CDP)
public class KubeConfigAuthPluginHelper {
  private static final int TIMEOUT_IN_MINUTES = 1;

  public static boolean isExecAuthPluginBinaryAvailable(String binaryName, LogCallback logCallback) {
    boolean shouldUseExecFormat = runCommand(binaryName + " --version", logCallback);
    if (!shouldUseExecFormat && logCallback != null) {
      logCallback.saveExecutionLog(
          "Auth Plugin is removed for kubernetes>=1.26. Please install %s on the delegate and add the binary in your PATH",
          LogLevel.WARN);
    }
    return shouldUseExecFormat;
  }

  private static boolean runCommand(final String command, LogCallback logCallback) {
    try {
      return executeShellCommand(command);
    } catch (Exception e) {
      if (logCallback != null) {
        logCallback.saveExecutionLog(String.format("Failed executing command: %s %n %s", command,
                                         ExceptionMessageSanitizer.sanitizeException(e)),
            LogLevel.WARN);
      }
      return false;
    }
  }

  private static boolean executeShellCommand(String command)
      throws IOException, InterruptedException, TimeoutException {
    final ProcessExecutor processExecutor = new ProcessExecutor()
                                                .timeout(TIMEOUT_IN_MINUTES, TimeUnit.MINUTES)
                                                .directory(null)
                                                .command("/bin/bash", "-c", command)
                                                .readOutput(true);

    final ProcessResult result = processExecutor.execute();
    if (result.getExitValue() != 0) {
      return false;
    }
    return true;
  }
}
