package software.wings.sm.states.spotinst;

import static io.harness.exception.WingsException.USER;
import static io.harness.spotinst.model.SpotInstConstants.CAPACITY;
import static io.harness.spotinst.model.SpotInstConstants.CAPACITY_MAXIMUM_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.CAPACITY_MINIMUM_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.CAPACITY_TARGET_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.CAPACITY_UNIT_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.COMPUTE;
import static io.harness.spotinst.model.SpotInstConstants.ELASTI_GROUP_IMAGE_CONFIG;
import static io.harness.spotinst.model.SpotInstConstants.ELASTI_GROUP_NAME_PLACEHOLDER;
import static io.harness.spotinst.model.SpotInstConstants.GROUP_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.LAUNCH_SPECIFICATION;
import static io.harness.spotinst.model.SpotInstConstants.LB_TYPE_TG;
import static io.harness.spotinst.model.SpotInstConstants.LOAD_BALANCERS_CONFIG;
import static io.harness.spotinst.model.SpotInstConstants.NAME_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.PHASE_PARAM;
import static io.harness.spotinst.model.SpotInstConstants.TG_ARN_PLACEHOLDER;
import static io.harness.spotinst.model.SpotInstConstants.TG_NAME_PLACEHOLDER;
import static io.harness.spotinst.model.SpotInstConstants.UNIT_INSTANCE;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.sm.states.spotinst.SpotInstServiceSetup.SPOTINST_SERVICE_SETUP_COMMAND;
import static software.wings.utils.Validator.notNullCheck;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.exception.WingsException;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import io.harness.spotinst.model.ElastiGroupLoadBalancer;
import io.harness.spotinst.model.ElastiGroupLoadBalancerConfig;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.SpotInstInfrastructureMapping;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.impl.spotinst.SpotInstCommandRequest.SpotInstCommandRequestBuilder;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Misc;
import software.wings.utils.ServiceVersionConvention;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class SpotInstStateHelper {
  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private ActivityService activityService;

  public SpotInstSetupStateExecutionData prepareStateExecutionData(
      ExecutionContext context, SpotInstServiceSetup serviceSetup) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    ServiceElement serviceElement = phaseElement.getServiceElement();

    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceElement.getUuid());
    if (artifact == null) {
      throw new WingsException(format("Unable to find artifact for service id: %s", serviceElement.getUuid()));
    }

    if (serviceSetup.getOlderActiveVersionCountToKeep() == null) {
      serviceSetup.setOlderActiveVersionCountToKeep(Integer.valueOf(3));
    }

    if (serviceSetup.getOlderActiveVersionCountToKeep() <= 0) {
      throw new WingsException("Value for Older Active Versions To Keep Must be > 0");
    }

    SpotInstInfrastructureMapping spotInstInfrastructureMapping =
        (SpotInstInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    Activity activity = createActivity(context, artifact, serviceSetup.getStateType(), SPOTINST_SERVICE_SETUP_COMMAND);

    SettingAttribute settingAttribute =
        settingsService.get(spotInstInfrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> awsEncryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());

    settingAttribute = settingsService.get(spotInstInfrastructureMapping.getSpotinstConnectorId());
    SpotInstConfig spotInstConfig = (SpotInstConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> spotinstEncryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());

    // change ${app.name}__${service.name}__${env.name} to actual name.
    String elastiGroupNamePrefix = serviceSetup.getElastiGroupNamePrefix();

    elastiGroupNamePrefix = isBlank(elastiGroupNamePrefix)
        ? Misc.normalizeExpression(
              ServiceVersionConvention.getPrefix(app.getName(), serviceElement.getName(), env.getName()))
        : Misc.normalizeExpression(context.renderExpression(elastiGroupNamePrefix));

    boolean blueGreen = serviceSetup.isBlueGreen();
    String elastiGroupOriginalJson = context.renderExpression(spotInstInfrastructureMapping.getElasticGroupJson());
    // Updated json with placeholder and 0 capacity
    String elastiGroupJson = generateElastiGroupJsonForDelegateRequest(
        elastiGroupOriginalJson, serviceSetup, spotInstInfrastructureMapping, artifact.getRevision());

    SpotInstTaskParameters spotInstTaskParameters =
        SpotInstSetupTaskParameters.builder()
            .accountId(app.getAccountId())
            .timeoutIntervalInMin(fetchTimeoutIntervalInMin(serviceSetup.getTimeoutIntervalInMin()))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .appId(app.getAppId())
            .activityId(activity.getUuid())
            .commandName(SPOTINST_SERVICE_SETUP_COMMAND)
            .elastiGroupJson(elastiGroupJson)
            .blueGreen(blueGreen)
            .elastiGroupNamePrefix(elastiGroupNamePrefix)
            .loadBalancerName(serviceSetup.getLoadBalancerName())
            .classicLoadBalancer(serviceSetup.isClassicLoadBalancer())
            .stageListenerPort(serviceSetup.getTargetListenerPort())
            .targetListenerProtocol(serviceSetup.getTargetListenerProtocol())
            .prodListenerPort(serviceSetup.getProdListenerPort())
            .image(artifact.getRevision())
            .awsRegion(spotInstInfrastructureMapping.getAwsRegion())
            .build();

    SpotInstCommandRequest commandRequest = SpotInstCommandRequest.builder()
                                                .spotInstTaskParameters(spotInstTaskParameters)
                                                .awsConfig(awsConfig)
                                                .spotInstConfig(spotInstConfig)
                                                .awsEncryptionDetails(awsEncryptedDataDetails)
                                                .spotinstEncryptionDetails(spotinstEncryptedDataDetails)
                                                .build();

    return SpotInstSetupStateExecutionData.builder()
        .envId(env.getUuid())
        .infraMappingId(spotInstInfrastructureMapping.getUuid())
        .commandName(SPOTINST_SERVICE_SETUP_COMMAND)
        .maxInstanceCount(serviceSetup.getMaxInstances())
        .useCurrentRunningInstanceCount(serviceSetup.isUseCurrentRunningCount())
        .currentRunningInstanceCount(fetchCurrentRunningCountForSetupRequest(serviceSetup))
        .serviceId(serviceElement.getUuid())
        .spotinstCommandRequest(commandRequest)
        .elastiGroupOriginalConfig(generateConfigFromJson(elastiGroupJson))
        .build();
  }

  private ElastiGroup generateConfigFromJson(String elastiGroupJson) {
    java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    Gson gson = new Gson();

    // Map<"group": {...entire config...}>, this is elastiGroupConfig json that spotinst exposes
    Map<String, Object> jsonConfigMap = gson.fromJson(elastiGroupJson, mapType);
    Map<String, Object> elastiGroupConfigMap = (Map<String, Object>) jsonConfigMap.get(GROUP_CONFIG_ELEMENT);
    String groupConfigJson = gson.toJson(elastiGroupConfigMap);
    return gson.fromJson(groupConfigJson, ElastiGroup.class);
  }

  public ActivityBuilder generateActivityBuilder(String appName, String appId, String commandName, Type type,
      ExecutionContext executionContext, String commandType, CommandUnitType commandUnitType, Environment environment) {
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);

    return Activity.builder()
        .applicationName(appName)
        .appId(appId)
        .commandName(commandName)
        .type(type)
        .commandType(commandType)
        .commandUnits(Collections.emptyList())
        .commandUnitType(commandUnitType)
        .status(ExecutionStatus.RUNNING)
        .environmentId(environment.getUuid())
        .environmentType(environment.getEnvironmentType())
        .environmentName(environment.getName())
        .workflowType(executionContext.getWorkflowType())
        .workflowExecutionName(executionContext.getWorkflowExecutionName())
        .workflowExecutionId(executionContext.getWorkflowExecutionId())
        .workflowId(executionContext.getWorkflowId())
        .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
        .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
        .triggeredBy(TriggeredBy.builder()
                         .name(workflowStandardParams.getCurrentUser().getName())
                         .email(workflowStandardParams.getCurrentUser().getEmail())
                         .build());
  }

  public DelegateTask getDelegateTask(String accountId, String appId, TaskType taskType, String waitId, String envId,
      String infrastructureMappingId, Object[] parameters, int timeout) {
    return DelegateTask.builder()
        .async(true)
        .accountId(accountId)
        .appId(appId)
        .waitId(waitId)
        .data(TaskData.builder()
                  .taskType(taskType.name())
                  .parameters(parameters)
                  .timeout(TimeUnit.MINUTES.toMillis(timeout))
                  .build())
        .envId(envId)
        .infrastructureMappingId(infrastructureMappingId)
        .build();
  }

  private String generateElastiGroupJsonForDelegateRequest(String elastiGroupJson, SpotInstServiceSetup serviceSetup,
      SpotInstInfrastructureMapping spotInstInfrastructureMapping, String image) {
    elastiGroupJson =
        updateElastiGroupJsonWithPlaceholders(elastiGroupJson, serviceSetup, spotInstInfrastructureMapping, image);
    return elastiGroupJson;
  }

  private String updateElastiGroupJsonWithPlaceholders(String elastiGroupJson, SpotInstServiceSetup serviceSetup,
      SpotInstInfrastructureMapping spotInstInfrastructureMapping, String image) {
    java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    Gson gson = new Gson();

    // Map<"group": {...entire config...}>, this is elastiGroupConfig json that spotinst exposes
    Map<String, Object> jsonConfigMap = gson.fromJson(elastiGroupJson, mapType);

    Map<String, Object> elastiGroupConfigMap = (Map<String, Object>) jsonConfigMap.get(GROUP_CONFIG_ELEMENT);
    // update name value with "${ELASTI_GROUP_NAME}"
    updateName(elastiGroupConfigMap);
    updateInitialCapacity(elastiGroupConfigMap);
    updateLaunchSpecification(serviceSetup, elastiGroupConfigMap, image);
    return gson.toJson(jsonConfigMap);
  }

  private void updateInitialCapacity(Map<String, Object> elastiGroupConfigMap) {
    Map<String, Object> capacityConfig = (Map<String, Object>) elastiGroupConfigMap.get(CAPACITY);

    capacityConfig.put(CAPACITY_MINIMUM_CONFIG_ELEMENT, 0);
    capacityConfig.put(CAPACITY_TARGET_CONFIG_ELEMENT, 0);
    capacityConfig.put(CAPACITY_MAXIMUM_CONFIG_ELEMENT, 0);

    if (!capacityConfig.containsKey(CAPACITY_UNIT_CONFIG_ELEMENT)) {
      capacityConfig.put(CAPACITY_UNIT_CONFIG_ELEMENT, UNIT_INSTANCE);
    }
  }

  private void updateLaunchSpecification(
      SpotInstServiceSetup serviceSetup, Map<String, Object> elastiGroupConfigMap, String image) {
    Map<String, Object> computeConfigMap = (Map<String, Object>) elastiGroupConfigMap.get(COMPUTE);
    Map<String, Object> launchSpecificationMap = (Map<String, Object>) computeConfigMap.get(LAUNCH_SPECIFICATION);

    updateLoadBalancerDetailsIfApplicable(serviceSetup, launchSpecificationMap);
    updateImageDetails(launchSpecificationMap, image);
  }

  private void updateImageDetails(Map<String, Object> launchSpecificationMap, String image) {
    launchSpecificationMap.put(ELASTI_GROUP_IMAGE_CONFIG, image);
  }

  private void updateLoadBalancerDetailsIfApplicable(
      SpotInstServiceSetup serviceSetup, Map<String, Object> launchSpecificationMap) {
    if (serviceSetup.isBlueGreen() || (serviceSetup.isUseLoadBalancer() && !serviceSetup.isClassicLoadBalancer())) {
      launchSpecificationMap.put(LOAD_BALANCERS_CONFIG,
          ElastiGroupLoadBalancerConfig.builder().loadBalancers(generateALBConfigWithPlaceholders()).build());
    }
  }

  private List<ElastiGroupLoadBalancer> generateALBConfigWithPlaceholders() {
    return Arrays.asList(
        ElastiGroupLoadBalancer.builder().arn(TG_ARN_PLACEHOLDER).name(TG_NAME_PLACEHOLDER).type(LB_TYPE_TG).build());
  }

  private void updateName(Map<String, Object> elastiGroupConfigMap) {
    elastiGroupConfigMap.put(NAME_CONFIG_ELEMENT, ELASTI_GROUP_NAME_PLACEHOLDER);
  }

  public Activity createActivity(
      ExecutionContext executionContext, Artifact artifact, String stateType, String command) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    ActivityBuilder activityBuilder = generateActivityBuilder(app.getName(), app.getUuid(), command, Type.Command,
        executionContext, stateType, CommandUnitType.SPOTINST_SETUP, env);

    if (artifact != null) {
      activityBuilder.artifactName(artifact.getDisplayName()).artifactId(artifact.getUuid());
    }

    return activityService.save(activityBuilder.build());
  }

  private int fetchTimeoutIntervalInMin(Integer timeoutIntervalInMin) {
    if (timeoutIntervalInMin == null || timeoutIntervalInMin.intValue() <= 0) {
      return 5;
    }

    return timeoutIntervalInMin.intValue();
  }

  private Integer fetchCurrentRunningCountForSetupRequest(SpotInstServiceSetup serviceSetup) {
    if (!serviceSetup.isUseCurrentRunningCount()) {
      return null;
    }

    if (serviceSetup.getCurrentRunningCount() == null || serviceSetup.getCurrentRunningCount().intValue() == 0) {
      return Integer.valueOf(2);
    }

    return serviceSetup.getCurrentRunningCount();
  }

  public int generateTimeOutForDelegateTask(Integer timeoutIntervalInMin) {
    return 5 + fetchTimeoutIntervalInMin(timeoutIntervalInMin);
  }

  public SpotInstCommandRequestBuilder generateSpotInstCommandRequest(
      SpotInstInfrastructureMapping spotInstInfrastructureMapping, ExecutionContext context) {
    // Details for SpotInstConfig
    SettingAttribute settingAttribute = settingsService.get(spotInstInfrastructureMapping.getSpotinstConnectorId());
    SpotInstConfig spotInstConfig = (SpotInstConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> spotinstEncryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) spotInstConfig, context.getAppId(), context.getWorkflowExecutionId());

    // Details for AwsConfig
    settingAttribute = settingsService.get(spotInstInfrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> awsEncryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) awsConfig, context.getAppId(), context.getWorkflowExecutionId());

    return SpotInstCommandRequest.builder()
        .awsConfig(awsConfig)
        .awsEncryptionDetails(awsEncryptedDataDetails)
        .spotInstConfig(spotInstConfig)
        .spotinstEncryptionDetails(spotinstEncryptedDataDetails);
  }

  public ElastiGroup prepareNewElastiGroupConfigForRollback(SpotInstSetupContextElement setupContextElement) {
    ElastiGroup elastiGroup = setupContextElement.getNewElastiGroupOriginalConfig() != null
        ? setupContextElement.getNewElastiGroupOriginalConfig().clone()
        : null;
    if (elastiGroup != null) {
      elastiGroup.setCapacity(ElastiGroupCapacity.builder().maximum(0).minimum(0).target(0).build());
    }
    return elastiGroup;
  }

  public ElastiGroup prepareOldElastiGroupConfigForRollback(SpotInstSetupContextElement setupContextElement) {
    return setupContextElement.getOldElastiGroupOriginalConfig() != null
        ? setupContextElement.getOldElastiGroupOriginalConfig()
        : null;
  }
}
