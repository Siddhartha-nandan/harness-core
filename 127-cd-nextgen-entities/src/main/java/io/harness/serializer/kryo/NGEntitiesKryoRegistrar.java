/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import io.harness.cdng.artifact.bean.yaml.AMIArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.AcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.AmazonS3ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.bean.yaml.ArtifactSource;
import io.harness.cdng.artifact.bean.yaml.ArtifactoryRegistryArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.AzureArtifactsConfig;
import io.harness.cdng.artifact.bean.yaml.CustomArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.EcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GithubPackagesArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GoogleArtifactRegistryConfig;
import io.harness.cdng.artifact.bean.yaml.JenkinsArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.NexusRegistryArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifactWrapper;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomArtifactScriptInfo;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomArtifactScriptSourceWrapper;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomArtifactScripts;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomArtifactSpecInfo;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomArtifactSpecVisitorHelper;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomScriptBaseSource;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomScriptInlineSource;
import io.harness.cdng.artifact.bean.yaml.customartifact.FetchAllArtifacts;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.Nexus2RegistryArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusRegistryDockerConfig;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusRegistryMavenConfig;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusRegistryNpmConfig;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusRegistryNugetConfig;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusRegistryRawConfig;
import io.harness.cdng.artifact.outcome.AMIArtifactOutcome;
import io.harness.cdng.artifact.outcome.AcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.ArtifactoryGenericArtifactOutcome;
import io.harness.cdng.artifact.outcome.AzureArtifactsOutcome;
import io.harness.cdng.artifact.outcome.DockerArtifactOutcome;
import io.harness.cdng.artifact.outcome.EcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.GarArtifactOutcome;
import io.harness.cdng.artifact.outcome.GcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.GithubPackagesArtifactOutcome;
import io.harness.cdng.artifact.outcome.JenkinsArtifactOutcome;
import io.harness.cdng.artifact.outcome.S3ArtifactOutcome;
import io.harness.cdng.azure.config.yaml.ApplicationSettingsConfiguration;
import io.harness.cdng.azure.config.yaml.ConnectionStringsConfiguration;
import io.harness.cdng.azure.config.yaml.StartupCommandConfiguration;
import io.harness.cdng.configfile.ConfigFile;
import io.harness.cdng.configfile.ConfigFileAttributes;
import io.harness.cdng.configfile.ConfigFileOutcome;
import io.harness.cdng.configfile.ConfigFileWrapper;
import io.harness.cdng.customdeployment.CustomDeploymentConnectorNGVariable;
import io.harness.cdng.customdeployment.CustomDeploymentNGVariableType;
import io.harness.cdng.customdeployment.CustomDeploymentNumberNGVariable;
import io.harness.cdng.customdeployment.CustomDeploymentSecretNGVariable;
import io.harness.cdng.customdeployment.CustomDeploymentStringNGVariable;
import io.harness.cdng.infra.beans.AsgInfrastructureOutcome;
import io.harness.cdng.infra.beans.CustomDeploymentInfrastructureOutcome;
import io.harness.cdng.infra.beans.EcsInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.infra.beans.ServerlessAwsLambdaInfrastructureOutcome;
import io.harness.cdng.infra.yaml.AsgInfrastructure;
import io.harness.cdng.infra.yaml.CustomDeploymentInfrastructure;
import io.harness.cdng.infra.yaml.EcsInfrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.cdng.infra.yaml.PdcInfrastructure;
import io.harness.cdng.infra.yaml.ServerlessAwsLambdaInfrastructure;
import io.harness.cdng.infra.yaml.SshWinRmAwsInfrastructure;
import io.harness.cdng.infra.yaml.SshWinRmAzureInfrastructure;
import io.harness.cdng.instance.outcome.DeploymentInfoOutcome;
import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.yaml.ArtifactoryStoreConfig;
import io.harness.cdng.manifest.yaml.AsgConfigurationManifestOutcome;
import io.harness.cdng.manifest.yaml.AsgLaunchTemplateManifestOutcome;
import io.harness.cdng.manifest.yaml.AsgScalingPolicyManifestOutcome;
import io.harness.cdng.manifest.yaml.AsgScheduledUpdateGroupActionManifestOutcome;
import io.harness.cdng.manifest.yaml.AutoScalerManifestOutcome;
import io.harness.cdng.manifest.yaml.AzureRepoStore;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.CustomRemoteStoreConfig;
import io.harness.cdng.manifest.yaml.EcsScalableTargetDefinitionManifestOutcome;
import io.harness.cdng.manifest.yaml.EcsScalingPolicyDefinitionManifestOutcome;
import io.harness.cdng.manifest.yaml.EcsServiceDefinitionManifestOutcome;
import io.harness.cdng.manifest.yaml.EcsTaskDefinitionManifestOutcome;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.cdng.manifest.yaml.GitLabStore;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.GoogleCloudFunctionDefinitionManifestOutcome;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome;
import io.harness.cdng.manifest.yaml.HelmCommandFlagType;
import io.harness.cdng.manifest.yaml.HelmManifestCommandFlag;
import io.harness.cdng.manifest.yaml.HttpStoreConfig;
import io.harness.cdng.manifest.yaml.InheritFromManifestStoreConfig;
import io.harness.cdng.manifest.yaml.K8sCommandFlagType;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.K8sStepCommandFlag;
import io.harness.cdng.manifest.yaml.KustomizeManifestOutcome;
import io.harness.cdng.manifest.yaml.KustomizePatchesManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSetWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.manifest.yaml.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.OciHelmChartConfig;
import io.harness.cdng.manifest.yaml.OciHelmChartStoreGenericConfig;
import io.harness.cdng.manifest.yaml.OpenshiftManifestOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftParamManifestOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.ServerlessAwsLambdaManifestOutcome;
import io.harness.cdng.manifest.yaml.TasManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.VarsManifestOutcome;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.manifest.yaml.kinds.AsgConfigurationManifest;
import io.harness.cdng.manifest.yaml.kinds.AsgLaunchTemplateManifest;
import io.harness.cdng.manifest.yaml.kinds.AsgScalingPolicyManifest;
import io.harness.cdng.manifest.yaml.kinds.AsgScheduledUpdateGroupActionManifest;
import io.harness.cdng.manifest.yaml.kinds.AutoScalerManifest;
import io.harness.cdng.manifest.yaml.kinds.EcsScalableTargetDefinitionManifest;
import io.harness.cdng.manifest.yaml.kinds.EcsScalingPolicyDefinitionManifest;
import io.harness.cdng.manifest.yaml.kinds.EcsServiceDefinitionManifest;
import io.harness.cdng.manifest.yaml.kinds.EcsTaskDefinitionManifest;
import io.harness.cdng.manifest.yaml.kinds.GitOpsDeploymentRepoManifest;
import io.harness.cdng.manifest.yaml.kinds.GoogleCloudFunctionDefinitionManifest;
import io.harness.cdng.manifest.yaml.kinds.HelmChartManifest;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.KustomizeManifest;
import io.harness.cdng.manifest.yaml.kinds.KustomizePatchesManifest;
import io.harness.cdng.manifest.yaml.kinds.OpenshiftManifest;
import io.harness.cdng.manifest.yaml.kinds.OpenshiftParamManifest;
import io.harness.cdng.manifest.yaml.kinds.ReleaseRepoManifest;
import io.harness.cdng.manifest.yaml.kinds.ServerlessAwsLambdaManifest;
import io.harness.cdng.manifest.yaml.kinds.TasManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.manifest.yaml.kinds.VarsManifest;
import io.harness.cdng.manifest.yaml.kinds.kustomize.OverlayConfiguration;
import io.harness.cdng.manifest.yaml.oci.OciHelmChartStoreConfigType;
import io.harness.cdng.manifest.yaml.oci.OciHelmChartStoreConfigWrapper;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.service.beans.AsgServiceSpec;
import io.harness.cdng.service.beans.AzureWebAppServiceSpec;
import io.harness.cdng.service.beans.CustomDeploymentServiceSpec;
import io.harness.cdng.service.beans.EcsServiceSpec;
import io.harness.cdng.service.beans.ElastigroupServiceSpec;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.NativeHelmServiceSpec;
import io.harness.cdng.service.beans.ServerlessAwsLambdaServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceConfigOutcome;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.cdng.service.beans.SshServiceSpec;
import io.harness.cdng.service.beans.StageOverridesConfig;
import io.harness.cdng.service.beans.TanzuApplicationServiceSpec;
import io.harness.cdng.service.beans.WinRmServiceSpec;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.cdng.variables.beans.NGVariableOverrideSets;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.NGEnvironmentGlobalOverride;
import io.harness.ng.core.environment.yaml.NGEnvironmentConfig;
import io.harness.ng.core.environment.yaml.NGEnvironmentInfoConfig;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.yaml.NGServiceV2InfoConfig;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideConfig;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideInfoConfig;
import io.harness.pcf.model.CfCliVersionNG;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class NGEntitiesKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ServiceEntity.class, 22002);
    kryo.register(K8sDirectInfrastructureOutcome.class, 8105);
    kryo.register(ManifestsOutcome.class, 8031);
    kryo.register(K8sGcpInfrastructureOutcome.class, 8300);
    kryo.register(DeploymentInfoOutcome.class, 12547);
    kryo.register(HelmChartManifestOutcome.class, 12524);
    kryo.register(HelmCommandFlagType.class, 12526);
    kryo.register(HelmManifestCommandFlag.class, 12525);
    kryo.register(K8sManifestOutcome.class, 12502);
    kryo.register(ArtifactoryStoreConfig.class, 12558);
    kryo.register(KustomizeManifestOutcome.class, 12532);
    kryo.register(OpenshiftManifestOutcome.class, 12535);
    kryo.register(OpenshiftParamManifestOutcome.class, 12537);
    kryo.register(ValuesManifestOutcome.class, 12503);
    kryo.register(ServiceOutcome.class, 8018);
    kryo.register(ServiceOutcome.ArtifactsOutcome.class, 8019);
    kryo.register(ServiceOutcome.StageOverridesOutcome.class, 12504);
    kryo.register(ServiceOutcome.ArtifactsWrapperOutcome.class, 12505);
    kryo.register(ServiceOutcome.ManifestsWrapperOutcome.class, 12506);
    kryo.register(ServiceOutcome.VariablesWrapperOutcome.class, 12507);
    kryo.register(StoreConfig.class, 8022);
    kryo.register(KustomizePatchesManifestOutcome.class, 12548);
    kryo.register(DockerArtifactOutcome.class, 8007);
    kryo.register(S3ArtifactOutcome.class, 8013);
    kryo.register(JenkinsArtifactOutcome.class, 13000);
    kryo.register(GcrArtifactOutcome.class, 390006);
    kryo.register(EcrArtifactOutcome.class, 390007);
    kryo.register(ServerlessAwsLambdaServiceSpec.class, 12576);
    kryo.register(ServerlessAwsLambdaManifest.class, 12578);
    kryo.register(ServerlessAwsLambdaInfrastructureOutcome.class, 390009);
    kryo.register(ArtifactoryGenericArtifactOutcome.class, 390010);
    kryo.register(AcrArtifactOutcome.class, 390011);
    kryo.register(ServerlessAwsLambdaManifestOutcome.class, 390012);
    kryo.register(ServiceConfigOutcome.class, 12508);
    kryo.register(ArtifactListConfig.class, 8009);
    kryo.register(ServiceConfig.class, 8010);
    kryo.register(DockerHubArtifactConfig.class, 8011);
    kryo.register(GcrArtifactConfig.class, 8012);
    kryo.register(KubernetesServiceSpec.class, 8015);
    kryo.register(SidecarArtifact.class, 8016);
    kryo.register(K8sManifest.class, 8021);
    kryo.register(StageOverridesConfig.class, 8024);
    kryo.register(ServiceUseFromStage.class, 8036);
    kryo.register(ManifestOverrideSets.class, 8043);
    kryo.register(ArtifactOverrideSets.class, 8044);
    kryo.register(NexusRegistryArtifactConfig.class, 8046);
    kryo.register(ServiceDefinition.class, 8103);
    kryo.register(ManifestConfig.class, 8104);
    kryo.register(PrimaryArtifact.class, 8106);
    kryo.register(KustomizeManifest.class, 12531);
    kryo.register(EcrArtifactConfig.class, 12533);
    kryo.register(OpenshiftManifest.class, 12534);
    kryo.register(OpenshiftParamManifest.class, 12536);
    kryo.register(StoreConfigWrapper.class, 8045);
    kryo.register(ServiceDefinitionType.class, 12551);
    kryo.register(ServiceYaml.class, 12553);
    kryo.register(SidecarArtifactWrapper.class, 12554);
    kryo.register(ManifestConfigWrapper.class, 12555);
    kryo.register(StoreConfigType.class, 12556);
    kryo.register(ManifestConfigType.class, 12557);
    kryo.register(NGVariableOverrideSets.class, 12560);
    kryo.register(SshServiceSpec.class, 12561);
    kryo.register(KustomizePatchesManifest.class, 12549);
    kryo.register(WinRmServiceSpec.class, 12562);
    kryo.register(CustomArtifactConfig.class, 12563);
    kryo.register(AcrArtifactConfig.class, 12564);
    kryo.register(ValuesManifest.class, 8037);
    kryo.register(ServiceUseFromStage.Overrides.class, 8038);
    kryo.register(ArtifactoryRegistryArtifactConfig.class, 8057);
    kryo.register(ArtifactOverrideSetWrapper.class, 12509);
    kryo.register(ManifestOverrideSetWrapper.class, 12510);
    kryo.register(NGVariableOverrideSetWrapper.class, 12511);
    kryo.register(HelmChartManifest.class, 12523);
    kryo.register(NativeHelmServiceSpec.class, 12542);
    kryo.register(HttpStoreConfig.class, 12530);
    kryo.register(S3StoreConfig.class, 12538);
    kryo.register(GcsStoreConfig.class, 12539);
    kryo.register(GithubStore.class, 12527);
    kryo.register(GitLabStore.class, 12528);
    kryo.register(BitbucketStore.class, 12529);
    kryo.register(GitStore.class, 8023);
    kryo.register(Environment.class, 22003);
    kryo.register(InheritFromManifestStoreConfig.class, 12565);
    kryo.register(ConfigFileWrapper.class, 12586);
    kryo.register(ConfigFile.class, 12587);
    kryo.register(ConfigFileAttributes.class, 12588);
    kryo.register(HarnessStore.class, 12590);
    kryo.register(NGServiceV2InfoConfig.class, 12592);
    kryo.register(OciHelmChartStoreGenericConfig.class, 12594);
    kryo.register(OciHelmChartStoreConfigType.class, 12595);
    kryo.register(OciHelmChartConfig.class, 12596);
    kryo.register(OciHelmChartStoreConfigWrapper.class, 12597);
    kryo.register(ReleaseRepoManifest.class, 12598);
    kryo.register(AzureWebAppServiceSpec.class, 12599);
    kryo.register(AmazonS3ArtifactConfig.class, 12569);
    kryo.register(AzureRepoStore.class, 12570);

    kryo.register(CustomRemoteStoreConfig.class, 12589);
    kryo.register(OverlayConfiguration.class, 12591);
    kryo.register(JenkinsArtifactConfig.class, 130012);

    kryo.register(CustomArtifactSpecInfo.class, 130017);
    kryo.register(CustomArtifactSpecVisitorHelper.class, 130018);
    kryo.register(CustomScriptInlineSource.class, 130019);
    kryo.register(FetchAllArtifacts.class, 130020);
    kryo.register(ConfigFileOutcome.class, 130013);
    kryo.register(CustomDeploymentServiceSpec.class, 130100);
    kryo.register(ApplicationSettingsConfiguration.class, 130014);
    kryo.register(ConnectionStringsConfiguration.class, 130015);
    kryo.register(StartupCommandConfiguration.class, 130016);
    kryo.register(GithubPackagesArtifactConfig.class, 130028);
    kryo.register(GithubPackagesArtifactOutcome.class, 130029);
    kryo.register(AzureArtifactsConfig.class, 14700);
    kryo.register(AzureArtifactsOutcome.class, 14701);
    kryo.register(AMIArtifactConfig.class, 14704);
    kryo.register(AMIArtifactOutcome.class, 14705);
    kryo.register(EcsServiceDefinitionManifest.class, 140001);
    kryo.register(EcsTaskDefinitionManifest.class, 140002);
    kryo.register(EcsTaskDefinitionManifestOutcome.class, 140003);
    kryo.register(EcsServiceDefinitionManifestOutcome.class, 140004);
    kryo.register(EcsScalingPolicyDefinitionManifestOutcome.class, 140005);
    kryo.register(EcsScalableTargetDefinitionManifestOutcome.class, 140006);
    kryo.register(EcsInfrastructureOutcome.class, 140007);
    kryo.register(EcsServiceSpec.class, 140008);
    kryo.register(EcsScalableTargetDefinitionManifest.class, 140009);
    kryo.register(EcsScalingPolicyDefinitionManifest.class, 140010);
    kryo.register(CustomArtifactScriptInfo.class, 140017);
    kryo.register(CustomArtifactScripts.class, 140018);
    kryo.register(CustomArtifactScriptSourceWrapper.class, 140019);
    kryo.register(CustomScriptBaseSource.class, 140020);
    kryo.register(Nexus2RegistryArtifactConfig.class, 140012);
    kryo.register(NexusRegistryNpmConfig.class, 140013);
    kryo.register(NexusRegistryMavenConfig.class, 140014);
    kryo.register(NexusRegistryNugetConfig.class, 140015);
    kryo.register(NexusRegistryDockerConfig.class, 140016);
    kryo.register(CustomDeploymentInfrastructureOutcome.class, 140022);
    kryo.register(GoogleArtifactRegistryConfig.class, 130021);
    kryo.register(GarArtifactOutcome.class, 130022);
    kryo.register(ArtifactSource.class, 130023);
    kryo.register(NGEnvironmentInfoConfig.class, 130025);
    kryo.register(NGServiceOverrideConfig.class, 130026);
    kryo.register(NGEnvironmentConfig.class, 130027);
    kryo.register(NGServiceOverrideInfoConfig.class, 130030);
    kryo.register(NGEnvironmentGlobalOverride.class, 130031);
    kryo.register(CustomDeploymentConnectorNGVariable.class, 130032);
    kryo.register(CustomDeploymentStringNGVariable.class, 130033);
    kryo.register(CustomDeploymentNumberNGVariable.class, 130034);
    kryo.register(CustomDeploymentSecretNGVariable.class, 130035);
    kryo.register(CustomDeploymentNGVariableType.class, 130036);
    kryo.register(GitOpsDeploymentRepoManifest.class, 130037);

    kryo.register(K8SDirectInfrastructure.class, 8028);
    kryo.register(K8sGcpInfrastructure.class, 8301);
    kryo.register(PdcInfrastructure.class, 8302);
    kryo.register(SshWinRmAzureInfrastructure.class, 8303);
    kryo.register(SshWinRmAwsInfrastructure.class, 8304);
    kryo.register(EcsInfrastructure.class, 12612);
    kryo.register(CustomDeploymentInfrastructure.class, 12613);
    kryo.register(ServerlessAwsLambdaInfrastructure.class, 12579);
    kryo.register(NexusRegistryRawConfig.class, 1400171);
    kryo.register(ElastigroupServiceSpec.class, 140011);
    kryo.register(TanzuApplicationServiceSpec.class, 140050);
    kryo.register(TasManifest.class, 140051);
    kryo.register(AutoScalerManifest.class, 140052);
    kryo.register(VarsManifest.class, 140053);
    kryo.register(TasManifestOutcome.class, 140054);
    kryo.register(AutoScalerManifestOutcome.class, 140055);
    kryo.register(VarsManifestOutcome.class, 140056);
    kryo.register(K8sCommandFlagType.class, 1400172);
    kryo.register(K8sStepCommandFlag.class, 1400173);
    kryo.register(AsgServiceSpec.class, 140057);
    kryo.register(AsgLaunchTemplateManifest.class, 140058);
    kryo.register(AsgScalingPolicyManifest.class, 140059);
    kryo.register(AsgConfigurationManifest.class, 140060);
    kryo.register(AsgScheduledUpdateGroupActionManifest.class, 140061);
    kryo.register(AsgLaunchTemplateManifestOutcome.class, 140062);
    kryo.register(AsgScalingPolicyManifestOutcome.class, 140063);
    kryo.register(AsgConfigurationManifestOutcome.class, 140064);
    kryo.register(AsgScheduledUpdateGroupActionManifestOutcome.class, 140065);
    kryo.register(AsgInfrastructureOutcome.class, 140066);
    kryo.register(AsgInfrastructure.class, 140067);
    kryo.register(GoogleCloudFunctionDefinitionManifest.class, 140068);
    kryo.register(GoogleCloudFunctionDefinitionManifestOutcome.class, 140069);
    kryo.register(CfCliVersionNG.class, 140070);
  }
}
