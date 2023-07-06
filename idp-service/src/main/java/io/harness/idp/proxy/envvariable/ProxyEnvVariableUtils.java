/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.proxy.envvariable;

import static io.harness.idp.common.Constants.PROXY_ENV_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.envvariable.service.BackstageEnvVariableService;
import io.harness.spec.server.idp.v1.model.BackstageEnvConfigVariable;
import io.harness.spec.server.idp.v1.model.BackstageEnvVariable;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.json.JSONObject;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.IDP)
public class ProxyEnvVariableUtils {
  BackstageEnvVariableService backstageEnvVariableService;

  public void createOrUpdateHostProxyEnvVariable(String accountIdentifier, Map<String, Boolean> hostProxyMap) {
    Optional<BackstageEnvVariable> envVariableOpt =
        backstageEnvVariableService.findByEnvNameAndAccountIdentifier(PROXY_ENV_NAME, accountIdentifier);
    if (envVariableOpt.isPresent()) {
      BackstageEnvConfigVariable envVariable = (BackstageEnvConfigVariable) envVariableOpt.get();
      String hostProxyString = envVariable.getValue();
      JSONObject hostProxyObj = new JSONObject(hostProxyString);
      hostProxyMap.forEach(hostProxyObj::put);
      envVariable.setValue(hostProxyObj.toString());
      backstageEnvVariableService.update(envVariable, accountIdentifier);
    } else {
      BackstageEnvConfigVariable envVariable = new BackstageEnvConfigVariable();
      JSONObject proxyIntegrationObj = new JSONObject();
      hostProxyMap.forEach(proxyIntegrationObj::put);
      envVariable.setType(BackstageEnvVariable.TypeEnum.CONFIG);
      envVariable.setEnvName(PROXY_ENV_NAME);
      envVariable.setValue(proxyIntegrationObj.toString());
      backstageEnvVariableService.create(envVariable, accountIdentifier);
    }
  }

  public void removeFromHostProxyEnvVariable(String accountIdentifier, Set<String> hostsToBeRemoved) {
    Optional<BackstageEnvVariable> envVariableOpt =
        backstageEnvVariableService.findByEnvNameAndAccountIdentifier(PROXY_ENV_NAME, accountIdentifier);
    if (envVariableOpt.isPresent()) {
      BackstageEnvConfigVariable envVariable = (BackstageEnvConfigVariable) envVariableOpt.get();
      String hostProxyString = envVariable.getValue();
      JSONObject hostProxyObj = new JSONObject(hostProxyString);
      hostsToBeRemoved.forEach(hostProxyObj::remove);
      envVariable.setValue(hostProxyObj.toString());
      backstageEnvVariableService.update(envVariable, accountIdentifier);
    }
  }
}
