/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ipallowlist.service.impl;

import static io.harness.springdata.PersistenceUtils.DEFAULT_RETRY_POLICY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.ipallowlist.IPAllowlistResourceUtils;
import io.harness.ipallowlist.entity.IPAllowlistEntity;
import io.harness.ipallowlist.service.IPAllowlistService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.ipallowlist.spring.IPAllowlistRepository;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(HarnessTeam.PL)
@Slf4j
public class IPAllowlistServiceImpl implements IPAllowlistService {
  private final IPAllowlistRepository ipAllowlistRepository;
  OutboxService outboxService;
  TransactionTemplate transactionTemplate;

  private final IPAllowlistResourceUtils ipAllowlistResourceUtil;

  @Inject
  public IPAllowlistServiceImpl(IPAllowlistRepository ipAllowlistRepository, OutboxService outboxService,
      TransactionTemplate transactionTemplate, IPAllowlistResourceUtils ipAllowlistResourceUtil) {
    this.ipAllowlistRepository = ipAllowlistRepository;
    this.outboxService = outboxService;
    this.transactionTemplate = transactionTemplate;
    this.ipAllowlistResourceUtil = ipAllowlistResourceUtil;
  }

  @Override
  public IPAllowlistEntity create(IPAllowlistEntity ipAllowlistEntity) {
    try {
      return Failsafe.with(DEFAULT_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
        IPAllowlistEntity savedIpAllowlistEntity = ipAllowlistRepository.save(ipAllowlistEntity);

        return savedIpAllowlistEntity;
      }));
    } catch (DuplicateKeyException exception) {
      String message =
          String.format("IP Allowlist config with identifier [%s] already exists.", ipAllowlistEntity.getIdentifier());
      log.error(message, exception);
      throw new DuplicateFieldException(message);
    }
  }

  @Override
  public IPAllowlistEntity update(String ipConfigIdentifier, IPAllowlistEntity ipAllowlistEntity) {
    ipAllowlistRepository.save(ipAllowlistEntity);
    return ipAllowlistEntity;
  }
}
