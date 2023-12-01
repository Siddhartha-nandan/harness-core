/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.configmanager.resource;

import static io.harness.idp.common.RbacConstants.IDP_PLUGIN;
import static io.harness.idp.common.RbacConstants.IDP_PLUGIN_EDIT;
import static io.harness.idp.common.RbacConstants.IDP_PLUGIN_TOGGLE;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.idp.configmanager.service.ConfigEnvVariablesService;
import io.harness.idp.configmanager.service.ConfigManagerService;
import io.harness.idp.configmanager.utils.ConfigType;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.spec.server.idp.v1.AppConfigApi;
import io.harness.spec.server.idp.v1.model.AppConfig;
import io.harness.spec.server.idp.v1.model.AppConfigRequest;
import io.harness.spec.server.idp.v1.model.AppConfigResponse;

import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(onConstructor = @__({ @com.google.inject.Inject }))
@NextGenManagerAuth
@Slf4j
public class AppConfigApiImpl implements AppConfigApi {
  private ConfigManagerService configManagerService;
  private ConfigEnvVariablesService configEnvVariablesService;

  @Override
  @NGAccessControlCheck(resourceType = IDP_PLUGIN, permission = IDP_PLUGIN_EDIT)
  public Response saveOrUpdatePluginAppConfig(@Valid AppConfigRequest body, @AccountIdentifier String harnessAccount) {
    try {
      AppConfig appConfig = body.getAppConfig();
      configManagerService.validateSchemaForPlugin(appConfig.getConfigs(), appConfig.getConfigId());
      configEnvVariablesService.validateConfigEnvVariables(appConfig);
      AppConfig savedOrUpdatedAppConfig =
          configManagerService.saveUpdateAndMergeConfigForAccount(appConfig, harnessAccount, ConfigType.PLUGIN);
      AppConfigResponse appConfigResponse = new AppConfigResponse();
      appConfigResponse.appConfig(savedOrUpdatedAppConfig);
      return Response.status(Response.Status.OK).entity(appConfigResponse).build();
    } catch (Exception e) {
      log.error("Error in saving or updating configs for Plugin id - {} in account - {}",
          body.getAppConfig().getConfigId(), harnessAccount, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @Override
  @NGAccessControlCheck(resourceType = IDP_PLUGIN, permission = IDP_PLUGIN_TOGGLE)
  public Response togglePluginForAccount(String pluginId, Boolean isEnabled, @AccountIdentifier String harnessAccount) {
    try {
      AppConfig disabledPluginAppConfig =
          configManagerService.toggleConfigForAccount(harnessAccount, pluginId, isEnabled, ConfigType.PLUGIN);
      configManagerService.mergeAndSaveAppConfig(harnessAccount);
      AppConfigResponse appConfigResponse = new AppConfigResponse();
      appConfigResponse.appConfig(disabledPluginAppConfig);
      return Response.status(Response.Status.OK).entity(appConfigResponse).build();
    } catch (Exception e) {
      log.error("Error in enabling - {} for plugin id - {} in account - {}", isEnabled, pluginId, harnessAccount, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }
}
