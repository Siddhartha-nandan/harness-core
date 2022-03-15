/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo.AppMetricInfoDTO;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.CVDataCollectionInfo;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.CustomHealthDataCollectionInfo;
import io.harness.cvng.beans.CustomHealthDataCollectionInfo.CustomHealthMetricInfo;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.DatadogLogDataCollectionInfo;
import io.harness.cvng.beans.DatadogMetricsDataCollectionInfo;
import io.harness.cvng.beans.DynatraceDataCollectionInfo;
import io.harness.cvng.beans.ErrorTrackingDataCollectionInfo;
import io.harness.cvng.beans.K8ActivityDataCollectionInfo;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.MetricPackDTO.MetricDefinitionDTO;
import io.harness.cvng.beans.MetricResponseMappingDTO;
import io.harness.cvng.beans.NewRelicDataCollectionInfo;
import io.harness.cvng.beans.PrometheusDataCollectionInfo;
import io.harness.cvng.beans.SplunkDataCollectionInfo;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
import io.harness.cvng.beans.SplunkValidationResponse.SplunkSampleResponse;
import io.harness.cvng.beans.StackdriverDataCollectionInfo;
import io.harness.cvng.beans.StackdriverLogDataCollectionInfo;
import io.harness.cvng.beans.SyncDataCollectionRequest;
import io.harness.cvng.beans.ThirdPartyApiResponseStatus;
import io.harness.cvng.beans.TimeSeriesCustomThresholdActions;
import io.harness.cvng.beans.TimeSeriesDataCollectionInfo;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdComparisonType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.cvng.beans.TimeSeriesThresholdDTO;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.beans.appd.AppDynamicFetchFileStructureRequest;
import io.harness.cvng.beans.appd.AppDynamicSingleMetricDataRequest;
import io.harness.cvng.beans.appd.AppDynamicsDataCollectionRequest;
import io.harness.cvng.beans.appd.AppDynamicsFetchAppRequest;
import io.harness.cvng.beans.appd.AppDynamicsFetchTiersRequest;
import io.harness.cvng.beans.appd.AppDynamicsFileDefinition;
import io.harness.cvng.beans.appd.AppDynamicsMetricDataValidationRequest;
import io.harness.cvng.beans.customhealth.CustomHealthFetchSampleDataRequest;
import io.harness.cvng.beans.customhealth.TimestampInfo;
import io.harness.cvng.beans.datadog.DatadogActiveMetricsRequest;
import io.harness.cvng.beans.datadog.DatadogDashboardDetailsRequest;
import io.harness.cvng.beans.datadog.DatadogDashboardListRequest;
import io.harness.cvng.beans.datadog.DatadogLogDefinition;
import io.harness.cvng.beans.datadog.DatadogLogIndexesRequest;
import io.harness.cvng.beans.datadog.DatadogLogSampleDataRequest;
import io.harness.cvng.beans.datadog.DatadogMetricTagsRequest;
import io.harness.cvng.beans.datadog.DatadogTimeSeriesPointsRequest;
import io.harness.cvng.beans.dynatrace.DynatraceMetricListRequest;
import io.harness.cvng.beans.dynatrace.DynatraceMetricPackValidationRequest;
import io.harness.cvng.beans.dynatrace.DynatraceSampleDataRequest;
import io.harness.cvng.beans.dynatrace.DynatraceServiceDetailsRequest;
import io.harness.cvng.beans.dynatrace.DynatraceServiceListRequest;
import io.harness.cvng.beans.newrelic.NewRelicApplicationFetchRequest;
import io.harness.cvng.beans.newrelic.NewRelicFetchSampleDataRequest;
import io.harness.cvng.beans.newrelic.NewRelicMetricPackValidationRequest;
import io.harness.cvng.beans.pagerduty.PagerDutyRegisterWebhookRequest;
import io.harness.cvng.beans.pagerduty.PagerDutyServicesRequest;
import io.harness.cvng.beans.pagerduty.PagerdutyDeleteWebhookRequest;
import io.harness.cvng.beans.prometheus.PrometheusFetchSampleDataRequest;
import io.harness.cvng.beans.prometheus.PrometheusLabelNamesFetchRequest;
import io.harness.cvng.beans.prometheus.PrometheusLabelValuesFetchRequest;
import io.harness.cvng.beans.prometheus.PrometheusMetricListFetchRequest;
import io.harness.cvng.beans.splunk.SplunkDataCollectionRequest;
import io.harness.cvng.beans.splunk.SplunkLatestHistogramDataCollectionRequest;
import io.harness.cvng.beans.splunk.SplunkSampleDataCollectionRequest;
import io.harness.cvng.beans.splunk.SplunkSavedSearchRequest;
import io.harness.cvng.beans.stackdriver.StackDriverMetricDefinition;
import io.harness.cvng.beans.stackdriver.StackdriverDashboardDetailsRequest;
import io.harness.cvng.beans.stackdriver.StackdriverDashboardRequest;
import io.harness.cvng.beans.stackdriver.StackdriverLogDefinition;
import io.harness.cvng.beans.stackdriver.StackdriverLogSampleDataRequest;
import io.harness.cvng.beans.stackdriver.StackdriverSampleDataRequest;
import io.harness.cvng.models.VerificationType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CV)
public class CvNextGenCommonsBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(MetricDefinitionDTO.class, 9001);
    kryo.register(DataSourceType.class, 9002);
    kryo.register(TimeSeriesMetricType.class, 9003);
    kryo.register(CVMonitoringCategory.class, 9004);
    kryo.register(AppDynamicsDataCollectionInfo.class, 9007);
    kryo.register(VerificationType.class, 9008);
    kryo.register(SplunkValidationResponse.Histogram.class, 9009);
    kryo.register(SplunkValidationResponse.Histogram.Bar.class, 9010);
    kryo.register(AppdynamicsValidationResponse.class, 9011);
    kryo.register(AppdynamicsValidationResponse.AppdynamicsMetricValueValidationResponse.class, 9012);
    kryo.register(ThirdPartyApiResponseStatus.class, 9013);
    kryo.register(SplunkSavedSearch.class, 9014);
    kryo.register(SplunkSampleResponse.class, 9015);
    kryo.register(MetricPackDTO.class, 9016);
    kryo.register(SplunkValidationResponse.class, 9017);
    kryo.register(SplunkValidationResponse.SampleLog.class, 9018);
    kryo.register(DataCollectionConnectorBundle.class, 9019);
    kryo.register(TimeSeriesThresholdDTO.class, 9022);
    kryo.register(TimeSeriesThresholdActionType.class, 9023);
    kryo.register(TimeSeriesThresholdCriteria.class, 9024);
    kryo.register(TimeSeriesThresholdComparisonType.class, 9025);
    kryo.register(TimeSeriesThresholdType.class, 9026);
    kryo.register(TimeSeriesCustomThresholdActions.class, 9027);
    kryo.register(DataCollectionType.class, 9028);
    kryo.register(CVDataCollectionInfo.class, 9029);
    kryo.register(K8ActivityDataCollectionInfo.class, 9030);
    kryo.register(KubernetesActivitySourceDTO.class, 9031);
    kryo.register(KubernetesActivitySourceConfig.class, 9032);
    kryo.register(DataCollectionRequest.class, 9033);
    kryo.register(SplunkDataCollectionRequest.class, 9034);
    kryo.register(SplunkSavedSearchRequest.class, 9035);
    kryo.register(DataCollectionRequestType.class, 9036);
    kryo.register(ActivityStatusDTO.class, 9037);
    kryo.register(ActivityVerificationStatus.class, 9038);
    kryo.register(StackdriverDashboardRequest.class, 9039);
    kryo.register(StackdriverDashboardDetailsRequest.class, 9040);
    kryo.register(StackdriverSampleDataRequest.class, 9041);
    kryo.register(StackDriverMetricDefinition.class, 9042);
    kryo.register(StackDriverMetricDefinition.Aggregation.class, 9043);
    kryo.register(StackdriverDataCollectionInfo.class, 9044);
    kryo.register(AppDynamicsDataCollectionRequest.class, 9045);
    kryo.register(AppDynamicsFetchAppRequest.class, 9046);
    kryo.register(AppDynamicsFetchTiersRequest.class, 9047);

    kryo.register(NewRelicApplicationFetchRequest.class, 9051);
    kryo.register(NewRelicMetricPackValidationRequest.class, 9052);
    kryo.register(AppDynamicsMetricDataValidationRequest.class, 9053);
    kryo.register(PrometheusMetricListFetchRequest.class, 9054);
    kryo.register(PrometheusLabelValuesFetchRequest.class, 9055);
    kryo.register(PrometheusLabelNamesFetchRequest.class, 9056);
    kryo.register(PrometheusFetchSampleDataRequest.class, 9057);
    kryo.register(StackdriverLogSampleDataRequest.class, 9058);
    kryo.register(StackdriverLogDataCollectionInfo.class, 9059);
    kryo.register(SplunkSampleDataCollectionRequest.class, 9060);
    kryo.register(SplunkLatestHistogramDataCollectionRequest.class, 9061);
    kryo.register(PagerDutyServicesRequest.class, 9062);
    kryo.register(PagerDutyRegisterWebhookRequest.class, 9063);
    kryo.register(PagerdutyDeleteWebhookRequest.class, 9064);
    kryo.register(AppDynamicsFileDefinition.class, 9065);
    kryo.register(AppDynamicFetchFileStructureRequest.class, 9066);
    kryo.register(AppDynamicSingleMetricDataRequest.class, 9067);
    kryo.register(DatadogDashboardListRequest.class, 9068);
    kryo.register(DatadogDashboardDetailsRequest.class, 9069);
    kryo.register(DatadogActiveMetricsRequest.class, 9070);
    kryo.register(DatadogMetricTagsRequest.class, 9071);
    kryo.register(DatadogTimeSeriesPointsRequest.class, 9072);
    kryo.register(DatadogLogSampleDataRequest.class, 9073);
    kryo.register(DatadogLogDataCollectionInfo.class, 9074);
    kryo.register(DatadogLogIndexesRequest.class, 9075);
    kryo.register(NewRelicFetchSampleDataRequest.class, 9076);
    kryo.register(SyncDataCollectionRequest.class, 9077);
    kryo.register(TimeSeriesDataCollectionInfo.class, 9078);
    kryo.register(DataCollectionInfo.class, 9079);
    kryo.register(AppMetricInfoDTO.class, 9080);
    kryo.register(CustomHealthFetchSampleDataRequest.class, 9081);
    kryo.register(TimestampInfo.class, 9082);
    kryo.register(TimestampInfo.TimestampFormat.class, 9083);
    kryo.register(PrometheusDataCollectionInfo.class, 9084);
    kryo.register(SplunkDataCollectionInfo.class, 9085);
    kryo.register(StackdriverLogDefinition.class, 9086);
    kryo.register(DatadogLogDefinition.class, 9087);
    kryo.register(DatadogMetricsDataCollectionInfo.class, 9088);
    kryo.register(NewRelicDataCollectionInfo.class, 9089);
    kryo.register(CustomHealthDataCollectionInfo.class, 9090);
    kryo.register(ErrorTrackingDataCollectionInfo.class, 9091);
    kryo.register(DynatraceServiceListRequest.class, 9092);
    kryo.register(DynatraceServiceDetailsRequest.class, 9093);
    kryo.register(DynatraceMetricPackValidationRequest.class, 9094);
    kryo.register(DynatraceSampleDataRequest.class, 9095);
    kryo.register(DynatraceMetricListRequest.class, 9096);
    kryo.register(DynatraceDataCollectionInfo.class, 9097);
    kryo.register(CustomHealthMetricInfo.class, 9098);
    kryo.register(MetricResponseMappingDTO.class, 9099);
    kryo.register(NewRelicDataCollectionInfo.NewRelicMetricInfoDTO.class, 9100);
    kryo.register(DatadogMetricsDataCollectionInfo.MetricCollectionInfo.class, 9101);
    kryo.register(PrometheusDataCollectionInfo.MetricCollectionInfo.class, 9102);
    kryo.register(DynatraceDataCollectionInfo.MetricCollectionInfo.class, 9104);
  }
}
