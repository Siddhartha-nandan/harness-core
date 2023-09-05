/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.executions.node;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionStatusResult;
import io.harness.mongo.helper.AnalyticsMongoTemplateHolder;
import io.harness.mongo.helper.SecondaryMongoTemplateHolder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@Slf4j
@OwnedBy(PIPELINE)
@Singleton
public class NodeExecutionReadHelper {
  private static final int MAX_BATCH_SIZE = 1000;
  private static final String COLLECTION_NAME = "nodeExecutions";
  private final MongoTemplate mongoTemplate;
  private final MongoTemplate analyticsMongoTemplate;

  private final MongoTemplate secondaryMongoTemplate;

  @Inject
  public NodeExecutionReadHelper(MongoTemplate mongoTemplate, AnalyticsMongoTemplateHolder analyticsMongoTemplateHolder,
      SecondaryMongoTemplateHolder secondaryMongoTemplateHolder) {
    this.mongoTemplate = mongoTemplate;
    this.analyticsMongoTemplate = analyticsMongoTemplateHolder.getAnalyticsMongoTemplate();
    this.secondaryMongoTemplate = secondaryMongoTemplateHolder.getSecondaryMongoTemplate();
  }

  @Deprecated
  /**
   * @deprecated Use getOne below method which checks for projection field necessary
   */
  public Optional<NodeExecution> getOneWithoutProjections(Query query) {
    return Optional.ofNullable(mongoTemplate.findOne(query, NodeExecution.class));
  }

  public Optional<NodeExecution> getOne(Query query) {
    validateNodeExecutionProjection(query);
    return Optional.ofNullable(mongoTemplate.findOne(query, NodeExecution.class));
  }

  public Optional<NodeExecutionStatusResult> getStatus(Query query) {
    validateNodeExecutionProjection(query);
    return Optional.ofNullable(mongoTemplate.findOne(query, NodeExecutionStatusResult.class, COLLECTION_NAME));
  }

  public CloseableIterator<NodeExecution> fetchNodeExecutions(Query query) {
    query.cursorBatchSize(MAX_BATCH_SIZE);
    validateNodeExecutionStreamQuery(query);
    return mongoTemplate.stream(query, NodeExecution.class);
  }

  public <T> CloseableIterator<T> fetchNodeExecutions(Query query, Class<T> clazz) {
    query.cursorBatchSize(MAX_BATCH_SIZE);
    validateNodeExecutionStreamQuery(query);
    return mongoTemplate.stream(query, clazz, COLLECTION_NAME);
  }

  public CloseableIterator<NodeExecution> fetchNodeExecutionsWithAllFields(Query query) {
    query.cursorBatchSize(MAX_BATCH_SIZE);
    return mongoTemplate.stream(query, NodeExecution.class);
  }

  public CloseableIterator<NodeExecution> fetchNodeExecutionsFromAnalytics(Query query) {
    query.cursorBatchSize(MAX_BATCH_SIZE);
    validateNodeExecutionStreamQuery(query);
    return analyticsMongoTemplate.stream(query, NodeExecution.class);
  }

  /**
   * Should be used only for nodeExecutionReads where there is no projection
   * Get approval before using this method
   */
  public CloseableIterator<NodeExecution> fetchNodeExecutionsIteratorWithoutProjections(Query query) {
    query.cursorBatchSize(MAX_BATCH_SIZE);
    return mongoTemplate.stream(query, NodeExecution.class);
  }

  // Get count from primary node
  public long findCount(Query query) {
    return mongoTemplate.count(Query.of(query).limit(-1).skip(-1), NodeExecution.class);
  }

  /**
   * Should be used only for nodeExecutionReads where there is no projection
   * Get approval before using this method
   */
  public List<NodeExecution> fetchNodeExecutionsWithoutProjections(Query query) {
    return mongoTemplate.find(query, NodeExecution.class);
  }

  private void validateNodeExecutionStreamQuery(Query query) {
    if (query.getMeta().getCursorBatchSize() == null || query.getMeta().getCursorBatchSize() <= 0
        || query.getMeta().getCursorBatchSize() > MAX_BATCH_SIZE) {
      throw new InvalidRequestException(
          "NodeExecution query should have cursorBatch limit within max batch size- " + MAX_BATCH_SIZE);
    }
    validateNodeExecutionProjection(query);
  }

  private void validateNodeExecutionProjection(Query query) {
    if (query.getFieldsObject().isEmpty()) {
      throw new InvalidRequestException("NodeExecution list query should have projection fields");
    }
  }

  public NodeExecution fetchNodeExecutionsFromSecondaryTemplate(Query query) {
    validateNodeExecutionProjection(query);
    return secondaryMongoTemplate.findOne(query, NodeExecution.class);
  }
}
