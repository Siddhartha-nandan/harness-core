/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.budgetGroup.service;

import io.harness.ccm.budget.BudgetBreakdown;
import io.harness.ccm.budget.BudgetSummary;
import io.harness.ccm.budget.ValueDataPoint;
import io.harness.ccm.budgetGroup.BudgetGroup;
import io.harness.ccm.budgetGroup.BudgetGroupChildEntityDTO;
import io.harness.ccm.commons.entities.budget.BudgetData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BudgetGroupService {
  String save(BudgetGroup budgetGroup);
  void update(String uuid, String accountId, BudgetGroup budgetGroup);
  BudgetGroup get(String uuid, String accountId);
  List<BudgetGroup> list(
      String accountId, Set<String> allowedFolderIds, HashMap<String, String> perspectiveIdAndFolderIds);
  List<BudgetGroup> list(String accountId, List<String> budgetGroupIds);
  boolean delete(String uuid, String accountId);
  BudgetGroup updateProportionsOnDeletion(BudgetGroupChildEntityDTO deletedChildEntity, BudgetGroup parentBudgetGroup);
  List<ValueDataPoint> getAggregatedAmount(
      String accountId, boolean areChildEntitiesBudgets, List<String> childEntityIds);
  List<BudgetSummary> listAllEntities(
      String accountId, Set<String> allowedFolderIds, HashMap<String, String> perspectiveIdAndFolderIds);
  List<BudgetSummary> listBudgetsAndBudgetGroupsSummary(
      String accountId, String id, Set<String> allowedFolderIds, HashMap<String, String> perspectiveIdAndFolderIds);
  BudgetData getBudgetGroupTimeSeriesStats(BudgetGroup budgetGroup, BudgetBreakdown breakdown);
  void cascadeBudgetGroupAmount(BudgetGroup budgetGroup);
  void updateBudgetGroupCosts(BudgetGroup budgetGroup);
  void updateCostsOfParentBudgetGroupsOnEntityDeletion(BudgetGroup immediateParent);
  List<String> findPerspectiveIdsGivenBudgetGroup(String accountId, List<BudgetGroup> budgetGroups);
  Set<String> getFolderIdsGivenBudgetIds(
      String accountId, List<String> budgetIds, Map<String, String> perspectiveIdAndFolderIds);
}
