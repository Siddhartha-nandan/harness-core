/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.tunnel.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.NextGenConfiguration;
import io.harness.ng.core.dto.TunnelRegisterRequestDTO;
import io.harness.ng.core.dto.TunnelResponseDTO;
import io.harness.ng.tunnel.entities.Tunnel;
import io.harness.ng.tunnel.entities.Tunnel.TunnelKeys;
import io.harness.persistence.HPersistence;
import io.harness.repositories.ng.tunnel.TunnelRepository;
import io.harness.services.TunnelService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CI)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class TunnelServiceImpl implements TunnelService {
  private final TunnelRepository tunnelRepository;
  private NextGenConfiguration nextGenConfiguration;
  private HPersistence persistence;

  @Override
  public Boolean registerTunnel(String accountId, TunnelRegisterRequestDTO tunnelRegisterRequestDTO) {
    if (!validateTunnel(tunnelRegisterRequestDTO)) {
      return Boolean.FALSE;
    }

    UpdateOperations<Tunnel> updateOperations = persistence.createUpdateOperations(Tunnel.class)
                                                    .setOnInsert(TunnelKeys.accountIdentifier, accountId)
                                                    .set(TunnelKeys.port, tunnelRegisterRequestDTO.getPort());
    Query<Tunnel> upsertQuery =
        persistence.createQuery(Tunnel.class, excludeAuthority).filter(TunnelKeys.accountIdentifier, accountId);
    persistence.upsert(upsertQuery, updateOperations);
    return Boolean.TRUE;
  }
  @Override
  public Boolean deleteTunnel(String accountId) {
    Query<Tunnel> deleteQuery =
        persistence.createQuery(Tunnel.class, excludeAuthority).filter(TunnelKeys.accountIdentifier, accountId);

    return persistence.delete(deleteQuery);
  }
  @Override
  public TunnelResponseDTO getTunnel(String accountId) {
    Optional<Tunnel> optionalTunnel = tunnelRepository.findByAccountIdentifier(accountId);
    if (optionalTunnel.isEmpty()) {
      return TunnelResponseDTO.builder().serverUrl("").port("").build();
    }
    return TunnelResponseDTO.builder()
        .serverUrl(nextGenConfiguration.getFrpsTunnelConfig().getHost())
        .port(optionalTunnel.get().getPort())
        .build();
  }

  private boolean validateTunnel(TunnelRegisterRequestDTO tunnelRegisterRequestDTO) {
    if (EmptyPredicate.isEmpty(tunnelRegisterRequestDTO.getPort())) {
      log.error("Port number cannot be empty for tunnel creation");
      return false;
    }

    try {
      int port = Integer.parseInt(tunnelRegisterRequestDTO.getPort());
      return port >= 0 && port <= 65535;
    } catch (NumberFormatException e) {
      // If parsing as an integer fails, it's not a valid port number.
      log.error("Port number should be an integer value", e);
      return false;
    }
  }
}
