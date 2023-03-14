/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.provision.service;

import static io.harness.idp.provision.ProvisionConstants.ACCOUNT_ID;
import static io.harness.idp.provision.ProvisionConstants.NAMESPACE;
import static io.harness.idp.provision.ProvisionConstants.PROVISION_MODULE_CONFIG;
import static io.harness.remote.client.CGRestUtils.getResponse;

import io.harness.client.NgConnectorManagerClient;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.idp.provision.ProvisionModuleConfig;
import io.harness.retry.RetryHelper;
import io.harness.security.SecurityContextBuilder;

import com.google.inject.name.Named;
import io.github.resilience4j.retry.Retry;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http2.ConnectionShutdownException;
import okhttp3.internal.http2.StreamResetException;

@Slf4j
public class ProvisionServiceImpl implements ProvisionService {
  @Inject @Named(PROVISION_MODULE_CONFIG) ProvisionModuleConfig provisionModuleConfig;
  private final Retry retry = buildRetryAndRegisterListeners();
  private final MediaType APPLICATION_JSON = MediaType.parse("application/json");
  @Inject NgConnectorManagerClient ngConnectorManagerClient;

  @Override
  public void triggerPipeline(String accountIdentifier, String namespace) {
    makeTriggerApi(accountIdentifier, namespace);
  }

  @Override
  public void checkUserAuthorization() {
    String userId = SecurityContextBuilder.getPrincipal().getName();
    boolean isAuthorized = getResponse(ngConnectorManagerClient.isHarnessSupportUser(userId));
    if (!isAuthorized) {
      String errorMessage = String.format("User : %s not allowed to provision IDP", userId);
      log.error(errorMessage);
      throw new AccessDeniedException(errorMessage, WingsException.USER);
    }
  }

  private void makeTriggerApi(String accountIdentifier, String namespace) {
    Request request = createHttpRequest(accountIdentifier, namespace);
    OkHttpClient client = new OkHttpClient();
    Supplier<Response> response = Retry.decorateSupplier(retry, () -> {
      try {
        return client.newCall(request).execute();
      } catch (IOException e) {
        String errMessage = "Error occurred while reaching pipeline trigger API";
        log.error(errMessage, e);
        throw new InvalidRequestException(errMessage);
      }
    });

    if (!response.get().isSuccessful()) {
      throw new InvalidRequestException("Pipeline Trigger http call failed");
    }
  }

  private Request createHttpRequest(String accountIdentifier, String namespace) {
    String url = provisionModuleConfig.getTriggerPipelineUrl();

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(ACCOUNT_ID, accountIdentifier);
    jsonObject.put(NAMESPACE, namespace);

    RequestBody requestBody = RequestBody.create(jsonObject.toString(), APPLICATION_JSON);

    return new Request.Builder().url(url).post(requestBody).build();
  }

  private Retry buildRetryAndRegisterListeners() {
    final Retry exponentialRetry = RetryHelper.getExponentialRetry(this.getClass().getSimpleName(),
        new Class[] {ConnectException.class, TimeoutException.class, ConnectionShutdownException.class,
            StreamResetException.class});
    RetryHelper.registerEventListeners(exponentialRetry);
    return exponentialRetry;
  }
}
