/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.configmanager.resource;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.idp.configmanager.ConfigType;
import io.harness.idp.configmanager.service.ConfigManagerService;
import io.harness.spec.server.idp.v1.AppConfigApi;
import io.harness.spec.server.idp.v1.model.AppConfig;
import io.harness.spec.server.idp.v1.model.AppConfigRequest;
import io.harness.spec.server.idp.v1.model.AppConfigResponse;

import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @com.google.inject.Inject }))
@Slf4j
public class AppConfigApiImpl implements AppConfigApi {
  private ConfigManagerService configManagerService;

  @Override
  public Response saveOrUpdatePluginAppConfig(@Valid AppConfigRequest body, String harnessAccount) {
    try {
      AppConfig updatedAppConfig =
          configManagerService.saveOrUpdateConfigForAccount(body.getAppConfig(), harnessAccount, ConfigType.PLUGIN);
      configManagerService.mergeAndSaveAppConfig(harnessAccount);
      AppConfigResponse appConfigResponse = new AppConfigResponse();
      appConfigResponse.appConfig(updatedAppConfig);
      return Response.status(Response.Status.OK).entity(appConfigResponse).build();
    } catch (Exception e) {
      log.error(e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @Override
  public Response togglePluginForAccount(String pluginId, Boolean isEnabled, String harnessAccount) {
    try {
      AppConfig disabledPluginAppConfig =
          configManagerService.toggleConfigForAccount(harnessAccount, pluginId, isEnabled, ConfigType.PLUGIN);
      configManagerService.mergeAndSaveAppConfig(harnessAccount);
      AppConfigResponse appConfigResponse = new AppConfigResponse();
      appConfigResponse.appConfig(disabledPluginAppConfig);
      return Response.status(Response.Status.OK).entity(appConfigResponse).build();
    } catch (Exception e) {
      log.error(e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }
}
