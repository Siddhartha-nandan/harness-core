/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.platform.remote;

import io.harness.security.annotations.PublicApi;
import io.harness.version.VersionInfoManagerV2;
import io.harness.version.VersionInfoV2;

import lombok.extern.slf4j.Slf4j;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.WebApplicationException;
import java.util.Map;
import io.dropwizard.jersey.errors.ErrorMessage;

@Api("version")
@Path("/v2/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes({"application/json", "application/yaml"})
@Slf4j
@PublicApi
public class VersionInfoResourceV2 {

  private final VersionInfoManagerV2 versionInfoManager;

  @Inject
  public VersionInfoResourceV2(VersionInfoManagerV2 versionInfoManager) {
    this.versionInfoManager = versionInfoManager;
  }

  @GET
  public VersionInfoV2 getVersionInfo() {
    try {
      VersionInfoV2 versionInfo = versionInfoManager.getVersionInfo();
      return versionInfo;
    } catch (Exception e) {
      log.error("Failed to retrieve version info: {}", e.getMessage(), e);
      ErrorMessage errorMessage = new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Failed to retrieve version info: " + e.getMessage());
      throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build());
    }
  }
}