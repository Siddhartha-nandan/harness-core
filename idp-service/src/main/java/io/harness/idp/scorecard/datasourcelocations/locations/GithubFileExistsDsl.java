/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasourcelocations.locations;

import static io.harness.idp.common.Constants.DSL_RESPONSE;
import static io.harness.idp.common.Constants.ERROR_MESSAGE_KEY;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPO_SCM;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.common.GsonUtils;
import io.harness.idp.scorecard.datapoints.entity.DataPointEntity;
import io.harness.idp.scorecard.datasourcelocations.beans.ApiRequestDetails;
import io.harness.idp.scorecard.datasourcelocations.client.DslClient;
import io.harness.idp.scorecard.datasourcelocations.client.DslClientFactory;
import io.harness.idp.scorecard.datasourcelocations.entity.DataSourceLocationEntity;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.IDP)
public class GithubFileExistsDsl implements DataSourceLocation {
  private static final String GITHUB_FILE_EXISTS_REPLACER = "{GITHUB_FILE_EXISTS_REPLACER}";
  DslClientFactory dslClientFactory;
  @Override
  public Map<String, Object> fetchData(String accountIdentifier, BackstageCatalogEntity backstageCatalogEntity,
      DataSourceLocationEntity dataSourceLocationEntity, Map<DataPointEntity, Set<String>> dataPointsAndInputValues,
      Map<String, String> replaceableHeaders, Map<String, String> possibleReplaceableRequestBodyPairs,
      Map<String, String> possibleReplaceableUrlPairs) {
    ApiRequestDetails apiRequestDetails = fetchApiRequestDetails(dataSourceLocationEntity);
    Map<String, String> headers = apiRequestDetails.getHeaders();
    matchAndReplaceHeaders(headers, replaceableHeaders);
    String requestBody =
        constructRequestBody(apiRequestDetails, possibleReplaceableRequestBodyPairs, dataPointsAndInputValues);
    DslClient dslClient =
        dslClientFactory.getClient(accountIdentifier, possibleReplaceableRequestBodyPairs.get(REPO_SCM));

    Response response;
    Map<String, Object> data = new HashMap<>();
    response = dslClient.call(
        accountIdentifier, apiRequestDetails.getUrl(), apiRequestDetails.getMethod(), headers, requestBody);
    if (response.getStatus() == 200) {
      data.put(DSL_RESPONSE, GsonUtils.convertJsonStringToObject(response.getEntity().toString(), Map.class));
    } else {
      data.put(ERROR_MESSAGE_KEY, ((Map<String, Object>) response.getEntity()).get("message"));
    }
    return data;
  }

  @Override
  public String replaceRequestBodyInputValuePlaceholdersIfAny(
      Map<String, Set<String>> dataPointsAndInputValues, String requestBody) {
    return requestBody.replace(GITHUB_FILE_EXISTS_REPLACER, "HEAD:");
  }
}
