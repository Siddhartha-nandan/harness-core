/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.steps.pluginstep;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import dev.morphia.annotations.Id;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("KubernetesInfraOutput")
@JsonTypeName("KubernetesInfraOutput")
@OwnedBy(CDP)
@RecasterAlias("io.harness.plancreator.steps.pluginstep.KubernetesInfraOutput")
public class KubernetesInfraOutput implements ExecutionSweepingOutput, Outcome {
  public static final String KUBERNETES_INFRA_OUTPUT = "KubernetesInfraOutput";
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore String uuid;
  String infraRefId;
}
