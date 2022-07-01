package io.harness.plancreator.steps.email;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.http.PmsAbstractStepNode;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.EMAIL)
@TypeAlias("EmailStepNode")
@OwnedBy(CDC)
@RecasterAlias("io.harness.steps.email.EmailStepNode")
public class EmailStepNode extends PmsAbstractStepNode {
  @JsonProperty("type") @NotNull StepType type = StepType.Email;
  @NotNull
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  EmailStepInfo emailStepInfo;
  @Override
  public String getType() {
    return StepSpecTypeConstants.EMAIL;
  }

  @Override
  public StepSpecType getStepSpecType() {
    return emailStepInfo;
  }

  enum StepType {
    Email(StepSpecTypeConstants.EMAIL);
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
