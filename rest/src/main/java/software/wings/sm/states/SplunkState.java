package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.service.impl.splunk.SplunkAnalysisResponse.Builder.anSplunkAnalysisResponse;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.InfraNodeRequest;
import software.wings.api.InstanceElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.TaskType;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.service.impl.splunk.SplunkAnalysisResponse;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.impl.splunk.SplunkExecutionData;
import software.wings.service.impl.splunk.SplunkLogCollectionCallback;
import software.wings.service.impl.splunk.SplunkSettingProvider;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 7/15/16.
 */
public class SplunkState extends State {
  private static final Logger logger = LoggerFactory.getLogger(SplunkState.class);
  private static final String SPLUNKML_ROOT = "SPLUNKML_ROOT";
  private static final String SPLUNKML_SHELL_FILE_NAME = "run_splunkml.sh";

  @EnumData(enumDataProvider = SplunkSettingProvider.class)
  @Attributes(required = true, title = "Splunk Server")
  private String splunkConfigId;

  @Attributes(required = true, title = "Query") private String query;

  @DefaultValue("15")
  @Attributes(title = "Analyze Time duration (in minutes)", description = "Default 15 minutes")
  private String timeDuration;

  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Transient @Inject private SettingsService settingsService;

  @Transient @Inject private AppService appService;

  @Transient @Inject private DelegateService delegateService;

  @Transient @Inject private MainConfiguration configuration;

  public SplunkState(String name) {
    super(name, StateType.SPLUNK.getType());
  }

  /**
   * Getter for property 'query'.
   *
   * @return Value for property 'query'.
   */
  public String getQuery() {
    return query;
  }

  /**
   * Setter for property 'query'.
   *
   * @param query Value to set for property 'query'.
   */
  public void setQuery(String query) {
    this.query = query;
  }

  public String getTimeDuration() {
    return timeDuration;
  }

  public void setTimeDuration(String timeDuration) {
    this.timeDuration = timeDuration;
  }

  public String getSplunkConfigId() {
    return splunkConfigId;
  }

  public void setSplunkConfigId(String splunkConfigId) {
    this.splunkConfigId = splunkConfigId;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    logger.debug("Executing splunk state");
    triggerSplunkDataCollection(context);
    final SplunkExecutionData executionData = SplunkExecutionData.Builder.anSplunkExecutionData()
                                                  .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                                  .withSplunkConfigID(splunkConfigId)
                                                  .withSplunkQueries(Lists.newArrayList(query.split(",")))
                                                  .withAnalysisDuration(Integer.parseInt(timeDuration))
                                                  .withCorrelationId(UUID.randomUUID().toString())
                                                  .build();
    final SplunkAnalysisResponse response = anSplunkAnalysisResponse()
                                                .withSplunkExecutionData(executionData)
                                                .withExecutionStatus(ExecutionStatus.SUCCESS)
                                                .build();
    final ScheduledExecutorService pythonExecutorService = createPythonExecutorService(context);
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.schedule(() -> {
      try {
        pythonExecutorService.shutdown();
        pythonExecutorService.awaitTermination(1, TimeUnit.MINUTES);
        waitNotifyEngine.notify(executionData.getCorrelationId(), response);
      } catch (InterruptedException e) {
        pythonExecutorService.shutdown();
      }
    }, Long.parseLong(timeDuration), TimeUnit.MINUTES);
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(executionData.getCorrelationId()))
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .withErrorMessage("Splunk Verification running")
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    SplunkAnalysisResponse executionResponse = (SplunkAnalysisResponse) response.values().iterator().next();
    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .withStateExecutionData(executionResponse.getSplunkExecutionData())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private ScheduledExecutorService createPythonExecutorService(ExecutionContext context) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(new SplunkAnalysisGenerator(context), 0, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private void triggerSplunkDataCollection(ExecutionContext context) {
    final SettingAttribute settingAttribute = settingsService.get(splunkConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No splunk setting with id: " + splunkConfigId + " found");
    }

    final SplunkConfig splunkConfig = (SplunkConfig) settingAttribute.getValue();
    final List<String> queries = Lists.newArrayList(query.split(","));
    final SplunkDataCollectionInfo dataCollectionInfo =
        new SplunkDataCollectionInfo(appService.get(context.getAppId()).getAccountId(), context.getAppId(),
            splunkConfig, queries, Integer.parseInt(timeDuration));
    String waitId = UUIDGenerator.getUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.SPLUNK_COLLECT_LOG_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .build();
    waitNotifyEngine.waitForAll(new SplunkLogCollectionCallback(context.getAppId()), waitId);
    delegateService.queueTask(delegateTask);
  }

  private class SplunkAnalysisGenerator implements Runnable {
    private final ExecutionContext context;
    private final String pythonScriptRoot;
    private final String serverUrl;
    private final String accountId;
    private final String applicationId;
    private final long startTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);
    private final List<String> newNodes;

    public SplunkAnalysisGenerator(ExecutionContext context) {
      this.context = context;
      this.pythonScriptRoot = System.getenv(SPLUNKML_ROOT);
      Preconditions.checkState(!StringUtils.isBlank(pythonScriptRoot), "SPLUNKML_ROOT can not be null or empty");

      String protocol = SplunkState.this.configuration.isSslEnabled() ? "https" : "http";
      this.serverUrl = protocol + "://localhost:" + SplunkState.this.configuration.getApplicationPort();
      this.applicationId = context.getAppId();
      this.accountId = SplunkState.this.appService.get(this.applicationId).getAccountId();
      this.newNodes = getCanaryNewHostNames(context);
    }

    @Override
    public void run() {
      try {
        final String logInputUrl = this.serverUrl + "/api/splunk/get-logs?accountId=" + accountId;
        final List<String> command = new ArrayList<>();
        command.add(this.pythonScriptRoot + "/" + SPLUNKML_SHELL_FILE_NAME);
        command.add("--url");
        command.add(logInputUrl);
        command.add("--application_id");
        command.add(applicationId);
        command.add("--control_window");
        command.add(String.valueOf(startTime));
        command.add(String.valueOf(System.currentTimeMillis()));
        command.add("--control_nodes");
        command.addAll(newNodes);
        command.add("--test_window");
        command.add(String.valueOf(startTime));
        command.add(String.valueOf(System.currentTimeMillis()));
        command.add("--test_nodes");
        command.addAll(newNodes);
        command.add("--sim_threshold");
        command.add(String.valueOf(0.5));
        command.add("--state_execution_id");
        command.add(context.getStateExecutionInstanceId());

        final ProcessResult result =
            new ProcessExecutor(command)
                .redirectOutput(
                    Slf4jStream
                        .of(LoggerFactory.getLogger(getClass().getName() + "." + context.getStateExecutionInstanceId()))
                        .asInfo())
                .execute();

        if (result.getExitValue() != 0) {
          logger.error("Splunk analysis failed for " + context.getStateExecutionInstanceId());
        }

      } catch (Exception e) {
        logger.error("error fetching splunk logs", e);
      }
    }

    private List<String> getCanaryNewHostNames(ExecutionContext context) {
      CanaryWorkflowStandardParams canaryWorkflowStandardParams =
          context.getContextElement(ContextElementType.STANDARD);
      List<InfraNodeRequest> infraNodeRequests = canaryWorkflowStandardParams.getInfraNodeRequests();
      logger.info("infraNodeRequests: {}", infraNodeRequests);
      logger.info("Current Phase Instances: {}", canaryWorkflowStandardParams.getInstances());
      final List<String> rv = new ArrayList<>();

      for (InstanceElement instanceElement : canaryWorkflowStandardParams.getInstances()) {
        rv.add(instanceElement.getHostName());
      }

      return rv;
    }
  }
}
