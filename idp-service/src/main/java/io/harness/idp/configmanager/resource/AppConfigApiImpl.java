/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.configmanager.resource;

import static java.lang.String.format;

import io.harness.eraro.ResponseMessage;
import io.harness.idp.configmanager.service.ConfigManagerService;
import io.harness.spec.server.idp.v1.AppconfigApi;
import io.harness.spec.server.idp.v1.model.AppConfig;
import io.harness.spec.server.idp.v1.model.AppConfigRequest;

import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @com.google.inject.Inject }))
public class AppConfigApiImpl implements AppconfigApi {
  private ConfigManagerService configManagerService;

  private static String PLUGIN_CONFIG_ALREADY_PRESENT =
      "Plugin config for plugin - %s is already present for account - %s";

  @Override
  public Response getPluginAppConfig(String pluginName, String harnessAccount) {
    try {
      AppConfig appConfig = configManagerService.getPluginConfig(harnessAccount, pluginName);
      return Response.status(Response.Status.OK).entity(appConfig).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @Override
  public Response insertPluginAppConfig(@Valid AppConfigRequest body, String harnessAccount) {
    try {
      AppConfig insertedAppConfig = configManagerService.savePluginConfig(body, harnessAccount);
      return Response.status(Response.Status.OK).entity(insertedAppConfig).build();
    } catch (DuplicateKeyException e) {
      String logMessage = format(PLUGIN_CONFIG_ALREADY_PRESENT, body.getAppConfig().getPluginName(), harnessAccount);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(logMessage).build())
          .build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @Override
  public Response updatePluginConfigs(@Valid AppConfigRequest body, String harnessAccount) {
    try {
      AppConfig updatedAppConfig = configManagerService.updatePluginConfig(body, harnessAccount);
      return Response.status(Response.Status.OK).entity(updatedAppConfig).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @Override
  public Response togglePlginForAccount(String harnessAccount, String pluginName, Boolean isEnabled) {
    try {
      AppConfig disabledPluginAppConfig =
          configManagerService.updatePluginEnablement(harnessAccount, pluginName, isEnabled);
      return Response.status(Response.Status.OK).entity(disabledPluginAppConfig).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }
}
