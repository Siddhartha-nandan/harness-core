package io.harness.testframework.framework;

import static io.harness.delegate.beans.Delegate.DelegateKeys;
import static io.harness.testframework.framework.utils.ExecutorUtils.addConfig;
import static io.harness.testframework.framework.utils.ExecutorUtils.addGCVMOptions;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJacocoAgentVM;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJar;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.filesystem.FileIo;
import io.harness.resource.Project;
import io.harness.rest.RestResponse;
import io.harness.threading.Poller;

import software.wings.beans.Account;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateStatus.DelegateInner;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.utils.Strings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

@Singleton
@Slf4j
public class DelegateExecutor {
  private static boolean failedAlready;
  private static AtomicBoolean startedAlready = new AtomicBoolean();
  private static Duration waiting = ofMinutes(5);

  @Inject private DelegateService delegateService;

  public void ensureDelegate(Account account, String bearerToken, Class clazz)
      throws IOException, InterruptedException {
    long t = System.currentTimeMillis();
    while (!isHealthy(account.getUuid(), bearerToken) && System.currentTimeMillis() - t < 300000) {
      Thread.sleep(2000);
    }
    //      executeLocalDelegate(account, bearerToken, clazz)
  }

  private void executeLocalDelegate(Account account, String bearerToken, Class clazz) throws IOException {
    if (failedAlready) {
      return;
    }

    String directoryPath = Project.rootDirectory(clazz);
    final File directory = new File(directoryPath);
    final File lockfile = new File(directoryPath, "delegate");

    if (FileIo.acquireLock(lockfile, waiting.plusSeconds(10))) {
      try {
        if (isHealthy(account.getUuid(), bearerToken)) {
          log.info("Delegate is healthy. New one will not be started.");
          return;
        }

        if (startedAlready.get()) {
          log.info("Delegate process is started already. New one will not be started.");
          return;
        }

        String home = System.getProperty("user.home");
        if (home.contains("root")) {
          home = "/home/jenkins";
        }

        final Path jar = Paths.get(home + "/.bazel-dirs/bin/260-delegate/module_deploy.jar");
        log.info("The delegate path is: {}", jar.toString());

        final Path config = Paths.get(directory.getPath(), "260-delegate", "config-delegate.yml");

        List<String> command = new ArrayList<>();
        command.add("java");

        addGCVMOptions(command);
        addJacocoAgentVM(jar, command);

        addJar(jar, command);
        addConfig(config, command);

        log.info(Strings.join(command, " "));

        ProcessExecutor processExecutor = new ProcessExecutor();
        processExecutor.directory(directory);
        processExecutor.command(command);

        processExecutor.redirectOutput(System.out);
        processExecutor.redirectError(System.err);
        //        processExecutor.redirectOutput(null);
        //        processExecutor.redirectError(null);

        log.info("Starting the delegate from {}", directory);
        StartedProcess startedProcess = processExecutor.start();

        boolean flagUpdatedSuccessfuly = startedAlready.compareAndSet(false, true);
        if (!flagUpdatedSuccessfuly) {
          log.warn("Process startup control flag was not set to false as expected.");
        }

        Poller.pollFor(waiting, ofSeconds(2),
            () -> startedProcess.getFuture().isDone() || isHealthy(account.getUuid(), bearerToken));

        if (!isHealthy(account.getUuid(), bearerToken)) {
          throw new RuntimeException("Delegate process finished without becoming healthy");
        }

      } catch (IOException ex) {
        startedAlready.set(false);
        log.error("Failed to start delegate process.", ex);
      } catch (RuntimeException exception) {
        failedAlready = true;
        throw exception;
      } finally {
        FileIo.releaseLock(lockfile);
      }
    }
  }

  private boolean isHealthy(String accountId, String bearerToken) {
    try {
      RestResponse<DelegateStatus> delegateStatusResponse =
          Setup.portal()
              .auth()
              .oauth2(bearerToken)
              .queryParam(DelegateKeys.accountId, accountId)
              .get("/setup/delegates/status")
              .as(new GenericType<RestResponse<DelegateStatus>>() {}.getType());

      DelegateStatus delegateStatus = delegateStatusResponse.getResource();
      if (!delegateStatus.getDelegates().isEmpty()) {
        for (DelegateInner delegateInner : delegateStatus.getDelegates()) {
          long lastMinuteMillis = System.currentTimeMillis() - 60000;
          if (delegateInner.getStatus() == DelegateInstanceStatus.ENABLED
              && delegateInner.getLastHeartBeat() > lastMinuteMillis) {
            log.info("Delegate with IP {} is healthy.", delegateInner.getIp());
            return true;
          }
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }
}
