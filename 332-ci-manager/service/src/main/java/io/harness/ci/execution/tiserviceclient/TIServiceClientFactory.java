/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.tiserviceclient;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.beans.entities.TIServiceConfig;
import io.harness.network.Http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@OwnedBy(HarnessTeam.CI)
public class TIServiceClientFactory implements Provider<TIServiceClient> {
  private TIServiceConfig tiConfig;

  @Inject
  public TIServiceClientFactory(TIServiceConfig tiConfig) {
    this.tiConfig = tiConfig;
  }

  @Override
  public TIServiceClient get() {
    Gson gson = new GsonBuilder().setLenient().create();
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(tiConfig.getBaseUrl())
                            .client(Http.getUnsafeOkHttpClient(this.getInternalUrl()))
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
    return retrofit.create(TIServiceClient.class);
  }

  private String getInternalUrl() {
    if (!isEmpty(this.tiConfig.getInternalUrl())) {
      return this.tiConfig.getInternalUrl();
    }
    return this.tiConfig.getBaseUrl();
  }
}
