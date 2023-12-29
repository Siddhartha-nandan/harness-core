/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.k8s.trafficrouting.util;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.k8s.model.smi.Backend;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Triple;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
public class SMITrafficRoutingUtils extends TrafficRoutingUtils {
  @Override
  public int sumDestinationsWeights(List destinations) {
    return sumDestinationWeights(destinations, d -> d != null, Backend::getWeight);
  }

  @Override
  public List<Triple<String, Integer, Integer>> getDestinationsNormalizationInfo(List destinations, int cap) {
    int sum = sumDestinationsWeights(destinations);

    List<Triple<String, Integer, Integer>> normalizedDestinations = new LinkedList<>();
    AtomicInteger normalizedSum = new AtomicInteger();
    ((List<Backend>) destinations).stream().forEach(dest -> {
      int normalizedWeight = normalizeWeight(sum, dest.getWeight(), destinations.size(), cap);
      normalizedDestinations.add(Triple.of(dest.getService(), dest.getWeight(), normalizedWeight));
      normalizedSum.addAndGet(normalizedWeight);
    });

    return getDestinationsNormalizationInfo(normalizedDestinations, normalizedSum.get(), cap);
  }

  @Override
  public List normalizeDestinations(List destinations, int cap) {
    List<Triple<String, Integer, Integer>> normalizedCorrectedDestinations =
        getDestinationsNormalizationInfo(destinations, cap);
    return normalizedCorrectedDestinations.stream()
        .map(dest -> Backend.builder().service(dest.getLeft()).weight(dest.getRight()).build())
        .collect(Collectors.toList());
  }
}
