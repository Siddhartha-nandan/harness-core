/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactSourceConfig;
import io.harness.cdng.azure.webapp.AzureWebAppRollbackStepNode;
import io.harness.cdng.azure.webapp.AzureWebAppSlotDeploymentStepNode;
import io.harness.cdng.azure.webapp.AzureWebAppSwapSlotStepNode;
import io.harness.cdng.azure.webapp.AzureWebAppTrafficShiftStepNode;
import io.harness.cdng.creator.plan.customDeployment.CustomDeploymentConfig;
import io.harness.cdng.creator.plan.stage.DeploymentStageNode;
import io.harness.cdng.ecs.EcsCanaryDeleteStepNode;
import io.harness.cdng.ecs.EcsCanaryDeployStepNode;
import io.harness.cdng.ecs.EcsRollingDeployStepNode;
import io.harness.cdng.ecs.EcsRollingRollbackStepNode;
import io.harness.cdng.gitops.CreatePRStepNode;
import io.harness.cdng.gitops.MergePRStepNode;
import io.harness.cdng.helm.HelmDeployStepNode;
import io.harness.cdng.helm.HelmRollbackStepNode;
import io.harness.cdng.jenkins.jenkinsstep.JenkinsBuildStepNode;
import io.harness.cdng.k8s.K8sApplyStepNode;
import io.harness.cdng.k8s.K8sBGSwapServicesStepNode;
import io.harness.cdng.k8s.K8sBlueGreenStepNode;
import io.harness.cdng.k8s.K8sCanaryDeleteStepNode;
import io.harness.cdng.k8s.K8sCanaryStepNode;
import io.harness.cdng.k8s.K8sDeleteStepNode;
import io.harness.cdng.k8s.K8sRollingRollbackStepNode;
import io.harness.cdng.k8s.K8sRollingStepNode;
import io.harness.cdng.k8s.K8sScaleStepNode;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.provision.azure.AzureARMRollbackStepNode;
import io.harness.cdng.provision.azure.AzureCreateARMResourceStepNode;
import io.harness.cdng.provision.azure.AzureCreateBPStepNode;
import io.harness.cdng.provision.cloudformation.CloudformationCreateStackStepNode;
import io.harness.cdng.provision.cloudformation.CloudformationDeleteStackStepNode;
import io.harness.cdng.provision.cloudformation.CloudformationRollbackStepNode;
import io.harness.cdng.provision.terraform.TerraformApplyStepNode;
import io.harness.cdng.provision.terraform.TerraformDestroyStepNode;
import io.harness.cdng.provision.terraform.TerraformPlanStepNode;
import io.harness.cdng.provision.terraform.TerraformRollbackStepNode;
import io.harness.cdng.serverless.ServerlessAwsLambdaDeployStepNode;
import io.harness.cdng.serverless.ServerlessAwsLambdaRollbackStepNode;
import io.harness.cdng.ssh.CommandStepNode;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.serializer.kryo.NGKryoRegistrar;
import io.harness.serializer.morphia.NGMorphiaRegistrar;
import io.harness.yaml.schema.beans.SchemaNamespaceConstants;
import io.harness.yaml.schema.beans.YamlGroup;
import io.harness.yaml.schema.beans.YamlSchemaMetadata;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.serializer.kryo.PollingKryoRegistrar;
import java.util.Arrays;
import java.util.Collections;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class CDNGRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(AccessControlClientRegistrars.kryoRegistrars)
          .addAll(ManagerRegistrars.kryoRegistrars)
          .addAll(SMCoreRegistrars.kryoRegistrars)
          .addAll(DelegateServiceDriverRegistrars.kryoRegistrars)
          .addAll(ConnectorNextGenRegistrars.kryoRegistrars)
          .addAll(GitOpsRegistrars.kryoRegistrars)
          .addAll(YamlBeansModuleRegistrars.kryoRegistrars)
          .addAll(CDNGEntityRegistrars.kryoRegistrars)
          .add(NGKryoRegistrar.class)
          .add(PollingKryoRegistrar.class)
          .addAll(DelegateTaskRegistrars.kryoRegistrars)
          .addAll(NGCommonModuleRegistrars.kryoRegistrars)
          .addAll(FileStoreRegistrars.kryoRegistrars)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(ManagerRegistrars.morphiaRegistrars)
          .addAll(SMCoreRegistrars.morphiaRegistrars)
          .addAll(DelegateServiceDriverRegistrars.morphiaRegistrars)
          .addAll(ConnectorNextGenRegistrars.morphiaRegistrars)
          .addAll(GitOpsRegistrars.morphiaRegistrars)
          .add(NGMorphiaRegistrar.class)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .addAll(YamlBeansModuleRegistrars.morphiaRegistrars)
          .addAll(CDNGEntityRegistrars.morphiaRegistrars)
          .addAll(InstanceRegistrars.morphiaRegistrars)
          .addAll(DelegateTaskRegistrars.morphiaRegistrars)
          .addAll(NGCommonModuleRegistrars.morphiaRegistrars)
          .addAll(FileStoreRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableList<YamlSchemaRootClass> yamlSchemaRegistrars =
      ImmutableList.<YamlSchemaRootClass>builder()
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.DEPLOYMENT_STAGE)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(DeploymentStageNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(ImmutableList.of(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STAGE.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.DEPLOYMENT_STEPS)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(CDStepInfo.class)
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.TEMPLATE_CUSTOM_DEPLOYMENT)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(true)
                   .availableAtAccountLevel(true)
                   .clazz(CustomDeploymentConfig.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(ImmutableList.of(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.GITOPS_CREATE_PR)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(CreatePRStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.GITOPS_MERGE_PR)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(MergePRStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_CANARY_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sCanaryStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_APPLY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sApplyStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_BLUE_GREEN_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sBlueGreenStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_ROLLING_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sRollingStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_ROLLING_ROLLBACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sRollingRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_SCALE_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sScaleStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_DELETE_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sDeleteStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_BG_SWAP_SERVICES_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sBGSwapServicesStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_CANARY_DELETE_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sCanaryDeleteStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.TERRAFORM_APPLY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(TerraformApplyStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.TERRAFORM_PLAN_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(TerraformPlanStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.TERRAFORM_DESTROY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(TerraformDestroyStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.TERRAFORM_ROLLBACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(TerraformRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.HELM_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(HelmDeployStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.HELM_ROLLBACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(HelmRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.CLOUDFORMATION_CREATE_STACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(CloudformationCreateStackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.CLOUDFORMATION_DELETE_STACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(CloudformationDeleteStackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.SERVERLESS_AWS_LAMBDA_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(ServerlessAwsLambdaDeployStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.SERVERLESS_AWS_LAMBDA_ROLLBACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(ServerlessAwsLambdaRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.CLOUDFORMATION_ROLLBACK_STACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(CloudformationRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.AZURE_CREATE_ARM_RESOURCE_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(AzureCreateARMResourceStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.AZURE_CREATE_BP_RESOURCE_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(AzureCreateBPStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.AZURE_ROLLBACK_ARM_RESOURCE_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(AzureARMRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.COMMAND_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(CommandStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.AZURE_SLOT_DEPLOYMENT_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(AzureWebAppSlotDeploymentStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.AZURE_TRAFFIC_SHIFT_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(AzureWebAppTrafficShiftStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.AZURE_SWAP_SLOT_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(AzureWebAppSwapSlotStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.AZURE_WEBAPP_ROLLBACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(AzureWebAppRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.JENKINS_BUILD)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(JenkinsBuildStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Arrays.asList(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.ECS_ROLLING_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(EcsRollingDeployStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.ECS_ROLLING_ROLLBACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(EcsRollingRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.ECS_CANARY_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(EcsCanaryDeployStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.ECS_CANARY_DELETE_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(EcsCanaryDeleteStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.ARTIFACT_SOURCE_TEMPLATE)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(true)
                   .availableAtAccountLevel(true)
                   .clazz(ArtifactSourceConfig.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(ImmutableList.of(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .build();
}
