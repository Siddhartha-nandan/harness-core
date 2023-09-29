/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.environment.custom;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitaware.helper.GitAwareEntityHelper;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.Environment.EnvironmentKeys;
import io.harness.ng.core.environment.mappers.EnvironmentFilterHelper;
import io.harness.ng.core.utils.CDGitXService;
import io.harness.ng.core.utils.GitXUtils;
import io.harness.springdata.PersistenceUtils;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class EnvironmentRepositoryCustomImpl implements EnvironmentRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
  private final int MAX_ATTEMPTS = 3;
  private final GitAwareEntityHelper gitAwareEntityHelper;
  private final CDGitXService cdGitXService;

  @Override
  public Page<Environment> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable).collation(Collation.of(Locale.ENGLISH).strength(1));
    List<Environment> projects = mongoTemplate.find(query, Environment.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Environment.class));
  }

  @Override
  public Environment saveGitAware(Environment environmentToSave) {
    GitAwareContextHelper.initDefaultScmGitMetaData();
    GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfo();

    // inline entity
    if (gitEntityInfo == null || StoreType.INLINE.equals(gitEntityInfo.getStoreType())
        || gitEntityInfo.getStoreType() == null) {
      environmentToSave.setStoreType(StoreType.INLINE);
      return mongoTemplate.save(environmentToSave);
    }

    if (!cdGitXService.isNewGitXEnabled(environmentToSave.getAccountId(), environmentToSave.getOrgIdentifier(),
            environmentToSave.getProjectIdentifier())) {
      throw new InvalidRequestException(GitXUtils.getErrorMessageForGitSimplificationNotEnabled(
          environmentToSave.getOrgIdentifier(), environmentToSave.getProjectIdentifier()));
    }

    addGitParamsToEnvironmentEntity(environmentToSave, gitEntityInfo);
    Scope scope = Scope.of(environmentToSave.getAccountId(), environmentToSave.getOrgIdentifier(),
        environmentToSave.getProjectIdentifier());
    String yamlToPush = environmentToSave.getYaml();

    gitAwareEntityHelper.createEntityOnGit(environmentToSave, yamlToPush, scope);
    return mongoTemplate.save(environmentToSave);
  }

  @Override
  public Environment upsert(Criteria criteria, Environment environment) {
    Query query = new Query(criteria);
    Update updateOperations = EnvironmentFilterHelper.getUpdateOperations(environment);
    RetryPolicy<Object> retryPolicy = getRetryPolicyWithDuplicateKeyException(
        "[Retrying]: Failed upserting Environment; attempt: {}", "[Failed]: Failed upserting Environment; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(query, updateOperations,
                     new FindAndModifyOptions().returnNew(true).upsert(true), Environment.class));
  }

  @Override
  public Environment update(Criteria criteria, Environment environment) {
    Query query = new Query(criteria);
    Update updateOperations = EnvironmentFilterHelper.getUpdateOperations(environment);
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed updating Environment; attempt: {}", "[Failed]: Failed updating Environment; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(
                     query, updateOperations, new FindAndModifyOptions().returnNew(true), Environment.class));
  }

  public boolean softDelete(Criteria criteria) {
    Query query = new Query(criteria);
    Update updateOperationsForDelete = EnvironmentFilterHelper.getUpdateOperationsForDelete();
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed deleting Environment; attempt: {}", "[Failed]: Failed deleting Environment; attempt: {}");
    UpdateResult updateResult =
        Failsafe.with(retryPolicy)
            .get(() -> mongoTemplate.updateFirst(query, updateOperationsForDelete, Environment.class));
    return updateResult.wasAcknowledged() && updateResult.getModifiedCount() == 1;
  }

  @Override
  public boolean delete(Criteria criteria) {
    Query query = new Query(criteria);
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed deleting Environment; attempt: {}", "[Failed]: Failed deleting Environment; attempt: {}");
    DeleteResult deleteResult = Failsafe.with(retryPolicy).get(() -> mongoTemplate.remove(query, Environment.class));
    return deleteResult.wasAcknowledged();
  }

  @Override
  public List<Environment> findAllRunTimeAccess(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, Environment.class);
  }

  @Override
  public List<String> fetchesNonDeletedEnvIdentifiersFromList(Criteria criteria) {
    Query query = new Query(criteria);
    query.fields().include(EnvironmentKeys.identifier);
    return mongoTemplate.find(query, Environment.class)
        .stream()
        .map(entity -> entity.getIdentifier())
        .collect(Collectors.toList());
  }

  @Override
  public List<Environment> fetchesNonDeletedEnvironmentFromListOfIdentifiers(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, Environment.class);
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return PersistenceUtils.getRetryPolicy(failedAttemptMessage, failureMessage);
  }

  private RetryPolicy<Object> getRetryPolicyWithDuplicateKeyException(
      String failedAttemptMessage, String failureMessage) {
    return PersistenceUtils.getRetryPolicyWithDuplicateKeyException(failedAttemptMessage, failureMessage);
  }

  @Override
  public List<Environment> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, Environment.class);
  }
  @Override
  public List<String> getEnvironmentIdentifiers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Criteria baseCriteria = Criteria.where(EnvironmentKeys.accountId)
                                .is(accountIdentifier)
                                .and(EnvironmentKeys.orgIdentifier)
                                .is(orgIdentifier)
                                .and(EnvironmentKeys.projectIdentifier)
                                .is(projectIdentifier);

    Query query = new Query(baseCriteria);

    query.fields().include(EnvironmentKeys.identifier).exclude(EnvironmentKeys.id);

    List<Environment> EnvironmentEntity = mongoTemplate.find(query, Environment.class);
    return EnvironmentEntity.stream().map(environment -> environment.getIdentifier()).collect(Collectors.toList());
  }

  private void addGitParamsToEnvironmentEntity(Environment environmentEntity, GitEntityInfo gitEntityInfo) {
    environmentEntity.setStoreType(StoreType.REMOTE);
    if (EmptyPredicate.isEmpty(environmentEntity.getRepoURL())) {
      environmentEntity.setRepoURL(gitAwareEntityHelper.getRepoUrl(environmentEntity.getAccountId(),
          environmentEntity.getOrgIdentifier(), environmentEntity.getProjectIdentifier()));
    }
    environmentEntity.setConnectorRef(gitEntityInfo.getConnectorRef());
    environmentEntity.setRepo(gitEntityInfo.getRepoName());
    environmentEntity.setFilePath(gitEntityInfo.getFilePath());
    environmentEntity.setFallBackBranch(gitEntityInfo.getBranch());
  }
}
