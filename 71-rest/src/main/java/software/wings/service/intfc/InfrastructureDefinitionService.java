package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.api.DeploymentType;
import software.wings.infra.InfrastructureDefinition;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

public interface InfrastructureDefinitionService {
  PageResponse<InfrastructureDefinition> list(PageRequest<InfrastructureDefinition> pageRequest);
  InfrastructureDefinition save(@Valid InfrastructureDefinition infrastructureDefinition);
  InfrastructureDefinition get(String appId, String infraDefinitionId);
  InfrastructureDefinition update(@Valid InfrastructureDefinition infrastructureDefinition);
  void delete(String appId, String infraDefinitionId);

  Map<DeploymentType, List<SettingVariableTypes>> getDeploymentTypeCloudProviderOptions();
}
