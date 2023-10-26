/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.creation;
import static io.harness.cf.pipeline.FeatureFlagStageFilterJsonCreator.FEATURE_FLAG_SUPPORTED_TYPE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.sdk.PmsSdkInstanceService;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
public class NodeTypeLookupServiceImpl implements NodeTypeLookupService {
  @Inject private PmsSdkInstanceService pmsSdkInstanceService;

  @Override
  public String findNodeTypeServiceName(String nodeType) {
    Map<String, Map<String, Set<String>>> map = pmsSdkInstanceService.getInstanceNameToSupportedTypes();
    if (isEmpty(map)) {
      throw new InvalidRequestException("Supported Types Map is empty");
    }
    for (Map.Entry<String, Map<String, Set<String>>> entry : map.entrySet()) {
      Set<String> supportedNodeTypes = new HashSet<>();
      for (Set<String> stringSet : entry.getValue().values()) {
        supportedNodeTypes.addAll(stringSet);
        if (isEmpty(supportedNodeTypes)) {
          continue;
        }

        if (supportedNodeTypes.stream().anyMatch(st -> st.equals(nodeType))) {
          if (nodeType.equals(FEATURE_FLAG_SUPPORTED_TYPE)) {
            return "cf";
          }
          if (nodeType.equals("Custom")) {
            return "pms";
          }
          return entry.getKey();
        }
      }
    }

    throw new InvalidRequestException("Unknown Node type: " + nodeType);
  }
  // create another function here
  @Override
  public Set<String> listOfDistinctModules(List<String> nodeType) {
    Map<String, Map<String, Set<String>>> map = pmsSdkInstanceService.getInstanceNameToSupportedTypes();
    Set<String> modules = new HashSet<>();
    if (isEmpty(map)) {
      throw new InvalidRequestException("Supported Types Map is empty");
    }

    for (String node : nodeType) {
      for (Map.Entry<String, Map<String, Set<String>>> entry : map.entrySet()) {
        Set<String> supportedNodeTypes = new HashSet<>();
        for (Set<String> stringSet : entry.getValue().values()) {
          supportedNodeTypes.addAll(stringSet);
          if (isEmpty(supportedNodeTypes)) {
            continue;
          }

          if (supportedNodeTypes.stream().anyMatch(st -> st.equals(node))) {
            if (node.equals(FEATURE_FLAG_SUPPORTED_TYPE)) {
              modules.add("cf");
            }
            if (node.equals("Custom")) {
              modules.add("pms");
            }
            // return entry.getKey();
            modules.add(entry.getKey());
          }
        }
      }
    }
    return modules;
  }

  @Override
  public Set<String> findStepNodeTypeServiceName(List<String> stepType) {
    Map<String, Map<String, Set<String>>> map = pmsSdkInstanceService.getInstanceNameToSupportedTypes();

    Set<String> modules = new HashSet<>();
    if (isEmpty(map)) {
      throw new InvalidRequestException("Supported Types Map is empty");
    }

    for (String nodeType : stepType) {
      for (Map.Entry<String, Map<String, Set<String>>> entry : map.entrySet()) {
        Set<String> supportedNodeTypes = new HashSet<>();
        for (Set<String> stringSet : entry.getValue().values()) {
          supportedNodeTypes.addAll(stringSet);
          if (isEmpty(supportedNodeTypes)) {
            continue;
          }

          if (supportedNodeTypes.stream().anyMatch(st -> st.equals(nodeType))) {
            modules.add(entry.getKey());
          }
        }
      }
    }
    return modules;
  }
  //    throw new InvalidRequestException("Unknown Node type: " + nodeType);
}
