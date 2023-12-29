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

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import org.apache.commons.lang3.tuple.Triple;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
public abstract class TrafficRoutingUtils {
  protected <T> int sumDestinationWeights(List<T> destinations, Predicate<T> predicate, ToIntFunction<T> function) {
    return destinations != null ? destinations.stream().filter(predicate).mapToInt(function).sum() : 0;
  }

  protected int normalizeWeight(int sum, Integer weight, int groupSize, int cap) {
    if (sum == 0) {
      return (int) Math.round((double) cap / (double) groupSize);
    }
    return weight == null ? 0 : (int) Math.round((double) weight * (double) cap / (double) sum);
  }

  public abstract int sumDestinationsWeights(List destinations);

  public boolean isNormalizationNeeded(List destinations) {
    return isNormalizationNeeded(destinations, 100);
  }

  public boolean isNormalizationNeeded(List destinations, int cap) {
    return sumDestinationsWeights(destinations) != cap;
  }

  public abstract List<Triple<String, Integer, Integer>> getDestinationsNormalizationInfo(List destinations, int cap);

  protected List<Triple<String, Integer, Integer>> getDestinationsNormalizationInfo(
      List<Triple<String, Integer, Integer>> normalizedDestinations, int normalizedSum, int cap) {
    int correctionCount = Math.abs(cap - normalizedSum);
    int step = 1;
    if (normalizedSum > cap) {
      step = -1;
    }

    List<Triple<String, Integer, Integer>> normalizedCorrectedDestinations = new LinkedList<>();
    for (int i = 0; i < normalizedDestinations.size(); i++) {
      if (normalizedDestinations.get(i).getRight() > 0 && correctionCount > 0) {
        normalizedCorrectedDestinations.add(Triple.of(normalizedDestinations.get(i).getLeft(),
            normalizedDestinations.get(i).getMiddle(), normalizedDestinations.get(i).getRight() + step));
        correctionCount--;
      } else {
        normalizedCorrectedDestinations.add(Triple.of(normalizedDestinations.get(i).getLeft(),
            normalizedDestinations.get(i).getMiddle(), normalizedDestinations.get(i).getRight()));
      }
    }

    return normalizedCorrectedDestinations;
  }

  public List normalizeDestinations(List destinations) {
    return normalizeDestinations(destinations, 100);
  }

  public abstract List normalizeDestinations(List destinations, int cap);
}
