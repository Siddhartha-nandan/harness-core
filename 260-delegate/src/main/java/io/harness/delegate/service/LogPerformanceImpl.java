/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.MemoryPerformanceUtils.memoryUsage;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.sun.management.OperatingSystemMXBean;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Slf4j
public class LogPerformanceImpl {
  private static final String TERM_ENV_VARIABLE = "TERM";
  private static final String DEFAULT_TERM_ENV_VALUE = "xterm";
  private static final int NOS_OF_TOP_PROCESS_LINES_TO_READ = 15;

  public Map<String, String> obtainDelegateCpuMemoryPerformance(ImmutableMap.Builder<String, String> builder) {
    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    builder.put("cpu-process",
        BigDecimal.valueOf(osBean.getProcessCpuLoad() * 100).setScale(2, BigDecimal.ROUND_HALF_UP).toString());
    builder.put("cpu-system",
        BigDecimal.valueOf(osBean.getSystemCpuLoad() * 100).setScale(2, BigDecimal.ROUND_HALF_UP).toString());

    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    memoryUsage(builder, "heap-", memoryMXBean.getHeapMemoryUsage());

    memoryUsage(builder, "non-heap-", memoryMXBean.getNonHeapMemoryUsage());

    return builder.build();
  }

  public void logTopCpuMemoryProcesses() {
    // Get the operating system management bean
    java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    // Ensure the operating system supports CPU usage monitoring
    if (osBean.getSystemLoadAverage() == -1) {
      log.info("Operating system does not support CPU usage monitoring.");
      return;
    }

    logTopProcessesByCpuAndMemory();
    log.info("The delegate process ID {}", getCurrentProcessId());
  }

  private static long getCurrentProcessId() {
    RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    String processName = runtimeBean.getName();
    return Long.parseLong(processName.split("@")[0]);
  }

  private static String getDelegateProcessId() {
    RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    String processName = runtimeBean.getName();
    return processName.split("@")[0];
  }

  private void logTopProcessesByCpuAndMemory() {
    try {
      int totalLinesToRead = 0;

      String termEnv = System.getenv(TERM_ENV_VARIABLE);
      if (StringUtils.isEmpty(termEnv)) {
        termEnv = DEFAULT_TERM_ENV_VALUE;
      }

      // ProcessBuilder is used to spawn a child process to run the given command
      // ProcessBuild allows the process to be killed through manually
      ProcessBuilder cpuProcessBuilder = new ProcessBuilder("top", "-b", "-n", "1");
      cpuProcessBuilder.environment().put(TERM_ENV_VARIABLE, termEnv);

      Process cpuProcess = cpuProcessBuilder.start();

      try (BufferedReader cpuReader = new BufferedReader(new InputStreamReader(cpuProcess.getInputStream()))) {
        String cpuLine;
        log.info("Top processes in the system :");
        while ((cpuLine = cpuReader.readLine()) != null) {
          log.info(cpuLine);
          totalLinesToRead++;
          if (totalLinesToRead >= NOS_OF_TOP_PROCESS_LINES_TO_READ) {
            // Close the input stream and kill the process.
            cpuProcess.getInputStream().close();
            cpuProcess.destroy();
            break;
          }
        }
      }

      // Ensure that the cpuProcess is terminated
      int exitCode = cpuProcess.waitFor();
      log.info("The process to dump Top processes exited with code {}", exitCode);

    } catch (IOException e) {
      log.error(e.toString());
    } catch (InterruptedException e) {
      log.error(e.toString());
    } catch (Exception e) {
      log.error(e.toString());
    }
  }

  public Map<String, Double> getDelegateCpuAndMemoryUsage() {
    Map<String, Double> resourceUsuageMap = new HashMap<>();
    try {
      int totalLinesToRead = 0;
      String termEnv = System.getenv(TERM_ENV_VARIABLE);
      if (StringUtils.isEmpty(termEnv)) {
        termEnv = DEFAULT_TERM_ENV_VALUE;
      }
      String processId = getDelegateProcessId();
      ProcessBuilder cpuProcessBuilder = new ProcessBuilder("top", "-b", "-n", "1", "-o", "-p", getDelegateProcessId());
      cpuProcessBuilder.environment().put(TERM_ENV_VARIABLE, termEnv);
      Process cpuProcess = cpuProcessBuilder.start();

      List<String> labels = new ArrayList<>();
      List<String> values = new ArrayList<>();
      try (BufferedReader cpuReader = new BufferedReader(new InputStreamReader(cpuProcess.getInputStream()))) {
        String line;
        while ((line = cpuReader.readLine()) != null) {
          line.trim();
          String[] processInfo = line.trim().split("\\s+");
          if (isNotEmpty(processInfo[0]) && processInfo[0].equals("PID")) {
            labels.addAll(Arrays.stream(processInfo).collect(Collectors.toList()));
          }
          if (isNotEmpty(processInfo[0]) && processInfo[0].equals(processId)) {
            values.addAll(Arrays.stream(processInfo).collect(Collectors.toList()));
          }
          // Look for specific keys(CPU and MEM) in the keys list and their corresponding values
          if (isNotEmpty(labels) && isNotEmpty(values)) {
            for (int i = 0; i < labels.size(); i++) {
              String currentKey = labels.get(i);
              if (currentKey.equals("%CPU") || currentKey.equals("%MEM")) {
                if (isNotEmpty(values.get(i))) {
                  resourceUsuageMap.put(currentKey, Double.parseDouble(values.get(i)));
                }
              }
            }
          }
          totalLinesToRead++;
          if (totalLinesToRead >= NOS_OF_TOP_PROCESS_LINES_TO_READ) {
            // Close the input stream and kill the process.
            cpuProcess.getInputStream().close();
            cpuProcess.destroy();
            break;
          }
        }
      }

    } catch (IOException e) {
      log.error(e.toString());
    }
    return resourceUsuageMap;
  }
}
