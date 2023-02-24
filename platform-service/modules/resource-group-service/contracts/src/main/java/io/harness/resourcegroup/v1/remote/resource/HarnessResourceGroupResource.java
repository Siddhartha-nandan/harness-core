/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.resourcegroup.v1.remote.resource;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_CODE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.commons.exceptions.AccessDeniedErrorDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.client.annotation.FeatureRestrictionCheck;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.resourcegroup.v1.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroup.v1.remote.dto.ResourceGroupRequest;
import io.harness.resourcegroup.v1.remote.dto.ResourceGroupResponse;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/resourcegroup")
@Path("resourcegroup")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@Tag(name = "Harness Resource Group", description = "This contains APIs specific to the Harness Resource Group")
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = BAD_REQUEST_CODE, description = BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
    })
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error"),
          @ApiResponse(code = 403, response = AccessDeniedErrorDTO.class, message = "Unauthorized")
    })
@NextGenManagerAuth
@OwnedBy(HarnessTeam.PL)
public interface HarnessResourceGroupResource {
  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get a resource group by Identifier", nickname = "getResourceGroup")
  @Operation(operationId = "getResourceGroup", summary = "Get a resource group by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "This returns a Resource Group specific to the Identifier")
      })
  @Deprecated
  ResponseDTO<ResourceGroupResponse>
  get(@Parameter(description = "This is the Identifier of the Entity", required = true) @NotNull @PathParam(
          NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @GET
  @Path("internal/{identifier}")
  @ApiOperation(
      value = "Get a resource group by identifier internal", nickname = "getResourceGroupInternal", hidden = true)
  @Operation(operationId = "getResourceGroupInternal", summary = "Get a resource group by Identifier internal",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Internal API to return Resource Group by Identifier")
      },
      hidden = true)
  @InternalApi
  ResponseDTO<ResourceGroupResponse>
  getInternal(@Parameter(description = "This is the Identifier of the Entity") @NotNull @PathParam(
                  NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @GET
  @ApiOperation(value = "Get list of resource groups", nickname = "getResourceGroupList")
  @Operation(operationId = "getResourceGroupList", summary = "Get list of resource groups",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "This contains a list of Resource Groups")
      })
  @Deprecated
  ResponseDTO<PageResponse<ResourceGroupResponse>>
  list(@Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
           NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Parameter(
          description =
              "Details of all the resource groups having this string in their name or identifier will be returned.")
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @BeanParam PageRequest pageRequest);

  @POST
  @Path("filter")
  @ApiOperation(value = "Get filtered resource group list", nickname = "getFilterResourceGroupList")
  @Operation(operationId = "getFilterResourceGroupList", summary = "This fetches a filtered list of Resource Groups",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "This fetches the list of Resource Groups filtered by multiple fields.")
      })
  @Deprecated
  ResponseDTO<PageResponse<ResourceGroupResponse>>
  list(@RequestBody(description = "Filter Resource Groups based on multiple parameters",
           required = true) @NotNull ResourceGroupFilterDTO resourceGroupFilterDTO,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier, @BeanParam PageRequest pageRequest);

  @POST
  @ApiOperation(value = "Create a resource group", nickname = "createResourceGroup")
  @Operation(operationId = "createResourceGroup", summary = "Create a resource group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Successfully created a Resource Group")
      })
  @FeatureRestrictionCheck(FeatureRestrictionName.CUSTOM_RESOURCE_GROUPS)
  @Deprecated
  ResponseDTO<ResourceGroupResponse>
  create(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @RequestBody(description = "This contains the details required to create a Resource Group",
          required = true) @Valid ResourceGroupRequest resourceGroupRequest);

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a resource group", nickname = "updateResourceGroup")
  @Operation(operationId = "updateResourceGroup", summary = "Update a resource group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Successfully updated a Resource Group")
      })
  @Deprecated
  ResponseDTO<ResourceGroupResponse>
  update(@Parameter(description = "Identifier for the entity") @NotNull @PathParam(
             NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @RequestBody(description = "This contains the details required to create a Resource Group",
          required = true) @Valid ResourceGroupRequest resourceGroupRequest);

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a resource group", nickname = "deleteResourceGroup")
  @Operation(operationId = "deleteResourceGroup", summary = "Delete a resource group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Successfully deleted a Resource Group")
      })
  @Produces("application/json")
  @Consumes()
  @Deprecated
  ResponseDTO<Boolean>
  delete(@NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);
}
