package io.harness;

import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.eventsframework.impl.redis.RedisAbstractProducer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.manage.GlobalContextManager;

import com.google.common.collect.ImmutableMap;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageProducer implements Runnable {
  private final Producer client;
  private final String color;
  private final boolean isGitAware;
  private final int start;
  private final int end;

  public MessageProducer(Producer client, String color, boolean isGitAware, int start, int end) {
    this.start = start;
    this.end = end;
    this.color = color;
    this.client = client;
    this.isGitAware = isGitAware;
  }

  @SneakyThrows
  @Override
  public void run() {
    if (!isGitAware) {
      publishMessages();
    } else {
      publishMessagesToGitAwareProducer();
    }
  }

  private void publishMessages() throws InterruptedException {
    int count = start;
    while (true) {
      if (count > end)
        break;
      Message projectEvent;
      if (count % 3 == 0) {
        projectEvent =
            Message.newBuilder()
                .putAllMetadata(ImmutableMap.of("accountId", String.valueOf(count)))
                .setData(AccountEntityChangeDTO.newBuilder().setAccountId(String.valueOf(count)).build().toByteString())
                .build();
      } else {
        projectEvent =
            Message.newBuilder()
                .putAllMetadata(ImmutableMap.of("accountId", String.valueOf(count)))
                .setData(
                    ProjectEntityChangeDTO.newBuilder().setIdentifier(String.valueOf(count)).build().toByteString())
                .build();
      }

      String messageId = null;
      try {
        messageId = client.send(projectEvent);
        log.info("{}Pushed pid: {} in redis, received: {}{}", color, count, messageId, ColorConstants.TEXT_RESET);
      } catch (EventsFrameworkDownException e) {
        e.printStackTrace();
        log.error("{}Pushing message {} failed due to producer shutdown.{}", color, count, ColorConstants.TEXT_RESET);
        break;
      }

      count += 1;
      TimeUnit.MILLISECONDS.sleep(200);
    }
  }

  private void publishMessagesToGitAwareProducer() throws InterruptedException {
    // Sending an event in git aware redis producer
    int count = start;
    while (true) {
      String messageId = null;
      if (count > end)
        break;
      Message messageInGitAwareProducer =
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", String.valueOf(count)))
              .setData(ProjectEntityChangeDTO.newBuilder().setIdentifier(String.valueOf(count)).build().toByteString())
              .build();
      /*
       *  In some case the git context will be there in the thread and in some case it won't be.
       *  We are testing that the producer gets created in both the cases
       */
      try {
        if (count % 3 == 0) {
          final GitEntityInfo newBranch = GitEntityInfo.builder().branch("branch").yamlGitConfigId("repo").build();
          try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {
            GlobalContextManager.upsertGlobalContextRecord(
                GitSyncBranchContext.builder().gitBranchInfo(newBranch).build());
            messageId = client.send(messageInGitAwareProducer);
          }
        } else {
          messageId = client.send(messageInGitAwareProducer);
        }
        log.info("{}Pushed pid: {} in redis, received: {}{}", color, count, messageId, ColorConstants.TEXT_RESET);
      } catch (EventsFrameworkDownException e) {
        e.printStackTrace();
        log.error("{}Pushing message {} failed due to producer shutdown.{}", color, count, ColorConstants.TEXT_RESET);
        break;
      }

      count += 1;
      TimeUnit.MILLISECONDS.sleep(200);
    }
  }
}