/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datapoints.parser.factory;

import static io.harness.idp.scorecard.datapoints.constants.DataPoints.ISSUES_COUNT;
import static io.harness.idp.scorecard.datapoints.constants.DataPoints.ISSUES_OPEN_CLOSE_RATIO;
import static io.harness.idp.scorecard.datapoints.constants.DataPoints.MEAN_TIME_TO_RESOLVE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.datapoints.parser.DataPointParser;
import io.harness.idp.scorecard.datapoints.parser.jira.JiraIssuesCountParser;
import io.harness.idp.scorecard.datapoints.parser.jira.JiraIssuesOpenCloseRatioParser;
import io.harness.idp.scorecard.datapoints.parser.jira.JiraMeanTimeToResolveParser;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.IDP)
public class JiraDataPointParserFactory implements DataPointParserFactory {
  JiraMeanTimeToResolveParser jiraMeanTimeToResolveParser;
  JiraIssuesCountParser jiraIssuesCountParser;
  JiraIssuesOpenCloseRatioParser jiraIssuesOpenCloseRatioParser;

  @Override
  public DataPointParser getParser(String identifier) {
    switch (identifier) {
      case MEAN_TIME_TO_RESOLVE:
        return jiraMeanTimeToResolveParser;
      case ISSUES_COUNT:
        return jiraIssuesCountParser;
      case ISSUES_OPEN_CLOSE_RATIO:
        return jiraIssuesOpenCloseRatioParser;
      default:
        throw new UnsupportedOperationException(String.format("Could not find DataPoint parser for %s", identifier));
    }
  }
}
