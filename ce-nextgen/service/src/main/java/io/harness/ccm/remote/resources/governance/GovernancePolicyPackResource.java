/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.remote.resources.governance;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.rbac.CCMRbacHelper;
import io.harness.ccm.scheduler.SchedulerClient;
import io.harness.ccm.scheduler.SchedulerDTO;
import io.harness.ccm.utils.LogAccountIdentifier;
import io.harness.ccm.views.dto.CreatePolicyPackDTO;
import io.harness.ccm.views.entities.Policy;
import io.harness.ccm.views.entities.PolicyPack;
import io.harness.ccm.views.service.GovernancePolicyService;
import io.harness.ccm.views.service.PolicyPackService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import retrofit2.Response;

@Api("governance")
@Path("governance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
@Service
@OwnedBy(CE)
@Slf4j
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })

public class GovernancePolicyPackResource {
  public static final String ACCOUNT_ID = "accountId";
  private final CCMRbacHelper rbacHelper;
  private final PolicyPackService policyPackService;
  private final GovernancePolicyService policyService;
  @Inject SchedulerClient schedulerClient;
  @Inject CENextGenConfiguration configuration;

  @Inject
  public GovernancePolicyPackResource(
      PolicyPackService policyPackService, CCMRbacHelper rbacHelper, GovernancePolicyService policyService) {
    this.rbacHelper = rbacHelper;
    this.policyPackService = policyPackService;
    this.policyService = policyService;
  }

  @POST
  @Path("policypack")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Add a new policy set", nickname = "addPolicySet")
  @Operation(operationId = "addPolicyNameInternal", summary = "Add a policy set ",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns newly created policy set")
      })
  public ResponseDTO<PolicyPack>
  create(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(required = true,
          description = "Request body containing Policy store object") @Valid CreatePolicyPackDTO createPolicySetDTO) {
    // rbacHelper.checkPolicySetEditPermission(accountId, null, null);
    PolicyPack policyPack = createPolicySetDTO.getPolicyPack();
    for (String identifiers : policyPack.getPoliciesIdentifier()) {
      policyService.listid(accountId, identifiers);
    }
    policyPack.setAccountId(accountId);
    policyPackService.save(policyPack);
    if (configuration.getGovernanceConfig().isUseDkron()) {
      log.info("USe dkron is enabled in config");
      try {
        SchedulerDTO schedulerDTO = SchedulerDTO.builder()
                                        .schedule("@every 30s")
                                        .disabled(true)
                                        .name("get-cedev-version")
                                        .displayname("get-cedev-version")
                                        .timezone("UTC")
                                        .executor("http")
                                        .executor_config(SchedulerDTO.ExecutorConfig.builder()
                                                             .method("GET")
                                                             .url("http://ce-dev.harness.io/api/version")
                                                             .build())
                                        .build();
        log.info(new Gson().toJson(schedulerDTO));
        okhttp3.RequestBody body =
            okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), new Gson().toJson(schedulerDTO));
        Response res = schedulerClient.createSchedule(body).execute();
        log.info("code: {}, message: {}, body: {}", res.code(), res.message(), res.body());
      } catch (Exception e) {
        log.info("{}", e.toString());
      }
    }
    // TODO: Add support for GCP cloud scheduler too

    return ResponseDTO.newResponse(policyPack.toDTO());
  }

  @PUT
  @Path("policypack")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update an existing policy set", nickname = "updatePolicySet")
  @LogAccountIdentifier
  @Operation(operationId = "updatePolicySet", description = "Update a Policy set", summary = "Update a Policy set",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "update a existing OOTB Policy",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<PolicyPack>
  updatePolicy(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                   NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(required = true,
          description = "Request body containing ceViewFolder object") @Valid CreatePolicyPackDTO createPolicySetDTO) {
    //  rbacHelper.checkPolicySetEditPermission(accountId, null, null);
    PolicyPack policyPack = createPolicySetDTO.getPolicyPack();
    policyPack.toDTO();
    policyPack.setAccountId(accountId);
    policyPackService.update(policyPack);
    return ResponseDTO.newResponse(policyPack);
  }

  @POST
  @Path("policypack/listpolicies/{id}")
  @ApiOperation(value = "Get policies for pack", nickname = "getPolicies")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(operationId = "getPolicies", description = "Fetch policies ", summary = "Fetch policies for account",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            description = "Returns List of policies", content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<List<Policy>>
  listPolicy(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                 NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(required = true,
          description = "Request body containing ceViewFolder object") @Valid CreatePolicyPackDTO createPolicyPackDTO,
      @PathParam("id") @Parameter(
          required = true, description = "Unique identifier for the policy") @NotNull @Valid String uuid) {
    // rbacHelper.checkPolicyPackPermission(accountId, null, null);
    PolicyPack query = createPolicyPackDTO.getPolicyPack();
    List<Policy> Policies = new ArrayList<>();
    for (String it : query.getPoliciesIdentifier()) {
      Policies.add(policyService.listid(accountId, it));
    }

    return ResponseDTO.newResponse(Policies);
  }

  @DELETE
  @Path("policypack/{policyPackId}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete a policy set", nickname = "deletePolicySet")
  @LogAccountIdentifier
  @Operation(operationId = "deletePolicySet", description = "Delete a Policy set for the given a ID.",
      summary = "Delete a policy set",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "A boolean whether the delete was successful or not",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<Boolean>
  delete(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @PathParam("policyPackId") @Parameter(
          required = true, description = "Unique identifier for the policy") @NotNull @Valid String uuid) {
    // rbacHelper.checkPolicySetDeletePermission(accountId, null, null);
    boolean result = policyPackService.delete(accountId, uuid);
    return ResponseDTO.newResponse(result);
  }
}
