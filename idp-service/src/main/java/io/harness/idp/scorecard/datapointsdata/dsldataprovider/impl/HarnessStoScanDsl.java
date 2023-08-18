/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.scorecard.datapointsdata.dsldataprovider.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboard.DashboardResourceClient;
import io.harness.idp.scorecard.datapoints.parser.DataPointParser;
import io.harness.idp.scorecard.datapoints.parser.DataPointParserFactory;
import io.harness.idp.scorecard.datapointsdata.datapointvalueparser.factory.PipelineInfoResponseFactory;
import io.harness.idp.scorecard.datapointsdata.dsldataprovider.DslConstants;
import io.harness.idp.scorecard.datapointsdata.dsldataprovider.base.DslDataProvider;
import io.harness.idp.scorecard.datapointsdata.utils.DslUtils;
import io.harness.ng.core.dashboard.DeploymentsInfo;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.pms.pipeline.PMSPipelineResponseDTO;
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
public class HarnessStoScanDsl implements DslDataProvider {
  PipelineServiceClient pipelineServiceClient;
  DataPointParserFactory dataPointParserFactory;
  PipelineInfoResponseFactory pipelineInfoFactory;
  DashboardResourceClient dashboardResourceClient;

  @Override
  public Map<String, Object> getDslData(String accountIdentifier, DataSourceDataPointInfo dataSourceDataPointInfo) {
    Map<String, Object> returnData = new HashMap<>();

    // ci pipeline detail
    Map<String, String> ciIdentifiers =
        DslUtils.getCiPipelineUrlIdentifiers(dataSourceDataPointInfo.getCiPipelineUrl());

    List<DataPointInputValues> dataPointInputValuesList =
        dataSourceDataPointInfo.getDataSourceLocation().getDataPoints();

    PMSPipelineResponseDTO responseCI = NGRestUtils.getResponse(
        pipelineServiceClient.getPipelineByIdentifier(ciIdentifiers.get(DslConstants.CI_PIPELINE_IDENTIFIER_KEY),
            ciIdentifiers.get(DslConstants.CI_ACCOUNT_IDENTIFIER_KEY),
            ciIdentifiers.get(DslConstants.CI_ORG_IDENTIFIER_KEY),
            ciIdentifiers.get(DslConstants.CI_PROJECT_IDENTIFIER_KEY), null, null, false));

    // cd pipeline detail
    Map<String, String> serviceIdentifiers =
        DslUtils.getCdServiceUrlIdentifiers(dataSourceDataPointInfo.getServiceUrl());
    long currentTime = System.currentTimeMillis();
    DeploymentsInfo serviceDeploymentInfo = NGRestUtils
                                                .getResponse(dashboardResourceClient.getDeploymentsByServiceId(
                                                    serviceIdentifiers.get(DslConstants.CD_ACCOUNT_IDENTIFIER_KEY),
                                                    serviceIdentifiers.get(DslConstants.CD_ORG_IDENTIFIER_KEY),
                                                    serviceIdentifiers.get(DslConstants.CD_PROJECT_IDENTIFIER_KEY),
                                                    serviceIdentifiers.get(DslConstants.CD_SERVICE_IDENTIFIER_KEY),
                                                    currentTime - DslConstants.ThirtyDaysInMillis, currentTime))
                                                .get();
    String cdPipelineId = null;
    if (!serviceDeploymentInfo.getDeployments().isEmpty()) {
      cdPipelineId = serviceDeploymentInfo.getDeployments().get(0).getPipelineIdentifier();
    }

    PMSPipelineResponseDTO responseCD = NGRestUtils.getResponse(pipelineServiceClient.getPipelineByIdentifier(
        cdPipelineId, serviceIdentifiers.get(DslConstants.CD_ACCOUNT_IDENTIFIER_KEY),
        serviceIdentifiers.get(DslConstants.CD_ORG_IDENTIFIER_KEY),
        serviceIdentifiers.get(DslConstants.CD_PROJECT_IDENTIFIER_KEY), null, null, false));

    for (DataPointInputValues dataPointInputValues : dataPointInputValuesList) {
      String dataPointIdentifier = dataPointInputValues.getDataPointIdentifier();
      Set<String> inputValues = new HashSet<>(dataPointInputValues.getValues());
      if (!inputValues.isEmpty()) {
        DataPointParser dataPointParser = dataPointParserFactory.getParser(dataPointIdentifier);
        String key = dataPointParser.getReplaceKey();
        log.info("replace key : {}, value: [{}]", key, inputValues);
      }
      returnData.putAll(pipelineInfoFactory.getResponseParser(dataPointIdentifier)
                            .getParsedValue(responseCI, responseCD, dataPointIdentifier));
    }
    return returnData;
  }
}
