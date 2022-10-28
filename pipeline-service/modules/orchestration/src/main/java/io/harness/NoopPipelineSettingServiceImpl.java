package io.harness;

public class NoopPipelineSettingServiceImpl implements PipelineSettingsService {
  @Override
  public PlanExecutionSettingResponse shouldQueuePlanExecution(
      String accountId, String orgId, String projectId, String pipelineIdentifier) {
    return PlanExecutionSettingResponse.builder().useNewFlow(false).shouldQueue(false).build();
  }
}
