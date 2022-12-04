/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.pcf;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.task.tas.TasNgConfigMapper;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfInBuiltVariablesUpdateValues;
import io.harness.delegate.beans.pcf.CfRollbackCommandResult;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.beans.pcf.CfSwapRouteCommandResult;
import io.harness.delegate.cf.apprenaming.AppNamingStrategy;
import io.harness.delegate.cf.apprenaming.AppRenamingOperator.NamingTransition;
import io.harness.delegate.task.cf.CfCommandTaskHelperNG;
import io.harness.delegate.task.pcf.TasTaskHelperBase;
import io.harness.delegate.task.pcf.request.CfCommandRequestNG;
import io.harness.delegate.task.pcf.request.CfCommandRouteUpdateRequest;
import io.harness.delegate.task.pcf.request.CfRollbackCommandRequestNG;
import io.harness.delegate.task.pcf.request.CfSwapRollbackCommandRequestNG;
import io.harness.delegate.task.pcf.request.CfSwapRoutesRequestNG;
import io.harness.delegate.task.pcf.response.CfCommandResponseNG;
import io.harness.delegate.task.pcf.response.CfRollbackCommandResponseNG;
import io.harness.delegate.task.pcf.response.CfSwapRouteCommandResponseNG;
import io.harness.delegate.task.pcf.response.TasInfraConfig;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.CloudFoundryConfig;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.cf.apprenaming.AppRenamingOperator.NamingTransition.NON_VERSION_TO_NON_VERSION;
import static io.harness.delegate.cf.apprenaming.AppRenamingOperator.NamingTransition.NON_VERSION_TO_VERSION;
import static io.harness.delegate.cf.apprenaming.AppRenamingOperator.NamingTransition.ROLLBACK_OPERATOR;
import static io.harness.pcf.PcfUtils.encodeColor;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

@NoArgsConstructor
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class CfSwapRouteCommandTaskHandlerNG extends CfCommandTaskNGHandler {
  @Inject TasTaskHelperBase tasTaskHelperBase;
  @Inject TasNgConfigMapper tasNgConfigMapper;
  @Inject CfDeploymentManager cfDeploymentManager;
  @Inject protected CfCommandTaskHelperNG cfCommandTaskHelperNG;

  @Override
  public CfCommandResponseNG executeTaskInternal(CfCommandRequestNG cfCommandRequestNG,
      ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    if (!(cfCommandRequestNG instanceof CfSwapRoutesRequestNG)) {
      throw new InvalidArgumentsException(
          Pair.of("cfCommandRequest", "Must be instance of CfSwapRoutesRequestNG"));
    }
    CfInBuiltVariablesUpdateValues updateValues = CfInBuiltVariablesUpdateValues.builder().build();
    LogCallback executionLogCallback = tasTaskHelperBase.getLogCallback(
        iLogStreamingTaskClient, cfCommandRequestNG.getCommandName(), true, commandUnitsProgress);
    CfSwapRouteCommandResult cfSwapRouteCommandResult = CfSwapRouteCommandResult.builder().build();
    CfSwapRouteCommandResponseNG cfSwapRouteCommandResponseNG = CfSwapRouteCommandResponseNG.builder().build();

    CfSwapRoutesRequestNG cfSwapRoutesRequestNG = (CfSwapRoutesRequestNG) cfCommandRequestNG;
    File workingDirectory = null;
    try {
      // This will be CF_HOME for any cli related operations
      workingDirectory = cfCommandTaskHelperNG.generateWorkingDirectoryForDeployment();
      TasInfraConfig tasInfraConfig = cfSwapRoutesRequestNG.getTasInfraConfig();
      CloudFoundryConfig cfConfig = tasNgConfigMapper.mapTasConfigWithDecryption(
          tasInfraConfig.getTasConnectorDTO(), tasInfraConfig.getEncryptionDataDetails());

      executionLogCallback.saveExecutionLog(color("--------- Starting PCF Route Update\n", White, Bold));
      ExceptionMessageSanitizer.storeAllSecretsForSanitizing(
          tasInfraConfig.getTasConnectorDTO(), tasInfraConfig.getEncryptionDataDetails());

      CfRequestConfig cfRequestConfig =
          CfRequestConfig.builder()
              .userName(String.valueOf(cfConfig.getUserName()))
              .endpointUrl(cfConfig.getEndpointUrl())
              .password(String.valueOf(cfConfig.getPassword()))
              .orgName(tasInfraConfig.getOrganization())
              .spaceName(tasInfraConfig.getSpace())
              .timeOutIntervalInMins(cfSwapRoutesRequestNG.getTimeoutIntervalInMin())
              .cfHomeDirPath(workingDirectory.getAbsolutePath())
              .cfCliPath(cfCommandTaskHelperNG.getCfCliPathOnDelegate(
                  true, cfSwapRoutesRequestNG.getCfCliVersion()))
              .cfCliVersion(cfSwapRoutesRequestNG.getCfCliVersion())
              .build();


      CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData =
          CfRouteUpdateRequestConfigData.builder()
              .isRollback(true)
              .existingApplicationDetails(
                      cfSwapRoutesRequestNG.getExistingApplicationDetails())
              .cfAppNamePrefix(cfSwapRoutesRequestNG.getCfAppNamePrefix())
              .downsizeOldApplication(cfSwapRoutesRequestNG.isDownsizeOldApplication())
              .existingApplicationNames(cfSwapRoutesRequestNG.getExistingApplicationNames())
              .tempRoutes(cfSwapRoutesRequestNG.getTempRoutes())
              .skipRollback(false)
              .isStandardBlueGreen(true)
              .newApplicationDetails(cfSwapRoutesRequestNG.getNewApplicationDetails())
              .versioningChanged(false)
              .nonVersioning(true)
              .newApplicationName(cfSwapRoutesRequestNG.getNewApplicationDetails().getApplicationName())
              .finalRoutes(cfSwapRoutesRequestNG.getFinalRoutes())
              .isMapRoutesOperation(false)
              .build();
        // Swap routes
        performRouteUpdateForStandardBlueGreen(cfRequestConfig, pcfRouteUpdateConfigData, executionLogCallback);

        // if deploy and downsizeOld is true
        updateValues = downsizeOldAppDuringDeployAndRenameApps(executionLogCallback, cfSwapRoutesRequestNG,
                cfRequestConfig, pcfRouteUpdateConfigData, workingDirectory.getAbsolutePath());

      cfSwapRouteCommandResult.setUpdatedValues(updateValues);
      executionLogCallback.saveExecutionLog(
          "\n--------- PCF Route Update completed successfully", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      cfSwapRouteCommandResponseNG.setErrorMessage(StringUtils.EMPTY);
      cfSwapRouteCommandResponseNG.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
    } catch (Exception e) {
      Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(e);
      log.error("Exception in processing PCF Route Update task", sanitizedException);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Route Update failed to complete successfully");
      executionLogCallback.saveExecutionLog("# Error: " + sanitizedException.getMessage());
      cfSwapRouteCommandResponseNG.setErrorMessage(sanitizedException.getMessage());
      cfSwapRouteCommandResponseNG.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    } finally {
      try {
        if (workingDirectory != null) {
          FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
        }
      } catch (IOException e) {
        log.warn("Failed to delete temp directory created for CF CLI login", e);
      }
    }
    return cfSwapRouteCommandResponseNG;
  }

  CfInBuiltVariablesUpdateValues downsizeOldAppDuringDeployAndRenameApps(LogCallback executionLogCallback,
                                                                         CfSwapRoutesRequestNG cfSwapRoutesRequestNG, CfRequestConfig cfRequestConfig,
                                                                         CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData, String configVarPath) throws PivotalClientApiException {
    if (pcfRouteUpdateConfigData.isDownsizeOldApplication()) {
      resizeOldApplications(cfSwapRoutesRequestNG, cfRequestConfig, pcfRouteUpdateConfigData, executionLogCallback, configVarPath);
    }
    return renameApps(pcfRouteUpdateConfigData, cfRequestConfig, executionLogCallback);
  }

  private CfInBuiltVariablesUpdateValues renameApps(CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData,
                                                    CfRequestConfig cfRequestConfig, LogCallback executionLogCallback) throws PivotalClientApiException {

    return performRenamingWhenExistingStrategyWasNonVersioning(
              pcfRouteUpdateConfigData, cfRequestConfig, executionLogCallback);
  }

  private CfInBuiltVariablesUpdateValues performRenamingWhenExistingStrategyWasNonVersioning(
          CfRouteUpdateRequestConfigData cfRouteUpdateConfigData, CfRequestConfig cfRequestConfig,
          LogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(color("\n# Starting Renaming apps", White, Bold));
    boolean nonVersioning = cfRouteUpdateConfigData.isNonVersioning();
    NamingTransition transition = nonVersioning ? NON_VERSION_TO_NON_VERSION : NON_VERSION_TO_VERSION;
    return performAppRenaming(transition, cfRouteUpdateConfigData, cfRequestConfig, executionLogCallback);
  }

  private String getAppNameBasedOnGuid(CfAppSetupTimeDetails existingInActiveApplicationDetails, String cfAppNamePrefix,
      CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    if (existingInActiveApplicationDetails == null) {
      return "";
    }
    if (isEmpty(existingInActiveApplicationDetails.getApplicationGuid())) {
      return existingInActiveApplicationDetails.getApplicationName();
    }
    String applicationGuid = existingInActiveApplicationDetails.getApplicationGuid();
    List<ApplicationSummary> previousReleases =
        cfDeploymentManager.getPreviousReleases(cfRequestConfig, cfAppNamePrefix);
    List<String> appNames = previousReleases.stream()
                                .filter(app -> app.getId().equalsIgnoreCase(applicationGuid))
                                .map(ApplicationSummary::getName)
                                .collect(Collectors.toList());
    if (appNames.size() == 1) {
      return appNames.get(0);
    }
    return existingInActiveApplicationDetails.getApplicationName();
  }

  @VisibleForTesting
  void resizeOldApplications(CfSwapRoutesRequestNG cfSwapRoutesRequestNG, CfRequestConfig cfRequestConfig,
      CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData, LogCallback executionLogCallback, String configVarPath) {
    String msg = "\n# Restoring Old Apps to original count";
    executionLogCallback.saveExecutionLog(msg);
    String appNameBeingDownsized = null;

    List<CfAppSetupTimeDetails> existingApplicationDetails = pcfRouteUpdateConfigData.getExistingApplicationDetails();
    if (isNotEmpty(existingApplicationDetails)) {
      try {
        CfAppSetupTimeDetails existingAppDetails = existingApplicationDetails.get(0);
        appNameBeingDownsized = existingAppDetails.getApplicationName();
        int count = existingAppDetails.getInitialInstanceCount();

        cfRequestConfig.setApplicationName(appNameBeingDownsized);
        cfRequestConfig.setDesiredCount(count);
        executionLogCallback.saveExecutionLog(new StringBuilder()
                                                  .append("Resizing Application: {")
                                                  .append(encodeColor(appNameBeingDownsized))
                                                  .append("} to Count: ")
                                                  .append(count)
                                                  .toString());

        CfAppAutoscalarRequestData appAutoscalarRequestData =
            performResizing(cfSwapRoutesRequestNG, cfRequestConfig, configVarPath, executionLogCallback);

        // After resize, enable autoscalar if it was attached.
        if (cfSwapRoutesRequestNG.isUseAppAutoscalar() && appAutoscalarRequestData != null) {
          appAutoscalarRequestData.setExpectedEnabled(false);
          cfDeploymentManager.changeAutoscalarState(appAutoscalarRequestData, executionLogCallback, true);
        }
      } catch (Exception e) {
        log.error("Failed to downsize PCF application: " + appNameBeingDownsized, e);
        executionLogCallback.saveExecutionLog(
            "Failed while downsizing old application: " + encodeColor(appNameBeingDownsized));
      }
    }
  }

  private CfAppAutoscalarRequestData performResizing(CfSwapRoutesRequestNG cfSwapRoutesRequestNG,
      CfRequestConfig cfRequestConfig, String configVarPath, LogCallback executionLogCallback)
      throws PivotalClientApiException {
    CfAppAutoscalarRequestData appAutoscalarRequestData = null;
    if (cfSwapRoutesRequestNG.isUseAppAutoscalar()) {
      ApplicationDetail applicationDetail = cfDeploymentManager.getApplicationByName(cfRequestConfig);
      appAutoscalarRequestData = CfAppAutoscalarRequestData.builder()
                                     .applicationGuid(applicationDetail.getId())
                                     .applicationName(applicationDetail.getName())
                                     .cfRequestConfig(cfRequestConfig)
                                     .configPathVar(configVarPath)
                                     .timeoutInMins(cfSwapRoutesRequestNG.getTimeoutIntervalInMin())
                                     .build();
    }

    // resize app (upsize in swap rollback, downsize in swap state)
    cfDeploymentManager.upsizeApplicationWithSteadyStateCheck(cfRequestConfig, executionLogCallback);

    return appAutoscalarRequestData;
  }

  private void performRouteUpdateForStandardBlueGreen(CfRequestConfig cfRequestConfig,
      CfRouteUpdateRequestConfigData data, LogCallback executionLogCallback) throws PivotalClientApiException {
    CfAppSetupTimeDetails newApplicationDetails = data.getNewApplicationDetails();
    List<String> newApps = cfCommandTaskHelperNG.getAppNameBasedOnGuid(
        cfRequestConfig, data.getCfAppNamePrefix(), newApplicationDetails.getApplicationGuid());
    data.setNewApplicationName(isEmpty(newApps) ? data.getNewApplicationName() : newApps.get(0));

    updateRoutesForExistingApplication(cfRequestConfig, executionLogCallback, data);
    if (data.isUpSizeInActiveApp()) {
      updateRoutesForInActiveApplication(cfRequestConfig, executionLogCallback, data);
    }
    clearRoutesAndEnvVariablesForNewApplication(
        cfRequestConfig, executionLogCallback, data.getNewApplicationName(), data.getFinalRoutes());
  }

  private void clearRoutesAndEnvVariablesForNewApplication(CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback, String appName, List<String> routeList) throws PivotalClientApiException {
    cfCommandTaskHelperNG.unmapRouteMaps(appName, routeList, cfRequestConfig, executionLogCallback);
    cfRequestConfig.setApplicationName(appName);
    cfDeploymentManager.unsetEnvironmentVariableForAppStatus(cfRequestConfig, executionLogCallback);
  }

  private void updateRoutesForInActiveApplication(CfRequestConfig cfRequestConfig, LogCallback executionLogCallback,
      CfRouteUpdateRequestConfigData data) throws PivotalClientApiException {
    CfAppSetupTimeDetails inActiveApplicationDetails = data.getExistingInActiveApplicationDetails();
    if (inActiveApplicationDetails == null || isEmpty(inActiveApplicationDetails.getApplicationGuid())) {
      executionLogCallback.saveExecutionLog(
          color("No in-active application found for updating routes. Hence skipping\n", White, Bold));
      return;
    }
    String inActiveAppName =
        getAppNameBasedOnGuid(inActiveApplicationDetails, data.getCfAppNamePrefix(), cfRequestConfig);
    if (isEmpty(inActiveAppName)) {
      executionLogCallback.saveExecutionLog(
          color("Could not find in active application. Hence skipping update route for In Active Application\n", White,
              Bold));
      return;
    }

    if (isNotEmpty(inActiveApplicationDetails.getUrls())) {
      executionLogCallback.saveExecutionLog(
          String.format("%nUpdating routes for In Active application - [%s]", encodeColor(inActiveAppName)));
      List<String> inActiveApplicationUrls = inActiveApplicationDetails.getUrls();
      cfCommandTaskHelperNG.mapRouteMaps(
          inActiveAppName, inActiveApplicationUrls, cfRequestConfig, executionLogCallback);
    } else {
      executionLogCallback.saveExecutionLog(
          color(String.format("No previous route defined for in active application - [%s]. Hence skipping",
                    encodeColor(inActiveAppName)),
              White, Bold));
    }
    updateEnvVariableForApplication(cfRequestConfig, executionLogCallback, inActiveAppName, false);
  }

  private void updateRoutesForExistingApplication(CfRequestConfig cfRequestConfig, LogCallback executionLogCallback,
      CfRouteUpdateRequestConfigData data) throws PivotalClientApiException {
    if (isNotEmpty(data.getExistingApplicationNames())) {
      List<String> mapRouteForExistingApp = data.getFinalRoutes();
      List<String> unmapRouteForExistingApp = data.getTempRoutes();
      for (String existingAppName : data.getExistingApplicationNames()) {
        cfCommandTaskHelperNG.mapRouteMaps(
            existingAppName, mapRouteForExistingApp, cfRequestConfig, executionLogCallback);
        cfCommandTaskHelperNG.unmapRouteMaps(
            existingAppName, unmapRouteForExistingApp, cfRequestConfig, executionLogCallback);
        updateEnvVariableForApplication(cfRequestConfig, executionLogCallback, existingAppName, true);
      }
    }
  }

  private void updateEnvVariableForApplication(CfRequestConfig cfRequestConfig, LogCallback executionLogCallback,
      String appName, boolean isActiveApplication) throws PivotalClientApiException {
    cfRequestConfig.setApplicationName(appName);
    cfDeploymentManager.setEnvironmentVariableForAppStatus(cfRequestConfig, isActiveApplication, executionLogCallback);
  }

  private CfInBuiltVariablesUpdateValues performAppRenaming(NamingTransition transition,
      CfRouteUpdateRequestConfigData cfRouteUpdateConfigData, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    return cfCommandTaskHelperNG.performAppRenaming(
        transition, cfRouteUpdateConfigData, cfRequestConfig, executionLogCallback);
  }
}
