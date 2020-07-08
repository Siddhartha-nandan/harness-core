package io.harness.service;

import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.DelegateServiceTest;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskBuilder;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.intfc.DelegateTaskService;
import io.harness.threading.Morpheus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DelegateTaskServiceTest extends DelegateServiceTest {
  @Inject HPersistence persistence;
  @Inject DelegateTaskService delegateTaskService;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testTouchExecutingTasksWithEmpty() {
    delegateTaskService.touchExecutingTasks(null, null, null);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testTouchExecutingTasks() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    DelegateTaskBuilder delegateTaskBuilder = DelegateTask.builder()
                                                  .accountId(accountId)
                                                  .delegateId(delegateId)
                                                  .status(STARTED)
                                                  .data(TaskData.builder().timeout(1000L).build())
                                                  .expiry(currentTimeMillis() + 1000L);
    DelegateTask delegateTask1 = delegateTaskBuilder.uuid(generateUuid()).build();
    persistence.save(delegateTask1);
    DelegateTask delegateTask2 = delegateTaskBuilder.uuid(generateUuid()).status(QUEUED).build();
    persistence.save(delegateTask2);

    Morpheus.sleep(ofMillis(1));

    delegateTaskService.touchExecutingTasks(
        accountId, delegateId, asList(delegateTask1.getUuid(), delegateTask2.getUuid()));

    DelegateTask updatedDelegateTask1 = persistence.get(DelegateTask.class, delegateTask1.getUuid());
    assertThat(updatedDelegateTask1.getExpiry()).isGreaterThan(delegateTask1.getExpiry());

    DelegateTask updatedDelegateTask2 = persistence.get(DelegateTask.class, delegateTask2.getUuid());
    assertThat(updatedDelegateTask2.getExpiry()).isEqualTo(delegateTask2.getExpiry());
  }
}
