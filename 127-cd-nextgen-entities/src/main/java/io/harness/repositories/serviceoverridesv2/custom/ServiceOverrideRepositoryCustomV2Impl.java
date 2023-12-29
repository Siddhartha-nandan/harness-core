/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.serviceoverridesv2.custom;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitaware.helper.GitAwareEntityHelper;
import io.harness.ng.core.serviceoverride.beans.NGServiceOverridesEntity;
import io.harness.springdata.PersistenceUtils;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class ServiceOverrideRepositoryCustomV2Impl implements ServiceOverrideRepositoryCustomV2 {
  private final MongoTemplate mongoTemplate;
  private final GitAwareEntityHelper gitAwareEntityHelper;

  @Override
  public NGServiceOverridesEntity update(Criteria criteria, NGServiceOverridesEntity serviceOverridesEntity) {
    Query query = new Query(criteria);
    Update updateOperations =
        ServiceOverrideRepositoryHelper.getUpdateOperationsForServiceOverrideV2(serviceOverridesEntity);
    RetryPolicy<Object> retryPolicy = getRetryPolicy("[Retrying]: Failed updating Service Override Entity; attempt: {}",
        "[Failed]: Failed updating Service Override Entity; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(query, updateOperations, new FindAndModifyOptions().returnNew(true),
                     NGServiceOverridesEntity.class));
  }

  @Override
  public DeleteResult delete(Criteria criteria) {
    Query query = new Query(criteria);
    RetryPolicy<Object> retryPolicy = getRetryPolicy("[Retrying]: Failed deleting Service Override; attempt: {}",
        "[Failed]: Failed deleting Service Override; attempt: {}");
    return Failsafe.with(retryPolicy).get(() -> mongoTemplate.remove(query, NGServiceOverridesEntity.class));
  }

  @Override
  public Page<NGServiceOverridesEntity> findAll(Criteria criteria, Pageable pageRequest) {
    Query query = new Query(criteria).with(pageRequest);
    List<NGServiceOverridesEntity> overridesEntities = mongoTemplate.find(query, NGServiceOverridesEntity.class);
    return PageableExecutionUtils.getPage(overridesEntities, pageRequest,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), NGServiceOverridesEntity.class));
  }

  @Override
  public List<NGServiceOverridesEntity> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, NGServiceOverridesEntity.class);
  }

  @Override
  public NGServiceOverridesEntity saveGitAware(NGServiceOverridesEntity overrideToSave) {
    if (GitAwareContextHelper.isRemoteEntity()) {
      createOverridesOnGit(overrideToSave);
    }

    // save in db
    return mongoTemplate.save(overrideToSave);
  }

  private void createOverridesOnGit(NGServiceOverridesEntity overrideToSave) {
    Scope scope = Scope.of(
        overrideToSave.getAccountId(), overrideToSave.getOrgIdentifier(), overrideToSave.getProjectIdentifier());
    String yamlToPush = overrideToSave.getYamlV2();
    gitAwareEntityHelper.createEntityOnGit(overrideToSave, yamlToPush, scope);
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return PersistenceUtils.getRetryPolicy(failedAttemptMessage, failureMessage);
  }
}
