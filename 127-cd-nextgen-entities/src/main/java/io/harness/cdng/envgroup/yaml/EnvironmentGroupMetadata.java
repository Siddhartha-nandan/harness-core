package io.harness.cdng.envgroup.yaml;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.yaml.YamlNode;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@RecasterAlias("io.harness.cdng.envgroup.yaml.EnvironmentGroupMetadata")
public class EnvironmentGroupMetadata {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;

  Boolean parallel;
}
