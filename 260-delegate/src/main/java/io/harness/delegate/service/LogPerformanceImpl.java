/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

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

  /* public double getContainerCpuUsage() {
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
       double cpu = -1.0;
       try (BufferedReader cpuReader = new BufferedReader(new InputStreamReader(cpuProcess.getInputStream()))) {
         String line;
         log.info("Top CPU processes in the system :");
         while ((line = cpuReader.readLine()) != null) {
           log.info(line);
           totalLinesToRead++;
           if (line.contains("%Cpu(s):")) {
             String[] cpuUsage= line.trim().split("\\s+");
             //cpuUsage = Double.parseDouble(tokens[8].replace(",", ""));
             //log.info("CPU Usage is {}", cpuUsage);
             log.info("CPU Usage: " + (100 - Double.parseDouble(cpuUsage[0])) + "%");
             break;
             break;
           }

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
       return cpu;
     } catch (IOException e) {
       log.error(e.toString());
     } catch (InterruptedException e) {
       log.error(e.toString());
     } catch (Exception e) {
       log.error(e.toString());
     }
     return -1.0;
   }*/

  public void getCpuUsage() {
    try {
      int totalLinesToRead = 0;

      String termEnv = System.getenv(TERM_ENV_VARIABLE);
      if (StringUtils.isEmpty(termEnv)) {
        termEnv = DEFAULT_TERM_ENV_VALUE;
      }
      String processId = getDelegateProcessId();
      ProcessBuilder cpuProcessBuilder =
          new ProcessBuilder("top", "-b", "-n", "1", "-o", "%CPU", "-p", getDelegateProcessId());
      cpuProcessBuilder.environment().put(TERM_ENV_VARIABLE, termEnv);
      Process cpuProcess = cpuProcessBuilder.start();
      Map<String, String> processInfoMap = new HashMap<>();
      List<String> label = new ArrayList<>();
      List<String> value = new ArrayList<>();
      try (BufferedReader cpuReader = new BufferedReader(new InputStreamReader(cpuProcess.getInputStream()))) {
        String line;
        while ((line = cpuReader.readLine()) != null) {
          log.info("From getCpuUsage: {}", line);
          line.trim();
          String[] processInfo = line.trim().split("\\s+");
          if (processInfo[0].equals("PID")) {
            label.addAll(Arrays.stream(processInfo).collect(Collectors.toList()));
          }
          if (processInfo[0].equals(processId)) {
            value.addAll(Arrays.stream(processInfo).collect(Collectors.toList()));
          }
          log.info("Process Info label: {}", label);
          log.info("Process Info value: {}", value);
        }
      }

    } catch (IOException e) {
      log.error(e.toString());
    }
  }

  public double getContainerCpuUsage() {
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
      String processId = "11539";

      try (BufferedReader cpuReader = new BufferedReader(new InputStreamReader(cpuProcess.getInputStream()))) {
        /* String line = "11539 root      20   0   14.9g 982376  22740 S   6.7   0.5  23:42.27";


         if (line.contains(processId)) {
           String[] tokens = line.trim().split("\\s+");
           if (tokens.length >= 10) {
             String cpuUsage = tokens[8];
             String memoryUsage = tokens[9];
             log.info("CPU Usage for process " + processId + ": " + cpuUsage + "%");
             // log.info("CPU Usage: " + (100 - Double.parseDouble(cpuUsage[0])) + "%");
           }
         }*/
        String line;
        while ((line = cpuReader.readLine()) != null) {
          log.info(line);

          if (line.contains(getDelegateProcessId())) {
            String[] tokens = line.trim().split("\\s+");
            if (tokens.length >= 10) {
              String cpuUsage = tokens[8];
              String memoryUsage = tokens[9];
              log.info("CPU Usage for process " + processId + ": " + cpuUsage + "%");
              log.info("Memory Usage: " + (100 - Double.parseDouble(memoryUsage)) + "%");
              break;
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

      // Ensure that the cpuProcess is terminated
      int exitCode = cpuProcess.waitFor();
      log.info("The process to dump Top processes exited with code {}", exitCode);
      return 0;
    } catch (IOException e) {
      log.error(e.toString());
    } catch (InterruptedException e) {
      log.error(e.toString());
    } catch (Exception e) {
      log.error(e.toString());
    }
    return -1.0;
  }
}
