/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.platform.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFlag;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.NoResultFoundException;
import io.harness.health.HealthException;
import io.harness.health.HealthService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rest.RestResponse;
import io.harness.security.MyObject;
import io.harness.security.annotations.PublicApi;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Api("health")
@Path("health")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@OwnedBy(PL)
@ExposeInternalException
@Slf4j
@PublicApi
public class HealthResource {
  private final List<HealthService> healthServices;
  private final ThreadDeadlockHealthCheck threadDeadlockHealthCheck;

  public HealthResource(List<HealthService> healthServices) {
    this.healthServices = healthServices;
    this.threadDeadlockHealthCheck = new ThreadDeadlockHealthCheck();
  }

  @GET
  @Timed
  @ExceptionMetered
  @Operation(hidden = true)
  public ResponseDTO<String> get(@Context MyObject myObject) throws Exception {
    if (getMaintenanceFlag()) {
      log.info("In maintenance mode. Throwing exception to prevent traffic.");
      throw NoResultFoundException.newBuilder()
          .code(ErrorCode.RESOURCE_NOT_FOUND)
          .message("in maintenance mode")
          .reportTargets(USER)
          .build();
    }

    for (HealthService healthService : healthServices) {
      final HealthCheck.Result healthCheck = healthService.check();
      if (!healthCheck.isHealthy()) {
        throw new HealthException(healthCheck.getMessage(), healthCheck.getError());
      }
    }

    return ResponseDTO.newResponse("healthy");
  }

  @GET
  @Path("liveness")
  @Timed
  @ExceptionMetered
  @Operation(hidden = true)
  public RestResponse<String> doLivenessCheck() {
    HealthCheck.Result check = threadDeadlockHealthCheck.execute();
    if (check.isHealthy()) {
      return new RestResponse<>("live");
    }
    log.info(check.getMessage());
    throw new HealthException(check.getMessage(), check.getError());
  }
}
