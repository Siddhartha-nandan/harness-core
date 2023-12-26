/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.delegate.beans.perpetualtask.PerpetualTaskConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.perpetualtask.PerpetualTaskConfigService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

@Api("delegate-background-job")
@Path("/delegate-background-job")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PerpetualTaskConfigResource {
  @Inject private PerpetualTaskConfigService perpetualTaskConfigService;

  @POST
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<String> save(
      @QueryParam("accountId") String accountId, @QueryParam("perpetualTaskType") String perpetualTaskType) {
    PerpetualTaskConfig perpetualTaskConfig =
        perpetualTaskConfigService.disableAccountPerpetualTask(accountId, perpetualTaskType);
    if (perpetualTaskConfig != null) {
      return new RestResponse<>("Disabled background job for the account");
    }
    throw new InvalidRequestException("Unable to disable background jobs");
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<String> reset(
      @QueryParam("accountId") String accountId, @QueryParam("perpetualTaskType") String perpetualTaskType) {
    if (perpetualTaskConfigService.resumeAccountPerpetualTask(accountId, perpetualTaskType)) {
      return new RestResponse<>("Enabled background job for the account");
    }
    throw new InvalidRequestException("Unable to enable background jobs");
  }
}
