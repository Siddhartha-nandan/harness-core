package io.harness.cdng.k8s;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.k8s.beans.K8sExecutionPassThroughData;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.k8s.K8sBGDeployRequest;
import io.harness.delegate.task.k8s.K8sBGDeployResponse;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.GeneralException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDP)
public class K8sBlueGreenStepTest extends AbstractK8sStepExecutorTestBase {
  @Mock ExecutionSweepingOutputService executionSweepingOutputService;
  @InjectMocks private K8sBlueGreenStep k8sBlueGreenStep;

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTask() {
    K8sBlueGreenStepParameters stepParameters = new K8sBlueGreenStepParameters();
    stepParameters.setSkipDryRun(ParameterField.createValueField(true));
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.createValueField("30m")).build();

    K8sBGDeployRequest request = executeTask(stepElementParameters, K8sBGDeployRequest.class);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.getTaskType()).isEqualTo(K8sTaskType.BLUE_GREEN_DEPLOY);
    assertThat(request.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
    assertThat(request.getManifestDelegateConfig()).isEqualTo(manifestDelegateConfig);
    assertThat(request.isSkipDryRun()).isTrue();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(30);
    assertThat(request.isSkipResourceVersioning()).isTrue();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTaskNullParameterFields() {
    K8sBlueGreenStepParameters stepParameters = new K8sBlueGreenStepParameters();
    stepParameters.setSkipDryRun(ParameterField.ofNull());
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.ofNull()).build();

    K8sBGDeployRequest request = executeTask(stepElementParameters, K8sBGDeployRequest.class);
    assertThat(request.isSkipDryRun()).isFalse();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(K8sStepHelper.getTimeoutInMin(stepElementParameters));
    assertThat(request.isSkipResourceVersioning()).isTrue();
  }

  @SneakyThrows
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testOutcomesInResponse() {
    K8sBlueGreenStepParameters stepParameters = new K8sBlueGreenStepParameters();
    final StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();

    K8sDeployResponse k8sDeployResponse =
        K8sDeployResponse.builder()
            .k8sNGTaskResponse(
                K8sBGDeployResponse.builder().primaryColor("blue").stageColor("green").releaseNumber(1).build())
            .commandUnitsProgress(UnitProgressData.builder().build())
            .commandExecutionStatus(SUCCESS)
            .build();
    when(k8sStepHelper.getReleaseName(any())).thenReturn("releaseName");
    StepResponse response = k8sBlueGreenStep.finalizeExecution(
        ambiance, stepElementParameters, K8sExecutionPassThroughData.builder().build(), () -> k8sDeployResponse);
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(response.getStepOutcomes()).hasSize(1);

    StepOutcome outcome = response.getStepOutcomes().stream().collect(Collectors.toList()).get(0);
    assertThat(outcome.getOutcome()).isInstanceOf(K8sBlueGreenOutcome.class);
    assertThat(outcome.getName()).isEqualTo(OutcomeExpressionConstants.OUTPUT);
    assertThat(outcome.getGroup()).isNull();

    ArgumentCaptor<K8sBlueGreenOutcome> argumentCaptor = ArgumentCaptor.forClass(K8sBlueGreenOutcome.class);
    verify(executionSweepingOutputService, times(1))
        .consume(eq(ambiance), eq(OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME), argumentCaptor.capture(),
            eq(StepOutcomeGroup.STAGE.name()));
    assertThat(argumentCaptor.getValue().getReleaseName()).isEqualTo("releaseName");
    assertThat(argumentCaptor.getValue().getPrimaryColor()).isEqualTo("blue");
    assertThat(argumentCaptor.getValue().getStageColor()).isEqualTo("green");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFinalizeExecutionException() {
    final StepElementParameters stepElementParameters = StepElementParameters.builder().build();
    final Exception thrownException = new GeneralException("Something went wrong");
    final K8sExecutionPassThroughData executionPassThroughData = K8sExecutionPassThroughData.builder().build();
    final StepResponse stepResponse = StepResponse.builder().status(Status.FAILED).build();

    doReturn(stepResponse).when(k8sStepHelper).handleTaskException(ambiance, executionPassThroughData, thrownException);

    StepResponse response = k8sBlueGreenStep.finalizeExecution(
        ambiance, stepElementParameters, executionPassThroughData, () -> { throw thrownException; });

    assertThat(response).isEqualTo(stepResponse);

    verify(k8sStepHelper, times(1)).handleTaskException(ambiance, executionPassThroughData, thrownException);
  }

  @Override
  protected K8sStepExecutor getK8sStepExecutor() {
    return k8sBlueGreenStep;
  }
}