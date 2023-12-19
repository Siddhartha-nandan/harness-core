/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.plan.creation.creators;

import static io.harness.pms.plan.creation.PlanCreatorConstants.YAML_VERSION;
import static io.harness.pms.sdk.PmsSdkModuleUtils.PLAN_CREATOR_SERVICE_EXECUTOR;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.ErrorMetadata;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.ErrorResponseV2;
import io.harness.pms.contracts.plan.FilterCreationBlobRequest;
import io.harness.pms.contracts.plan.FilterCreationBlobResponse;
import io.harness.pms.contracts.plan.FilterCreationResponse;
import io.harness.pms.contracts.plan.PlanCreationBlobRequest;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc.PlanCreationServiceImplBase;
import io.harness.pms.contracts.plan.VariablesCreationBlobRequest;
import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.contracts.plan.VariablesCreationResponse;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.PmsSdkModuleUtils;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreatorService;
import io.harness.pms.sdk.core.plan.creation.PlanCreationResponseBlobHelper;
import io.harness.pms.sdk.core.plan.creation.beans.MergePlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.variables.VariableCreatorService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_PIPELINE, HarnessModuleComponent.CDS_TEMPLATE_LIBRARY})
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class PlanCreatorService extends PlanCreationServiceImplBase {
  @Inject @Named(PLAN_CREATOR_SERVICE_EXECUTOR) private Executor executor;
  @Inject ExceptionManager exceptionManager;
  @Inject @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME) String serviceName;

  private final FilterCreatorService filterCreatorService;
  private final VariableCreatorService variableCreatorService;
  private final List<PartialPlanCreator<?>> planCreators;
  private final PlanCreationResponseBlobHelper planCreationResponseBlobHelper;
  private final PmsGitSyncHelper pmsGitSyncHelper;
  @Inject
  public PlanCreatorService(@NotNull PipelineServiceInfoDecorator serviceInfoDecorator,
      @NotNull FilterCreatorService filterCreatorService, VariableCreatorService variableCreatorService,
      PlanCreationResponseBlobHelper planCreationResponseBlobHelper, PmsGitSyncHelper pmsGitSyncHelper) {
    this.planCreators = serviceInfoDecorator.getPlanCreators();
    this.filterCreatorService = filterCreatorService;
    this.variableCreatorService = variableCreatorService;
    this.planCreationResponseBlobHelper = planCreationResponseBlobHelper;
    this.pmsGitSyncHelper = pmsGitSyncHelper;
  }

  @Override
  public void createPlan(PlanCreationBlobRequest request,
      StreamObserver<io.harness.pms.contracts.plan.PlanCreationResponse> responseObserver) {
    PlanCreationContextValue planCreationContextValue = null;
    if (request != null && request.getContext() != null) {
      List<PlanCreationContextValue> planCreationContextValues =
          request.getContext().values().stream().collect(Collectors.toList());
      if (planCreationContextValues.size() != 0) {
        planCreationContextValue = planCreationContextValues.get(0);
      }
    }
    try (AutoLogContext autoLogContext =
             PlanCreatorServiceHelper.autoLogContextFromPlanCreationContextValue(planCreationContextValue)) {
      io.harness.pms.contracts.plan.PlanCreationResponse planCreationResponse;
      long start = System.currentTimeMillis();
      PlanCreationContext ctx = PlanCreationContext.builder().globalContext(request.getContextMap()).build();
      try (AutoLogContext ignore = PlanCreatorServiceHelper.autoLogContextWithRandomRequestId(ctx)) {
        try {
          MergePlanCreationResponse finalResponse =
              createPlanForDependenciesRecursive(request.getDeps(), ctx, request.getServiceAffinityMap());
          planCreationResponse = getPlanCreationResponseFromFinalResponse(finalResponse);
        } catch (Exception ex) {
          log.error(ExceptionUtils.getMessage(ex), ex);
          WingsException processedException = exceptionManager.processException(ex);
          planCreationResponse =
              io.harness.pms.contracts.plan.PlanCreationResponse.newBuilder()
                  .setErrorResponse(
                      ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(processedException)).build())
                  .build();
        } finally {
          log.info(
              "[PMS_PlanCreatorService_Time] Total Sdk Plan Creation time took {}ms for initial dependencies size {}",
              System.currentTimeMillis() - start, request.getDeps().getDependenciesMap().size());
        }
      }

      responseObserver.onNext(planCreationResponse);
      responseObserver.onCompleted();
    }
  }

  private MergePlanCreationResponse createPlanForDependenciesRecursive(
      Dependencies initialDependencies, PlanCreationContext ctx, Map<String, String> serviceAffinityMap) {
    // TODO: Add patch version before sending the response back
    MergePlanCreationResponse finalResponse =
        MergePlanCreationResponse.builder().serviceAffinityMap(serviceAffinityMap).build();
    if (EmptyPredicate.isEmpty(planCreators) || EmptyPredicate.isEmpty(initialDependencies.getDependenciesMap())) {
      return finalResponse;
    }

    long start = System.currentTimeMillis();
    try (PmsGitSyncBranchContextGuard ignore =
             pmsGitSyncHelper.createGitSyncBranchContextGuardFromBytes(ctx.getGitSyncBranchContext(), true)) {
      Dependencies dependencies = initialDependencies.toBuilder().build();
      YamlField fullField;
      try {
        fullField = YamlUtils.readTree(dependencies.getYaml());
      } catch (IOException ex) {
        String message = "Invalid yaml during plan creation";
        log.error(message, ex);
        throw new InvalidRequestException(message);
      }
      Map<String, JsonNode> fqnToJsonMap = new HashMap<>();
      while (!dependencies.getDependenciesMap().isEmpty()) {
        dependencies = createPlanForDependencies(
            ctx, finalResponse, dependencies, finalResponse.getServiceAffinityMap(), fullField, fqnToJsonMap);
        PlanCreatorServiceHelper.removeInitialDependencies(dependencies, initialDependencies);
      }
      if (finalResponse.getDependencies() != null
          && EmptyPredicate.isNotEmpty(finalResponse.getDependencies().getDependenciesMap())) {
        finalResponse.setDependencies(
            PlanCreatorServiceHelper.removeInitialDependencies(finalResponse.getDependencies(), initialDependencies));
      }
    }

    return finalResponse;
  }

  public Dependencies createPlanForDependencies(PlanCreationContext ctx, MergePlanCreationResponse finalResponse,
      Dependencies dependencies, Map<String, String> serviceAffinityMap, YamlField fullField,
      Map<String, JsonNode> fqnToJsonMap) {
    if (EmptyPredicate.isEmpty(dependencies.getDependenciesMap())) {
      return dependencies;
    }
    CompletableFutures<PlanCreationResponse> completableFutures = new CompletableFutures<>(executor);
    List<Map.Entry<String, String>> dependenciesList = new ArrayList<>(dependencies.getDependenciesMap().entrySet());
    String currentYaml = dependencies.getYaml();
    long start = System.currentTimeMillis();

    // Iterating dependencies to create plan for each dependency by submitting parallel threads of executor thread.
    dependenciesList.forEach(key -> completableFutures.supplyAsync(() -> {
      try {
        return createPlanForDependencyInternal(currentYaml, fullField.fromYamlPath(key.getValue()), ctx,
            dependencies.getDependencyMetadataMap().get(key.getKey()), serviceAffinityMap.get(key.getKey()));
      } catch (IOException e) {
        throw new InvalidRequestException("Unable to parse the field in the path:" + key.getValue());
      }
    }));

    try {
      List<PlanCreationResponse> planCreationResponses = completableFutures.allOf().get(5, TimeUnit.MINUTES);
      Dependencies dep = PlanCreatorServiceHelper.handlePlanCreationResponses(
          planCreationResponses, finalResponse, currentYaml, dependencies, dependenciesList, fullField, fqnToJsonMap);
      return dep;
    } catch (Exception ex) {
      throw new UnexpectedException(format("Unexpected plan creation error: %s", ex.getMessage()), ex);
    }
  }

  @Override
  public void createFilter(FilterCreationBlobRequest request, StreamObserver<FilterCreationResponse> responseObserver) {
    try (AutoLogContext autoLogContext =
             PlanCreatorServiceHelper.autoLogContextFromSetupMetadata(request.getSetupMetadata())) {
      FilterCreationResponse filterCreationResponse;
      try {
        FilterCreationBlobResponse response = filterCreatorService.createFilterBlobResponse(request);
        filterCreationResponse = FilterCreationResponse.newBuilder().setBlobResponse(response).build();
      } catch (Exception ex) {
        log.error(ExceptionUtils.getMessage(ex), ex);
        WingsException processedException = exceptionManager.processException(ex);
        filterCreationResponse =
            FilterCreationResponse.newBuilder()
                .setErrorResponseV2(
                    ErrorResponseV2.newBuilder()
                        .addErrors(ErrorMetadata.newBuilder()
                                       .setWingsExceptionErrorCode(String.valueOf(processedException.getCode()))
                                       .setErrorMessage(ExceptionUtils.getMessage(processedException))
                                       .build())
                        .build())
                .build();
      }

      responseObserver.onNext(filterCreationResponse);
      responseObserver.onCompleted();
    }
  }

  @Override
  public void createVariablesYaml(
      VariablesCreationBlobRequest request, StreamObserver<VariablesCreationResponse> responseObserver) {
    VariablesCreationResponse variablesCreationResponse;
    try {
      VariablesCreationBlobResponse response = variableCreatorService.createVariablesResponse(request);
      variablesCreationResponse = VariablesCreationResponse.newBuilder().setBlobResponse(response).build();
    } catch (Exception ex) {
      log.error(
          String.format("Error in creating variables YAML for the following accountId %s, orgId %s, and projId %s.",
              request.getMetadata().getMetadata().get("accountIdentifier"),
              request.getMetadata().getMetadata().get("orgIdentifier"),
              request.getMetadata().getMetadata().get("projectIdentifier")),
          ex);
      variablesCreationResponse =
          VariablesCreationResponse.newBuilder()
              .setErrorResponse(ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(ex)).build())
              .build();
    }

    responseObserver.onNext(variablesCreationResponse);
    responseObserver.onCompleted();
  }

  private io.harness.pms.contracts.plan.PlanCreationResponse getPlanCreationResponseFromFinalResponse(
      MergePlanCreationResponse finalResponse) {
    if (EmptyPredicate.isNotEmpty(finalResponse.getErrorMessages())) {
      return io.harness.pms.contracts.plan.PlanCreationResponse.newBuilder()
          .setErrorResponse(ErrorResponse.newBuilder().addAllMessages(finalResponse.getErrorMessages()).build())
          .build();
    }
    return io.harness.pms.contracts.plan.PlanCreationResponse.newBuilder()
        .setBlobResponse(planCreationResponseBlobHelper.toBlobResponse(finalResponse))
        .build();
  }

  // Method to create plan for single dependency.
  // Dependency passed from parent to its children plan creator
  private PlanCreationResponse createPlanForDependencyInternal(String currentYaml, YamlField field,
      PlanCreationContext ctx, Dependency dependency, String currentNodeServiceAffinity) {
    try (AutoLogContext ignore = PlanCreatorUtils.autoLogContext(ctx.getAccountIdentifier(), ctx.getOrgIdentifier(),
             ctx.getProjectIdentifier(), ctx.getPipelineIdentifier(), ctx.getExecutionUuid())) {
      try {
        String fullyQualifiedName = YamlUtils.getFullyQualifiedName(field.getNode());
        String yamlVersion = getYamlVersion(ctx, dependency);
        Optional<PartialPlanCreator<?>> planCreatorOptional =
            PlanCreatorServiceHelper.findPlanCreator(planCreators, field, yamlVersion);
        if (!planCreatorOptional.isPresent()) {
          return null;
        }

        PartialPlanCreator planCreator = planCreatorOptional.get();
        Object obj;
        String executionInputTemplate = "";
        if (HarnessYamlVersion.V0.equals(ctx.getYamlVersion())) {
          executionInputTemplate = planCreator.getExecutionInputTemplateAndModifyYamlField(field);
        }
        if (HarnessYamlVersion.isV1(yamlVersion)) {
          obj = planCreator.getFieldObject(field);
        } else {
          Class<?> cls = planCreator.getFieldClass();
          obj = YamlField.class.isAssignableFrom(cls) ? field : YamlUtils.read(field.getNode().toString(), cls);
        }
        try {
          // TODO: look into performance of the AbstractStagePlanCreator
          PlanCreationResponse planForField = planCreator.createPlanForField(
              PlanCreationContext.cloneWithCurrentField(ctx, field, currentYaml, dependency, executionInputTemplate),
              obj);
          if (!EmptyPredicate.isEmpty(executionInputTemplate)) {
            planForField.setExecutionInputTemplateInPlanNode(executionInputTemplate);
          }
          PlanCreatorServiceHelper.decorateResponseWithRollbackModeBehaviour(dependency, planForField);
          PlanCreatorServiceHelper.decorateNodesWithStageFqn(field, planForField, ctx.getYamlVersion());
          PlanCreatorServiceHelper.decorateCreationResponseWithServiceAffinity(
              planForField, serviceName, field, currentNodeServiceAffinity);
          PlanCreatorServiceHelper.decorateResponseWithParentInfo(dependency, planForField);
          return planForField;
        } catch (Exception ex) {
          log.error(format("Error creating plan for node: %s", fullyQualifiedName), ex);
          return PlanCreationResponse.builder()
              .errorMessage(
                  format("Could not create plan for node [%s]: %s", fullyQualifiedName, ExceptionUtils.getMessage(ex)))
              .build();
        }
      } catch (IOException ex) {
        String message = format("Invalid yaml path [%s] during execution plan creation", field.getYamlPath());
        if (ex.getCause() instanceof InvalidYamlException) {
          message = ex.getCause().getMessage();
        }
        log.error(message, ex);
        return PlanCreationResponse.builder().errorMessage(message).build();
      }
    }
  }

  private String getYamlVersion(PlanCreationContext ctx, Dependency dependency) {
    String yamlVersion = ctx.getYamlVersion();
    // If the yamlVersion is V0 then do v0 only.
    if (!HarnessYamlVersion.V0.equals(yamlVersion)) {
      if (dependency != null && dependency.getParentInfo().getDataMap().containsKey(YAML_VERSION)) {
        yamlVersion = dependency.getParentInfo().getDataMap().get(YAML_VERSION).getStringValue();
      }
    }
    return yamlVersion;
  }
}
