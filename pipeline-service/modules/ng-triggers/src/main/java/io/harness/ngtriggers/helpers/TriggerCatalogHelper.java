/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngtriggers.helpers;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.FeatureName;
import io.harness.ngtriggers.beans.entity.metadata.catalog.TriggerCatalogItem;
import io.harness.ngtriggers.beans.entity.metadata.catalog.TriggerCatalogType;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.featureflagfilter.TriggerCatalogFilter;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.Singleton;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ECS})
@Singleton
public class TriggerCatalogHelper {
  @Inject TriggerCatalogFilter enumFilter;

  public List<TriggerCatalogItem> getTriggerTypeToCategoryMapping(String accountIdentifier) {
    final Map<NGTriggerType, List<TriggerCatalogType>> triggerCategoryListMap =
        Arrays.stream(TriggerCatalogType.values())
            .filter(enumFilter.filter(accountIdentifier, FeatureName.NG_SVC_ENV_REDESIGN))
            .filter(enumFilter.filter(accountIdentifier, FeatureName.CODE_ENABLED))
            .collect(Collectors.groupingBy(catalogType -> TriggerCatalogType.getTriggerCategory(catalogType)));
    return triggerCategoryListMap.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry
            -> TriggerCatalogItem.builder()
                   .category(entry.getKey())
                   .triggerCatalogType(new ArrayList<>(entry.getValue()))
                   .build())
        .collect(Collectors.toList());
  }
}
