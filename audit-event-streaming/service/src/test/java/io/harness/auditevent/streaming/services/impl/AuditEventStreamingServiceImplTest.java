/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.auditevent.streaming.services.impl;

import static io.harness.audit.entities.AuditEvent.AuditEventKeys.ACCOUNT_IDENTIFIER_KEY;
import static io.harness.audit.entities.AuditEvent.AuditEventKeys.createdAt;
import static io.harness.auditevent.streaming.AuditEventStreamingConstants.ACCOUNT_IDENTIFIER_PARAMETER_KEY;
import static io.harness.auditevent.streaming.AuditEventStreamingConstants.AWS_S3_STREAMING_PUBLISHER;
import static io.harness.auditevent.streaming.AuditEventStreamingConstants.JOB_START_TIME_PARAMETER_KEY;
import static io.harness.auditevent.streaming.entities.BatchStatus.IN_PROGRESS;
import static io.harness.auditevent.streaming.entities.BatchStatus.READY;
import static io.harness.auditevent.streaming.entities.BatchStatus.SUCCESS;
import static io.harness.rule.OwnerRule.NISHANT;
import static io.harness.spec.server.audit.v1.model.StreamingDestinationSpecDTO.TypeEnum.AWS_S3;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.audit.entities.AuditEvent;
import io.harness.audit.entities.streaming.AwsS3StreamingDestination;
import io.harness.audit.entities.streaming.StreamingDestination;
import io.harness.auditevent.streaming.AuditEventRepository;
import io.harness.auditevent.streaming.entities.BatchStatus;
import io.harness.auditevent.streaming.entities.StreamingBatch;
import io.harness.auditevent.streaming.entities.outgoing.OutgoingAuditMessage;
import io.harness.auditevent.streaming.publishers.StreamingPublisher;
import io.harness.auditevent.streaming.publishers.impl.AwsS3StreamingPublisher;
import io.harness.auditevent.streaming.services.BatchProcessorService;
import io.harness.auditevent.streaming.services.StreamingBatchService;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.data.mongodb.core.query.Criteria;

public class AuditEventStreamingServiceImplTest extends CategoryTest {
  public static final int MINUTES_15_IN_MILLS = 15 * 60 * 1000;
  public static final int MINUTES_10_IN_MILLS = 10 * 60 * 1000;
  public static final int MINUTES_30_IN_MILLS = 30 * 60 * 1000;
  @Mock private BatchProcessorService batchProcessorService;
  @Mock private StreamingBatchService streamingBatchService;
  @Mock private AuditEventRepository auditEventRepository;
  @Mock private AwsS3StreamingPublisher awsS3StreamingPublisher;
  private Map<String, StreamingPublisher> streamingPublisherMap;
  private AuditEventStreamingServiceImpl auditEventStreamingService;

  ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
  ArgumentCaptor<StreamingBatch> streamingBatchArgumentCaptor = ArgumentCaptor.forClass(StreamingBatch.class);

  private static final String ACCOUNT_IDENTIFIER = randomAlphabetic(10);

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    streamingPublisherMap = Map.of(AWS_S3_STREAMING_PUBLISHER, awsS3StreamingPublisher);
    this.auditEventStreamingService = new AuditEventStreamingServiceImpl(
        batchProcessorService, streamingBatchService, auditEventRepository, streamingPublisherMap);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testStream_whenStatusInProgress() {
    StreamingDestination streamingDestination = getStreamingDestination();
    StreamingBatch streamingBatch = getStreamingBatch(streamingDestination, IN_PROGRESS);
    JobParameters jobParameters = getJobParameters();
    when(streamingBatchService.getLastStreamingBatch(
             streamingDestination, jobParameters.getLong(JOB_START_TIME_PARAMETER_KEY)))
        .thenReturn(streamingBatch);
    StreamingBatch streamingBatchAsReturned = auditEventStreamingService.stream(streamingDestination, jobParameters);

    assertThat(streamingBatchAsReturned).isEqualToComparingFieldByField(streamingBatch);

    verify(streamingBatchService, times(1))
        .getLastStreamingBatch(streamingDestination, jobParameters.getLong(JOB_START_TIME_PARAMETER_KEY));
    verify(auditEventRepository, times(0)).loadAuditEvents(any(), any());
    verify(streamingBatchService, times(0)).update(ACCOUNT_IDENTIFIER, streamingBatch);
    verify(batchProcessorService, times(0)).processAuditEvent(any());
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testStream_whenStatusIsReadyAndNoNewAuditRecords() {
    StreamingDestination streamingDestination = getStreamingDestination();
    StreamingBatch streamingBatch = getStreamingBatch(streamingDestination, READY);
    JobParameters jobParameters = getJobParameters();
    when(streamingBatchService.getLastStreamingBatch(
             streamingDestination, jobParameters.getLong(JOB_START_TIME_PARAMETER_KEY)))
        .thenReturn(streamingBatch);
    when(auditEventRepository.loadAuditEvents(any(), any())).thenReturn(List.of());
    when(streamingBatchService.update(any(), any())).thenReturn(streamingBatch);

    StreamingBatch streamingBatchAsReturned = auditEventStreamingService.stream(streamingDestination, jobParameters);

    assertThat(streamingBatchAsReturned).isNotNull();
    assertCriteria(streamingBatch, 1);

    verify(streamingBatchService, times(1)).update(eq(ACCOUNT_IDENTIFIER), streamingBatchArgumentCaptor.capture());
    assertThat(streamingBatchArgumentCaptor.getValue().getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testStream_whenStatusIsReadyAndNewAuditRecordsFound() {
    StreamingDestination streamingDestination = getStreamingDestination();
    StreamingBatch streamingBatch = getStreamingBatch(streamingDestination, READY);
    streamingBatch.setLastSuccessfulRecordTimestamp(streamingBatch.getStartTime() + MINUTES_15_IN_MILLS);
    JobParameters jobParameters = getJobParameters();
    when(streamingBatchService.getLastStreamingBatch(
             streamingDestination, jobParameters.getLong(JOB_START_TIME_PARAMETER_KEY)))
        .thenReturn(streamingBatch);
    when(auditEventRepository.loadAuditEvents(any(), any()))
        .thenReturn(
            List.of(AuditEvent.builder().createdAt(streamingBatch.getStartTime() + MINUTES_10_IN_MILLS).build()))
        .thenReturn(List.of());
    when(batchProcessorService.processAuditEvent(any())).thenReturn(List.of(OutgoingAuditMessage.builder().build()));
    when(awsS3StreamingPublisher.publish(any(), any())).thenReturn(true);
    when(streamingBatchService.update(any(), any())).thenReturn(streamingBatch);

    StreamingBatch streamingBatchAsReturned = auditEventStreamingService.stream(streamingDestination, jobParameters);

    assertThat(streamingBatchAsReturned).isNotNull();
    assertCriteria(streamingBatch, 2);

    verify(streamingBatchService, times(2)).update(eq(ACCOUNT_IDENTIFIER), streamingBatchArgumentCaptor.capture());
    StreamingBatch streamingBatchCaptured = streamingBatchArgumentCaptor.getValue();
    assertThat(streamingBatchCaptured.getStatus()).isEqualTo(SUCCESS);
    assertThat(streamingBatchCaptured.getLastSuccessfulRecordTimestamp())
        .isEqualTo(streamingBatch.getStartTime() + MINUTES_10_IN_MILLS);
    assertThat(streamingBatchCaptured.getNumberOfRecordsPublished()).isEqualTo(1);
  }

  private void assertCriteria(StreamingBatch streamingBatch, int times) {
    verify(auditEventRepository, times(times)).loadAuditEvents(criteriaArgumentCaptor.capture(), any());
    Criteria criteria = criteriaArgumentCaptor.getValue();
    Document document = criteria.getCriteriaObject();
    assertThat(document).containsEntry(ACCOUNT_IDENTIFIER_KEY, ACCOUNT_IDENTIFIER);
    assertThat(document).containsKey(createdAt);
    Document createdAtDocument = (Document) document.get(createdAt);
    Long expectedStartTime = streamingBatch.getLastSuccessfulRecordTimestamp() != null
        ? streamingBatch.getLastSuccessfulRecordTimestamp()
        : streamingBatch.getStartTime();
    assertThat(createdAtDocument)
        .containsAllEntriesOf(
            Map.ofEntries(Map.entry("$gt", expectedStartTime), Map.entry("$lte", streamingBatch.getEndTime())));
  }

  private StreamingDestination getStreamingDestination() {
    String streamingDestinationIdentifier = randomAlphabetic(10);
    StreamingDestination streamingDestination =
        AwsS3StreamingDestination.builder().bucket(randomAlphabetic(10)).build();
    streamingDestination.setIdentifier(streamingDestinationIdentifier);
    streamingDestination.setAccountIdentifier(ACCOUNT_IDENTIFIER);
    streamingDestination.setType(AWS_S3);
    return streamingDestination;
  }

  private JobParameters getJobParameters() {
    Long timestamp = System.currentTimeMillis();
    Map<String, JobParameter> parameters = Map.of(JOB_START_TIME_PARAMETER_KEY, new JobParameter(timestamp),
        ACCOUNT_IDENTIFIER_PARAMETER_KEY, new JobParameter(ACCOUNT_IDENTIFIER));
    return new JobParameters(parameters);
  }

  private StreamingBatch getStreamingBatch(StreamingDestination streamingDestination, BatchStatus status) {
    long endTime = System.currentTimeMillis();
    long startTime = endTime - MINUTES_30_IN_MILLS;
    return StreamingBatch.builder()
        .accountIdentifier(ACCOUNT_IDENTIFIER)
        .streamingDestinationIdentifier(streamingDestination.getIdentifier())
        .startTime(startTime)
        .endTime(endTime)
        .status(status)
        .build();
  }
}
