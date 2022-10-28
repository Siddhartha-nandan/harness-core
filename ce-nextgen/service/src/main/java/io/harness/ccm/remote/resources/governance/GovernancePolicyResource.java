/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.remote.resources.governance;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.governance.faktory.FaktoryProducer;
import io.harness.ccm.rbac.CCMRbacHelper;
import io.harness.ccm.utils.LogAccountIdentifier;
import io.harness.ccm.views.dto.CreatePolicyDTO;
import io.harness.ccm.views.dto.GovernanceEnqueueResponseDTO;
import io.harness.ccm.views.dto.GovernanceJobEnqueueDTO;
import io.harness.ccm.views.dto.ListDTO;
import io.harness.ccm.views.entities.GovernanceJobDetailsAWS;
import io.harness.ccm.views.entities.GovernancePolicyFilter;
import io.harness.ccm.views.entities.Policy;
import io.harness.ccm.views.entities.PolicyCloudProviderType;
import io.harness.ccm.views.entities.PolicyEnforcement;
import io.harness.ccm.views.entities.PolicyPack;
import io.harness.ccm.views.entities.PolicyStoreType;
import io.harness.ccm.views.service.GovernancePolicyService;
import io.harness.ccm.views.service.PolicyEnforcementService;
import io.harness.ccm.views.service.PolicyPackService;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.FilterType;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.PublicApi;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Api("governance")
@Path("governance")
@OwnedBy(CE)
@Tag(name = "Policy", description = "This contains APIs related to Policy Management ")
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
@PublicApi
//@NextGenManagerAuth
public class GovernancePolicyResource {
  private final GovernancePolicyService governancePolicyService;
  private final PolicyPackService policyPackService;
  private final PolicyEnforcementService policyEnforcementService;
  private final CCMRbacHelper rbacHelper;
  @Autowired private ConnectorResourceClient connectorResourceClient;

  @Inject
  public GovernancePolicyResource(GovernancePolicyService governancePolicyService, CCMRbacHelper rbacHelper,
      PolicyEnforcementService policyEnforcementService, PolicyPackService policyPackService) {
    this.governancePolicyService = governancePolicyService;
    this.rbacHelper = rbacHelper;
    this.policyEnforcementService = policyEnforcementService;
    this.policyPackService = policyPackService;
  }

  // Internal API for OOTB policy creation
  @POST
  @Hidden
  @Path("policy")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Add a new policy internal api", nickname = "addPolicyNameInternal")
  @Operation(operationId = "addPolicyNameInternal", summary = "Add a new OOTB policy to be executed",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns newly created policy")
      })
  public ResponseDTO<Policy>
  create(@Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(required = true,
          description = "Request body containing Policy store object") @Valid CreatePolicyDTO createPolicyDTO) {
    // rbacHelper.checkPolicyEditPermission(accountId, null, null);
    Policy policy = createPolicyDTO.getPolicy();
    if (governancePolicyService.listName(accountId, policy.getName(), true) != null) {
      throw new InvalidRequestException("Policy  with given name already exits");
    }
    if (!policy.getIsOOTB()) {
      policy.setAccountId(accountId);
    } else {
      policy.setAccountId("");
    }
    // TODO: Handle this for custom policies and git connectors
    policy.setStoreType(PolicyStoreType.INLINE);
    policy.setVersionLabel("0.0.1");
    policy.setDeleted(false);
    governancePolicyService.save(policy);
    return ResponseDTO.newResponse(policy.toDTO());
  }

  // Update a policy already made
  @PUT
  @Path("policy")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a existing OOTB Policy", nickname = "updatePolicy")
  @LogAccountIdentifier
  @Operation(operationId = "updatePolicy", description = "Update a Policy", summary = "Update a Policy",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "update a existing OOTB Policy",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<Policy>
  updatePolicy(@Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
                   NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(required = true,
          description = "Request body containing policy object") @Valid CreatePolicyDTO createPolicyDTO) {
    // rbacHelper.checkPolicyEditPermission(accountId, null, null);
    Policy policy = createPolicyDTO.getPolicy();
    policy.toDTO();
    governancePolicyService.listName(accountId, policy.getName(), false);
    return ResponseDTO.newResponse(governancePolicyService.update(policy, accountId));
  }

  @PUT
  @Hidden
  @Path("policyOOTB")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a existing OOTB Policy", nickname = "updatePolicy")
  @LogAccountIdentifier
  @Operation(operationId = "updatePolicy", description = "Update a Policy", summary = "Update a Policy",
          responses =
                  {
                          @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "update a existing OOTB Policy",
                                  content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
                  })
  public ResponseDTO<Policy>
  updatePolicy(@RequestBody(required = true,
                       description = "Request body containing policy object") @Valid CreatePolicyDTO createPolicyDTO) {
    // rbacHelper.checkPolicyEditPermission(accountId, null, null);
    Policy policy = createPolicyDTO.getPolicy();
    policy.toDTO();
    governancePolicyService.listName("", policy.getName(), false);
    return ResponseDTO.newResponse(governancePolicyService.update(policy, ""));
  }
  // Internal API for deletion of OOTB policies

  @DELETE
  @Hidden
  @Path("{policyId}")
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered
  @ApiOperation(value = "Delete a policy", nickname = "deletePolicy")
  @LogAccountIdentifier
  @Operation(operationId = "deletePolicy", description = "Delete a Policy for the given a ID.",
      summary = "Delete a policy",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "A boolean whether the delete was successful or not",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<Boolean>
  deleteOOTB(@PathParam("policyId") @Parameter(
      required = true, description = "Unique identifier for the policy") @NotNull @Valid String uuid) {
    boolean result = governancePolicyService.deleteOOTB(uuid);
    return ResponseDTO.newResponse(result);
  }

  @DELETE
  @Path("policy/{policyName}")
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered
  @ApiOperation(value = "Delete a policy", nickname = "deletePolicy")
  @LogAccountIdentifier
  @Operation(operationId = "deletePolicy", description = "Delete a Policy for the given a ID.",
      summary = "Delete a policy",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "A boolean whether the delete was successful or not",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<Boolean>
  delete(@Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @PathParam("policyName") @Parameter(
          required = true, description = "Unique identifier for the policy") @NotNull @Valid String name) {
    // rbacHelper.checkPolicyDeletePermission(accountId, null, null);

    boolean result =
        governancePolicyService.delete(accountId, governancePolicyService.listName(accountId, name, false).getUuid());
    return ResponseDTO.newResponse(result);
  }

  // API to list all OOTB Policies

  @POST
  @Path("policy/list")
  @ApiOperation(value = "Get OOTB policies for account", nickname = "getPolicies")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(operationId = "getPolicies", description = "Fetch policies ", summary = "Fetch policies for account",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            description = "Returns List of policies", content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<List<Policy>>
  listPolicy(@Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
                 NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(required = true, description = "Request body containing policy object") @Valid ListDTO listDTO) {
    // rbacHelper.checkPolicyViewPermission(accountId, null, null);
    GovernancePolicyFilter query = listDTO.getGovernancePolicyFilter();
    query.setAccountId(accountId);
    log.info("assigned {} {}", query.getAccountId(), query.getIsOOTB());
    return ResponseDTO.newResponse(governancePolicyService.list(query));
  }

  @POST
  @Path("enqueue")
  @Timed
  @ExceptionMetered
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Enqueues job for execution", nickname = "enqueueGovernanceJob")
  // TODO: Also check with PL team as this does not require accountId to be passed, how to add accountId in the log
  // context here ?
  @Operation(operationId = "enqueueGovernanceJob", description = "Enqueues job for execution.",
      summary = "Enqueues job for execution",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "Returns success when job is enqueued",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<GovernanceEnqueueResponseDTO>
  enqueue(@RequestBody(required = true, description = "Request body for queuing the governance job")
      @Valid GovernanceJobEnqueueDTO governanceJobEnqueueDTO) throws IOException {
    // TODO: Refactor and make this method smaller
    // Step-1 Fetch from mongo
    String policyEnforcementUuid = governanceJobEnqueueDTO.getPolicyEnforcementId();
    log.info("Policy enforcement config id is {}", policyEnforcementUuid);
    PolicyEnforcement policyEnforcement = policyEnforcementService.get(policyEnforcementUuid);
    if (policyEnforcement == null) {
      log.error("No policy enforcement setting {} found in db. Skipping enqueuing in faktory", policyEnforcementUuid);
      // TODO: Return simple response to dkron instead of empty for debugging purposes
      return ResponseDTO.newResponse();
    }
    String accountId = policyEnforcement.getAccountId();

    if (policyEnforcement.getCloudProvider() != PolicyCloudProviderType.AWS) {
      log.error("Support for non AWS cloud providers is not present atm. Skipping enqueuing in faktory");
      // TODO: Return simple response to dkron instead of empty for debugging purposes
      return ResponseDTO.newResponse();
    }

    if (policyEnforcement.getTargetAccounts() == null || policyEnforcement.getTargetAccounts().size() == 0) {
      log.error("Need at least one target cloud accountId to work on. Skipping enqueuing in faktory");
      // TODO: Return simple response to dkron instead of empty for debugging purposes
      return ResponseDTO.newResponse();
    }

    // Step-2 Prep unique policy Ids set from this enforcement
    Set<String> uniquePolicyIds = new HashSet<>();
    if (policyEnforcement.getPolicyIds().size() > 0) {
      // Assumption: The policyIds in the enforcement records are all valid ones
      uniquePolicyIds.addAll(policyEnforcement.getPolicyIds());
    }
    if (policyEnforcement.getPolicyPackIDs().size() > 0) {
      List<PolicyPack> policyPacks = policyPackService.listPacks(accountId, policyEnforcement.getPolicyPackIDs());
      for (PolicyPack policyPack : policyPacks) {
        uniquePolicyIds.addAll(policyPack.getPoliciesIdentifier());
      }
    }
    log.info("uniquePolicyIds: {}", uniquePolicyIds);

    // Step-3 Figure out roleArn and externalId from the connector listv2 api call for all target accounts.
    List<ConnectorResponseDTO> nextGenConnectorResponses = new ArrayList<>();
    PageResponse<ConnectorResponseDTO> response = null;
    ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO =
        ConnectorFilterPropertiesDTO.builder()
            .types(Arrays.asList(ConnectorType.CE_AWS))
            .ccmConnectorFilter(CcmConnectorFilter.builder()
                                    .featuresEnabled(Arrays.asList(CEFeatures.GOVERNANCE))
                                    .awsAccountId(policyEnforcement.getTargetAccounts())
                                    .build())
            .build();
    connectorFilterPropertiesDTO.setFilterType(FilterType.CONNECTOR);
    int page = 0;
    int size = 100;
    do {
      response = execute(connectorResourceClient.listConnectors(
          accountId, null, null, page, size, connectorFilterPropertiesDTO, false));
      if (response != null && isNotEmpty(response.getContent())) {
        nextGenConnectorResponses.addAll(response.getContent());
      }
      page++;
    } while (response != null && isNotEmpty(response.getContent()));

    log.info("Got connector data: {}", nextGenConnectorResponses);

    // Step-4 Enqueue in faktory
    for (ConnectorResponseDTO connector : nextGenConnectorResponses) {
      ConnectorInfoDTO connectorInfo = connector.getConnector();
      CEAwsConnectorDTO ceAwsConnectorDTO = (CEAwsConnectorDTO) connectorInfo.getConnectorConfig();
      for (String region : policyEnforcement.getTargetRegions()) {
        for (String policyId : uniquePolicyIds) {
          try {
            GovernanceJobDetailsAWS governanceJobDetailsAWS =
                GovernanceJobDetailsAWS.builder()
                    .awsAccountId(ceAwsConnectorDTO.getAwsAccountId())
                    .externalId(ceAwsConnectorDTO.getCrossAccountAccess().getExternalId())
                    .roleArn(ceAwsConnectorDTO.getCrossAccountAccess().getCrossAccountRoleArn())
                    .isDryRun(policyEnforcement.getIsDryRun())
                    .policyId(policyId)
                    .region(region)
                    .policyEnforcementId(policyEnforcementUuid)
                    .policy("") // TODO
                    .build();
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(governanceJobDetailsAWS);
            log.info("Enqueuing job in Faktory {}", json);
            // TODO: Test bulk enqueue here
            FaktoryProducer.Push(policyEnforcement.getCloudProvider().toString(), "aws", json);
            log.info("Pushed job in Faktory!");
          } catch (Exception e) {
            log.warn("Exception processing aws tags for connectorId: {} for CCM accountId: {}",
                connectorInfo.getIdentifier(), accountId, e);
          }
        }
      }
    }

    //    try {
    //
    //      FaktoryProducer.Push("aws", "aws", "{}");
    //      log.info("Pushed job in Faktory!");
    //    } catch (IOException e) {
    //      log.error("{}", e);
    //    }

    return ResponseDTO.newResponse();
  }
}
