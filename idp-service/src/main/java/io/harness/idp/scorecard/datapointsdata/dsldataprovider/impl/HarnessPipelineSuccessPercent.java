/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.scorecard.datapointsdata.dsldataprovider.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.datapoints.parser.DataPointParser;
import io.harness.idp.scorecard.datapoints.parser.DataPointParserFactory;
import io.harness.idp.scorecard.datapointsdata.datapointvalueparser.factory.PipelineSuccessPercentResponseFactory;
import io.harness.idp.scorecard.datapointsdata.dsldataprovider.DslConstants;
import io.harness.idp.scorecard.datapointsdata.dsldataprovider.base.DslDataProvider;
import io.harness.idp.scorecard.datapointsdata.utils.DslUtils;
import io.harness.pipeline.dashboards.PMSDashboardResourceClient;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.pms.dashboard.DashboardPipelineHealthInfo;
import io.harness.remote.client.NGRestUtils;
import io.harness.spec.server.idp.v1.model.DataPointInputValues;
import io.harness.spec.server.idp.v1.model.DataSourceDataPointInfo;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.IDP)
@Slf4j
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class HarnessPipelineSuccessPercent implements DslDataProvider {
  PipelineServiceClient pipelineServiceClient;
  DataPointParserFactory dataPointParserFactory;
  PMSDashboardResourceClient pmsDashboardResourceClient;
  PipelineSuccessPercentResponseFactory pipelineSuccessPercentResponseFactory;

  @Override
  public Map<String, Object> getDslData(String accountIdentifier, DataSourceDataPointInfo dataSourceDataPointInfo) {
    Map<String, String> ciIdentifiers =
        DslUtils.getCiPipelineUrlIdentifiers(dataSourceDataPointInfo.getCiPipelineUrl());

    long currentTime = System.currentTimeMillis();

    DashboardPipelineHealthInfo dashboard = null;

    try {
      dashboard = NGRestUtils.getResponse(
          pmsDashboardResourceClient.fetchPipelinedHealth(ciIdentifiers.get(DslConstants.CI_ACCOUNT_IDENTIFIER_KEY),
              ciIdentifiers.get(DslConstants.CI_ORG_IDENTIFIER_KEY),
              ciIdentifiers.get(DslConstants.CI_PROJECT_IDENTIFIER_KEY),
              ciIdentifiers.get(DslConstants.CI_PIPELINE_IDENTIFIER_KEY), null,
              currentTime - DslConstants.SevenDaysInMillis, currentTime));
    } catch (Exception e) {
      log.error(
          String.format(
              "Error in getting the ci pipeline dash board summary info of success percent in seven days check in account - %s, org - %s, project - %s, and pipeline - %s",
              ciIdentifiers.get(DslConstants.CI_ACCOUNT_IDENTIFIER_KEY),
              ciIdentifiers.get(DslConstants.CI_ORG_IDENTIFIER_KEY),
              ciIdentifiers.get(DslConstants.CI_PROJECT_IDENTIFIER_KEY),
              ciIdentifiers.get(DslConstants.CI_PIPELINE_IDENTIFIER_KEY)),
          e);
    }

    Map<String, Object> returnData = new HashMap<>();
    List<DataPointInputValues> dataPointInputValuesList =
        dataSourceDataPointInfo.getDataSourceLocation().getDataPoints();

    for (DataPointInputValues dataPointInputValues : dataPointInputValuesList) {
      String dataPointIdentifier = dataPointInputValues.getDataPointIdentifier();
      Set<String> inputValues = new HashSet<>(dataPointInputValues.getValues());
      if (!inputValues.isEmpty()) {
        DataPointParser dataPointParser = dataPointParserFactory.getParser(dataPointIdentifier);
        String key = dataPointParser.getReplaceKey();
        log.info("replace key : {}, value: [{}]", key, inputValues);
      }
      returnData.putAll(pipelineSuccessPercentResponseFactory.getResponseParser(dataPointIdentifier)
                            .getParsedValue(dashboard, dataPointIdentifier));
    }

    return returnData;
  }
}
