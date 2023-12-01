package io.harness.ng.core.certificates.resource;


import com.google.inject.Inject;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.certificates.service.CertificateService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.certificates.entities.Certificate;
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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

import java.util.Objects;

import static io.harness.NGCommonEntityConstants.*;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;

@OwnedBy(PL)
@Path("certificates")
@Api("certificates")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@Tag(name = "Certificates", description = "This contains APIs related to certificates as defined in Harness")
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
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found",
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
@AllArgsConstructor(onConstructor = @__({ @Inject}))
@NextGenManagerAuth
@Slf4j

public class CertificateResource {

    @Inject private CertificateService CertificateService;

    @POST
    @Consumes({"application/json", "application/yaml"})
    @ApiOperation(value = "Create a certificates", nickname = "CreateCertificates")
    @Operation(operationId = "CreateCertificates", summary = "Creates a certificates at given Scope",
            responses =
                    {
                            @io.swagger.v3.oas.annotations.responses.
                                    ApiResponse(responseCode = "default", description = "Returns the created certificates details")
                    })

    public ResponseDTO<Certificate> create(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
            NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
                                           @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
                                           @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
                                              NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
                                           @RequestBody(required = true,
                                              description = "Details required to create the certificates") @Valid @NotNull Certificate certificate)
    {
        if (!Objects.equals(orgIdentifier, certificate.getOrgIdentifier())
                || !Objects.equals(projectIdentifier, certificate.getProjectIdentifier())) {
            throw new InvalidRequestException("Invalid request, scope in payload and params do not match.", USER);
        }


        return ResponseDTO.newResponse(CertificateService.create(certificate));
    }




}
