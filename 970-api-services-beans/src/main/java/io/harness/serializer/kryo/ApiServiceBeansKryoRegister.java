/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.kryo;
import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.adfs.AdfsAccessTokenResponse;
import io.harness.ami.AMITagsResponse;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifactory.ArtifactoryImagePath;
import io.harness.aws.AwsAccessKeyCredential;
import io.harness.aws.AwsCFTemplatesType;
import io.harness.aws.AwsConfig;
import io.harness.aws.CrossAccountAccess;
import io.harness.azure.AzureEnvironmentType;
import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConfiguration;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceConnectionStringType;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.azure.model.AzureVMData;
import io.harness.azure.model.SubscriptionData;
import io.harness.azure.model.VirtualMachineScaleSetData;
import io.harness.beans.ArtifactMetaInfo;
import io.harness.beans.ArtifactMetadata;
import io.harness.beans.NGInstanceUnitType;
import io.harness.container.ContainerInfo;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.beans.connector.docker.DockerRegistryProviderType;
import io.harness.deployment.InstanceDetails;
import io.harness.ecs.EcsContainerDetails;
import io.harness.exception.HelmClientException;
import io.harness.exception.OciHelmDockerApiException;
import io.harness.exception.OpenShiftClientException;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.git.UsernamePasswordAuthRequest;
import io.harness.git.model.AuthInfo;
import io.harness.git.model.ChangeType;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.CommitResult;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.git.model.GitFileChange;
import io.harness.git.model.GitRepositoryType;
import io.harness.git.model.ListRemoteRequest;
import io.harness.git.model.ListRemoteResult;
import io.harness.git.model.PushResultGit;
import io.harness.gitpolling.github.GitPollingWebhookData;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.globalcontex.DelegateTokenGlobalContextData;
import io.harness.globalcontex.EntityOperationIdentifier;
import io.harness.globalcontex.PurgeGlobalContextData;
import io.harness.helm.HelmCliCommandType;
import io.harness.helm.HelmCommandType;
import io.harness.helm.HelmSubCommandType;
import io.harness.jira.JiraAction;
import io.harness.jira.JiraActionNG;
import io.harness.jira.JiraCreateIssueRequestNG;
import io.harness.jira.JiraCreateMetaResponse;
import io.harness.jira.JiraCustomFieldValue;
import io.harness.jira.JiraField;
import io.harness.jira.JiraFieldAllowedValueNG;
import io.harness.jira.JiraFieldNG;
import io.harness.jira.JiraFieldSchema;
import io.harness.jira.JiraFieldSchemaNG;
import io.harness.jira.JiraFieldTypeNG;
import io.harness.jira.JiraIssueCreateMetadataNG;
import io.harness.jira.JiraIssueCreateMetadataNGFields;
import io.harness.jira.JiraIssueCreateMetadataNGIssueTypes;
import io.harness.jira.JiraIssueNG;
import io.harness.jira.JiraIssueTransitionNG;
import io.harness.jira.JiraIssueTransitionRequestNG;
import io.harness.jira.JiraIssueTransitionsNG;
import io.harness.jira.JiraIssueType;
import io.harness.jira.JiraIssueTypeNG;
import io.harness.jira.JiraIssueUpdateMetadataNG;
import io.harness.jira.JiraProjectBasicNG;
import io.harness.jira.JiraProjectData;
import io.harness.jira.JiraProjectNG;
import io.harness.jira.JiraStatus;
import io.harness.jira.JiraStatusCategory;
import io.harness.jira.JiraStatusCategoryNG;
import io.harness.jira.JiraStatusNG;
import io.harness.jira.JiraTimeTrackingFieldNG;
import io.harness.jira.JiraUpdateIssueRequestNG;
import io.harness.jira.JiraUserData;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.ImageDetails;
import io.harness.k8s.model.IstioDestinationWeight;
import io.harness.k8s.model.K8sContainer;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.OidcGrantType;
import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;
import io.harness.logging.CommandExecutionStatus;
import io.harness.manifest.CustomManifestSource;
import io.harness.manifest.CustomSourceConfig;
import io.harness.manifest.CustomSourceFile;
import io.harness.nexus.NexusRequest;
import io.harness.ngtriggers.WebhookSecretData;
import io.harness.pcf.model.CfCliVersion;
import io.harness.pcf.model.ManifestType;
import io.harness.provision.TfVarScriptRepositorySource;
import io.harness.provision.TfVarSource;
import io.harness.provision.TfVarSource.TfVarSourceType;
import io.harness.provision.model.TfConfigInspectVersion;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.security.encryption.setting.EncryptableSettingWithEncryptionDetails;
import io.harness.serializer.KryoRegistrar;
import io.harness.servicenow.ServiceNowActionNG;
import io.harness.servicenow.ServiceNowFieldAllowedValueNG;
import io.harness.servicenow.ServiceNowFieldNG;
import io.harness.servicenow.ServiceNowFieldSchemaNG;
import io.harness.servicenow.ServiceNowFieldTypeNG;
import io.harness.servicenow.ServiceNowFieldValueNG;
import io.harness.servicenow.ServiceNowImportSetResponseNG;
import io.harness.servicenow.ServiceNowImportSetTransformMapResult;
import io.harness.servicenow.ServiceNowStagingTable;
import io.harness.servicenow.ServiceNowTemplate;
import io.harness.servicenow.ServiceNowTicketNG;
import io.harness.servicenow.ServiceNowTicketTypeDTO;
import io.harness.servicenow.auth.refreshtoken.AccessTokenResponse;
import io.harness.shell.AccessType;
import io.harness.shell.AuthenticationScheme;
import io.harness.shell.CommandExecutionData;
import io.harness.shell.ExecuteCommandResponse;
import io.harness.shell.KerberosConfig;
import io.harness.shell.ScriptType;
import io.harness.shell.ShellExecutionData;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import io.harness.ssh.FileSourceType;

import software.wings.api.ContainerServiceData;
import software.wings.beans.AmazonClientSDKDefaultBackoffStrategy;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.AzureImageDefinition;
import software.wings.beans.AzureImageGallery;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.GcpKubernetesCluster;
import software.wings.beans.command.CodeDeployCommandExecutionData;
import software.wings.beans.command.ResizeCommandUnitExecutionData;
import software.wings.helpers.ext.azure.devops.AzureArtifactsFeed;
import software.wings.helpers.ext.azure.devops.AzureArtifactsFeeds;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackage;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackageFile;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackageFileInfo;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackageVersion;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackageVersions;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackages;
import software.wings.helpers.ext.azure.devops.AzureArtifactsProtocolMetadata;
import software.wings.helpers.ext.azure.devops.AzureArtifactsProtocolMetadataData;
import software.wings.helpers.ext.azure.devops.AzureDevopsProject;
import software.wings.helpers.ext.azure.devops.AzureDevopsProjects;
import software.wings.helpers.ext.gcb.models.BuildOptions;
import software.wings.helpers.ext.gcb.models.BuiltImage;
import software.wings.helpers.ext.gcb.models.GcbArtifactObjects;
import software.wings.helpers.ext.gcb.models.GcbArtifacts;
import software.wings.helpers.ext.gcb.models.GcbBuildSource;
import software.wings.helpers.ext.gcb.models.GcbResult;
import software.wings.helpers.ext.gcb.models.HashType;
import software.wings.helpers.ext.gcb.models.LogStreamingOption;
import software.wings.helpers.ext.gcb.models.LoggingMode;
import software.wings.helpers.ext.gcb.models.MachineType;
import software.wings.helpers.ext.gcb.models.RepoSource;
import software.wings.helpers.ext.gcb.models.SourceProvenance;
import software.wings.helpers.ext.gcb.models.StorageSource;
import software.wings.helpers.ext.gcb.models.SubstitutionOption;
import software.wings.helpers.ext.gcb.models.TimeSpan;
import software.wings.helpers.ext.gcb.models.VerifyOption;
import software.wings.helpers.ext.gcb.models.Volume;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.helpers.ext.kustomize.KustomizeConfig;
import software.wings.helpers.ext.nexus.NexusRepositories;
import software.wings.settings.SettingVariableTypes;
import software.wings.utils.RepositoryFormat;
import software.wings.utils.RepositoryType;

import com.amazonaws.SdkClientException;
import com.amazonaws.internal.SdkInternalList;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.CapacityReservationSpecificationResponse;
import com.amazonaws.services.ec2.model.CapacityReservationTargetResponse;
import com.amazonaws.services.ec2.model.CpuOptions;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDevice;
import com.amazonaws.services.ec2.model.EnclaveOptions;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.HibernationOptions;
import com.amazonaws.services.ec2.model.IamInstanceProfile;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.InstanceIpv6Address;
import com.amazonaws.services.ec2.model.InstanceMaintenanceOptions;
import com.amazonaws.services.ec2.model.InstanceMetadataOptionsResponse;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceAssociation;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceAttachment;
import com.amazonaws.services.ec2.model.InstancePrivateIpAddress;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.LicenseConfiguration;
import com.amazonaws.services.ec2.model.Monitoring;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.PrivateDnsNameOptionsResponse;
import com.amazonaws.services.ec2.model.ProductCode;
import com.amazonaws.services.ec2.model.StateReason;
import com.amazonaws.services.ec2.model.Tag;
import com.esotericsoftware.kryo.Kryo;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.monitoring.v3.model.BucketOptions;
import com.google.api.services.monitoring.v3.model.Distribution;
import com.google.api.services.monitoring.v3.model.Exponential;
import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.google.api.services.monitoring.v3.model.Metric;
import com.google.api.services.monitoring.v3.model.MonitoredResource;
import com.google.api.services.monitoring.v3.model.Point;
import com.google.api.services.monitoring.v3.model.TimeInterval;
import com.google.api.services.monitoring.v3.model.TimeSeries;
import com.google.api.services.monitoring.v3.model.TypedValue;
import com.sumologic.client.SumoServerException;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.nio.file.NoSuchFileException;
import javax.ws.rs.core.MultivaluedHashMap;
import org.json.JSONException;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_APPROVALS})
@OwnedBy(DEL)
public class ApiServiceBeansKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(Instance.class, 1001);
    kryo.register(SdkInternalList.class, 1002);
    kryo.register(InstanceBlockDeviceMapping.class, 1003);
    kryo.register(EbsInstanceBlockDevice.class, 1004);
    kryo.register(IamInstanceProfile.class, 1005);
    kryo.register(Monitoring.class, 1006);
    kryo.register(InstanceNetworkInterface.class, 1007);
    kryo.register(InstanceNetworkInterfaceAssociation.class, 1008);
    kryo.register(InstanceNetworkInterfaceAttachment.class, 1009);
    kryo.register(GroupIdentifier.class, 1010);
    kryo.register(InstancePrivateIpAddress.class, 1011);
    kryo.register(Placement.class, 1012);
    kryo.register(InstanceState.class, 1013);
    kryo.register(Tag.class, 1014);
    kryo.register(com.amazonaws.AbortedException.class, 1015);
    kryo.register(StateReason.class, 1016);
    kryo.register(SdkClientException.class, 1017);
    kryo.register(InstanceIpv6Address.class, 1018);
    kryo.register(ProductCode.class, 1019);
    kryo.register(Filter.class, 1020);
    kryo.register(Regions.class, 1021);
    kryo.register(CpuOptions.class, 1022);
    kryo.register(CapacityReservationSpecificationResponse.class, 1023);
    kryo.register(CapacityReservationTargetResponse.class, 1024);
    kryo.register(InstanceMaintenanceOptions.class, 1027);
    kryo.register(PrivateDnsNameOptionsResponse.class, 1028);
    kryo.register(KubernetesClientException.class, 2000);
    kryo.register(JSONException.class, 2001);
    kryo.register(SumoServerException.class, 2003);
    kryo.register(ListTimeSeriesResponse.class, 2004);
    kryo.register(TimeSeries.class, 2005);
    kryo.register(Point.class, 2006);
    kryo.register(Metric.class, 2007);
    kryo.register(MonitoredResource.class, 2008);
    kryo.register(TimeInterval.class, 2009);
    kryo.register(TypedValue.class, 2010);
    kryo.register(ArrayMap.class, 2011);
    kryo.register(Distribution.class, 2012);
    kryo.register(BucketOptions.class, 2013);
    kryo.register(Exponential.class, 2014);
    kryo.register(CommandExecutionStatus.class, 5037);
    kryo.register(ResizeCommandUnitExecutionData.class, 5053);
    kryo.register(EncryptionConfig.class, 5305);
    kryo.register(EncryptionType.class, 5123);
    kryo.register(EncryptedDataDetail.class, 5125);
    kryo.register(ImageDetails.class, 5151);
    kryo.register(ContainerServiceData.class, 5157);
    kryo.register(AccessType.class, 5072);
    kryo.register(ContainerInfo.Status.class, 5076);
    kryo.register(ContainerInfo.class, 5075);
    kryo.register(CodeDeployCommandExecutionData.class, 5032);
    kryo.register(ShellExecutionData.class, 5528);
    kryo.register(KerberosConfig.class, 5549);
    kryo.register(AuthenticationScheme.class, 5550);
    kryo.register(K8sPod.class, 7145);
    kryo.register(K8sContainer.class, 7146);
    kryo.register(AuditGlobalContextData.class, 7172);
    kryo.register(PurgeGlobalContextData.class, 7173);
    kryo.register(EcsContainerDetails.class, 7179);
    kryo.register(OidcGrantType.class, 7318);

    kryo.register(CEK8sDelegatePrerequisite.class, 7490);
    kryo.register(CEK8sDelegatePrerequisite.MetricsServerCheck.class, 7491);
    kryo.register(CEK8sDelegatePrerequisite.Rule.class, 7492);

    kryo.register(ElastiGroup.class, 1025);
    kryo.register(ElastiGroupCapacity.class, 1026);
    kryo.register(EncryptedRecordData.class, 1401);
    kryo.register(ManifestType.class, 1402);
    kryo.register(HibernationOptions.class, 1403);
    kryo.register(InstanceMetadataOptionsResponse.class, 1404);
    kryo.register(LicenseConfiguration.class, 1405);
    kryo.register(InstanceDetails.class, 1406);
    kryo.register(InstanceDetails.PCF.class, 1407);
    kryo.register(InstanceDetails.AWS.class, 1408);
    kryo.register(InstanceDetails.InstanceType.class, 1409);
    kryo.register(InstanceDetails.PHYSICAL_HOST.class, 1411);
    kryo.register(InstanceDetails.K8s.class, 1412);
    kryo.register(EncryptedDataParams.class, 1413);
    kryo.register(SubscriptionData.class, 1414);
    kryo.register(VirtualMachineScaleSetData.class, 1415);
    kryo.register(InstanceDetails.AZURE_VMSS.class, 1416);
    kryo.register(AzureVMData.class, 1417);

    kryo.register(SettingVariableTypes.class, 5131);
    kryo.register(ScriptType.class, 5253);

    kryo.register(GitFile.class, 5574);
    kryo.register(EncryptableSettingWithEncryptionDetails.class, 7258);
    kryo.register(HelmVersion.class, 7303);
    kryo.register(KubernetesClusterAuthType.class, 7317);
    kryo.register(IstioDestinationWeight.class, 7183);

    kryo.register(ChangeType.class, 5212);
    kryo.register(ListRemoteRequest.class, 1400);
    kryo.register(ListRemoteResult.class, 1410);
    kryo.register(GitFileChange.class, 1418);
    kryo.register(CommitAndPushRequest.class, 1419);
    kryo.register(UsernamePasswordAuthRequest.class, 1420);
    kryo.register(AuthInfo.class, 1421);
    kryo.register(AuthInfo.AuthType.class, 1422);
    kryo.register(CommitAndPushResult.class, 1423);
    kryo.register(CommitResult.class, 1424);
    kryo.register(PushResultGit.class, 1425);
    kryo.register(PushResultGit.RefUpdate.class, 1426);
    kryo.register(FetchFilesResult.class, 1427);
    kryo.register(CommandExecutionData.class, 5035);
    kryo.register(GitRepositoryType.class, 5270);
    kryo.register(JiraAction.class, 5580);
    kryo.register(JiraCustomFieldValue.class, 7177);
    kryo.register(JiraProjectData.class, 7198);
    kryo.register(JiraIssueType.class, 7199);
    kryo.register(JiraField.class, 7200);
    kryo.register(JiraCreateMetaResponse.class, 7201);
    kryo.register(JiraFieldSchema.class, 7510);

    kryo.register(AwsConfig.class, 1428);
    kryo.register(CrossAccountAccess.class, 1429);
    kryo.register(AwsAccessKeyCredential.class, 1430);
    kryo.register(AzureEnvironmentType.class, 1436);

    kryo.register(JiraStatus.class, 1431);
    kryo.register(JiraStatusCategory.class, 1432);

    kryo.register(TfVarScriptRepositorySource.class, 1433);
    kryo.register(TfVarSource.class, 1434);
    kryo.register(TfVarSourceType.class, 1435);
    kryo.register(InstanceDetails.AZURE_WEBAPP.class, 1437);
    kryo.register(ExecuteCommandResponse.class, 1438);
    kryo.register(AzureAppServiceConnectionStringType.class, 1439);
    kryo.register(AzureAppServiceConfiguration.class, 1440);

    kryo.register(NexusRequest.class, 1441);
    kryo.register(RepositoryFormat.class, 7204);
    kryo.register(RepositoryType.class, 7171);

    kryo.register(AzureAppServiceApplicationSetting.class, 1442);
    kryo.register(AzureAppServiceConnectionString.class, 1443);
    kryo.register(ArtifactoryConfigRequest.class, 1444);
    kryo.register(NGInstanceUnitType.class, 1445);
    kryo.register(AzureDeploymentMode.class, 1446);
    kryo.register(ARMScopeType.class, 1447);
    kryo.register(HelmSubCommandType.class, 1448);
    kryo.register(CustomManifestSource.class, 1449);
    kryo.register(CustomSourceFile.class, 1450);
    kryo.register(ArtifactMetadata.class, 1451);
    kryo.register(ArtifactMetaInfo.class, 1452);

    kryo.register(AdditionalMetadata.class, 72101);
    kryo.register(AwsCrossAccountAttributes.class, 7203);

    kryo.register(JiraActionNG.class, 97001);
    kryo.register(JiraCreateIssueRequestNG.class, 97002);
    kryo.register(JiraFieldAllowedValueNG.class, 97003);
    kryo.register(JiraFieldNG.class, 97004);
    kryo.register(JiraFieldSchemaNG.class, 97005);
    kryo.register(JiraFieldTypeNG.class, 97006);
    kryo.register(JiraIssueCreateMetadataNG.class, 97007);
    kryo.register(JiraIssueNG.class, 97008);
    kryo.register(JiraIssueTypeNG.class, 9709);
    kryo.register(JiraUserData.class, 9710);
    kryo.register(JiraProjectBasicNG.class, 97010);
    kryo.register(JiraProjectNG.class, 97011);
    kryo.register(JiraStatusCategoryNG.class, 97012);
    kryo.register(JiraStatusNG.class, 97013);
    kryo.register(JiraTimeTrackingFieldNG.class, 97014);
    kryo.register(JiraIssueTransitionNG.class, 97015);
    kryo.register(JiraIssueTransitionRequestNG.class, 97016);
    kryo.register(JiraIssueTransitionsNG.class, 97017);
    kryo.register(JiraIssueUpdateMetadataNG.class, 97018);
    kryo.register(JiraUpdateIssueRequestNG.class, 97019);
    kryo.register(JiraIssueCreateMetadataNGIssueTypes.class, 97020);
    kryo.register(EntityOperationIdentifier.class, 97021);
    kryo.register(EntityOperationIdentifier.EntityOperation.class, 97022);
    kryo.register(CfCliVersion.class, 97023);
    kryo.register(JiraIssueCreateMetadataNGFields.class, 97024);
    kryo.register(KubernetesResourceId.class, 97031);
    kryo.register(HelmClientException.class, 97032);
    kryo.register(HelmCliCommandType.class, 97033);
    kryo.register(EnclaveOptions.class, 97034);
    kryo.register(KubernetesResource.class, 97035);
    kryo.register(OciHelmDockerApiException.class, 97036);
    kryo.register(OpenShiftClientException.class, 97037);

    kryo.register(BuildOptions.class, 7414);
    kryo.register(BuiltImage.class, 7426);
    kryo.register(GcbArtifactObjects.class, 7464);
    kryo.register(GcbArtifacts.class, 7413);
    kryo.register(GcbBuildSource.class, 7420);
    kryo.register(GcbResult.class, 7424);
    kryo.register(LoggingMode.class, 7416);
    kryo.register(MachineType.class, 7417);
    kryo.register(LogStreamingOption.class, 7415);
    kryo.register(VerifyOption.class, 7418);
    kryo.register(SubstitutionOption.class, 7419);
    kryo.register(StorageSource.class, 7421);
    kryo.register(SourceProvenance.class, 7422);
    kryo.register(TimeSpan.class, 7425);
    kryo.register(RepoSource.class, 7438);
    kryo.register(HashType.class, 7450);
    kryo.register(Volume.class, 7451);
    kryo.register(AzureImageGallery.class, 40017);
    kryo.register(AzureImageDefinition.class, 40018);
    kryo.register(ExecutionCredential.ExecutionType.class, 5023);
    kryo.register(JobDetails.JobParameter.class, 5241);
    kryo.register(JobDetails.class, 5121);
    kryo.register(AzureDevopsProject.class, 7285);
    kryo.register(AzureArtifactsPackage.class, 7287);
    kryo.register(CustomSourceConfig.class, 97100);
    kryo.register(ServiceNowActionNG.class, 97101);
    kryo.register(ServiceNowFieldAllowedValueNG.class, 97102);
    kryo.register(ServiceNowFieldNG.class, 97103);
    kryo.register(ServiceNowFieldSchemaNG.class, 97104);
    kryo.register(ServiceNowFieldTypeNG.class, 97105);
    kryo.register(ServiceNowTicketNG.class, 97106);
    kryo.register(ServiceNowFieldValueNG.class, 97107);
    kryo.register(DelegateTokenGlobalContextData.class, 97108);
    kryo.register(AmazonClientSDKDefaultBackoffStrategy.class, 97109);
    kryo.register(BuildDetails.class, 97110);
    kryo.register(BuildDetails.BuildStatus.class, 97111);
    kryo.register(ArtifactFileMetadata.class, 97112);
    kryo.register(AwsCFTemplatesType.class, 97114);
    kryo.register(GcpKubernetesCluster.class, 5537);
    kryo.register(KustomizeConfig.class, 7323);
    kryo.register(AzureKubernetesCluster.class, 5538);
    kryo.register(SecretManagementDelegateException.class, 5585);
    kryo.register(NoSuchFileException.class, 5586);
    kryo.register(ServiceNowTemplate.class, 97113);
    kryo.register(FileSourceType.class, 97115);
    kryo.register(DockerRegistryProviderType.class, 19434);
    kryo.register(HelmCommandType.class, 98400);
    kryo.register(ServiceNowImportSetResponseNG.class, 97116);
    kryo.register(ServiceNowImportSetTransformMapResult.class, 97117);
    kryo.register(ServiceNowStagingTable.class, 97118);
    kryo.register(ServiceNowTicketTypeDTO.class, 97120);
    kryo.register(AdfsAccessTokenResponse.class, 10000121);
    kryo.register(GitPollingWebhookData.class, 73001);
    kryo.register(MultivaluedHashMap.class, 73002);
    kryo.register(AzureArtifactsFeed.class, 7286);
    kryo.register(AzureArtifactsFeeds.class, 80302);
    kryo.register(AzureArtifactsPackageFile.class, 80303);
    kryo.register(AzureArtifactsPackageFileInfo.class, 80304);
    kryo.register(AzureArtifactsPackages.class, 80305);
    kryo.register(AzureArtifactsPackageVersion.class, 7288);
    kryo.register(AzureArtifactsPackageVersions.class, 80307);
    kryo.register(AzureArtifactsProtocolMetadata.class, 80308);
    kryo.register(AzureArtifactsProtocolMetadataData.class, 80309);
    kryo.register(AzureDevopsProjects.class, 80310);
    kryo.register(ArtifactoryImagePath.class, 80311);
    kryo.register(WebhookSecretData.class, 80312);
    kryo.register(AMITagsResponse.class, 81001);
    kryo.register(NexusRepositories.class, 9000312);
    kryo.register(TfConfigInspectVersion.class, 9000313);
    kryo.register(AccessTokenResponse.class, 10000123);
  }
}
