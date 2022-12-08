package io.harness.queueservice;

import static java.util.stream.Collectors.toList;

import io.harness.beans.DelegateTask;
import io.harness.hsqs.client.HsqsServiceClient;
import io.harness.hsqs.client.model.DequeueRequest;
import io.harness.hsqs.client.model.DequeueResponse;
import io.harness.hsqs.client.model.EnqueueRequest;
import io.harness.queueservice.config.DelegateQueueServiceConfig;
import io.harness.queueservice.infc.DelegateServiceQueue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelegateTaskQueueService implements DelegateServiceQueue<DelegateTask> {
  @Inject private HsqsServiceClient hsqsServiceClient;
  @Inject private DelegateQueueServiceConfig delegateQueueServiceConfig;
  private ObjectMapper objectMapper;

  public DelegateTaskQueueService(HsqsServiceClient hsqsServiceClient,
      DelegateQueueServiceConfig delegateQueueServiceConfig, ObjectMapper objectMapper) {
    this.hsqsServiceClient = hsqsServiceClient;
    this.delegateQueueServiceConfig = delegateQueueServiceConfig;
    this.objectMapper = objectMapper;
  }

  @Override
  public void enqueue(DelegateTask delegateTask) {
    String topic = delegateQueueServiceConfig.getTopic();
    try {
      String task =
          java.util.Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(delegateTask).getBytes());
      EnqueueRequest enqueueRequest = EnqueueRequest.builder()
                                          .topic(topic)
                                          .payload(task)
                                          .subTopic(delegateTask.getAccountId())
                                          .producerName(topic)
                                          .build();
      hsqsServiceClient.enqueue(enqueueRequest, "sampleToken");
    } catch (Exception e) {
      log.error("Error while enqueue delegate task ", e);
    }
  }

  @Override
  public <T> Object dequeue() throws IOException {
    DequeueRequest dequeueRequest = DequeueRequest.builder()
                                        .batchSize(100)
                                        .consumerName(delegateQueueServiceConfig.getTopic())
                                        .topic(delegateQueueServiceConfig.getTopic())
                                        .build();
    List<DequeueResponse> dequeueResponses = hsqsServiceClient.dequeue(dequeueRequest, "sampleToken").execute().body();
    List<DelegateTaskDequeue> delegateTasksDequeueList =
        dequeueResponses.stream()
            .map(dequeueResponse
                -> DelegateTaskDequeue.builder()
                       .payload(dequeueResponse.getPayload())
                       .itemId(dequeueResponse.getItemId())
                       .delegateTask(convertToDelegateTask(dequeueResponse.getPayload(), objectMapper))
                       .build())
            .filter(this::isResourceAvailableToAssignTask)
            .collect(toList());
   delegateTasksDequeueList.forEach(this::AcknowledgeAndProcessDelegateTask);
    return true;
  }

  private boolean isResourceAvailableToAssignTask(DelegateTaskDequeue delegateTaskDequeue) {
    return true;
  }

  private void AcknowledgeAndProcessDelegateTask(DelegateTaskDequeue delegateTaskDequeue) {

  }

  public DelegateTask convertToDelegateTask(String payload, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(Base64.getDecoder().decode(payload), DelegateTask.class);
    } catch (Exception e) {
      return null;
    }
  }
}
