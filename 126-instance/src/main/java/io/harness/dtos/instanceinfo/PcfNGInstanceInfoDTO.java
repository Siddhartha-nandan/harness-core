package io.harness.dtos.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.util.InstanceSyncKey;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PcfNGInstanceInfoDTO extends InstanceInfoDTO {
  @NotNull private String id;
  @NotNull private String organization;
  @NotNull private String space;
  @NotNull private String pcfApplicationName;
  private String pcfApplicationGuid;
  private String instanceIndex;
  @Override
  public String prepareInstanceKey() {
    return InstanceSyncKey.builder().clazz(PcfNGInstanceInfoDTO.class).part(id).build().toString();
  }

  @Override
  public String prepareInstanceSyncHandlerKey() {
    return InstanceSyncKey.builder().part(pcfApplicationName).build().toString();
  }

  @Override
  public String getPodName() {
    return id;
  }

  @Override
  public String getType() {
    return "Pcf";
  }
}
