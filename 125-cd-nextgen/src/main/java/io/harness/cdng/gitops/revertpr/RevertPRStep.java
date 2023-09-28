/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.gitops.revertpr;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.ListUtils.trimStrings;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executions.steps.ExecutionNodeType.GITOPS_REVERT_PR;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.trim;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.DelegateTaskRequest;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.gitops.githubrestraint.services.GithubRestraintInstanceService;
import io.harness.cdng.gitops.steps.GitOpsStepHelper;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitOpsTaskType;
import io.harness.delegate.task.git.NGGitOpsResponse;
import io.harness.delegate.task.git.NGGitOpsTaskParams;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.ConsumerId;
import io.harness.distribution.constraint.InvalidPermitsException;
import io.harness.distribution.constraint.PermanentlyBlockedConsumerException;
import io.harness.distribution.constraint.UnableToRegisterConsumerException;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.gitopsprovider.entity.GithubRestraintInstance;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncChainExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.steps.TaskRequestsUtils;
import io.harness.steps.executable.AsyncChainExecutableWithRbac;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_GITOPS})
@OwnedBy(GITOPS)
@Slf4j
public class RevertPRStep implements AsyncChainExecutableWithRbac<StepElementParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(GITOPS_REVERT_PR.getYamlType()).setStepCategory(StepCategory.STEP).build();

  private static final String CONSTRAINT_OPERATION = "CREATE_PR";

  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private GitOpsStepHelper gitOpsStepHelper;
  @Inject private CDStepHelper cdStepHelper;
  @Inject @Named("referenceFalseKryoSerializer") private KryoSerializer referenceFalseKryoSerializer;
  @Inject private GithubRestraintInstanceService githubRestraintInstanceService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  public GitFetchFilesConfig getGitFetchFilesConfig(
      Ambiance ambiance, ManifestOutcome manifestOutcome, String commitId) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) manifestOutcome.getStore();
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = cdStepHelper.getConnector(connectorId, ambiance);

    List<String> gitFilePaths = new ArrayList<>();

    GitStoreDelegateConfig gitStoreDelegateConfig = cdStepHelper.getGitStoreDelegateConfigWithApiAccess(
        gitStoreConfig, connectorDTO, gitFilePaths, ambiance, manifestOutcome);

    ScmConnector scmConnector = (ScmConnector) connectorDTO.getConnectorConfig();

    // Overriding the gitStoreDelegateConfig to set the correct version of scmConnector that allows
    // to retain gitConnector metadata required for updating release repo and the commitId.
    GitStoreDelegateConfig rebuiltGitStoreDelegateConfig =
        GitStoreDelegateConfig.builder()
            .gitConfigDTO(scmConnector)
            .apiAuthEncryptedDataDetails(gitStoreDelegateConfig.getApiAuthEncryptedDataDetails())
            .sshKeySpecDTO(gitStoreDelegateConfig.getSshKeySpecDTO())
            .encryptedDataDetails(gitStoreDelegateConfig.getEncryptedDataDetails())
            .fetchType(gitStoreConfig.getGitFetchType())
            .branch(trim(getParameterFieldValue(gitStoreConfig.getBranch())))
            .commitId(commitId)
            .paths(trimStrings(gitFilePaths))
            .connectorName(connectorDTO.getName())
            .manifestType(manifestOutcome.getType())
            .manifestId(manifestOutcome.getIdentifier())
            .optimizedFilesFetch(gitStoreDelegateConfig.isOptimizedFilesFetch())
            .build();

    return GitFetchFilesConfig.builder()
        .identifier(manifestOutcome.getIdentifier())
        .manifestType(manifestOutcome.getType())
        .succeedIfFileNotFound(true)
        .gitStoreDelegateConfig(rebuiltGitStoreDelegateConfig)
        .build();
  }

  private String extractToken(ConnectorInfoDTO connectorInfoDTO) {
    String tokenRefIdentifier = null;

    GithubConnectorDTO githubConnectorDTO = (GithubConnectorDTO) connectorInfoDTO.getConnectorConfig();
    GithubCredentialsDTO githubCredentialsDTO = githubConnectorDTO.getAuthentication().getCredentials();

    if (githubCredentialsDTO instanceof GithubHttpCredentialsDTO) {
      GithubUsernameTokenDTO githubUsernameTokenDTO =
          (GithubUsernameTokenDTO) ((GithubHttpCredentialsDTO) githubCredentialsDTO).getHttpCredentialsSpec();
      tokenRefIdentifier = githubUsernameTokenDTO.getTokenRef().getIdentifier();
    } else if (githubCredentialsDTO instanceof GithubSshCredentialsDTO) {
      tokenRefIdentifier = ((GithubSshCredentialsDTO) githubCredentialsDTO).getSshKeyRef().getIdentifier();
    }
    return tokenRefIdentifier;
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, StepElementParameters stepParameters, AsyncChainExecutableResponse executableResponse) {}

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {}

  @Override
  public AsyncChainExecutableResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    try {
      RevertPRStepParameters gitOpsSpecParams = (RevertPRStepParameters) stepParameters.getSpec();
      ManifestOutcome releaseRepoOutcome = gitOpsStepHelper.getReleaseRepoOutcome(ambiance);
      ConnectorInfoDTO connectorInfoDTO =
          cdStepHelper.getConnector(releaseRepoOutcome.getStore().getConnectorReference().getValue(), ambiance);

      String tokenRefIdentifier = extractToken(connectorInfoDTO);
      String constraintUnitIdentifier =
          CONSTRAINT_OPERATION + AmbianceUtils.getAccountId(ambiance) + tokenRefIdentifier;
      Constraint constraint = githubRestraintInstanceService.createAbstraction(constraintUnitIdentifier);
      String releaseEntityId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
      String consumerId = generateUuid();
      ConstraintUnit constraintUnit = new ConstraintUnit(constraintUnitIdentifier);

      Map<String, Object> constraintContext = populateConstraintContext(constraintUnit, releaseEntityId);

      try {
        Consumer.State state = constraint.registerConsumer(
            constraintUnit, new ConsumerId(consumerId), 1, constraintContext, githubRestraintInstanceService);
        switch (state) {
          case BLOCKED:
            return AsyncChainExecutableResponse.newBuilder().setCallbackId(consumerId).build();
          case ACTIVE:
            try {
              String taskId =
                  queueDelegateTask(ambiance, stepParameters, releaseRepoOutcome, gitOpsSpecParams, connectorInfoDTO);
              return AsyncChainExecutableResponse.newBuilder().setCallbackId(taskId).setChainEnd(true).build();

            } catch (Exception e) {
              log.error("Failed to execute Update Release Repo step", e);
              throw new InvalidRequestException(
                  String.format("Failed to execute Update Release Repo step. %s", e.getMessage()));
            }
          case REJECTED:
            throw new GeneralException("Found already running resourceConstrains, marking this execution as failed");
          default:
            throw new IllegalStateException("This should never happen");
        }

      } catch (InvalidPermitsException | UnableToRegisterConsumerException | PermanentlyBlockedConsumerException e) {
        log.error("Exception on UpdateReleaseRepoStep for id [{}]", AmbianceUtils.obtainCurrentRuntimeId(ambiance), e);
        throw e;
      }

    } catch (Exception e) {
      log.error("Failed to execute Revert PR Repo step", e);
      throw new InvalidRequestException(String.format("Failed to execute Revert PR step. %s", e.getMessage()));
    }
  }

  @Override
  public AsyncChainExecutableResponse executeNextLinkWithSecurityContext(Ambiance ambiance,
      StepElementParameters stepParameters, StepInputPackage inputPackage,
      ThrowingSupplier<ResponseData> responseSupplier) throws Exception {
    try {
      RevertPRStepParameters gitOpsSpecParams = (RevertPRStepParameters) stepParameters.getSpec();
      ManifestOutcome releaseRepoOutcome = gitOpsStepHelper.getReleaseRepoOutcome(ambiance);
      ConnectorInfoDTO connectorInfoDTO =
          cdStepHelper.getConnector(releaseRepoOutcome.getStore().getConnectorReference().getValue(), ambiance);

      String taskId =
          queueDelegateTask(ambiance, stepParameters, releaseRepoOutcome, gitOpsSpecParams, connectorInfoDTO);
      return AsyncChainExecutableResponse.newBuilder().setCallbackId(taskId).setChainEnd(true).build();
    } catch (Exception e) {
      log.error("Failed to execute RevertPR step", e);
      throw new InvalidRequestException(String.format("Failed to execute RevertPR step. %s", e.getMessage()));
    }
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    ResponseData responseData = responseDataSupplier.get();

    NGGitOpsResponse ngGitOpsResponse = (NGGitOpsResponse) responseData;
    if (TaskStatus.SUCCESS.equals(ngGitOpsResponse.getTaskStatus())) {
      RevertPROutcome revertPROutcome = RevertPROutcome.builder()
                                            .prlink(ngGitOpsResponse.getPrLink())
                                            .prNumber(ngGitOpsResponse.getPrNumber())
                                            .commitId(ngGitOpsResponse.getCommitId())
                                            .ref(ngGitOpsResponse.getRef())
                                            .build();

      executionSweepingOutputService.consume(
          ambiance, OutcomeExpressionConstants.REVERT_PR_OUTCOME, revertPROutcome, StepOutcomeGroup.STAGE.name());

      return StepResponse.builder()
          .unitProgressList(ngGitOpsResponse.getUnitProgressData().getUnitProgresses())
          .status(Status.SUCCEEDED)
          .stepOutcome(StepResponse.StepOutcome.builder()
                           .name(OutcomeExpressionConstants.REVERT_PR_OUTCOME)
                           .outcome(revertPROutcome)
                           .build())
          .build();
    }

    return StepResponse.builder()
        .unitProgressList(ngGitOpsResponse.getUnitProgressData().getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder().setErrorMessage(ngGitOpsResponse.getErrorMessage()).build())
        .build();
  }

  private Map<String, Object> populateConstraintContext(ConstraintUnit constraintUnit, String releaseEntityId) {
    Map<String, Object> constraintContext = new HashMap<>();
    constraintContext.put(GithubRestraintInstance.GithubRestraintInstanceKeys.releaseEntityId, releaseEntityId);
    constraintContext.put(GithubRestraintInstance.GithubRestraintInstanceKeys.order,
        githubRestraintInstanceService.getMaxOrder(constraintUnit.getValue()) + 1);

    return constraintContext;
  }

  private String queueDelegateTask(Ambiance ambiance, StepElementParameters stepParameters,
      ManifestOutcome releaseRepoOutcome, RevertPRStepParameters gitOpsSpecParams, ConnectorInfoDTO connectorInfoDTO) {
    List<GitFetchFilesConfig> gitFetchFilesConfig = new ArrayList<>();
    gitFetchFilesConfig.add(getGitFetchFilesConfig(
        ambiance, releaseRepoOutcome, trim(getParameterFieldValue(gitOpsSpecParams.getCommitId()))));

    NGGitOpsTaskParams ngGitOpsTaskParams = NGGitOpsTaskParams.builder()
                                                .gitOpsTaskType(GitOpsTaskType.REVERT_PR)
                                                .gitFetchFilesConfig(gitFetchFilesConfig.get(0))
                                                .accountId(AmbianceUtils.getAccountId(ambiance))
                                                .connectorInfoDTO(connectorInfoDTO)
                                                .prTitle(trim(getParameterFieldValue(gitOpsSpecParams.getPrTitle())))
                                                .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepParameters))
                                  .taskType(TaskType.GITOPS_TASK_NG.name())
                                  .parameters(new Object[] {ngGitOpsTaskParams})
                                  .build();

    TaskRequest taskRequest =
        TaskRequestsUtils.prepareTaskRequestWithTaskSelector(ambiance, taskData, referenceFalseKryoSerializer,
            TaskCategory.DELEGATE_TASK_V2, emptyList(), false, taskData.getTaskType(), emptyList());

    DelegateTaskRequest delegateTaskRequest =
        cdStepHelper.mapTaskRequestToDelegateTaskRequest(taskRequest, taskData, emptySet(), "", true);

    return delegateGrpcClientWrapper.submitAsyncTaskV2(delegateTaskRequest, Duration.ZERO);
  }
}