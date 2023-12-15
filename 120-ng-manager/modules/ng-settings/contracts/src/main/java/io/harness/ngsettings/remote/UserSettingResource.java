package io.harness.ngsettings.remote;

import static io.harness.NGCommonEntityConstants.*;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingConstants;
import io.harness.ngsettings.dto.*;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import retrofit2.http.Body;

@OwnedBy(PL)
@Api("/user-settings")
@Path("/user-settings")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Tag(name = "UserSetting", description = "This contains APIs related to User Settings as defined in Harness")
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
public interface UserSettingResource {
  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Resolves and gets a user setting value by Identifier", nickname = "getUserSettingValue")
  @Operation(operationId = "getUserSettingValue", summary = "Get a user setting value by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "This returns a setting value by the Identifier")
      })
  ResponseDTO<SettingValueResponseDTO>
  get(@Parameter(description = "This is the Identifier of the Entity", required = true) @NotNull @PathParam(
          NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = USER_ID, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.USER_ID) String userIdentifier);

  @GET
  @ApiOperation(value = "Get list of user settings", nickname = "getUserSettingsList")
  @Operation(operationId = "getUserSettingsList", summary = "Get list of user settings under the specified category",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "This contains a list of user Settings")
      })
  ResponseDTO<List<UserSettingResponseDTO>>
  list(@Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
           NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = USER_ID, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.USER_ID) String userIdentifier,
      @Parameter(description = SettingConstants.CATEGORY) @NotNull @QueryParam(
          SettingConstants.CATEGORY_KEY) SettingCategory category,
      @Parameter(description = SettingConstants.GROUP_ID) @QueryParam(
          SettingConstants.GROUP_KEY) String groupIdentifier);


  @PUT
  @ApiOperation(value = "Updates the user settings", nickname = "updateUserSettingValue")
  @Operation(operationId = "updateUserSettingValue", summary = "Update user settings",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "This updates the user settings")
      })
  ResponseDTO<List<UserSettingUpdateResponseDTO>>
  update(@Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = USER_ID, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.USER_ID) String userIdentifier,
      @RequestBody(description = SettingConstants.SETTING_UPDATE_REQUEST_LIST) @Body
      @NotNull List<UserSettingRequestDTO> userSettingRequestDTOList);
}
