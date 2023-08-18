/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.scorecard.datapointsdata.dsldataprovider.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.datapointsdata.datapointvalueparser.ValueParserConstants;

import com.google.gson.Gson;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

@OwnedBy(HarnessTeam.IDP)
@Slf4j
@UtilityClass
public class DslDataProviderUtil {
  public String getRunSequenceForPipelineExecution(Object executionResponse) {
    String runSequence = null;
    String jsonInStringExecutions = new Gson().toJson(executionResponse);
    JSONObject listOfPipelineExecutions = new JSONObject(jsonInStringExecutions);
    JSONArray pipelineExecutions = listOfPipelineExecutions.getJSONArray(ValueParserConstants.CONTENT_KEY);
    if (pipelineExecutions.length() > 0) {
      JSONObject latestPipelineExecution = pipelineExecutions.getJSONObject(0);
      runSequence = latestPipelineExecution.getString("runSequence");
    }
    return runSequence;
  }
}
