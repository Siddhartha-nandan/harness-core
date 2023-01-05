/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws.asg.manifest;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.aws.asg.manifest.AsgManifestType.AsgConfiguration;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.asg.AsgMapper;
import io.harness.aws.asg.AsgSdkManager;
import io.harness.aws.asg.manifest.request.AsgConfigurationManifestRequest;
import io.harness.manifest.request.ManifestRequest;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(CDP)
public class AsgConfigurationManifestHandler extends AsgManifestHandler<CreateAutoScalingGroupRequest> {
  @Inject private AsgMapper asgMapper;
  public interface OverrideProperties {
    String minSize = "minSize";
    String maxSize = "maxSize";
    String desiredCapacity = "desiredCapacity";
  }

  public AsgConfigurationManifestHandler(AsgSdkManager asgSdkManager, ManifestRequest manifestRequest) {
    super(asgSdkManager, manifestRequest);
  }

  @Override
  public Class<CreateAutoScalingGroupRequest> getManifestContentUnmarshallClass() {
    return CreateAutoScalingGroupRequest.class;
  }

  public void applyOverrideProperties(
      List<CreateAutoScalingGroupRequest> manifests, Map<String, Object> overrideProperties) {
    CreateAutoScalingGroupRequest createAutoScalingGroupRequest = manifests.get(0);
    overrideProperties.entrySet().stream().forEach(entry -> {
      switch (entry.getKey()) {
        case OverrideProperties.minSize:
          createAutoScalingGroupRequest.setMinSize((Integer) entry.getValue());
          break;
        case OverrideProperties.maxSize:
          createAutoScalingGroupRequest.setMaxSize((Integer) entry.getValue());
          break;
        case OverrideProperties.desiredCapacity:
          createAutoScalingGroupRequest.setDesiredCapacity((Integer) entry.getValue());
          break;
        default:
          // do nothing
      }
    });
  }

  @Override
  public AsgManifestHandlerChainState upsert(AsgManifestHandlerChainState chainState, ManifestRequest manifestRequest) {
    List<CreateAutoScalingGroupRequest> manifests =
        manifestRequest.getManifests().stream().map(this::parseContentToManifest).collect(Collectors.toList());

    AsgConfigurationManifestRequest asgConfigurationManifestRequest = (AsgConfigurationManifestRequest) manifestRequest;

    String asgName = chainState.getAsgName();
    AutoScalingGroup autoScalingGroup = asgSdkManager.getASG(asgName);

    if (asgConfigurationManifestRequest.isUseAlreadyRunningInstances()) {
      if (autoScalingGroup != null) {
        Integer currentAsgMinSize = autoScalingGroup.getMinSize();
        Integer currentAsgMaxSize = autoScalingGroup.getMaxSize();
        Integer currentAsgDesiredCapacity = autoScalingGroup.getDesiredCapacity();
        Map<String, Object> asgConfigurationOverrideProperties = new HashMap<>() {
          {
            put(AsgConfigurationManifestHandler.OverrideProperties.minSize, currentAsgMinSize);
            put(AsgConfigurationManifestHandler.OverrideProperties.maxSize, currentAsgMaxSize);
            put(AsgConfigurationManifestHandler.OverrideProperties.desiredCapacity, currentAsgDesiredCapacity);
          }
        };
        asgConfigurationManifestRequest.setOverrideProperties(asgConfigurationOverrideProperties);
      }
    }

    Map<String, Object> overrideProperties = asgConfigurationManifestRequest.getOverrideProperties();

    if (isNotEmpty(overrideProperties)) {
      applyOverrideProperties(manifests, overrideProperties);
    }

    CreateAutoScalingGroupRequest createAutoScalingGroupRequest = manifests.get(0);
    createAutoScalingGroupRequest.setAutoScalingGroupName(asgName);

    if (autoScalingGroup == null) {
      String operationName = format("Create Asg %s", asgName);
      asgSdkManager.info("Operation `%s` has started", operationName);
      asgSdkManager.createASG(asgName, chainState.getLaunchTemplateVersion(), createAutoScalingGroupRequest);
      asgSdkManager.waitReadyState(asgName, asgSdkManager::checkAllInstancesInReadyState, operationName);
      asgSdkManager.infoBold("Operation `%s` ended successfully", operationName);
    } else {
      String operationName = format("Update Asg %s", asgName);
      asgSdkManager.info("Operation `%s` has started", operationName);
      asgSdkManager.updateASG(asgName, chainState.getLaunchTemplateVersion(), createAutoScalingGroupRequest);
      asgSdkManager.waitReadyState(asgName, asgSdkManager::checkAllInstancesInReadyState, operationName);
      asgSdkManager.infoBold("Operation `%s` ended successfully", operationName);
    }

    AutoScalingGroup finalAutoScalingGroup = asgSdkManager.getASG(asgName);
    chainState.setAutoScalingGroup(finalAutoScalingGroup);

    return chainState;
  }

  @Override
  public AsgManifestHandlerChainState delete(AsgManifestHandlerChainState chainState, ManifestRequest manifestRequest) {
    return chainState;
  }

  @Override
  public AsgManifestHandlerChainState getManifestTypeContent(
      AsgManifestHandlerChainState chainState, ManifestRequest manifestRequest) {
    if (chainState.getAutoScalingGroup() == null) {
      AutoScalingGroup autoScalingGroup = asgSdkManager.getASG(chainState.getAsgName());
      chainState.setAutoScalingGroup(autoScalingGroup);
    }

    AutoScalingGroup autoScalingGroup = chainState.getAutoScalingGroup();
    if (autoScalingGroup != null) {
      String asgConfiguration =
          asgMapper.createAutoScalingGroupRequestFromAutoScalingGroupConfiguration(autoScalingGroup);

      chainState.getPrepareRollbackDataAsgStoreManifestsContent().put(
          AsgConfiguration, Collections.singletonList(asgConfiguration));
    }
    return chainState;
  }
}
