package software.wings.service.impl.workflow;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_NOTIFICATION;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.PIPELINE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.persistence.HQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongodb.morphia.query.FieldEnd;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureNotification;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.Service;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.states.PhaseSubWorkflow;

import java.util.ArrayList;
import java.util.List;

public class WorkflowNotificationHelperTest extends WingsBaseTest {
  private static final String BASE_URL = "https://env.harness.io/";
  private static final String EXPECTED_WORKFLOW_URL =
      "https://env.harness.io/#/account/ACCOUNT_ID/app/APP_ID/env/ENV_ID/executions/WORKFLOW_EXECUTION_ID/details";
  private static final String EXPECTED_PIPELINE_URL =
      "https://env.harness.io/#/account/ACCOUNT_ID/app/APP_ID/pipeline-execution/PIPELINE_EXECUTION_ID/workflow-execution/WORKFLOW_EXECUTION_ID/details";

  @Mock private WorkflowService workflowService;
  @Mock private NotificationService notificationService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private MainConfiguration configuration;
  @Mock private WingsPersistence wingsPersistence;
  @Inject @InjectMocks private WorkflowNotificationHelper workflowNotificationHelper;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ExecutionContextImpl executionContext;
  @Mock private HQuery<StateExecutionInstance> stateExecutionInstanceQuery;
  @Mock private FieldEnd stateExecutionInstanceEnd;
  @Mock private HQuery<WorkflowExecution> workflowExecutionQuery;
  @Mock private FieldEnd workflowExecutionEnd;
  @Mock private NotificationSetupService notificationSetupService;

  @Before
  public void setUp() throws Exception {
    when(executionContext.getApp())
        .thenReturn(anApplication().accountId(ACCOUNT_ID).uuid(APP_ID).name(APP_NAME).build());
    when(executionContext.getArtifacts())
        .thenReturn(ImmutableList.of(anArtifact()
                                         .withArtifactSourceName("artifact-1")
                                         .withMetadata(ImmutableMap.of(ArtifactMetadataKeys.BUILD_NO, "build-1"))
                                         .withServiceIds(ImmutableList.of("service-1"))
                                         .build(),
            anArtifact()
                .withArtifactSourceName("artifact-2")
                .withMetadata(ImmutableMap.of(ArtifactMetadataKeys.BUILD_NO, "build-2"))
                .withServiceIds(ImmutableList.of("service-2"))
                .build(),
            anArtifact()
                .withArtifactSourceName("artifact-3")
                .withMetadata(ImmutableMap.of(ArtifactMetadataKeys.BUILD_NO, "build-3"))
                .withServiceIds(ImmutableList.of("service-3"))
                .build()));
    when(executionContext.getEnv()).thenReturn(anEnvironment().uuid(ENV_ID).name(ENV_NAME).appId(APP_ID).build());
    when(executionContext.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    when(executionContext.getWorkflowExecutionName()).thenReturn(WORKFLOW_NAME);
    when(executionContext.getAppId()).thenReturn(APP_ID);
    when(executionContext.getWorkflowId()).thenReturn(WORKFLOW_ID);
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(aWorkflowStandardParams().build());
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID, true, emptySet()))
        .thenReturn(WorkflowExecution.builder()
                        .serviceIds(asList("service-1", "service-2"))
                        .triggeredBy(EmbeddedUser.builder().name(USER_NAME).build())
                        .build());
    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setUrl(BASE_URL);
    when(configuration.getPortal()).thenReturn(portalConfig);
    when(serviceResourceService.get(APP_ID, "service-1", false))
        .thenReturn(Service.builder().uuid("service-1").name("Service One").build());
    when(serviceResourceService.get(APP_ID, "service-2", false))
        .thenReturn(Service.builder().uuid("service-2").name("Service Two").build());
    when(wingsPersistence.createQuery(StateExecutionInstance.class)).thenReturn(stateExecutionInstanceQuery);
    when(stateExecutionInstanceQuery.filter(any(), any())).thenReturn(stateExecutionInstanceQuery);
    when(stateExecutionInstanceQuery.get()).thenReturn(aStateExecutionInstance().startTs(1234L).endTs(2345L).build());
    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(workflowExecutionQuery);
    when(workflowExecutionQuery.filter(any(), any())).thenReturn(workflowExecutionQuery);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSendWorkflowStatusChangeNotification() {
    NotificationRule notificationRule =
        setupNotificationRule(ExecutionScope.WORKFLOW, asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS));

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.SUCCESS);

    verifyNotifications(notificationRule);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSendWorkflowStatusChangeNotificationForTemplatedNotificationGroup() {
    List<NotificationGroup> notificationGroupList = new ArrayList<>();
    notificationGroupList.add(aNotificationGroup().withName("${workflow.variables.MyNotificationGroups}").build());

    NotificationRule notificationRule = aNotificationRule()
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withConditions(asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS))
                                            .withNotificationGroupAsExpression(true)
                                            .withNotificationGroups(notificationGroupList)
                                            .build();

    when(workflowService.readWorkflow(any(), any()))
        .thenReturn(aWorkflow()
                        .orchestrationWorkflow(
                            aCanaryOrchestrationWorkflow().withNotificationRules(asList(notificationRule)).build())
                        .build());

    when(executionContext.renderExpression("${workflow.variables.MyNotificationGroups}")).thenReturn("Group1, Group2");

    when(notificationSetupService.readNotificationGroupByName(ACCOUNT_ID, "Group1"))
        .thenReturn(aNotificationGroup().withName("Group1").build());

    when(notificationSetupService.readNotificationGroupByName(ACCOUNT_ID, "Group2"))
        .thenReturn(aNotificationGroup().withName("Group2").build());

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.SUCCESS);

    verifyNotifications(notificationRule);
  }

  private void verifyNotifications(NotificationRule notificationRule) {
    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(InformationNotification.class);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.<String, String>builder()
            .put("WORKFLOW_NAME", WORKFLOW_NAME)
            .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
            .put("VERB", "completed")
            .put("PHASE_NAME", "")
            .put("ARTIFACTS", "Service One: artifact-1 (build# build-1), Service Two: artifact-2 (build# build-2)")
            .put("USER_NAME", USER_NAME)
            .put("PIPELINE", "")
            .put("APP_NAME", APP_NAME)
            .put("ENV_NAME", ENV_NAME)
            .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSendWorkflowStatusChangeNotificationPipeline() {
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID, true, emptySet()))
        .thenReturn(WorkflowExecution.builder()
                        .serviceIds(asList("service-1", "service-2"))
                        .triggeredBy(EmbeddedUser.builder().name(USER_NAME).build())
                        .pipelineExecutionId(PIPELINE_EXECUTION_ID)
                        .pipelineSummary(
                            PipelineSummary.builder().pipelineId(PIPELINE_ID).pipelineName("Pipeline Name").build())
                        .build());
    NotificationRule notificationRule =
        setupNotificationRule(ExecutionScope.WORKFLOW, asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS));

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.<String, String>builder()
            .put("WORKFLOW_NAME", WORKFLOW_NAME)
            .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
            .put("VERB", "failed")
            .put("PHASE_NAME", "")
            .put("ARTIFACTS", "Service One: artifact-1 (build# build-1), Service Two: artifact-2 (build# build-2)")
            .put("USER_NAME", USER_NAME)
            .put("PIPELINE", " as part of <<<" + EXPECTED_PIPELINE_URL + "|-|Pipeline Name>>> pipeline")
            .put("APP_NAME", APP_NAME)
            .put("ENV_NAME", ENV_NAME)
            .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSendWorkflowPhaseStatusChangeNotification() {
    NotificationRule notificationRule =
        setupNotificationRule(ExecutionScope.WORKFLOW_PHASE, singletonList(ExecutionStatus.FAILED));

    PhaseSubWorkflow phaseSubWorkflow = Mockito.mock(PhaseSubWorkflow.class);
    when(phaseSubWorkflow.getName()).thenReturn("Phase1");
    when(phaseSubWorkflow.getServiceId()).thenReturn("service-2");
    workflowNotificationHelper.sendWorkflowPhaseStatusChangeNotification(
        executionContext, ExecutionStatus.FAILED, phaseSubWorkflow);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders = ImmutableMap.<String, String>builder()
                                                    .put("WORKFLOW_NAME", WORKFLOW_NAME)
                                                    .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
                                                    .put("VERB", "failed")
                                                    .put("PHASE_NAME", "Phase1 of ")
                                                    .put("ARTIFACTS", "Service Two: artifact-2 (build# build-2)")
                                                    .put("USER_NAME", USER_NAME)
                                                    .put("PIPELINE", "")
                                                    .put("ENV_NAME", ENV_NAME)
                                                    .put("APP_NAME", APP_NAME)
                                                    .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSendWorkflowStatusChangeNotificationNoArtifacts() {
    when(executionContext.getArtifacts()).thenReturn(null);
    NotificationRule notificationRule =
        setupNotificationRule(ExecutionScope.WORKFLOW, asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS));

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.<String, String>builder()
            .put("WORKFLOW_NAME", WORKFLOW_NAME)
            .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
            .put("VERB", "failed")
            .put("PHASE_NAME", "")
            .put("ARTIFACTS", "Service One: no artifact, Service Two: no artifact")
            .put("USER_NAME", USER_NAME)
            .put("PIPELINE", "")
            .put("ENV_NAME", ENV_NAME)
            .put("APP_NAME", APP_NAME)
            .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSendWorkflowStatusChangeNotificationSomeArtifacts() {
    when(executionContext.getArtifacts())
        .thenReturn(ImmutableList.of(anArtifact()
                                         .withArtifactSourceName("artifact-1")
                                         .withMetadata(ImmutableMap.of(ArtifactMetadataKeys.BUILD_NO, "build-1"))
                                         .withServiceIds(ImmutableList.of("service-1"))
                                         .build()));
    NotificationRule notificationRule =
        setupNotificationRule(ExecutionScope.WORKFLOW, asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS));

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.<String, String>builder()
            .put("WORKFLOW_NAME", WORKFLOW_NAME)
            .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
            .put("VERB", "failed")
            .put("PHASE_NAME", "")
            .put("ARTIFACTS", "Service One: artifact-1 (build# build-1), Service Two: no artifact")
            .put("USER_NAME", USER_NAME)
            .put("PIPELINE", "")
            .put("ENV_NAME", ENV_NAME)
            .put("APP_NAME", APP_NAME)
            .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSendWorkflowStatusChangeNotificationNoServices() {
    when(executionContext.getArtifacts()).thenReturn(null);
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID, true, emptySet()))
        .thenReturn(WorkflowExecution.builder().triggeredBy(EmbeddedUser.builder().name(USER_NAME).build()).build());
    NotificationRule notificationRule =
        setupNotificationRule(ExecutionScope.WORKFLOW, asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS));

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(asList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId())
        .isEqualTo(NotificationMessageType.WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders = ImmutableMap.<String, String>builder()
                                                    .put("WORKFLOW_NAME", WORKFLOW_NAME)
                                                    .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
                                                    .put("VERB", "failed")
                                                    .put("PHASE_NAME", "")
                                                    .put("ARTIFACTS", "no services")
                                                    .put("USER_NAME", USER_NAME)
                                                    .put("PIPELINE", "")
                                                    .put("ENV_NAME", ENV_NAME)
                                                    .put("APP_NAME", APP_NAME)
                                                    .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSendWorkflowStatusChangeNotificationBuildWorkflow() {
    when(executionContext.getEnv()).thenReturn(null);
    when(executionContext.getArtifacts()).thenReturn(null);
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID, true, emptySet()))
        .thenReturn(WorkflowExecution.builder().triggeredBy(EmbeddedUser.builder().name(USER_NAME).build()).build());
    NotificationRule notificationRule =
        setupNotificationRule(ExecutionScope.WORKFLOW, asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS));

    when(executionContext.getOrchestrationWorkflowType()).thenReturn(OrchestrationWorkflowType.BUILD);

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId())
        .isEqualTo(NotificationMessageType.WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.<String, String>builder()
            .put("WORKFLOW_NAME", WORKFLOW_NAME)
            .put("WORKFLOW_URL",
                "https://env.harness.io/#/account/ACCOUNT_ID/app/APP_ID/env/build/executions/WORKFLOW_EXECUTION_ID/details")
            .put("VERB", "failed")
            .put("PHASE_NAME", "")
            .put("ARTIFACTS", "no services")
            .put("USER_NAME", USER_NAME)
            .put("PIPELINE", "")
            .put("ENV_NAME", "no environment")
            .put("APP_NAME", APP_NAME)
            .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  public NotificationRule setupNotificationRule(ExecutionScope scope, List<ExecutionStatus> executionStatuses) {
    NotificationRule notificationRule =
        aNotificationRule().withExecutionScope(scope).withConditions(executionStatuses).build();

    when(workflowService.readWorkflow(any(), any()))
        .thenReturn(aWorkflow()
                        .orchestrationWorkflow(
                            aCanaryOrchestrationWorkflow().withNotificationRules(asList(notificationRule)).build())
                        .build());
    return notificationRule;
  }
}
