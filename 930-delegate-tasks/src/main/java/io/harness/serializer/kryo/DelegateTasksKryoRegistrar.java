/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SecretChangeLog;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskParameters;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskResponse;
import io.harness.delegate.task.winrm.AuthenticationScheme;
import io.harness.exception.SecretManagementException;
import io.harness.helpers.ext.vault.SSHVaultAuthResult;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;
import io.harness.perpetualtask.internal.AssignmentTaskResponse;
import io.harness.serializer.KryoRegistrar;

import software.wings.api.AwsLambdaContextElement;
import software.wings.api.AwsLambdaExecutionData;
import software.wings.api.DeploymentType;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.ScriptStateExecutionSummary;
import software.wings.api.ServiceNowExecutionData;
import software.wings.api.ServiceNowImportSetResponse;
import software.wings.api.ServiceNowImportSetResult;
import software.wings.api.ShellScriptApprovalExecutionData;
import software.wings.api.TerraformExecutionData;
import software.wings.api.WorkflowElement;
import software.wings.api.jira.JiraExecutionData;
import software.wings.api.jira.JiraExecutionData.JiraIssueData;
import software.wings.api.shellscript.provision.ShellScriptProvisionExecutionData;
import software.wings.api.terragrunt.TerragruntExecutionData;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureContainerRegistry;
import software.wings.beans.AzureResourceGroup;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.BaseVaultConfig;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.ClusterSelectionCriteriaEntry;
import software.wings.beans.ConnectionType;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.GcpSecretsManagerConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostValidationTaskParameters;
import software.wings.beans.HttpStateExecutionResponse;
import software.wings.beans.InstanaConfig;
import software.wings.beans.JiraConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.RancherConfig;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SSHVaultConfig;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SftpConfig;
import software.wings.beans.SmbConfig;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.TerraformInputVariablesTaskResponse;
import software.wings.beans.VaultConfig;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.apm.Method;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.approval.ConditionalOperator;
import software.wings.beans.approval.Criteria;
import software.wings.beans.approval.ServiceNowApprovalParams;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactoryCollectionTaskParameters;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.container.EcsSteadyStateCheckParams;
import software.wings.beans.container.EcsSteadyStateCheckResponse;
import software.wings.beans.container.KubernetesSteadyStateCheckParams;
import software.wings.beans.container.KubernetesSwapServiceSelectorsParams;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.delegation.TerragruntProvisionParameters;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.beans.servicenow.ServiceNowFieldType;
import software.wings.beans.servicenow.ServiceNowMetaDTO;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.beans.servicenow.ServiceNowTicketType;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.beans.trigger.TriggerCommand.TriggerCommandType;
import software.wings.beans.trigger.WebHookTriggerResponseData;
import software.wings.beans.trigger.WebhookTriggerParameters;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.delegatetasks.BambooTask;
import software.wings.delegatetasks.DelegateStateType;
import software.wings.delegatetasks.GcbDelegateResponse;
import software.wings.delegatetasks.buildsource.BuildCollectParameters;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.delegatetasks.buildsource.BuildSourceResponse;
import software.wings.delegatetasks.collect.artifacts.AzureArtifactsCollectionTaskParameters;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.delegatetasks.cv.beans.CustomLogResponseMapper;
import software.wings.delegatetasks.rancher.RancherResolveClustersResponse;
import software.wings.delegatetasks.rancher.RancherResolveClustersTaskParameters;
import software.wings.delegatetasks.validation.capabilities.BasicValidationInfo;
import software.wings.delegatetasks.validation.capabilities.ClusterMasterUrlValidationCapability;
import software.wings.delegatetasks.validation.capabilities.GitConnectionCapability;
import software.wings.delegatetasks.validation.capabilities.SSHHostValidationCapability;
import software.wings.delegatetasks.validation.capabilities.ShellConnectionCapability;
import software.wings.delegatetasks.validation.capabilities.WinrmHostValidationCapability;
import software.wings.helpers.ext.azure.devops.AzureArtifactsFeed;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackageVersion;
import software.wings.helpers.ext.external.comm.CollaborationProviderRequest;
import software.wings.helpers.ext.external.comm.CollaborationProviderResponse;
import software.wings.helpers.ext.external.comm.EmailRequest;
import software.wings.helpers.ext.helm.request.HelmChartCollectionParams;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.response.HelmCollectChartResponse;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.helpers.ext.pcf.request.CfCommandSetupRequest;
import software.wings.helpers.ext.trigger.request.TriggerDeploymentNeededRequest;
import software.wings.helpers.ext.trigger.request.TriggerRequest;
import software.wings.helpers.ext.trigger.response.TriggerDeploymentNeededResponse;
import software.wings.helpers.ext.trigger.response.TriggerResponse;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.MasterUrlFetchTaskParameter;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.impl.apm.CustomAPMDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppDynamicsDataCollectionInfoV2;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.aws.model.AwsAmiAllPhaseRollbackData;
import software.wings.service.impl.aws.model.AwsAmiRequest;
import software.wings.service.impl.aws.model.AwsAmiRequest.AwsAmiRequestType;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse;
import software.wings.service.impl.aws.model.AwsAmiServiceTrafficShiftAlbDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceTrafficShiftAlbSetupRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceTrafficShiftAlbSetupResponse;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesRequest;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesResponse;
import software.wings.service.impl.aws.model.AwsAmiTrafficShiftAlbSwitchRouteRequest;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountRequest;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountResponse;
import software.wings.service.impl.aws.model.AwsAsgListAllNamesRequest;
import software.wings.service.impl.aws.model.AwsAsgListAllNamesResponse;
import software.wings.service.impl.aws.model.AwsAsgListDesiredCapacitiesRequest;
import software.wings.service.impl.aws.model.AwsAsgListDesiredCapacitiesResponse;
import software.wings.service.impl.aws.model.AwsAsgListInstancesRequest;
import software.wings.service.impl.aws.model.AwsAsgListInstancesResponse;
import software.wings.service.impl.aws.model.AwsAsgRequest;
import software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType;
import software.wings.service.impl.aws.model.AwsCFGetTemplateParamsRequest;
import software.wings.service.impl.aws.model.AwsCFGetTemplateParamsResponse;
import software.wings.service.impl.aws.model.AwsCFRequest;
import software.wings.service.impl.aws.model.AwsCFRequest.AwsCFRequestType;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesRequest;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesResponse;
import software.wings.service.impl.aws.model.AwsEc2ListRegionsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListRegionsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListSGsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListSGsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListSubnetsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListSubnetsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListTagsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListTagsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListVpcsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListVpcsResponse;
import software.wings.service.impl.aws.model.AwsEc2Request;
import software.wings.service.impl.aws.model.AwsEc2Request.AwsEc2RequestType;
import software.wings.service.impl.aws.model.AwsEc2ValidateCredentialsRequest;
import software.wings.service.impl.aws.model.AwsEc2ValidateCredentialsResponse;
import software.wings.service.impl.aws.model.AwsEcrGetAuthTokenRequest;
import software.wings.service.impl.aws.model.AwsEcrGetAuthTokenResponse;
import software.wings.service.impl.aws.model.AwsEcrGetImageUrlRequest;
import software.wings.service.impl.aws.model.AwsEcrGetImageUrlResponse;
import software.wings.service.impl.aws.model.AwsEcrRequest;
import software.wings.service.impl.aws.model.AwsEcrRequest.AwsEcrRequestType;
import software.wings.service.impl.aws.model.AwsEcsListClusterServicesRequest;
import software.wings.service.impl.aws.model.AwsEcsListClusterServicesResponse;
import software.wings.service.impl.aws.model.AwsEcsListClustersRequest;
import software.wings.service.impl.aws.model.AwsEcsListClustersResponse;
import software.wings.service.impl.aws.model.AwsEcsRequest;
import software.wings.service.impl.aws.model.AwsEcsRequest.AwsEcsRequestType;
import software.wings.service.impl.aws.model.AwsElbListAppElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListAppElbsResponse;
import software.wings.service.impl.aws.model.AwsElbListClassicElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListClassicElbsResponse;
import software.wings.service.impl.aws.model.AwsElbListListenerRequest;
import software.wings.service.impl.aws.model.AwsElbListListenerResponse;
import software.wings.service.impl.aws.model.AwsElbListTargetGroupsRequest;
import software.wings.service.impl.aws.model.AwsElbListTargetGroupsResponse;
import software.wings.service.impl.aws.model.AwsElbRequest;
import software.wings.service.impl.aws.model.AwsElbRequest.AwsElbRequestType;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesRequest;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesResponse;
import software.wings.service.impl.aws.model.AwsIamListRolesRequest;
import software.wings.service.impl.aws.model.AwsIamListRolesResponse;
import software.wings.service.impl.aws.model.AwsIamRequest;
import software.wings.service.impl.aws.model.AwsIamRequest.AwsIamRequestType;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfResponse;
import software.wings.service.impl.aws.model.AwsLambdaFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResult;
import software.wings.service.impl.aws.model.AwsLambdaRequest;
import software.wings.service.impl.aws.model.AwsLambdaRequest.AwsLambdaRequestType;
import software.wings.service.impl.aws.model.AwsRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.impl.aws.model.AwsRoute53ListHostedZonesRequest;
import software.wings.service.impl.aws.model.AwsRoute53ListHostedZonesResponse;
import software.wings.service.impl.aws.model.AwsRoute53Request;
import software.wings.service.impl.aws.model.AwsRoute53Request.AwsRoute53RequestType;
import software.wings.service.impl.aws.model.AwsS3ListBucketNamesRequest;
import software.wings.service.impl.aws.model.AwsS3ListBucketNamesResponse;
import software.wings.service.impl.aws.model.AwsS3Request;
import software.wings.service.impl.aws.model.AwsS3Request.AwsS3RequestType;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;
import software.wings.service.impl.aws.model.response.AwsLambdaDetailsResponse;
import software.wings.service.impl.aws.model.response.HostReachabilityResponse;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.service.impl.azure.manager.AzureVMSSCommandRequest;
import software.wings.service.impl.bugsnag.BugsnagApplication;
import software.wings.service.impl.bugsnag.BugsnagSetupTestData;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchDataCollectionInfo;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.cloudwatch.CloudWatchSetupTestNodeData;
import software.wings.service.impl.dynatrace.DynaTraceApplication;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataResponse;
import software.wings.service.impl.dynatrace.DynaTraceSetupTestNodeData;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkDataCollectionInfoV2;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.instana.InstanaAnalyzeMetricRequest;
import software.wings.service.impl.instana.InstanaAnalyzeMetrics;
import software.wings.service.impl.instana.InstanaDataCollectionInfo;
import software.wings.service.impl.instana.InstanaInfraMetricMetadata;
import software.wings.service.impl.instana.InstanaInfraMetricRequest;
import software.wings.service.impl.instana.InstanaInfraMetrics;
import software.wings.service.impl.instana.InstanaMetricItem;
import software.wings.service.impl.instana.InstanaMetricValues;
import software.wings.service.impl.instana.InstanaTagFilter;
import software.wings.service.impl.instana.InstanaTimeFrame;
import software.wings.service.impl.logz.LogzDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfoV2;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicSetupTestNodeData;
import software.wings.service.impl.splunk.SplunkDataCollectionInfoV2;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.impl.stackdriver.StackDriverDataCollectionInfo;
import software.wings.service.impl.stackdriver.StackDriverLogDataCollectionInfo;
import software.wings.service.impl.stackdriver.StackDriverMetric;
import software.wings.service.impl.stackdriver.StackDriverSetupTestNodeData;
import software.wings.service.impl.stackdriver.StackdriverGcpConfigTaskParams;
import software.wings.service.impl.stackdriver.StackdriverLogGcpConfigTaskParams;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.settings.validation.ConnectivityValidationDelegateRequest;
import software.wings.settings.validation.ConnectivityValidationDelegateResponse;
import software.wings.settings.validation.SlackConnectivityValidationAttributes;
import software.wings.settings.validation.SmtpConnectivityValidationAttributes;
import software.wings.settings.validation.SshConnectionConnectivityValidationAttributes;
import software.wings.settings.validation.WinRmConnectivityValidationAttributes;
import software.wings.sm.StateExecutionData;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.ArtifactType;
import software.wings.verification.stackdriver.StackDriverMetricDefinition;

import com.esotericsoftware.kryo.Kryo;

public class DelegateTasksKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ArtifactStreamAttributes.class, 5007);
    kryo.register(BambooConfig.class, 5009);
    kryo.register(DockerConfig.class, 5010);
    kryo.register(NexusConfig.class, 5016);
    kryo.register(ElkConfig.class, 5017);
    kryo.register(ArtifactoryConfig.class, 5018);
    kryo.register(SSHExecutionCredential.class, 5022);
    kryo.register(ExecutionLogCallback.class, 5044);
    kryo.register(HostConnectionAttributes.class, 5070);
    kryo.register(HostConnectionAttributes.ConnectionType.class, 5071);
    kryo.register(BastionConnectionAttributes.class, 5073);
    kryo.register(AppDynamicsConfig.class, 5074);
    kryo.register(WorkflowElement.class, 5086);
    kryo.register(ArtifactType.class, 5117);
    kryo.register(EncryptedData.class, 5124);
    kryo.register(StateExecutionData.class, 5143);
    kryo.register(AppdynamicsDataCollectionInfo.class, 5168);
    kryo.register(ElkDataCollectionInfo.class, 5169);
    kryo.register(LogzDataCollectionInfo.class, 5170);
    kryo.register(NewRelicDataCollectionInfo.class, 5171);
    kryo.register(SumoDataCollectionInfo.class, 5173);
    kryo.register(NewRelicConfig.class, 5175);
    kryo.register(LogzConfig.class, 5176);
    kryo.register(SplunkConfig.class, 5177);
    kryo.register(SumoConfig.class, 5178);
    kryo.register(KmsConfig.class, 5183);
    kryo.register(DataCollectionTaskResult.class, 5184);
    kryo.register(DataCollectionTaskResult.DataCollectionTaskStatus.class, 5185);
    kryo.register(ShellScriptParameters.class, 5186);
    kryo.register(WorkflowStandardParams.class, 5227);
    kryo.register(AwsLambdaContextElement.class, 5228);
    kryo.register(AwsLambdaContextElement.FunctionMeta.class, 5229);
    kryo.register(ScriptStateExecutionData.class, 5235);
    kryo.register(DynaTraceConfig.class, 5237);
    kryo.register(DynaTraceDataCollectionInfo.class, 5238);
    kryo.register(DynaTraceTimeSeries.class, 5239);
    kryo.register(AnalysisComparisonStrategy.class, 5240);
    kryo.register(AzureConfig.class, 5242);
    kryo.register(KubernetesClusterConfig.class, 5244);
    kryo.register(ConnectionType.class, 5254);
    kryo.register(ElkQueryType.class, 5275);
    kryo.register(PcfConfig.class, 5296);
    kryo.register(SmtpConfig.class, 5304);
    kryo.register(CloudWatchDataCollectionInfo.class, 5317);
    kryo.register(CloudWatchMetric.class, 5318);
    kryo.register(AwsNameSpace.class, 5319);
    kryo.register(APMDataCollectionInfo.class, 5320);
    kryo.register(APMMetricInfo.class, 5321);
    kryo.register(APMMetricInfo.ResponseMapper.class, 5322);
    kryo.register(APMValidateCollectorConfig.class, 5323);
    kryo.register(TerraformProvisionParameters.class, 5342);
    kryo.register(TerraformExecutionData.class, 5343);
    kryo.register(ScriptStateExecutionSummary.class, 5346);
    kryo.register(TimeSeriesMlAnalysisType.class, 5347);
    kryo.register(BambooTask.BambooExecutionResponse.class, 5374);
    kryo.register(HttpStateExecutionResponse.class, 5375);
    kryo.register(ElkLogFetchRequest.class, 5376);
    kryo.register(AwsRequest.class, 5380);
    kryo.register(AwsResponse.class, 5381);
    kryo.register(AwsEcrRequest.class, 5382);
    kryo.register(AwsEcrRequestType.class, 5383);
    kryo.register(AwsEcrGetImageUrlRequest.class, 5384);
    kryo.register(AwsEcrGetImageUrlResponse.class, 5385);
    kryo.register(AwsEcrGetAuthTokenRequest.class, 5386);
    kryo.register(AwsEcrGetAuthTokenResponse.class, 5387);
    kryo.register(AwsElbRequest.class, 5388);
    kryo.register(AwsElbRequestType.class, 5389);
    kryo.register(AwsElbListAppElbsRequest.class, 5390);
    kryo.register(AwsElbListAppElbsResponse.class, 5391);
    kryo.register(AwsElbListClassicElbsRequest.class, 5392);
    kryo.register(AwsElbListClassicElbsResponse.class, 5393);
    kryo.register(AwsElbListTargetGroupsRequest.class, 5394);
    kryo.register(AwsElbListTargetGroupsResponse.class, 5395);
    kryo.register(AwsEcsRequest.class, 5398);
    kryo.register(AwsEcsRequestType.class, 5399);
    kryo.register(AwsEcsListClustersRequest.class, 5400);
    kryo.register(AwsEcsListClustersResponse.class, 5401);
    kryo.register(AwsIamRequest.class, 5402);
    kryo.register(AwsIamRequestType.class, 5403);
    kryo.register(AwsIamListInstanceRolesRequest.class, 5404);
    kryo.register(AwsIamListInstanceRolesResponse.class, 5405);
    kryo.register(AwsIamListRolesRequest.class, 5406);
    kryo.register(AwsIamListRolesResponse.class, 5407);
    kryo.register(AwsEc2Request.class, 5408);
    kryo.register(AwsEc2RequestType.class, 5409);
    kryo.register(AwsEc2ListInstancesRequest.class, 5410);
    kryo.register(AwsEc2ListInstancesResponse.class, 5411);
    kryo.register(AwsEc2ListRegionsRequest.class, 5412);
    kryo.register(AwsEc2ListRegionsResponse.class, 5413);
    kryo.register(AwsEc2ListSGsRequest.class, 5414);
    kryo.register(AwsEc2ListSGsResponse.class, 5415);
    kryo.register(AwsEc2ListSubnetsRequest.class, 5416);
    kryo.register(AwsEc2ListSubnetsResponse.class, 5417);
    kryo.register(AwsEc2ListTagsRequest.class, 5418);
    kryo.register(AwsEc2ListTagsResponse.class, 5419);
    kryo.register(AwsEc2ListVpcsRequest.class, 5420);
    kryo.register(AwsEc2ListVpcsResponse.class, 5421);
    kryo.register(AwsEc2ValidateCredentialsRequest.class, 5422);
    kryo.register(AwsEc2ValidateCredentialsResponse.class, 5423);
    kryo.register(AwsAsgRequest.class, 5424);
    kryo.register(AwsAsgRequestType.class, 5425);
    kryo.register(AwsAsgListAllNamesRequest.class, 5426);
    kryo.register(AwsAsgListAllNamesResponse.class, 5427);
    kryo.register(AwsAsgListInstancesRequest.class, 5428);
    kryo.register(AwsAsgListInstancesResponse.class, 5429);
    kryo.register(AwsLambdaRequest.class, 5446);
    kryo.register(AwsLambdaRequestType.class, 5447);
    kryo.register(AwsLambdaExecuteWfRequest.class, 5448);
    kryo.register(AwsLambdaExecuteWfResponse.class, 5451);
    kryo.register(AwsLambdaFunctionResult.class, 5452);
    kryo.register(AwsLambdaExecuteFunctionRequest.class, 5453);
    kryo.register(AwsLambdaExecuteFunctionResponse.class, 5454);
    kryo.register(AwsAmiRequest.class, 5457);
    kryo.register(AwsAmiRequestType.class, 5458);
    kryo.register(AwsAmiServiceSetupRequest.class, 5459);
    kryo.register(AwsAmiServiceSetupResponse.class, 5460);
    kryo.register(AwsAmiServiceDeployRequest.class, 5461);
    kryo.register(AwsAmiServiceDeployResponse.class, 5462);
    kryo.register(AwsAsgListDesiredCapacitiesRequest.class, 5463);
    kryo.register(AwsAsgListDesiredCapacitiesResponse.class, 5464);
    kryo.register(AwsCFRequest.class, 5474);
    kryo.register(AwsCFRequestType.class, 5475);
    kryo.register(AwsCFGetTemplateParamsRequest.class, 5476);
    kryo.register(AwsCFGetTemplateParamsResponse.class, 5477);
    kryo.register(Method.class, 5481);
    kryo.register(LogElement.class, 5486);
    kryo.register(BugsnagConfig.class, 5490);
    kryo.register(BugsnagApplication.class, 5491);
    kryo.register(CustomLogDataCollectionInfo.class, 5492);
    kryo.register(CustomLogResponseMapper.class, 5493);
    kryo.register(CloudWatchSetupTestNodeData.class, 5494);
    kryo.register(DynaTraceSetupTestNodeData.class, 5512);
    kryo.register(DynaTraceMetricDataResponse.class, 5513);
    kryo.register(AwsAmiResizeData.class, 5518);
    kryo.register(DynaTraceMetricDataResponse.DynaTraceMetricDataResult.class, 5514);
    kryo.register(AwsAmiSwitchRoutesRequest.class, 5543);
    kryo.register(AwsAmiSwitchRoutesResponse.class, 5544);
    kryo.register(ContainerServiceParams.class, 5156);
    kryo.register(GitFetchFilesTaskParams.class, 5575);
    kryo.register(GitFetchFilesConfig.class, 5616);
    kryo.register(SecretManagementException.class, 5517);
    kryo.register(TerraformInputVariablesTaskResponse.class, 5522);
    kryo.register(NewRelicSetupTestNodeData.class, 5529);
    kryo.register(SetupTestNodeData.class, 5530);
    kryo.register(AppdynamicsSetupTestNodeData.class, 5531);
    kryo.register(JiraConfig.JiraSetupType.class, 5569);
    kryo.register(StackDriverSetupTestNodeData.class, 5592);
    kryo.register(StackDriverMetric.class, 5593);
    kryo.register(StackDriverDataCollectionInfo.class, 5594);
    kryo.register(AwsElbListListenerRequest.class, 5601);
    kryo.register(AwsElbListListenerResponse.class, 5602);
    kryo.register(AwsLambdaExecutionData.class, 5603);
    kryo.register(GitFetchFilesFromMultipleRepoResult.class, 5615);
    kryo.register(BuildSourceParameters.class, 5618);
    kryo.register(BuildSourceResponse.class, 5619);
    kryo.register(BuildSourceExecutionResponse.class, 5620);
    kryo.register(BuildSourceParameters.BuildSourceRequestType.class, 5621);
    kryo.register(ServiceNowTicketType.class, 7163);
    kryo.register(ServiceNowMetaDTO.class, 7164);
    kryo.register(ServiceNowExecutionData.class, 7170);
    kryo.register(AwsSecretsManagerConfig.class, 7178);
    kryo.register(LocalEncryptionConfig.class, 7180);
    kryo.register(AzureVaultConfig.class, 7205);
    kryo.register(ServiceNowImportSetResponse.class, 7212);
    kryo.register(ServiceNowImportSetResult.class, 7213);
    kryo.register(CyberArkConfig.class, 7228);
    kryo.register(AppManifestKind.class, 7243);
    kryo.register(GcpKmsConfig.class, 7290);
    kryo.register(SmbConfig.class, 5551);
    kryo.register(TriggerDeploymentNeededRequest.class, 5553);
    kryo.register(TriggerDeploymentNeededResponse.class, 5554);
    kryo.register(TriggerCommandType.class, 5555);
    kryo.register(TriggerResponse.class, 5556);
    kryo.register(TriggerRequest.class, 5557);
    kryo.register(SftpConfig.class, 5560);
    kryo.register(ConnectivityValidationDelegateResponse.class, 5566);
    kryo.register(SlackConnectivityValidationAttributes.class, 5572);
    kryo.register(SmtpConnectivityValidationAttributes.class, 5571);
    kryo.register(JiraTaskParameters.class, 5579);
    kryo.register(JiraConfig.class, 5581);
    kryo.register(JiraExecutionData.class, 5582);
    kryo.register(AwsRoute53Request.class, 7106);
    kryo.register(AwsRoute53RequestType.class, 7107);
    kryo.register(AwsRoute53ListHostedZonesRequest.class, 7108);
    kryo.register(AwsRoute53ListHostedZonesResponse.class, 7109);
    kryo.register(AwsRoute53HostedZoneData.class, 7110);
    kryo.register(ApprovalDetails.Action.class, 7111);
    kryo.register(ShellScriptApprovalExecutionData.class, 7112);
    kryo.register(AwsLambdaFunctionRequest.class, 7116);
    kryo.register(AwsLambdaFunctionResponse.class, 7117);
    kryo.register(ShellScriptProvisionExecutionData.class, 7152);
    kryo.register(ServiceNowTaskParameters.class, 7154);
    kryo.register(ServiceNowConfig.class, 7155);
    kryo.register(AwsAsgGetRunningCountRequest.class, 7188);
    kryo.register(AwsAsgGetRunningCountResponse.class, 7189);
    kryo.register(StackDriverLogDataCollectionInfo.class, 7191);
    kryo.register(AwsAmiAllPhaseRollbackData.class, 7197);
    kryo.register(AwsEcsListClusterServicesRequest.class, 7206);
    kryo.register(AwsEcsListClusterServicesResponse.class, 7207);
    kryo.register(SplunkDataCollectionInfoV2.class, 7236);
    kryo.register(AssignmentTaskResponse.class, 7238);
    kryo.register(NewRelicDataCollectionInfoV2.class, 7247);
    kryo.register(StackDriverMetricDefinition.class, 7249);
    kryo.register(AwsLambdaDetailsRequest.class, 7255);
    kryo.register(AwsLambdaDetailsResponse.class, 7256);
    kryo.register(JiraIssueData.class, 7259);
    kryo.register(AwsS3Request.class, 7266);
    kryo.register(AwsS3RequestType.class, 7267);
    kryo.register(AwsS3ListBucketNamesRequest.class, 7268);
    kryo.register(AwsS3ListBucketNamesResponse.class, 7269);
    kryo.register(ElkDataCollectionInfoV2.class, 7283);
    kryo.register(AzureArtifactsPATConfig.class, 7284);
    kryo.register(AzureArtifactsFeed.class, 7286);
    kryo.register(AzureArtifactsPackageVersion.class, 7288);
    kryo.register(AzureArtifactsCollectionTaskParameters.class, 7289);
    kryo.register(InstanaConfig.class, 7293);
    kryo.register(InstanaInfraMetricMetadata.class, 7294);
    kryo.register(InstanaInfraMetricRequest.class, 7295);
    kryo.register(InstanaTimeFrame.class, 7296);
    kryo.register(InstanaInfraMetrics.class, 7297);
    kryo.register(DataCollectionException.class, 7298);
    kryo.register(InstanaMetricItem.class, 7299);
    kryo.register(InstanaMetricValues.class, 7300);
    kryo.register(InstanaDataCollectionInfo.class, 7301);
    kryo.register(InstanaTagFilter.class, 7304);
    kryo.register(CustomAPMDataCollectionInfo.class, 7305);
    kryo.register(APMVerificationConfig.class, 7306);
    kryo.register(APMVerificationConfig.KeyValues.class, 7307);
    kryo.register(InstanaAnalyzeMetricRequest.class, 7311);
    kryo.register(InstanaAnalyzeMetricRequest.Group.class, 7312);
    kryo.register(InstanaAnalyzeMetricRequest.Metric.class, 7313);
    kryo.register(InstanaTagFilter.Operator.class, 7314);
    kryo.register(InstanaAnalyzeMetrics.class, 7315);
    kryo.register(InstanaAnalyzeMetrics.Item.class, 7316);
    kryo.register(AppDynamicsDataCollectionInfoV2.class, 7332);
    kryo.register(EncryptedDataParent.class, 7335);
    kryo.register(ClusterMasterUrlValidationCapability.class, 7345);
    kryo.register(NewRelicMetricDataRecord.class, 7347);
    kryo.register(ClusterLevel.class, 7348);
    kryo.register(StackdriverLogGcpConfigTaskParams.class, 7360);
    kryo.register(StackdriverGcpConfigTaskParams.class, 7361);
    kryo.register(StackDriverMetricDefinition.Aggregation.class, 7380);
    kryo.register(Criteria.class, 7382);
    kryo.register(ConditionalOperator.class, 7383);
    kryo.register(ServiceNowApprovalParams.class, 7389);
    kryo.register(GitConnectionCapability.class, 7391);
    kryo.register(GcbDelegateResponse.class, 7409);
    kryo.register(AwsAmiServiceTrafficShiftAlbSetupResponse.class, 7432);
    kryo.register(AwsAmiServiceTrafficShiftAlbSetupRequest.class, 7433);
    kryo.register(AwsAmiServiceTrafficShiftAlbDeployRequest.class, 7434);
    kryo.register(AwsAmiTrafficShiftAlbSwitchRouteRequest.class, 7435);
    kryo.register(ServiceNowFieldType.class, 7452);
    kryo.register(SetupTestNodeData.Instance.class, 7470);
    kryo.register(DynaTraceApplication.class, 8074);
    kryo.register(BatchCapabilityCheckTaskParameters.class, 8200);
    kryo.register(BatchCapabilityCheckTaskResponse.class, 8201);
    kryo.register(ArtifactoryCollectionTaskParameters.class, 8203);
    kryo.register(TerragruntProvisionParameters.TerragruntCommand.class, 8502);
    kryo.register(TerragruntProvisionParameters.TerragruntCommandUnit.class, 8503);
    kryo.register(TerragruntProvisionParameters.class, 8504);
    kryo.register(TerragruntExecutionData.class, 8507);
    kryo.register(WebhookTriggerParameters.class, 8550);
    kryo.register(WebHookTriggerResponseData.class, 8552);
    kryo.register(AuthenticationScheme.class, 8600);
    kryo.register(DelegateStateType.class, 8601);
    kryo.register(SplunkValidationResponse.Histogram.class, 9009);
    kryo.register(SplunkValidationResponse.Histogram.Bar.class, 9010);
    kryo.register(SplunkSavedSearch.class, 9014);
    kryo.register(SplunkValidationResponse.SplunkSampleResponse.class, 9015);
    kryo.register(SplunkValidationResponse.class, 9017);
    kryo.register(SplunkValidationResponse.SampleLog.class, 9018);
    kryo.register(DataCollectionRequest.class, 9033);
    kryo.register(DataCollectionRequestType.class, 9036);

    kryo.register(SSHVaultConfig.class, 15012);
    kryo.register(BaseVaultConfig.class, 15014);
    kryo.register(BuildCollectParameters.class, 8602);
    kryo.register(RancherConfig.class, 50006);
    kryo.register(SecretChangeLog.class, 5598);
    kryo.register(VaultConfig.class, 5214);
    kryo.register(WinrmHostValidationCapability.class, 7327);
    kryo.register(WinRmConnectionAttributes.class, 5255);
    kryo.register(SecretEngineSummary.class, 7239);
    kryo.register(VaultAppRoleLoginResult.class, 7240);
    kryo.register(SSHVaultAuthResult.class, 15013);
    kryo.register(DeploymentType.class, 5096);
    kryo.register(HostReachabilityResponse.class, 5187);
    kryo.register(CfCommandSetupRequest.class, 5279);
    kryo.register(ConnectivityValidationDelegateRequest.class, 5565);
    kryo.register(SshConnectionConnectivityValidationAttributes.class, 5568);
    kryo.register(WinRmConnectivityValidationAttributes.class, 5570);
    kryo.register(K8sClusterConfig.class, 7128);
    kryo.register(BasicValidationInfo.class, 7325);
    kryo.register(HostValidationTaskParameters.class, 7341);
    kryo.register(ShellConnectionCapability.class, 7390);
    kryo.register(GcpSecretsManagerConfig.class, 72100);
    kryo.register(AzureTaskExecutionRequest.class, 8095);
    kryo.register(HelmChartCollectionParams.class, 8058);
    kryo.register(HelmChartConfigParams.class, 7167);
    kryo.register(BugsnagSetupTestData.class, 7168);
    kryo.register(HelmCollectChartResponse.class, 71111);
    kryo.register(HelmChartCollectionParams.HelmChartCollectionType.class, 400134);
    kryo.register(AzureVMSSCommandRequest.class, 8035);
    kryo.register(AzureContainerRegistry.class, 40013);
    kryo.register(AzureResourceGroup.class, 40016);
    kryo.register(ClusterSelectionCriteriaEntry.class, 50008);
    kryo.register(RancherResolveClustersTaskParameters.class, 50004);
    kryo.register(RancherResolveClustersResponse.class, 50005);
    kryo.register(SpotInstCommandRequest.class, 7220);
    kryo.register(EcsSteadyStateCheckParams.class, 5370);
    kryo.register(EcsSteadyStateCheckResponse.class, 5371);
    kryo.register(KubernetesSteadyStateCheckParams.class, 5276);
    kryo.register(KubernetesSwapServiceSelectorsParams.class, 5365);
    kryo.register(SSHHostValidationCapability.class, 7326);
    kryo.register(MasterUrlFetchTaskParameter.class, 7226);
    kryo.register(EmailData.class, 5303);
    kryo.register(EmailRequest.class, 5309);
    kryo.register(CollaborationProviderRequest.CommunicationType.class, 5307);
    kryo.register(CollaborationProviderRequest.class, 5306);
    kryo.register(CollaborationProviderResponse.class, 5308);
  }
}
