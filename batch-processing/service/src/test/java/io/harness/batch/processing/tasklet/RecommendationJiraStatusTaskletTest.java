/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.tasklet;

import static io.harness.ccm.commons.beans.recommendation.RecommendationState.APPLIED;
import static io.harness.ccm.commons.beans.recommendation.ResourceType.AZURE_INSTANCE;
import static io.harness.ccm.commons.beans.recommendation.ResourceType.EC2_INSTANCE;
import static io.harness.ccm.commons.beans.recommendation.ResourceType.ECS_SERVICE;
import static io.harness.ccm.commons.beans.recommendation.ResourceType.GOVERNANCE;
import static io.harness.ccm.commons.beans.recommendation.ResourceType.NODE_POOL;
import static io.harness.ccm.commons.beans.recommendation.ResourceType.WORKLOAD;
import static io.harness.ccm.commons.constants.ViewFieldConstants.THRESHOLD_DAYS_TO_SHOW_RECOMMENDATION;
import static io.harness.ccm.commons.utils.TimeUtils.offsetDateTimeNow;
import static io.harness.rule.OwnerRule.ANMOL;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static io.harness.timescaledb.Tables.CE_RECOMMENDATIONS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.recommendation.CCMJiraDetails;
import io.harness.ccm.commons.beans.recommendation.CCMServiceNowDetails;
import io.harness.ccm.commons.beans.recommendation.ResourceType;
import io.harness.ccm.commons.dao.recommendation.AzureRecommendationDAO;
import io.harness.ccm.commons.dao.recommendation.EC2RecommendationDAO;
import io.harness.ccm.commons.dao.recommendation.ECSRecommendationDAO;
import io.harness.ccm.commons.dao.recommendation.K8sRecommendationDAO;
import io.harness.ccm.jira.CCMJiraHelper;
import io.harness.ccm.serviceNow.CCMServiceNowHelper;
import io.harness.ccm.serviceNow.CCMServiceNowUtils;
import io.harness.ccm.views.dao.RuleExecutionDAO;
import io.harness.jira.JiraConstantsNG;
import io.harness.jira.JiraIssueNG;
import io.harness.rule.Owner;
import io.harness.servicenow.ServiceNowFieldValueNG;
import io.harness.servicenow.ServiceNowTicketNG;
import io.harness.testsupport.BaseTaskletTest;
import io.harness.timescaledb.tables.pojos.CeRecommendations;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jooq.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.batch.repeat.RepeatStatus;

@RunWith(MockitoJUnitRunner.class)
public class RecommendationJiraStatusTaskletTest extends BaseTaskletTest {
  @Mock private CCMJiraHelper mockJiraHelper;
  @Mock private CCMServiceNowHelper mockServiceNowHelper;
  @Mock private CCMServiceNowUtils mockServiceNowUtils;
  @Mock private K8sRecommendationDAO mockK8sRecommendationDAO;
  @Mock private ECSRecommendationDAO mockEcsRecommendationDAO;
  @Mock private EC2RecommendationDAO mockEc2RecommendationDAO;
  @Mock private RuleExecutionDAO mockRuleExecutionDAO;
  @Mock private AzureRecommendationDAO mockAzureRecommendationDAO;

  @InjectMocks private RecommendationJiraStatusTasklet recommendationJiraStatusTaskletUnderTest;

  private final String ACCOUNT_ID = "accountId";
  private final String CONNECTOR_REF = "connectorRef";
  private final String JIRA_KEY_ISSUE = "jiraissuekey";
  private final String SERVICENOW_TASK_KEY = "servicenowissuekey";
  private final String RECOMMENDATION_ID = "recommendationId";
  private final String SERVICENOW_TICKET_TYPE = "Ticket";

  private static final long BATCH_SIZE = 100;
  private List<CeRecommendations> ceRecommendations;
  private JiraIssueNG jiraIssueNG;
  private ServiceNowTicketNG serviceNowTicketNG;

  @Before
  public void setUp() throws IllegalAccessException, IOException {
    when(mockK8sRecommendationDAO.fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationFilter())).thenReturn(1);
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testExecute_WorkloadToDoToDone() throws Exception {
    ceRecommendations = getCeRecommendation(WORKLOAD);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    jiraIssueNG = getJiraIssueNG();
    when(mockJiraHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE)).thenReturn(jiraIssueNG);

    // Run the test
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    // Verify the results
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO).fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE);
    verify(mockJiraHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE);
    verify(mockK8sRecommendationDAO).updateJiraInTimescale(RECOMMENDATION_ID, CONNECTOR_REF, JIRA_KEY_ISSUE, "done");
    verify(mockK8sRecommendationDAO).updateRecommendationState(RECOMMENDATION_ID, APPLIED);
    verify(mockK8sRecommendationDAO)
        .updateJiraInWorkloadRecommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMJiraDetails.builder().connectorRef(CONNECTOR_REF).jiraIssue(jiraIssueNG).build());
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testExecute_WorkloadToDoToDone_Servicenow() throws Exception {
    setupForServicenow();
    ceRecommendations = getCeRecommendationWitServicenowDetails(WORKLOAD);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    serviceNowTicketNG = getServicenowIssueNG();
    when(mockServiceNowHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY))
        .thenReturn(serviceNowTicketNG);
    when(mockServiceNowUtils.getStatus(serviceNowTicketNG)).thenReturn("Completed");

    // Run the test
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    // Verify the results
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE);
    verify(mockServiceNowHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY);
    verify(mockServiceNowUtils).getStatus(serviceNowTicketNG);

    verify(mockK8sRecommendationDAO)
        .updateServicenowDetailsInTimescale(
            RECOMMENDATION_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY, "Completed");
    verify(mockK8sRecommendationDAO)
        .updateServicenowDetailsInWorkloadRecommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMServiceNowDetails.builder().connectorRef(CONNECTOR_REF).serviceNowTicket(serviceNowTicketNG).build());
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testExecute_NodePoolToDoToDone() throws Exception {
    ceRecommendations = getCeRecommendation(NODE_POOL);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    jiraIssueNG = getJiraIssueNG();
    when(mockJiraHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE)).thenReturn(jiraIssueNG);

    // Run the test
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    // Verify the results
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO).fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE);
    verify(mockJiraHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE);
    verify(mockK8sRecommendationDAO).updateJiraInTimescale(RECOMMENDATION_ID, CONNECTOR_REF, JIRA_KEY_ISSUE, "done");
    verify(mockK8sRecommendationDAO).updateRecommendationState(RECOMMENDATION_ID, APPLIED);
    verify(mockK8sRecommendationDAO)
        .updateJiraInNodeRecommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMJiraDetails.builder().connectorRef(CONNECTOR_REF).jiraIssue(jiraIssueNG).build());
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testExecute_NodePoolToDoToDone_Servicenow() throws Exception {
    setupForServicenow();
    ceRecommendations = getCeRecommendationWitServicenowDetails(NODE_POOL);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    serviceNowTicketNG = getServicenowIssueNG();
    when(mockServiceNowHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY))
        .thenReturn(serviceNowTicketNG);
    when(mockServiceNowUtils.getStatus(serviceNowTicketNG)).thenReturn("Completed");

    // Run the test
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    // Verify the results
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE);
    verify(mockServiceNowHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY);
    verify(mockServiceNowUtils).getStatus(serviceNowTicketNG);

    verify(mockK8sRecommendationDAO)
        .updateServicenowDetailsInTimescale(
            RECOMMENDATION_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY, "Completed");
    verify(mockK8sRecommendationDAO)
        .updateServicenowDetailsInNodeRecommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMServiceNowDetails.builder().connectorRef(CONNECTOR_REF).serviceNowTicket(serviceNowTicketNG).build());
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testExecute_ECSToDoToDone() throws Exception {
    ceRecommendations = getCeRecommendation(ECS_SERVICE);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    jiraIssueNG = getJiraIssueNG();
    when(mockJiraHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE)).thenReturn(jiraIssueNG);

    // Run the test
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    // Verify the results
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO).fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE);
    verify(mockJiraHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE);
    verify(mockK8sRecommendationDAO).updateJiraInTimescale(RECOMMENDATION_ID, CONNECTOR_REF, JIRA_KEY_ISSUE, "done");
    verify(mockK8sRecommendationDAO).updateRecommendationState(RECOMMENDATION_ID, APPLIED);
    verify(mockEcsRecommendationDAO)
        .updateJiraInECSRecommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMJiraDetails.builder().connectorRef(CONNECTOR_REF).jiraIssue(jiraIssueNG).build());
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testExecute_ECSToDoToDone_Servicenow() throws Exception {
    setupForServicenow();
    ceRecommendations = getCeRecommendationWitServicenowDetails(ECS_SERVICE);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    serviceNowTicketNG = getServicenowIssueNG();
    when(mockServiceNowHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY))
        .thenReturn(serviceNowTicketNG);
    when(mockServiceNowUtils.getStatus(serviceNowTicketNG)).thenReturn("Completed");

    // Run the test
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    // Verify the results
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE);
    verify(mockServiceNowHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY);
    verify(mockServiceNowUtils).getStatus(serviceNowTicketNG);

    verify(mockK8sRecommendationDAO)
        .updateServicenowDetailsInTimescale(
            RECOMMENDATION_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY, "Completed");
    verify(mockEcsRecommendationDAO)
        .updateServicenowDetailsInECSRecommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMServiceNowDetails.builder().connectorRef(CONNECTOR_REF).serviceNowTicket(serviceNowTicketNG).build());
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testExecute_EC2ToDoToDone() throws Exception {
    ceRecommendations = getCeRecommendation(EC2_INSTANCE);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    jiraIssueNG = getJiraIssueNG();
    when(mockJiraHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE)).thenReturn(jiraIssueNG);

    // Run the test
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    // Verify the results
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO).fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE);
    verify(mockJiraHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE);
    verify(mockK8sRecommendationDAO).updateJiraInTimescale(RECOMMENDATION_ID, CONNECTOR_REF, JIRA_KEY_ISSUE, "done");
    verify(mockK8sRecommendationDAO).updateRecommendationState(RECOMMENDATION_ID, APPLIED);
    verify(mockEc2RecommendationDAO)
        .updateJiraInEC2Recommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMJiraDetails.builder().connectorRef(CONNECTOR_REF).jiraIssue(jiraIssueNG).build());
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testExecute_EC2ToDoToDone_Servicenow() throws Exception {
    setupForServicenow();
    ceRecommendations = getCeRecommendationWitServicenowDetails(EC2_INSTANCE);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    serviceNowTicketNG = getServicenowIssueNG();
    when(mockServiceNowHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY))
        .thenReturn(serviceNowTicketNG);
    when(mockServiceNowUtils.getStatus(serviceNowTicketNG)).thenReturn("Completed");

    // Run the test
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    // Verify the results
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE);
    verify(mockServiceNowHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY);
    verify(mockServiceNowUtils).getStatus(serviceNowTicketNG);

    verify(mockK8sRecommendationDAO)
        .updateServicenowDetailsInTimescale(
            RECOMMENDATION_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY, "Completed");
    verify(mockEc2RecommendationDAO)
        .updateServicenowDetailsInEC2Recommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMServiceNowDetails.builder().connectorRef(CONNECTOR_REF).serviceNowTicket(serviceNowTicketNG).build());
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testExecute_GovernanceToDoToDone() throws Exception {
    ceRecommendations = getCeRecommendation(GOVERNANCE);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    jiraIssueNG = getJiraIssueNG();
    when(mockJiraHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE)).thenReturn(jiraIssueNG);

    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO).fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE);
    verify(mockJiraHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE);
    verify(mockK8sRecommendationDAO).updateJiraInTimescale(RECOMMENDATION_ID, CONNECTOR_REF, JIRA_KEY_ISSUE, "done");
    verify(mockK8sRecommendationDAO).updateRecommendationState(RECOMMENDATION_ID, APPLIED);
    verify(mockRuleExecutionDAO)
        .updateJiraInGovernanceRecommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMJiraDetails.builder().connectorRef(CONNECTOR_REF).jiraIssue(jiraIssueNG).build());
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testExecute_GovernanceToDoToDone_Servicenow() throws Exception {
    setupForServicenow();
    ceRecommendations = getCeRecommendationWitServicenowDetails(GOVERNANCE);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    serviceNowTicketNG = getServicenowIssueNG();
    when(mockServiceNowHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY))
        .thenReturn(serviceNowTicketNG);
    when(mockServiceNowUtils.getStatus(serviceNowTicketNG)).thenReturn("Completed");

    // Run the test
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    // Verify the results
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE);
    verify(mockServiceNowHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY);
    verify(mockServiceNowUtils).getStatus(serviceNowTicketNG);

    verify(mockK8sRecommendationDAO)
        .updateServicenowDetailsInTimescale(
            RECOMMENDATION_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY, "Completed");
    verify(mockRuleExecutionDAO)
        .updateServicenowDetailsInGovernanceRecommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMServiceNowDetails.builder().connectorRef(CONNECTOR_REF).serviceNowTicket(serviceNowTicketNG).build());
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testExecute_AzureInstanceToDoToDone() throws Exception {
    ceRecommendations = getCeRecommendation(AZURE_INSTANCE);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    jiraIssueNG = getJiraIssueNG();
    when(mockJiraHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE)).thenReturn(jiraIssueNG);

    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO).fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationFilter(), 0L, BATCH_SIZE);
    verify(mockJiraHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, JIRA_KEY_ISSUE);
    verify(mockK8sRecommendationDAO).updateJiraInTimescale(RECOMMENDATION_ID, CONNECTOR_REF, JIRA_KEY_ISSUE, "done");
    verify(mockK8sRecommendationDAO).updateRecommendationState(RECOMMENDATION_ID, APPLIED);
    verify(mockAzureRecommendationDAO)
        .updateJiraInAzureRecommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMJiraDetails.builder().connectorRef(CONNECTOR_REF).jiraIssue(jiraIssueNG).build());
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testExecute_AzureToDoToDone_Servicenow() throws Exception {
    setupForServicenow();
    ceRecommendations = getCeRecommendationWitServicenowDetails(AZURE_INSTANCE);
    when(mockK8sRecommendationDAO.fetchRecommendationsOverview(
             ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE))
        .thenReturn(ceRecommendations);

    serviceNowTicketNG = getServicenowIssueNG();
    when(mockServiceNowHelper.getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY))
        .thenReturn(serviceNowTicketNG);
    when(mockServiceNowUtils.getStatus(serviceNowTicketNG)).thenReturn("Completed");

    // Run the test
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);

    // Verify the results
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter());
    verify(mockK8sRecommendationDAO)
        .fetchRecommendationsOverview(ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter(), 0L, BATCH_SIZE);
    verify(mockServiceNowHelper).getIssue(ACCOUNT_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY);
    verify(mockServiceNowUtils).getStatus(serviceNowTicketNG);

    verify(mockK8sRecommendationDAO)
        .updateServicenowDetailsInTimescale(
            RECOMMENDATION_ID, CONNECTOR_REF, SERVICENOW_TICKET_TYPE, SERVICENOW_TASK_KEY, "Completed");
    verify(mockAzureRecommendationDAO)
        .updateServicenowDetailsInAzureRecommendation(ACCOUNT_ID, RECOMMENDATION_ID,
            CCMServiceNowDetails.builder().connectorRef(CONNECTOR_REF).serviceNowTicket(serviceNowTicketNG).build());
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testExecute_K8sRecommendationDAOFetchRecommendationsOverviewReturnsNoItems() throws Exception {
    when(mockK8sRecommendationDAO.fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationFilter())).thenReturn(0);
    final RepeatStatus result = recommendationJiraStatusTaskletUnderTest.execute(null, chunkContext);
    assertThat(result).isNull();
    verify(mockK8sRecommendationDAO).fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationFilter());
  }

  private static Condition getValidRecommendationFilter() {
    return CE_RECOMMENDATIONS.ISVALID
        .eq(true)
        // based on current-gen workload recommendation dataFetcher
        .and(CE_RECOMMENDATIONS.LASTPROCESSEDAT.greaterOrEqual(
            offsetDateTimeNow().truncatedTo(ChronoUnit.DAYS).minusDays(THRESHOLD_DAYS_TO_SHOW_RECOMMENDATION)))
        .and(nonDelegate())
        .and(CE_RECOMMENDATIONS.RECOMMENDATIONSTATE.notEqual("APPLIED"))
        .and(CE_RECOMMENDATIONS.JIRACONNECTORREF.isNotNull())
        .and(CE_RECOMMENDATIONS.JIRACONNECTORREF.notIn("", " "))
        .and(CE_RECOMMENDATIONS.JIRAISSUEKEY.isNotNull())
        .and(CE_RECOMMENDATIONS.JIRAISSUEKEY.notIn("", " "));
  }

  private static Condition getValidRecommendationWithServicenowDetailsFilter() {
    return CE_RECOMMENDATIONS.ISVALID
        .eq(true)
        // based on current-gen workload recommendation dataFetcher
        .and(CE_RECOMMENDATIONS.LASTPROCESSEDAT.greaterOrEqual(
            offsetDateTimeNow().truncatedTo(ChronoUnit.DAYS).minusDays(THRESHOLD_DAYS_TO_SHOW_RECOMMENDATION)))
        .and(nonDelegate())
        .and(CE_RECOMMENDATIONS.RECOMMENDATIONSTATE.notEqual("APPLIED"))
        .and(CE_RECOMMENDATIONS.SERVICENOWCONNECTORREF.isNotNull())
        .and(CE_RECOMMENDATIONS.SERVICENOWCONNECTORREF.notIn("", " "))
        .and(CE_RECOMMENDATIONS.SERVICENOWISSUEKEY.isNotNull())
        .and(CE_RECOMMENDATIONS.SERVICENOWISSUEKEY.notIn("", " "));
  }

  private void setupForServicenow() {
    when(mockK8sRecommendationDAO.fetchRecommendationsCount(ACCOUNT_ID, getValidRecommendationFilter())).thenReturn(0);
    when(mockK8sRecommendationDAO.fetchRecommendationsCount(
             ACCOUNT_ID, getValidRecommendationWithServicenowDetailsFilter()))
        .thenReturn(1);
  }

  private static Condition nonDelegate() {
    return CE_RECOMMENDATIONS.RESOURCETYPE.notEqual(WORKLOAD.name())
        .or(CE_RECOMMENDATIONS.RESOURCETYPE.eq(WORKLOAD.name())
                .and(CE_RECOMMENDATIONS.NAMESPACE.notIn("harness-delegate", "harness-delegate-ng")));
  }

  private List<CeRecommendations> getCeRecommendation(ResourceType resourceType) {
    return List.of(
        new CeRecommendations(RECOMMENDATION_ID, "name", "namespace", 0.0, 0.0, "clustername", resourceType.name(),
            ACCOUNT_ID, false, OffsetDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0), ZoneOffset.UTC),
            OffsetDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0), ZoneOffset.UTC), CONNECTOR_REF, JIRA_KEY_ISSUE,
            "todo", "OPEN", null, null, null, null, null, null));
  }

  private List<CeRecommendations> getCeRecommendationWitServicenowDetails(ResourceType resourceType) {
    return List.of(
        new CeRecommendations(RECOMMENDATION_ID, "name", "namespace", 0.0, 0.0, "clustername", resourceType.name(),
            ACCOUNT_ID, false, OffsetDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0), ZoneOffset.UTC),
            OffsetDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0), ZoneOffset.UTC), null, SERVICENOW_TASK_KEY,
            null, null, null, null, CONNECTOR_REF, SERVICENOW_TASK_KEY, "Open", "Ticket"));
  }

  private JiraIssueNG getJiraIssueNG() {
    return new JiraIssueNG("url", "restUrl", "id", "key",
        Map.ofEntries(Map.entry(JiraConstantsNG.STATUS_INTERNAL_NAME,
            Map.ofEntries(
                Map.entry("name", "done"), Map.entry("statusCategory", Map.ofEntries(Map.entry("key", "done")))))));
  }

  private ServiceNowTicketNG getServicenowIssueNG() {
    Map<String, ServiceNowFieldValueNG> fields = new HashMap<>();
    fields.put(
        "description", ServiceNowFieldValueNG.builder().value("description").displayValue("Description").build());
    fields.put("state", ServiceNowFieldValueNG.builder().value("Completed").displayValue("Completed").build());
    return new ServiceNowTicketNG("url", "number", fields);
  }
}
