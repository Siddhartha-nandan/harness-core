package io.harness.delegate.beans.ecs;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class EcsBlueGreenRollbackResult {
  private String region;
  private List<EcsTask> ecsTasks;
  private boolean isFirstDeployment;
  private String loadBalancer;
  private String prodListenerArn;
  private String prodListenerRuleArn;
  private String prodTargetGroupArn;
  private String stageListenerArn;
  private String stageListenerRuleArn;
  private String stageTargetGroupArn;
  private String infrastructureKey;
}
