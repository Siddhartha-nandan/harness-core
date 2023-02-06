/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.step.elastigroup;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cdng.elastigroup.AwsCloudProviderBasicConfig;
import io.harness.cdng.elastigroup.AwsLoadBalancerConfigYaml;
import io.harness.cdng.elastigroup.CloudProvider;
import io.harness.cdng.elastigroup.CloudProviderType;
import io.harness.cdng.elastigroup.ElastigroupBGStageSetupStepInfo;
import io.harness.cdng.elastigroup.ElastigroupBGStageSetupStepNode;
import io.harness.cdng.elastigroup.ElastigroupCurrentRunningInstances;
import io.harness.cdng.elastigroup.ElastigroupFixedInstances;
import io.harness.cdng.elastigroup.ElastigroupInstances;
import io.harness.cdng.elastigroup.ElastigroupInstancesType;
import io.harness.cdng.elastigroup.ElastigroupSetupStepInfo;
import io.harness.cdng.elastigroup.ElastigroupSetupStepNode;
import io.harness.cdng.elastigroup.LoadBalancer;
import io.harness.cdng.elastigroup.LoadBalancerType;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.ngmigration.beans.WorkflowMigrationContext;
import io.harness.ngmigration.beans.WorkflowStepSupportStatus;
import io.harness.ngmigration.service.step.StepMapper;
import io.harness.ngmigration.utils.MigratorUtility;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.Timeout;

import software.wings.beans.GraphNode;
import software.wings.sm.State;
import software.wings.sm.states.spotinst.SpotInstServiceSetup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ElastigroupSetupStepMapperImpl extends StepMapper {
  @Override
  public String getStepType(GraphNode stepYaml) {
    // Fix: This is incorrect as this mapper can generate 2 types of steps.
    // ELASTIGROUP_SETUP and ELASTIGROUP_BG_STAGE_SETUP;
    return StepSpecTypeConstants.ELASTIGROUP_BG_STAGE_SETUP;
  }

  @Override
  public ParameterField<Timeout> getTimeout(State state) {
    SpotInstServiceSetup elastigroupState = (SpotInstServiceSetup) state;
    Integer timeoutIntervalInMin = elastigroupState.getTimeoutIntervalInMin();
    if (null != timeoutIntervalInMin) {
      return MigratorUtility.getTimeout(timeoutIntervalInMin * 60 * 1000);
    } else {
      return MigratorUtility.getTimeout(state.getTimeoutMillis());
    }
  }

  @Override
  public State getState(GraphNode stepYaml) {
    Map<String, Object> properties = getProperties(stepYaml);
    SpotInstServiceSetup state = new SpotInstServiceSetup(stepYaml.getName());
    state.parseProperties(properties);
    return state;
  }

  @Override
  public AbstractStepNode getSpec(WorkflowMigrationContext context, GraphNode graphNode) {
    SpotInstServiceSetup state = (SpotInstServiceSetup) getState(graphNode);
    ElastigroupInstances instances;
    if (state.isUseCurrentRunningCount()) {
      instances = ElastigroupInstances.builder()
                      .type(ElastigroupInstancesType.CURRENT_RUNNING)
                      .spec(ElastigroupCurrentRunningInstances.builder().build())
                      .build();
    } else {
      instances = ElastigroupInstances.builder()
                      .type(ElastigroupInstancesType.FIXED)
                      .spec(ElastigroupFixedInstances.builder()
                                .min(getParameterFieldInteger(state.getMinInstances()))
                                .max(getParameterFieldInteger(state.getMaxInstances()))
                                .desired(getParameterFieldInteger(state.getTargetInstances()))
                                .build())
                      .build();
    }

    AbstractStepNode result;

    if (isNotEmpty(state.getAwsLoadBalancerConfigs())) {
      ElastigroupBGStageSetupStepNode node = new ElastigroupBGStageSetupStepNode();
      baseSetup(state, node);
      List<LoadBalancer> loadBalancers = new ArrayList<>();
      List<LoadBalancerDetailsForBGDeployment> awsLoadBalancerConfigs = state.getAwsLoadBalancerConfigs();
      for (LoadBalancerDetailsForBGDeployment lb : awsLoadBalancerConfigs) {
        AwsLoadBalancerConfigYaml configYaml =
            AwsLoadBalancerConfigYaml.builder()
                .loadBalancer(ParameterField.createValueField(lb.getLoadBalancerName()))
                .prodListenerPort(ParameterField.createValueField(lb.getProdListenerPort()))
                .prodListenerRuleArn(ParameterField.createValueField(lb.getProdRuleArn()))
                .stageListenerPort(ParameterField.createValueField(lb.getStageListenerPort()))
                .stageListenerRuleArn(ParameterField.createValueField(lb.getStageRuleArn()))
                .build();
        loadBalancers.add(
            LoadBalancer.builder().type(LoadBalancerType.AWS_LOAD_BALANCER_CONFIG).spec(configYaml).build());
      }

      CloudProvider cloudProvider = CloudProvider.builder()
                                        .type(CloudProviderType.AWS)
                                        .spec(AwsCloudProviderBasicConfig.builder()
                                                  .connectorRef(ParameterField.createValueField("<+input>"))
                                                  .region(ParameterField.createValueField("<+input>"))
                                                  .build())
                                        .build();
      ElastigroupBGStageSetupStepInfo info =
          ElastigroupBGStageSetupStepInfo.infoBuilder()
              .name(ParameterField.createValueField(state.getElastiGroupNamePrefix()))
              .instances(instances)
              .connectedCloudProvider(cloudProvider)
              .loadBalancers(loadBalancers)
              .build();

      node.setElastigroupBGStageSetupStepInfo(info);
      result = node;
    } else {
      ElastigroupSetupStepNode node = new ElastigroupSetupStepNode();
      baseSetup(state, node);
      ElastigroupSetupStepInfo elastigroupSetupStepInfo =
          ElastigroupSetupStepInfo.infoBuilder()
              .name(ParameterField.createValueField(state.getElastiGroupNamePrefix()))
              .instances(instances)
              .build();

      node.setElastigroupSetupStepInfo(elastigroupSetupStepInfo);
      result = node;
    }
    return result;
  }

  private static ParameterField<Integer> getParameterFieldInteger(String expressionValue) {
    try {
      return ParameterField.createValueField(Integer.valueOf(expressionValue));
    } catch (Exception e) {
      return new ParameterField(null, null, true, false, expressionValue, null, false);
    }
  }

  @Override
  public boolean areSimilar(GraphNode stepYaml1, GraphNode stepYaml2) {
    // @deepak: Please re-evaluate
    return false;
  }

  @Override
  public WorkflowStepSupportStatus stepSupportStatus(GraphNode graphNode) {
    return WorkflowStepSupportStatus.SUPPORTED;
  }
}
