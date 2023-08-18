/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.scorecard.datapointsdata.datapointvalueparser.factory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.datapointsdata.datapointvalueparser.ValueParserConstants;
import io.harness.idp.scorecard.datapointsdata.datapointvalueparser.base.PipelineTestSummaryReport;
import io.harness.idp.scorecard.datapointsdata.datapointvalueparser.impl.PipelineTestSummaryReportParser;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class PipelineTestSummaryReportResponseFactory {
  PipelineTestSummaryReportParser pipelineTestSummaryReportParser;

  public PipelineTestSummaryReport getResponseParser(String identifier) {
    switch (identifier) {
      case ValueParserConstants.CI_PIPELINE_TEST_FAILING_IS_ZERO:
        return pipelineTestSummaryReportParser;
      default:
        throw new UnsupportedOperationException(String.format("Could not find response parser for %s", identifier));
    }
  }
}
