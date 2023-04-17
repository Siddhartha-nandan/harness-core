package io.harness.ccm.service.impl;

import static io.harness.ccm.views.graphql.QLCEViewAggregateOperation.SUM;

import io.harness.ccm.graphql.query.perspectives.OverviewQuery;
import io.harness.ccm.remote.beans.CostOverviewDTO;
import io.harness.ccm.service.intf.CCMActiveSpendService;
import io.harness.ccm.service.intf.CCMOverviewService;
import io.harness.ccm.views.dto.CcmOverviewDTO;
import io.harness.ccm.views.dto.TimeSeriesDataPoints;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.graphql.*;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CCMOverviewServiceImpl implements CCMOverviewService {
  @Inject CCMActiveSpendService activeSpendService;
  @Inject OverviewQuery overviewQuery;

  @Override
  public CcmOverviewDTO getCCMAccountOverviewData(
      String accountId, long startTime, long endTime, QLCEViewTimeGroupType groupBy) {
    CostOverviewDTO costOverview = getTotalCostStats(accountId, startTime, endTime);
    return CcmOverviewDTO.builder()
        .costPerDay(getCostTimeSeriesData(accountId, startTime, endTime, groupBy))
        .totalCost(costOverview.getValue())
        .totalCostTrend(costOverview.getStatsTrend())
        .build();
  }

  @Override
  public List<TimeSeriesDataPoints> getCostTimeSeriesData(
      String accountId, long startTime, long endTime, QLCEViewTimeGroupType groupBy) {
    List<QLCEViewAggregation> aggregations =
        Collections.singletonList(QLCEViewAggregation.builder().columnName("cost").operationType(SUM).build());
    List<QLCEViewGroupBy> groupByTime =
        Collections.singletonList(QLCEViewGroupBy.builder()
                                      .timeTruncGroupBy(QLCEViewTimeTruncGroupBy.builder().resolution(groupBy).build())
                                      .build());
    List<QLCEViewFilterWrapper> timeFilters = new ArrayList<>();
    timeFilters.add(QLCEViewFilterWrapper.builder()
                        .timeFilter(QLCEViewTimeFilter.builder()
                                        .field(QLCEViewFieldInput.builder()
                                                   .fieldId("startTime")
                                                   .fieldName("startTime")
                                                   .identifier(ViewFieldIdentifier.COMMON)
                                                   .build())
                                        .operator(QLCEViewTimeFilterOperator.AFTER)
                                        .value(startTime)
                                        .build())
                        .build());
    timeFilters.add(QLCEViewFilterWrapper.builder()
                        .timeFilter(QLCEViewTimeFilter.builder()
                                        .field(QLCEViewFieldInput.builder()
                                                   .fieldId("startTime")
                                                   .fieldName("startTime")
                                                   .identifier(ViewFieldIdentifier.COMMON)
                                                   .build())
                                        .operator(QLCEViewTimeFilterOperator.BEFORE)
                                        .value(endTime)
                                        .build())
                        .build());
    return overviewQuery.totalCostTimeSeriesStats(aggregations, timeFilters, groupByTime, accountId).getStats();
  }

  @Override
  public CostOverviewDTO getTotalCostStats(String accountId, long startTime, long endTime) {
    return activeSpendService.getActiveSpendStats(startTime, endTime, accountId);
  }

  @Override
  public Integer getRecommendationsCount(String accountId) {
    return null;
  }
}
