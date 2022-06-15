/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import io.harness.cdng.artifact.bean.yaml.AcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.bean.yaml.ArtifactoryRegistryArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.CustomArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.EcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.NexusRegistryArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifactWrapper;
import io.harness.cdng.artifact.outcome.AcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.ArtifactoryGenericArtifactOutcome;
import io.harness.cdng.artifact.outcome.DockerArtifactOutcome;
import io.harness.cdng.artifact.outcome.EcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.GcrArtifactOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.infra.beans.ServerlessAwsLambdaInfrastructureOutcome;
import io.harness.cdng.instance.outcome.DeploymentInfoOutcome;
import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.yaml.ArtifactoryStoreConfig;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.cdng.manifest.yaml.GitLabStore;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome;
import io.harness.cdng.manifest.yaml.HelmCommandFlagType;
import io.harness.cdng.manifest.yaml.HelmManifestCommandFlag;
import io.harness.cdng.manifest.yaml.HttpStoreConfig;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.KustomizeManifestOutcome;
import io.harness.cdng.manifest.yaml.KustomizePatchesManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSetWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.manifest.yaml.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftManifestOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftParamManifestOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.ServerlessAwsLambdaManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.kinds.HelmChartManifest;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.KustomizeManifest;
import io.harness.cdng.manifest.yaml.kinds.KustomizePatchesManifest;
import io.harness.cdng.manifest.yaml.kinds.OpenshiftManifest;
import io.harness.cdng.manifest.yaml.kinds.OpenshiftParamManifest;
import io.harness.cdng.manifest.yaml.kinds.ServerlessAwsLambdaManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
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
import io.harness.cdng.service.beans.WinRmServiceSpec;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.cdng.variables.beans.NGVariableOverrideSets;
import io.harness.ng.core.service.entity.ServiceEntity;
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
  }
}
