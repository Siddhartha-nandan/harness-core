/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.instancesyncv2;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.delegate.AccountId;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.exception.runtime.NoInstancesException;
import io.harness.ff.FeatureFlagService;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.CommandExecutionStatus;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.instancesyncv2.CgDeploymentReleaseDetails;
import io.harness.perpetualtask.instancesyncv2.CgInstanceSyncResponse;
import io.harness.perpetualtask.instancesyncv2.InstanceSyncData;
import io.harness.perpetualtask.instancesyncv2.InstanceSyncTrackedDeploymentDetails;
import io.harness.perpetualtask.instancesyncv2.ResponseBatchConfig;
import io.harness.serializer.KryoSerializer;

import software.wings.api.DeploymentEvent;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.instancesyncv2.handler.CgInstanceSyncV2DeploymentHelper;
import software.wings.instancesyncv2.handler.CgInstanceSyncV2DeploymentHelperFactory;
import software.wings.instancesyncv2.model.CgReleaseIdentifiers;
import software.wings.instancesyncv2.model.InstanceSyncTaskDetails;
import software.wings.instancesyncv2.service.CgInstanceSyncTaskDetailsService;
import software.wings.service.impl.SettingsServiceImpl;
import software.wings.service.impl.instance.InstanceHandler;
import software.wings.service.impl.instance.InstanceHandlerFactoryService;
import software.wings.service.impl.instance.InstanceSyncByPerpetualTaskHandler;
import software.wings.service.impl.instance.InstanceSyncPerpetualTaskService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.settings.SettingVariableTypes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Durations;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.ws.rs.NotSupportedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.CDP)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class CgInstanceSyncServiceV2 {
  private final DeploymentService deploymentService;
  public static final String AUTO_SCALE = "AUTO_SCALE";
  public static final int PERPETUAL_TASK_INTERVAL = 10;
  public static final int PERPETUAL_TASK_TIMEOUT = 5;
  public static final int HANDLE_NEW_DEPLOYMENT_MAX_RETRIES = 3;
  private final CgInstanceSyncV2DeploymentHelperFactory helperFactory;
  private final DelegateServiceGrpcClient delegateServiceClient;
  private final CgInstanceSyncTaskDetailsService taskDetailsService;
  private final InfrastructureMappingService infrastructureMappingService;
  private final SettingsServiceImpl cloudProviderService;
  private final KryoSerializer kryoSerializer;
  private final InstanceHandlerFactoryService instanceHandlerFactory;
  private final PersistentLocker persistentLocker;
  private final FeatureFlagService featureFlagService;
  private final InstanceSyncPerpetualTaskService instanceSyncPerpetualTaskService;

  private static final int INSTANCE_COUNT_LIMIT =
      Integer.parseInt(System.getenv().getOrDefault("INSTANCE_SYNC_RESPONSE_BATCH_INSTANCE_COUNT", "100"));
  private static final int RELEASE_COUNT_LIMIT =
      Integer.parseInt(System.getenv().getOrDefault("INSTANCE_SYNC_RESPONSE_BATCH_RELEASE_COUNT", "5"));

  public void handleInstanceSync(DeploymentEvent event) {
    if (Objects.isNull(event)) {
      log.error("Null event sent for Instance Sync Processing. Doing nothing");
      return;
    }

    if (CollectionUtils.isEmpty(event.getDeploymentSummaries())) {
      log.error("No deployment summaries present in the deployment event. Doing nothing");
      return;
    }

    String infraMappingId = event.getDeploymentSummaries().iterator().next().getInfraMappingId();
    String appId = event.getDeploymentSummaries().iterator().next().getAppId();
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    isSupportedCloudProvider(infrastructureMapping.getComputeProviderType());

    int retryCount = 0;
    while (retryCount < HANDLE_NEW_DEPLOYMENT_MAX_RETRIES) {
      try (AcquiredLock lock = persistentLocker.waitToAcquireLock(InfrastructureMapping.class,
               infrastructureMapping.getUuid(), Duration.ofSeconds(200), Duration.ofSeconds(220))) {
        if (lock == null) {
          log.warn("Couldn't acquire infra lock. appId [{}]", infrastructureMapping.getAppId());
          retryCount++;
          continue;
        }
        handleInstanceSyncForNewDeployement(event, infrastructureMapping);
        break;
      } catch (NotSupportedException ex) {
        throw ex;
      } catch (Exception ex) {
        log.error(
            format("Failed Attempt no. [%s] while handling deployment event for executionId [%s], infraMappingId [%s]",
                retryCount + 1, event.getDeploymentSummaries().iterator().next().getWorkflowExecutionId(),
                infrastructureMapping.getUuid()),
            ex);
        retryCount++;
        if (retryCount >= HANDLE_NEW_DEPLOYMENT_MAX_RETRIES) {
          // We have to catch all kinds of exceptions, In case of any Failure we switch to instance sync V1
          throw new RuntimeException(
              format("Exception while handling deployment event for executionId [%s], infraMappingId [%s]",
                  event.getDeploymentSummaries().iterator().next().getWorkflowExecutionId(),
                  infrastructureMapping.getUuid()));
        }
      }
    }
  }

  public void handleInstanceSyncForNewDeployement(DeploymentEvent event, InfrastructureMapping infrastructureMapping) {
    List<DeploymentSummary> deploymentSummaries = event.getDeploymentSummaries();

    if (isEmpty(deploymentSummaries)) {
      log.error("Deployment Summaries can not be empty or null");
      return;
    }

    deploymentSummaries = deploymentSummaries.stream().filter(this::hasDeploymentKey).collect(Collectors.toList());

    deploymentSummaries.forEach(deploymentSummary -> saveDeploymentSummary(deploymentSummary, false));

    InstanceHandler instanceHandler = instanceHandlerFactory.getInstanceHandler(infrastructureMapping);
    instanceHandler.handleNewDeployment(
        event.getDeploymentSummaries(), event.isRollback(), event.getOnDemandRollbackInfo());

    event.getDeploymentSummaries()
        .stream()
        .filter(deployment -> Objects.nonNull(deployment.getDeploymentInfo()))
        .filter(this::hasDeploymentKey)
        .forEach(this::handlePerpetualTask);

    instanceSyncPerpetualTaskService.createPerpetualTasksForNewDeploymentBackup(
        deploymentSummaries, infrastructureMapping);
  }

  private void isSupportedCloudProvider(String cloudProviderType) {
    if (!Objects.equals(cloudProviderType, SettingVariableTypes.KUBERNETES_CLUSTER.name())) {
      throw new NotSupportedException(
          String.format("Cloud Provider Type [%s] is not supported for Instance sync V2", cloudProviderType));
    }
  }

  private void handlePerpetualTask(DeploymentSummary deploymentSummary) {
    SettingAttribute cloudProvider = fetchCloudProvider(deploymentSummary);
    CgInstanceSyncV2DeploymentHelper instanceSyncV2DeploymentHelper =
        helperFactory.getHelper(cloudProvider.getValue().getSettingType());
    String configuredPerpetualTaskId =
        getConfiguredPerpetualTaskId(deploymentSummary, cloudProvider.getUuid(), instanceSyncV2DeploymentHelper);
    if (StringUtils.isEmpty(configuredPerpetualTaskId)) {
      String perpetualTaskId = createInstanceSyncPerpetualTask(cloudProvider);
      trackDeploymentRelease(
          cloudProvider.getUuid(), perpetualTaskId, deploymentSummary, instanceSyncV2DeploymentHelper);
    } else {
      updateInstanceSyncPerpetualTask(cloudProvider, configuredPerpetualTaskId);
    }
  }

  public void processInstanceSyncResult(String perpetualTaskId, CgInstanceSyncResponse result) {
    log.info("Got the result. Starting to process. Perpetual Task Id: [{}] and response [{}]", perpetualTaskId, result);

    if (!result.getExecutionStatus().equals(CommandExecutionStatus.SUCCESS.name())) {
      log.error("Instance Sync failed for perpetual task: [{}] and response [{}], with error: [{}]", perpetualTaskId,
          result, result.getErrorMessage());

      if (!featureFlagService.isEnabled(FeatureName.INSTANCE_SYNC_V2_CG, result.getAccountId())) {
        log.info("Instance sync v2 was disabled for account {} and PT id {}. Restore V1 Perpetual tasks",
            result.getAccountId(), perpetualTaskId);
        try (AcquiredLock lock =
                 persistentLocker.tryToAcquireLock("InstanceSyncV2" + perpetualTaskId, Duration.ofSeconds(180))) {
          if (lock == null) {
            log.warn("Couldn't acquire infra lock. perpetualTaskId [{}]", perpetualTaskId);
            return;
          }
          List<InstanceSyncTaskDetails> instanceSyncTaskDetails =
              taskDetailsService.fetchAllForPerpetualTask(result.getAccountId(), perpetualTaskId);
          restorePerpetualTasks(perpetualTaskId, result, instanceSyncTaskDetails);
        }
      }
      return;
    }

    if (!featureFlagService.isEnabled(FeatureName.INSTANCE_SYNC_V2_CG, result.getAccountId())) {
      log.info("Instance sync v2 was disabled for account {} and PT id {}. Restore V1 Perpetual tasks",
          result.getAccountId(), perpetualTaskId);

      List<InstanceSyncTaskDetails> instanceSyncTaskDetails =
          result.getInstanceDataList()
              .stream()
              .filter(instanceSyncData -> isNotEmpty(instanceSyncData.getTaskDetailsId()))
              .map(instanceSyncData
                  -> taskDetailsService.getInstanceSyncTaskDetails(
                      result.getAccountId(), instanceSyncData.getTaskDetailsId()))
              .collect(Collectors.toList());

      restorePerpetualTasks(perpetualTaskId, result, instanceSyncTaskDetails);
    }

    Map<String, List<InstanceSyncData>> instancesPerTask = new HashMap<>();
    for (InstanceSyncData instanceSyncData : result.getInstanceDataList()) {
      if (!instanceSyncData.getExecutionStatus().equals(CommandExecutionStatus.SUCCESS.name())) {
        log.error("Instance Sync failed for perpetual task: [{}], for task details: [{}], with error: [{}]",
            perpetualTaskId, instanceSyncData.getTaskDetailsId(), instanceSyncData.getErrorMessage());
        continue;
      }

      if (!instancesPerTask.containsKey(instanceSyncData.getTaskDetailsId())) {
        instancesPerTask.put(instanceSyncData.getTaskDetailsId(), new ArrayList<>());
      }

      instancesPerTask.get(instanceSyncData.getTaskDetailsId()).addAll(Arrays.asList(instanceSyncData));
    }

    handlingInstanceSync(instancesPerTask);
  }

  private void handlingInstanceSync(Map<String, List<InstanceSyncData>> instancesPerTask) {
    for (String taskDetailsId : instancesPerTask.keySet()) {
      InstanceSyncTaskDetails taskDetails = taskDetailsService.getForId(taskDetailsId);
      InfrastructureMapping infraMapping =
          infrastructureMappingService.get(taskDetails.getAppId(), taskDetails.getInfraMappingId());
      Optional<InstanceHandler> instanceHandler = Optional.of(instanceHandlerFactory.getInstanceHandler(infraMapping));
      InstanceSyncByPerpetualTaskHandler instanceSyncHandler =
          (InstanceSyncByPerpetualTaskHandler) instanceHandler.get();

      try (AcquiredLock lock = persistentLocker.tryToAcquireLock(
               InfrastructureMapping.class, infraMapping.getUuid(), Duration.ofSeconds(180))) {
        if (lock == null) {
          log.warn("Couldn't acquire infra lock. appId [{}]", infraMapping.getAppId());
          return;
        }
        SettingAttribute cloudProvider = cloudProviderService.get(taskDetails.getCloudProviderId());
        CgInstanceSyncV2DeploymentHelper helper = helperFactory.getHelper(cloudProvider.getValue().getSettingType());
        Map<CgReleaseIdentifiers, InstanceSyncData> cgReleaseIdentifiersInstanceSyncDataMap =
            helper.getCgReleaseIdentifiersList(instancesPerTask.get(taskDetailsId));

        Set<CgReleaseIdentifiers> cgReleaseIdentifiersResult =
            Sets.intersection(taskDetails.getReleaseIdentifiers(), cgReleaseIdentifiersInstanceSyncDataMap.keySet());

        Set<CgReleaseIdentifiers> releasesToDelete = new HashSet<>();
        Set<CgReleaseIdentifiers> releasesToUpdate = new HashSet<>();
        for (CgReleaseIdentifiers cgReleaseIdentifiers : cgReleaseIdentifiersResult) {
          long deleteReleaseAfter = helper.getDeleteReleaseAfter(
              cgReleaseIdentifiers, cgReleaseIdentifiersInstanceSyncDataMap.get(cgReleaseIdentifiers));
          cgReleaseIdentifiers.setDeleteAfter(deleteReleaseAfter);
          if (deleteReleaseAfter > System.currentTimeMillis()) {
            releasesToUpdate.add(cgReleaseIdentifiers);
          } else {
            releasesToDelete.add(cgReleaseIdentifiers);
          }
        }
        try {
          for (InstanceSyncData instanceSyncData : instancesPerTask.get(taskDetailsId)) {
            DelegateResponseData delegateResponse =
                (DelegateResponseData) kryoSerializer.asObject(instanceSyncData.getTaskResponse().toByteArray());

            instanceSyncHandler.processInstanceSyncResponseFromPerpetualTask(infraMapping, delegateResponse);
          }
        } catch (NoInstancesException e) {
          log.error(e.getMessage());
          taskDetailsService.updateLastRun(taskDetailsId, releasesToUpdate, releasesToDelete);
          return;
        }
        taskDetailsService.updateLastRun(taskDetailsId, releasesToUpdate, releasesToDelete);
      }
    }
  }

  private void restorePerpetualTasks(
      String perpetualTaskId, CgInstanceSyncResponse result, List<InstanceSyncTaskDetails> instanceSyncTaskDetails) {
    Map<String, List<InstanceSyncTaskDetails>> instanceTaskMap = new HashMap<>();
    for (InstanceSyncTaskDetails taskDetails : instanceSyncTaskDetails) {
      if (!instanceTaskMap.containsKey(taskDetails.getInfraMappingId())) {
        instanceTaskMap.put(taskDetails.getInfraMappingId(), new ArrayList<>());
      }
      instanceTaskMap.get(taskDetails.getInfraMappingId()).add(taskDetails);
    }

    for (String infraMappingId : instanceTaskMap.keySet()) {
      InfrastructureMapping infraMapping =
          infrastructureMappingService.get(instanceSyncTaskDetails.iterator().next().getAppId(), infraMappingId);
      try (AcquiredLock lock = persistentLocker.tryToAcquireLock(
               InfrastructureMapping.class, infraMapping.getUuid(), Duration.ofSeconds(180))) {
        for (InstanceSyncTaskDetails taskDetails : instanceTaskMap.get(infraMappingId)) {
          if (lock == null) {
            log.warn("Couldn't acquire infra lock. infraMapping [{}]", infraMapping.getUuid());
            return;
          }
          instanceSyncPerpetualTaskService.restorePerpetualTasks(result.getAccountId(), infraMapping);

          taskDetailsService.delete(taskDetails.getUuid());
        }
      }
    }
    if (!taskDetailsService.isInstanceSyncTaskDetailsExist(result.getAccountId(), perpetualTaskId)) {
      delegateServiceClient.deletePerpetualTask(AccountId.newBuilder().setId(result.getAccountId()).build(),
          PerpetualTaskId.newBuilder().setId(perpetualTaskId).build());
    }
  }

  @VisibleForTesting
  DeploymentSummary saveDeploymentSummary(DeploymentSummary deploymentSummary, boolean rollback) {
    if (shouldSaveDeploymentSummary(deploymentSummary, rollback)) {
      return deploymentService.save(deploymentSummary);
    }
    return deploymentSummary;
  }

  @VisibleForTesting
  boolean shouldSaveDeploymentSummary(DeploymentSummary summary, boolean isRollback) {
    if (summary == null) {
      return false;
    }
    if (!isRollback) {
      return true;
    }
    // save rollback for lambda deployments
    return summary.getAwsLambdaDeploymentKey() != null;
  }

  @VisibleForTesting
  boolean hasDeploymentKey(DeploymentSummary deploymentSummary) {
    return deploymentSummary.getK8sDeploymentKey() != null || deploymentSummary.getContainerDeploymentKey() != null
        || deploymentSummary.getAwsAmiDeploymentKey() != null
        || deploymentSummary.getAwsCodeDeployDeploymentKey() != null
        || deploymentSummary.getSpotinstAmiDeploymentKey() != null
        || deploymentSummary.getAwsLambdaDeploymentKey() != null
        || deploymentSummary.getAzureVMSSDeploymentKey() != null
        || deploymentSummary.getAzureWebAppDeploymentKey() != null
        || deploymentSummary.getCustomDeploymentKey() != null;
  }

  private SettingAttribute fetchCloudProvider(DeploymentSummary deploymentSummary) {
    InfrastructureMapping infraMapping =
        infrastructureMappingService.get(deploymentSummary.getAppId(), deploymentSummary.getInfraMappingId());
    return cloudProviderService.get(infraMapping.getComputeProviderSettingId());
  }

  private void updateInstanceSyncPerpetualTask(SettingAttribute cloudProvider, String perpetualTaskId) {
    delegateServiceClient.resetPerpetualTask(AccountId.newBuilder().setId(cloudProvider.getAccountId()).build(),
        PerpetualTaskId.newBuilder().setId(perpetualTaskId).build(),
        helperFactory.getHelper(cloudProvider.getValue().getSettingType()).fetchInfraConnectorDetails(cloudProvider));
  }

  private String createInstanceSyncPerpetualTask(SettingAttribute cloudProvider) {
    String accountId = cloudProvider.getAccountId();

    PerpetualTaskId taskId = delegateServiceClient.createPerpetualTask(AccountId.newBuilder().setId(accountId).build(),
        "CG_INSTANCE_SYNC_V2", preparePerpetualTaskSchedule(),
        PerpetualTaskClientContextDetails.newBuilder()
            .setExecutionBundle(helperFactory.getHelper(cloudProvider.getValue().getSettingType())
                                    .fetchInfraConnectorDetails(cloudProvider))
            .build(),
        true, "CloudProvider: [" + cloudProvider.getUuid() + "] Instance Sync V2 Perpetual Task");
    log.info("Created Perpetual Task with ID: [{}], for account: [{}], and cloud provider: [{}]", taskId.getId(),
        accountId, cloudProvider.getUuid());
    return taskId.getId();
  }

  private PerpetualTaskSchedule preparePerpetualTaskSchedule() {
    return PerpetualTaskSchedule.newBuilder()
        .setInterval(Durations.fromMinutes(PERPETUAL_TASK_INTERVAL))
        .setTimeout(Durations.fromMinutes(PERPETUAL_TASK_TIMEOUT))
        .build();
  }

  private String getConfiguredPerpetualTaskId(DeploymentSummary deploymentSummary, String cloudProviderId,
      CgInstanceSyncV2DeploymentHelper instanceSyncHandler) {
    InstanceSyncTaskDetails instanceSyncTaskDetails =
        taskDetailsService.getForInfraMapping(deploymentSummary.getAccountId(), deploymentSummary.getInfraMappingId());

    if (Objects.nonNull(instanceSyncTaskDetails)) {
      instanceSyncTaskDetails.setReleaseIdentifiers(
          instanceSyncHandler.mergeReleaseIdentifiers(instanceSyncTaskDetails.getReleaseIdentifiers(),
              instanceSyncHandler.buildReleaseIdentifiers(deploymentSummary.getDeploymentInfo())));

      taskDetailsService.save(instanceSyncTaskDetails);
      return instanceSyncTaskDetails.getPerpetualTaskId();
    }

    log.info("No Instance Sync details found for InfraMappingId: [{}]. Proceeding to handling it.",
        deploymentSummary.getInfraMappingId());
    instanceSyncTaskDetails =
        taskDetailsService.fetchForCloudProvider(deploymentSummary.getAccountId(), cloudProviderId);
    if (Objects.isNull(instanceSyncTaskDetails)) {
      log.info("No Perpetual task found for cloud providerId: [{}].", cloudProviderId);
      return StringUtils.EMPTY;
    }

    String perpetualTaskId = instanceSyncTaskDetails.getPerpetualTaskId();
    InstanceSyncTaskDetails newTaskDetails =
        instanceSyncHandler.prepareTaskDetails(deploymentSummary, cloudProviderId, perpetualTaskId);
    taskDetailsService.save(newTaskDetails);
    return perpetualTaskId;
  }

  private void trackDeploymentRelease(String cloudProviderId, String perpetualTaskId,
      DeploymentSummary deploymentSummary, CgInstanceSyncV2DeploymentHelper instanceSyncHandler) {
    InstanceSyncTaskDetails newTaskDetails =
        instanceSyncHandler.prepareTaskDetails(deploymentSummary, cloudProviderId, perpetualTaskId);
    taskDetailsService.save(newTaskDetails);
  }

  public InstanceSyncTrackedDeploymentDetails fetchTaskDetails(String perpetualTaskId, String accountId) {
    List<InstanceSyncTaskDetails> instanceSyncTaskDetails =
        taskDetailsService.fetchAllForPerpetualTask(accountId, perpetualTaskId);
    Map<String, SettingAttribute> cloudProviders = new ConcurrentHashMap<>();

    List<CgDeploymentReleaseDetails> deploymentReleaseDetails = new ArrayList<>();
    instanceSyncTaskDetails.stream().forEach((InstanceSyncTaskDetails taskDetails) -> {
      SettingAttribute cloudProvider =
          cloudProviders.computeIfAbsent(taskDetails.getCloudProviderId(), cloudProviderService::get);
      CgInstanceSyncV2DeploymentHelper instanceSyncV2DeploymentHelper =
          helperFactory.getHelper(cloudProvider.getValue().getSettingType());
      deploymentReleaseDetails.addAll(instanceSyncV2DeploymentHelper.getDeploymentReleaseDetails(taskDetails));
    });

    if (cloudProviders.size() > 1) {
      log.warn("Multiple cloud providers are being tracked by perpetual task: [{}]. This should not happen.",
          perpetualTaskId);
    }

    return InstanceSyncTrackedDeploymentDetails.newBuilder()
        .setAccountId(accountId)
        .setPerpetualTaskId(perpetualTaskId)
        .addAllDeploymentDetails(deploymentReleaseDetails)
        .setResponseBatchConfig(ResponseBatchConfig.newBuilder()
                                    .setReleaseCount(RELEASE_COUNT_LIMIT)
                                    .setInstanceCount(INSTANCE_COUNT_LIMIT)
                                    .build())
        .build();
  }
}
