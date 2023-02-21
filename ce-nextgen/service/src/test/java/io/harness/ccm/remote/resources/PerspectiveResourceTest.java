/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.remote.resources;

import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.graphql.core.budget.BudgetCostService;
import io.harness.ccm.graphql.core.budget.BudgetService;
import io.harness.ccm.rbac.CCMRbacHelper;
import io.harness.ccm.remote.resources.perspectives.PerspectiveResource;
import io.harness.ccm.service.intf.CCMNotificationService;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.entities.ViewType;
import io.harness.ccm.views.helper.AwsAccountFieldHelper;
import io.harness.ccm.views.service.CEReportScheduleService;
import io.harness.ccm.views.service.CEViewFolderService;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.outbox.api.OutboxService;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PerspectiveResourceTest extends CategoryTest {
  private CEViewService ceViewService = mock(CEViewService.class);
  private CEViewFolderService ceViewFolderService = mock(CEViewFolderService.class);
  private ViewCustomFieldService viewCustomFieldService = mock(ViewCustomFieldService.class);
  private CEReportScheduleService ceReportScheduleService = mock(CEReportScheduleService.class);
  private BudgetCostService budgetCostService = mock(BudgetCostService.class);
  private BudgetService budgetService = mock(BudgetService.class);
  private CCMNotificationService notificationService = mock(CCMNotificationService.class);
  private AwsAccountFieldHelper awsAccountFieldHelper = mock(AwsAccountFieldHelper.class);
  private TelemetryReporter telemetryReporter = mock(TelemetryReporter.class);
  private TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
  private OutboxService outboxService = mock(OutboxService.class);
  private CCMRbacHelper rbacHelper = mock(CCMRbacHelper.class);
  private PerspectiveResource perspectiveResource;

  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String NAME = "PERSPECTIVE_NAME";
  private final String PERSPECTIVE_ID = "PERSPECTIVE_ID";
  private final ViewState PERSPECTIVE_STATE = ViewState.DRAFT;
  private final ViewType PERSPECTIVE_TYPE = ViewType.CUSTOMER;
  private final String perspectiveVersion = "v1";
  private final String NEW_NAME = "PERSPECTIVE_NAME_NEW";
  private final String UNIFIED_TABLE_NAME = "unified";
  private final String PERSPECTIVE_FOLDER_ID = "FOLDER_ID";

  private CEView perspective;

  @Before
  public void setUp() throws IllegalAccessException, IOException {
    perspective = CEView.builder()
                      .accountId(ACCOUNT_ID)
                      .viewState(PERSPECTIVE_STATE)
                      .viewType(PERSPECTIVE_TYPE)
                      .name(NAME)
                      .uuid(PERSPECTIVE_ID)
                      .viewVersion(perspectiveVersion)
                      .folderId(PERSPECTIVE_FOLDER_ID)
                      .build();
    when(ceViewService.get(PERSPECTIVE_ID)).thenReturn(perspective);
    when(ceViewService.save(perspective, false)).thenReturn(perspective);
    when(ceViewService.update(perspective)).thenReturn(perspective);
    when(budgetService.deleteBudgetsForPerspective(ACCOUNT_ID, PERSPECTIVE_ID)).thenReturn(true);
    when(notificationService.delete(PERSPECTIVE_ID, ACCOUNT_ID)).thenReturn(true);

    perspectiveResource = new PerspectiveResource(ceViewService, ceReportScheduleService, viewCustomFieldService,
        budgetCostService, budgetService, notificationService, awsAccountFieldHelper, telemetryReporter,
        transactionTemplate, outboxService, rbacHelper, ceViewFolderService);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testCreatePerspective() {
    perspectiveResource.create(ACCOUNT_ID, false, perspective);
    verify(ceViewService).save(perspective, false);
    verify(ceViewService).updateTotalCost(perspective);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testCreatePerspectiveWithoutName() {
    perspective.setName("");
    ResponseDTO<CEView> response = perspectiveResource.create(ACCOUNT_ID, false, perspective);
    assertThat(response.getData()).isNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testModifyPerspective() {
    perspective.setName(NEW_NAME);
    perspectiveResource.update(ACCOUNT_ID, perspective);
    verify(ceViewService).update(perspective);
    verify(ceViewService).updateTotalCost(perspective);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testDeleteReportSetting() {
    perspectiveResource.delete(ACCOUNT_ID, PERSPECTIVE_ID);
    verify(ceViewService).delete(PERSPECTIVE_ID, ACCOUNT_ID);
  }
}
