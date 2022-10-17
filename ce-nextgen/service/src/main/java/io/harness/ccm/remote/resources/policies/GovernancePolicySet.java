package io.harness.ccm.remote.resources.policies;


import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.rbac.CCMRbacHelper;
import io.harness.ccm.utils.LogAccountIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.CE;

@Api("policyset")
@Path("policyset")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
//@PublicApi
@Service
@OwnedBy(CE)
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

public class GovernancePolicySet {
    private final CCMRbacHelper rbacHelper;
    private final PolicySetService  policySetService;

    @Inject
    public GovernancePolicySet(PolicySetService policySetService, CCMRbacHelper rbacHelper)
    {
        this.rbacHelper = rbacHelper;
        this.policySetService = policySetService;
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add a new policy set", nickname = "addPolicySet")
    @Operation(operationId = "addPolicyNameInternal", summary = "Add a policy set ",
            responses =
                    {
                            @io.swagger.v3.oas.annotations.responses.
                                    ApiResponse(responseCode = "default", description = "Returns newly created policy set")
                    })
    public ResponseDTO<PolicySet>
    create(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
            NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
           @RequestBody(required = true,
                   description = "Request body containing Policy store object") @Valid CreatePolicySetDTO createPolicySetDTO) {
        //rbacHelper.checkPolicySetEditPermission(accountId, null, null);
        PolicySet policySet = createPolicySetDTO.getPolicyset();
        policySet.setAccountId(accountId);
        policySetService.save(policySet);
        return ResponseDTO.newResponse(policySet.toDTO());
    }


    @PUT
    @Path("update")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update an existing policy set", nickname = "updatePolicySet")
    @LogAccountIdentifier
    @Operation(operationId = "updatePolicySet", description = "Update a Policy set", summary = "Update a Policy set",
            responses =
                    {
                            @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "update a existing OOTB Policy",
                                    content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
                    })
    public ResponseDTO<PolicySet>
    updatePolicy(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
            NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
                 @RequestBody(required = true,
                         description = "Request body containing ceViewFolder object") @Valid CreatePolicySetDTO createPolicySetDTO) {
      //  rbacHelper.checkPolicySetEditPermission(accountId, null, null);
        PolicySet policySet = createPolicySetDTO.getPolicyset();
        policySet.toDTO();
        policySet.setAccountId(accountId);
        policySetService.update(policySet);
        return ResponseDTO.newResponse(policySet);
    }


    @DELETE
    @Path("{policySetId}")
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
           @PathParam("policySetId") @Parameter(
                   required = true, description = "Unique identifier for the policy") @NotNull @Valid String uuid) {
       // rbacHelper.checkPolicySetDeletePermission(accountId, null, null);
        boolean result = policySetService.delete(accountId, uuid);
        return ResponseDTO.newResponse(result);
    }
}
