package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

import software.wings.beans.AppContainer;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.infrastructure.Host;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 6/9/16.
 */
public class CommandExecutionContext {
  private String accountId;
  private String envId;
  private Host host;
  private String appId;
  private String activityId;
  private String runtimePath;
  private String stagingPath;
  private String backupPath;
  private String serviceTemplateId;
  private ExecutionCredential executionCredential;
  private AppContainer appContainer;
  private List<ArtifactFile> artifactFiles;
  private Map<String, String> serviceVariables = Maps.newHashMap();
  private Map<String, String> envVariables = Maps.newHashMap();
  private SettingAttribute hostConnectionAttributes;
  private SettingAttribute bastionConnectionAttributes;
  private ArtifactStreamAttributes artifactStreamAttributes;
  private SettingAttribute cloudProviderSetting;
  private String clusterName;
  private String serviceName;
  private String region;
  private CodoDeployParams codoDeployParams;
  private Integer desiredCount;
  private Integer desiredPercentage;
  private CommandExecutionData commandExecutionData;

  /**
   * Instantiates a new Command execution context.
   */
  public CommandExecutionContext() {}

  /**
   * Instantiates a new Command execution context.
   *
   * @param other the other
   */
  public CommandExecutionContext(CommandExecutionContext other) {
    this.accountId = other.accountId;
    this.envId = other.envId;
    this.appId = other.appId;
    this.activityId = other.activityId;
    this.runtimePath = other.runtimePath;
    this.stagingPath = other.stagingPath;
    this.backupPath = other.backupPath;
    this.serviceTemplateId = other.serviceTemplateId;
    this.appContainer = other.appContainer;
    this.executionCredential = other.executionCredential;
    this.artifactFiles = other.artifactFiles;
    this.envVariables = other.envVariables;
    this.serviceVariables = other.serviceVariables;
    this.host = other.host;
    this.hostConnectionAttributes = other.hostConnectionAttributes;
    this.bastionConnectionAttributes = other.bastionConnectionAttributes;
    this.artifactStreamAttributes = other.artifactStreamAttributes;
  }

  /**
   * Getter for property 'artifactFiles'.
   *
   * @return Value for property 'artifactFiles'.
   */
  public List<ArtifactFile> getArtifactFiles() {
    return artifactFiles;
  }

  /**
   * Setter for property 'artifactFiles'.
   *
   * @param artifactFiles Value to set for property 'artifactFiles'.
   */
  public void setArtifactFiles(List<ArtifactFile> artifactFiles) {
    this.artifactFiles = artifactFiles;
  }

  /**
   * Gets activity id.
   *
   * @return the activity id
   */
  public String getActivityId() {
    return activityId;
  }

  /**
   * Sets activity id.
   *
   * @param activityId the activity id
   */
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  /**
   * Gets runtime path.
   *
   * @return the runtime path
   */
  public String getRuntimePath() {
    return runtimePath;
  }

  /**
   * Sets runtime path.
   *
   * @param runtimePath the runtime path
   */
  public void setRuntimePath(String runtimePath) {
    this.runtimePath = runtimePath;
  }

  /**
   * Gets staging path.
   *
   * @return the staging path
   */
  public String getStagingPath() {
    return stagingPath;
  }

  /**
   * Sets staging path.
   *
   * @param stagingPath the staging path
   */
  public void setStagingPath(String stagingPath) {
    this.stagingPath = stagingPath;
  }

  /**
   * Gets backup path.
   *
   * @return the backup path
   */
  public String getBackupPath() {
    return backupPath;
  }

  /**
   * Sets backup path.
   *
   * @param backupPath the backup path
   */
  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  /**
   * Gets execution credential.
   *
   * @return the execution credential
   */
  public ExecutionCredential getExecutionCredential() {
    return executionCredential;
  }

  /**
   * Sets execution credential.
   *
   * @param executionCredential the execution credential
   */
  public void setExecutionCredential(ExecutionCredential executionCredential) {
    this.executionCredential = executionCredential;
  }

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets service instance.
   *
   * @return the service instance
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets service instance.
   *
   * @param envId the service instance
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Getter for property 'serviceVariables'.
   *
   * @return Value for property 'serviceVariables'.
   */
  public Map<String, String> getServiceVariables() {
    return serviceVariables;
  }

  /**
   * Setter for property 'serviceVariables'.
   *
   * @param serviceVariables Value to set for property 'serviceVariables'.
   */
  public void setServiceVariables(Map<String, String> serviceVariables) {
    this.serviceVariables = serviceVariables;
  }

  /**
   * Getter for property 'host'.
   *
   * @return Value for property 'host'.
   */
  public Host getHost() {
    return host;
  }

  /**
   * Setter for property 'host'.
   *
   * @param host Value to set for property 'host'.
   */
  public void setHost(Host host) {
    this.host = host;
  }

  /**
   * Getter for property 'hostConnectionAttributes'.
   *
   * @return Value for property 'hostConnectionAttributes'.
   */
  public SettingAttribute getHostConnectionAttributes() {
    return hostConnectionAttributes;
  }

  /**
   * Setter for property 'hostConnectionAttributes'.
   *
   * @param hostConnectionAttributes Value to set for property 'hostConnectionAttributes'.
   */
  public void setHostConnectionAttributes(SettingAttribute hostConnectionAttributes) {
    this.hostConnectionAttributes = hostConnectionAttributes;
  }

  /**
   * Getter for property 'bastionConnectionAttributes'.
   *
   * @return Value for property 'bastionConnectionAttributes'.
   */
  public SettingAttribute getBastionConnectionAttributes() {
    return bastionConnectionAttributes;
  }

  /**
   * Setter for property 'bastionConnectionAttributes'.
   *
   * @param bastionConnectionAttributes Value to set for property 'bastionConnectionAttributes'.
   */
  public void setBastionConnectionAttributes(SettingAttribute bastionConnectionAttributes) {
    this.bastionConnectionAttributes = bastionConnectionAttributes;
  }

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Add env variables.
   *
   * @param envVariables the env variables
   */
  public void addEnvVariables(Map<String, String> envVariables) {
    for (Entry<String, String> envVariable : envVariables.entrySet()) {
      this.envVariables.put(envVariable.getKey(), evaluateVariable(envVariable.getValue()));
    }
  }

  /**
   * Evaluate variable string.
   *
   * @param text the text
   * @return the string
   */
  protected String evaluateVariable(String text) {
    if (isNotBlank(text)) {
      for (Entry<String, String> entry : envVariables.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        text = text.replaceAll("\\$\\{" + key + "\\}", value);
        text = text.replaceAll("\\$" + key, value);
      }
    }
    return text;
  }

  /**
   * Gets artifact stream attributes.
   *
   * @return the artifact stream attributes
   */
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return artifactStreamAttributes;
  }

  /**
   * Sets artifact stream attributes.
   *
   * @param artifactStreamAttributes the artifact stream attributes
   */
  public void setArtifactStreamAttributes(ArtifactStreamAttributes artifactStreamAttributes) {
    this.artifactStreamAttributes = artifactStreamAttributes;
  }

  /**
   * Gets service template id.
   *
   * @return the service template id
   */
  public String getServiceTemplateId() {
    return serviceTemplateId;
  }

  /**
   * Sets service template id.
   *
   * @param serviceTemplateId the service template id
   */
  public void setServiceTemplateId(String serviceTemplateId) {
    this.serviceTemplateId = serviceTemplateId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountId, envId, host, appId, activityId, runtimePath, stagingPath, backupPath,
        serviceTemplateId, executionCredential, artifactFiles, serviceVariables, envVariables, hostConnectionAttributes,
        bastionConnectionAttributes, artifactStreamAttributes, cloudProviderSetting, clusterName, serviceName, region,
        desiredCount, desiredPercentage, commandExecutionData);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CommandExecutionContext other = (CommandExecutionContext) obj;
    return Objects.equals(this.accountId, other.accountId) && Objects.equals(this.envId, other.envId)
        && Objects.equals(this.host, other.host) && Objects.equals(this.appId, other.appId)
        && Objects.equals(this.activityId, other.activityId) && Objects.equals(this.runtimePath, other.runtimePath)
        && Objects.equals(this.stagingPath, other.stagingPath) && Objects.equals(this.backupPath, other.backupPath)
        && Objects.equals(this.serviceTemplateId, other.serviceTemplateId)
        && Objects.equals(this.executionCredential, other.executionCredential)
        && Objects.equals(this.artifactFiles, other.artifactFiles)
        && Objects.equals(this.serviceVariables, other.serviceVariables)
        && Objects.equals(this.envVariables, other.envVariables)
        && Objects.equals(this.hostConnectionAttributes, other.hostConnectionAttributes)
        && Objects.equals(this.bastionConnectionAttributes, other.bastionConnectionAttributes)
        && Objects.equals(this.artifactStreamAttributes, other.artifactStreamAttributes)
        && Objects.equals(this.cloudProviderSetting, other.cloudProviderSetting)
        && Objects.equals(this.clusterName, other.clusterName) && Objects.equals(this.serviceName, other.serviceName)
        && Objects.equals(this.region, other.region) && Objects.equals(this.desiredCount, other.desiredCount)
        && Objects.equals(this.desiredPercentage, other.desiredPercentage)
        && Objects.equals(this.commandExecutionData, other.commandExecutionData);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accountId", accountId)
        .add("envId", envId)
        .add("host", host)
        .add("appId", appId)
        .add("activityId", activityId)
        .add("runtimePath", runtimePath)
        .add("stagingPath", stagingPath)
        .add("backupPath", backupPath)
        .add("serviceTemplateId", serviceTemplateId)
        .add("executionCredential", executionCredential)
        .add("artifactFiles", artifactFiles)
        .add("serviceVariables", serviceVariables)
        .add("envVariables", envVariables)
        .add("hostConnectionAttributes", hostConnectionAttributes)
        .add("bastionConnectionAttributes", bastionConnectionAttributes)
        .add("artifactStreamAttributes", artifactStreamAttributes)
        .add("cloudProviderSetting", cloudProviderSetting)
        .add("clusterName", clusterName)
        .add("serviceName", serviceName)
        .add("region", region)
        .add("desiredCount", desiredCount)
        .add("desiredPercentage", desiredPercentage)
        .add("commandExecutionData", commandExecutionData)
        .toString();
  }

  /**
   * Gets cloud provider setting.
   *
   * @return the cloud provider setting
   */
  public SettingAttribute getCloudProviderSetting() {
    return cloudProviderSetting;
  }

  /**
   * Sets cloud provider setting.
   *
   * @param cloudProviderSetting the cloud provider setting
   */
  public void setCloudProviderSetting(SettingAttribute cloudProviderSetting) {
    this.cloudProviderSetting = cloudProviderSetting;
  }

  /**
   * Gets cluster name.
   *
   * @return the cluster name
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Sets cluster name.
   *
   * @param clusterName the cluster name
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * Gets service name.
   *
   * @return the service name
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Sets service name.
   *
   * @param serviceName the service name
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Gets region.
   *
   * @return the region
   */
  public String getRegion() {
    return region;
  }

  /**
   * Sets region.
   *
   * @param region the region
   */
  public void setRegion(String region) {
    this.region = region;
  }

  /**
   * Gets desired count.
   *
   * @return the desired count
   */
  public Integer getDesiredCount() {
    return desiredCount;
  }

  /**
   * Sets desired count.
   *
   * @param desiredCount the desired count
   */
  public void setDesiredCount(Integer desiredCount) {
    this.desiredCount = desiredCount;
  }

  /**
   * Gets desired percentage.
   *
   * @return the desired percentage
   */
  public Integer getDesiredPercentage() {
    return desiredPercentage;
  }

  /**
   * Sets desired percentage.
   *
   * @param desiredPercentage the desired percentage
   */
  public void setDesiredPercentage(Integer desiredPercentage) {
    this.desiredPercentage = desiredPercentage;
  }

  /**
   * Gets command execution data.
   *
   * @return the command execution data
   */
  public CommandExecutionData getCommandExecutionData() {
    return commandExecutionData;
  }

  /**
   * Sets command execution data.
   *
   * @param commandExecutionData the command execution data
   */
  public void setCommandExecutionData(CommandExecutionData commandExecutionData) {
    this.commandExecutionData = commandExecutionData;
  }

  /**
   * Gets app container.
   *
   * @return the app container
   */
  public AppContainer getAppContainer() {
    return appContainer;
  }

  /**
   * Sets app container.
   *
   * @param appContainer the app container
   */
  public void setAppContainer(AppContainer appContainer) {
    this.appContainer = appContainer;
  }

  public CodoDeployParams getCodoDeployParams() {
    return codoDeployParams;
  }

  public void setCodoDeployParams(CodoDeployParams codoDeployParams) {
    this.codoDeployParams = codoDeployParams;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String accountId;
    private String envId;
    private Host host;
    private String appId;
    private String activityId;
    private String runtimePath;
    private String stagingPath;
    private String backupPath;
    private String serviceTemplateId;
    private ExecutionCredential executionCredential;
    private AppContainer appContainer;
    private List<ArtifactFile> artifactFiles;
    private Map<String, String> serviceVariables = Maps.newHashMap();
    private Map<String, String> envVariables = Maps.newHashMap();
    private SettingAttribute hostConnectionAttributes;
    private SettingAttribute bastionConnectionAttributes;
    private ArtifactStreamAttributes artifactStreamAttributes;
    private SettingAttribute cloudProviderSetting;
    private String clusterName;
    private String serviceName;
    private String region;
    private Integer desiredCount;
    private Integer desiredPercentage;
    private CommandExecutionData commandExecutionData;

    private Builder() {}

    /**
     * A command execution context builder.
     *
     * @return the builder
     */
    public static Builder aCommandExecutionContext() {
      return new Builder();
    }

    /**
     * With account id builder.
     *
     * @param accountId the account id
     * @return the builder
     */
    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With host builder.
     *
     * @param host the host
     * @return the builder
     */
    public Builder withHost(Host host) {
      this.host = host;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With activity id builder.
     *
     * @param activityId the activity id
     * @return the builder
     */
    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    /**
     * With runtime path builder.
     *
     * @param runtimePath the runtime path
     * @return the builder
     */
    public Builder withRuntimePath(String runtimePath) {
      this.runtimePath = runtimePath;
      return this;
    }

    /**
     * With staging path builder.
     *
     * @param stagingPath the staging path
     * @return the builder
     */
    public Builder withStagingPath(String stagingPath) {
      this.stagingPath = stagingPath;
      return this;
    }

    /**
     * With backup path builder.
     *
     * @param backupPath the backup path
     * @return the builder
     */
    public Builder withBackupPath(String backupPath) {
      this.backupPath = backupPath;
      return this;
    }

    /**
     * With service template id builder.
     *
     * @param serviceTemplateId the service template id
     * @return the builder
     */
    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    /**
     * With execution credential builder.
     *
     * @param executionCredential the execution credential
     * @return the builder
     */
    public Builder withExecutionCredential(ExecutionCredential executionCredential) {
      this.executionCredential = executionCredential;
      return this;
    }

    /**
     * With app container builder.
     *
     * @param appContainer the app container
     * @return the builder
     */
    public Builder withAppContainer(AppContainer appContainer) {
      this.appContainer = appContainer;
      return this;
    }

    /**
     * With artifact files builder.
     *
     * @param artifactFiles the artifact files
     * @return the builder
     */
    public Builder withArtifactFiles(List<ArtifactFile> artifactFiles) {
      this.artifactFiles = artifactFiles;
      return this;
    }

    /**
     * With service variables builder.
     *
     * @param serviceVariables the service variables
     * @return the builder
     */
    public Builder withServiceVariables(Map<String, String> serviceVariables) {
      this.serviceVariables = serviceVariables;
      return this;
    }

    /**
     * With env variables builder.
     *
     * @param envVariables the env variables
     * @return the builder
     */
    public Builder withEnvVariables(Map<String, String> envVariables) {
      this.envVariables = envVariables;
      return this;
    }

    /**
     * With host connection attributes builder.
     *
     * @param hostConnectionAttributes the host connection attributes
     * @return the builder
     */
    public Builder withHostConnectionAttributes(SettingAttribute hostConnectionAttributes) {
      this.hostConnectionAttributes = hostConnectionAttributes;
      return this;
    }

    /**
     * With bastion connection attributes builder.
     *
     * @param bastionConnectionAttributes the bastion connection attributes
     * @return the builder
     */
    public Builder withBastionConnectionAttributes(SettingAttribute bastionConnectionAttributes) {
      this.bastionConnectionAttributes = bastionConnectionAttributes;
      return this;
    }

    /**
     * With artifact stream attributes builder.
     *
     * @param artifactStreamAttributes the artifact stream attributes
     * @return the builder
     */
    public Builder withArtifactStreamAttributes(ArtifactStreamAttributes artifactStreamAttributes) {
      this.artifactStreamAttributes = artifactStreamAttributes;
      return this;
    }

    /**
     * With cloud provider setting builder.
     *
     * @param cloudProviderSetting the cloud provider setting
     * @return the builder
     */
    public Builder withCloudProviderSetting(SettingAttribute cloudProviderSetting) {
      this.cloudProviderSetting = cloudProviderSetting;
      return this;
    }

    /**
     * With cluster name builder.
     *
     * @param clusterName the cluster name
     * @return the builder
     */
    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    /**
     * With service name builder.
     *
     * @param serviceName the service name
     * @return the builder
     */
    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    /**
     * With region builder.
     *
     * @param region the region
     * @return the builder
     */
    public Builder withRegion(String region) {
      this.region = region;
      return this;
    }

    /**
     * With desired count builder.
     *
     * @param desiredCount the desired count
     * @return the builder
     */
    public Builder withDesiredCount(Integer desiredCount) {
      this.desiredCount = desiredCount;
      return this;
    }

    /**
     * With desired percentage builder.
     *
     * @param desiredPercentage the desired percentage
     * @return the builder
     */
    public Builder withDesiredPercentage(Integer desiredPercentage) {
      this.desiredPercentage = desiredPercentage;
      return this;
    }

    /**
     * With command execution data builder.
     *
     * @param commandExecutionData the command execution data
     * @return the builder
     */
    public Builder withCommandExecutionData(CommandExecutionData commandExecutionData) {
      this.commandExecutionData = commandExecutionData;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCommandExecutionContext()
          .withAccountId(accountId)
          .withEnvId(envId)
          .withHost(host)
          .withAppId(appId)
          .withActivityId(activityId)
          .withRuntimePath(runtimePath)
          .withStagingPath(stagingPath)
          .withBackupPath(backupPath)
          .withServiceTemplateId(serviceTemplateId)
          .withExecutionCredential(executionCredential)
          .withAppContainer(appContainer)
          .withArtifactFiles(artifactFiles)
          .withServiceVariables(serviceVariables)
          .withEnvVariables(envVariables)
          .withHostConnectionAttributes(hostConnectionAttributes)
          .withBastionConnectionAttributes(bastionConnectionAttributes)
          .withArtifactStreamAttributes(artifactStreamAttributes)
          .withCloudProviderSetting(cloudProviderSetting)
          .withClusterName(clusterName)
          .withServiceName(serviceName)
          .withRegion(region)
          .withDesiredCount(desiredCount)
          .withDesiredPercentage(desiredPercentage)
          .withCommandExecutionData(commandExecutionData);
    }

    /**
     * Build command execution context.
     *
     * @return the command execution context
     */
    public CommandExecutionContext build() {
      CommandExecutionContext commandExecutionContext = new CommandExecutionContext();
      commandExecutionContext.setAccountId(accountId);
      commandExecutionContext.setEnvId(envId);
      commandExecutionContext.setHost(host);
      commandExecutionContext.setAppId(appId);
      commandExecutionContext.setActivityId(activityId);
      commandExecutionContext.setRuntimePath(runtimePath);
      commandExecutionContext.setStagingPath(stagingPath);
      commandExecutionContext.setBackupPath(backupPath);
      commandExecutionContext.setServiceTemplateId(serviceTemplateId);
      commandExecutionContext.setExecutionCredential(executionCredential);
      commandExecutionContext.setAppContainer(appContainer);
      commandExecutionContext.setArtifactFiles(artifactFiles);
      commandExecutionContext.setServiceVariables(serviceVariables);
      commandExecutionContext.setHostConnectionAttributes(hostConnectionAttributes);
      commandExecutionContext.setBastionConnectionAttributes(bastionConnectionAttributes);
      commandExecutionContext.setArtifactStreamAttributes(artifactStreamAttributes);
      commandExecutionContext.setCloudProviderSetting(cloudProviderSetting);
      commandExecutionContext.setClusterName(clusterName);
      commandExecutionContext.setServiceName(serviceName);
      commandExecutionContext.setRegion(region);
      commandExecutionContext.setDesiredCount(desiredCount);
      commandExecutionContext.setDesiredPercentage(desiredPercentage);
      commandExecutionContext.setCommandExecutionData(commandExecutionData);
      commandExecutionContext.envVariables = this.envVariables;
      return commandExecutionContext;
    }
  }

  /**
   * The type Codo deploy params.
   */
  public static class CodoDeployParams {
    private String applicationName;
    private String deploymentConfigurationName;
    private String deploymentGroupName;
    private String region;

    private CodoDeployParams(Builder builder) {
      setApplicationName(builder.applicationName);
      setDeploymentConfigurationName(builder.deploymentConfigurationName);
      setDeploymentGroupName(builder.deploymentGroupName);
      setRegion(builder.region);
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public static Builder newBuilder(CodoDeployParams copy) {
      Builder builder = new Builder();
      builder.applicationName = copy.applicationName;
      builder.deploymentConfigurationName = copy.deploymentConfigurationName;
      builder.deploymentGroupName = copy.deploymentGroupName;
      builder.region = copy.region;
      return builder;
    }

    /**
     * Gets application name.
     *
     * @return the application name
     */
    public String getApplicationName() {
      return applicationName;
    }

    /**
     * Sets application name.
     *
     * @param applicationName the application name
     */
    public void setApplicationName(String applicationName) {
      this.applicationName = applicationName;
    }

    /**
     * Gets deployment configuration name.
     *
     * @return the deployment configuration name
     */
    public String getDeploymentConfigurationName() {
      return deploymentConfigurationName;
    }

    /**
     * Sets deployment configuration name.
     *
     * @param deploymentConfigurationName the deployment configuration name
     */
    public void setDeploymentConfigurationName(String deploymentConfigurationName) {
      this.deploymentConfigurationName = deploymentConfigurationName;
    }

    /**
     * Gets deployment group name.
     *
     * @return the deployment group name
     */
    public String getDeploymentGroupName() {
      return deploymentGroupName;
    }

    /**
     * Sets deployment group name.
     *
     * @param deploymentGroupName the deployment group name
     */
    public void setDeploymentGroupName(String deploymentGroupName) {
      this.deploymentGroupName = deploymentGroupName;
    }

    /**
     * Gets region.
     *
     * @return the region
     */
    public String getRegion() {
      return region;
    }

    /**
     * Sets region.
     *
     * @param region the region
     */
    public void setRegion(String region) {
      this.region = region;
    }

    public static final class Builder {
      private String applicationName;
      private String deploymentConfigurationName;
      private String deploymentGroupName;
      private String region;

      private Builder() {}

      public Builder withApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
      }

      public Builder withDeploymentConfigurationName(String deploymentConfigurationName) {
        this.deploymentConfigurationName = deploymentConfigurationName;
        return this;
      }

      public Builder withDeploymentGroupName(String deploymentGroupName) {
        this.deploymentGroupName = deploymentGroupName;
        return this;
      }

      public Builder withRegion(String region) {
        this.region = region;
        return this;
      }

      public CodoDeployParams build() {
        return new CodoDeployParams(this);
      }
    }
  }
}
