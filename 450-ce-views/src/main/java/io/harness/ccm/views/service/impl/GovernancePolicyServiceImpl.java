/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.views.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.dao.PolicyDAO;
import io.harness.ccm.views.dao.PolicyEnforcementDAO;
import io.harness.ccm.views.entities.GovernancePolicyFilter;
import io.harness.ccm.views.entities.Policy;
import io.harness.ccm.views.service.GovernancePolicyService;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class GovernancePolicyServiceImpl implements GovernancePolicyService {
  @Inject private PolicyDAO policyDao;
  @Inject private PolicyEnforcementDAO policyEnforcementDAO;

  @Override
  public boolean save(Policy policy) {
    return policyDao.save(policy);
  }

  @Override
  public boolean delete(String accountId, String uuid) {
    return policyDao.delete(accountId, uuid);
  }

  @Override
  public Policy update(Policy policy, String accountId) {
    return policyDao.update(policy, accountId);
  }

  @Override
  public List<Policy> list(GovernancePolicyFilter governancePolicyFilter) {
    return policyDao.list(governancePolicyFilter);
  }

  @Override
  public List<Policy> list(String accountId, List<String> uuid) {
    return policyDao.check(accountId, uuid);
  }

  @Override
  public Policy listName(String accountId, String name, boolean create) {
    return policyDao.listName(accountId, name, create);
  }

  @Override
  public Policy listId(String accountId, String name, boolean create) {
    return policyDao.listid(accountId, name, create);
  }

  @Override
  public void check(String accountId, List<String> policiesIdentifier) {
    List<Policy> policies = policyDao.check(accountId, policiesIdentifier);
    if (policies.size() != policiesIdentifier.size()) {
      for (Policy it : policies) {
        log.info("{} {} ", it, it.getUuid());
        policiesIdentifier.remove(it.getUuid());
      }
      if (policiesIdentifier.size() != 0) {
        throw new InvalidRequestException("No such policies exist:" + policiesIdentifier.toString());
      }
    }
  }
}
