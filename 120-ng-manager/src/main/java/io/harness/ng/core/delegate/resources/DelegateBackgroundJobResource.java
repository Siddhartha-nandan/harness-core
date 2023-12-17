package io.harness.ng.core.delegate.resources;

import static io.harness.delegate.utils.RbacConstants.DELEGATE_RESOURCE_TYPE;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_VIEW_PERMISSION;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.SupportedDelegateVersion;
import io.harness.ng.core.delegate.client.DelegateNgManagerCgManagerClient;
import io.harness.rest.RestResponse;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.IOException;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api("delegate-background-job")
@Path("/delegate-background-job")
@Consumes({"application/json"})
@Produces({"application/json"})
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateBackgroundJobResource {
  private final DelegateNgManagerCgManagerClient delegateNgManagerCgManagerClient;
  private final AccessControlClient accessControlClient;

  @Inject
  public DelegateBackgroundJobResource(
      DelegateNgManagerCgManagerClient delegateNgManagerCgManagerClient, AccessControlClient accessControlClient) {
    this.delegateNgManagerCgManagerClient = delegateNgManagerCgManagerClient;
    this.accessControlClient = accessControlClient;
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Disable perpetual task for the account", nickname = "disablePerpetualTask")
  @Operation(operationId = "disablePerpetualTask", summary = "Disable perpetual task for the account",
      responses = { @ApiResponse(responseCode = "default", description = "Disable perpetual task for the account") })
  public RestResponse<SupportedDelegateVersion>
  disableBackgroundJobs(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
                            NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Parameter(description = NGCommonEntityConstants.PERPETUAL_TASK_TYPE_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PERPETUALTASK_TYPE_KEY) @NotNull String perpetualTaskType) throws IOException {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, null, null),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_VIEW_PERMISSION);
    delegateNgManagerCgManagerClient.disableBackgroundJobs(accountIdentifier, perpetualTaskType);
    return new RestResponse<>();
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Enable perpetual task for the account", nickname = "enablePerpetualTask")
  @Operation(operationId = "enablePerpetualTask", summary = "Enabled perpetual task for the account",
      responses = { @ApiResponse(responseCode = "default", description = "Enable perpetual task for the account") })
  public RestResponse<SupportedDelegateVersion>
  enableBackgroundJobs(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
                           NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Parameter(description = NGCommonEntityConstants.PERPETUAL_TASK_TYPE_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PERPETUALTASK_TYPE_KEY) @NotNull String perpetualTaskType) throws IOException {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, null, null),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_VIEW_PERMISSION);
    delegateNgManagerCgManagerClient.enableBackgroundJobs(accountIdentifier, perpetualTaskType);
    return new RestResponse<>();
  }
}
