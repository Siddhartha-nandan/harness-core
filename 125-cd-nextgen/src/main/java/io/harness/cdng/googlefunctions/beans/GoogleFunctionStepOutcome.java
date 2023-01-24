package io.harness.cdng.googlefunctions.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
@TypeAlias("googleFunctionStepOutcome")
@JsonTypeName("googleFunctionStepOutcome")
@RecasterAlias("io.harness.cdng.googlefunctions.GoogleFunctionStepOutcome")
public class GoogleFunctionStepOutcome implements Outcome, ExecutionSweepingOutput {
  String functionName;
  String runtime;
  String state;
  String environment;
  @Nonnull GoogleCloudRunService cloudRunService;
  @Nonnull List<GoogleCloudRunRevision> activeCloudRunRevisions;

  @Value
  @Builder
  public static class GoogleCloudRunService {
    String serviceName;
    String memory;
    String revision;
  }

  @Value
  @Builder
  public static class GoogleCloudRunRevision {
    String revision;
    Integer trafficPercent;
  }
}
