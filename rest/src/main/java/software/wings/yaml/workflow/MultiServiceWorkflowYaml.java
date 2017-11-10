package software.wings.yaml.workflow;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author rktummala on 11/1/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MultiServiceWorkflowYaml extends WorkflowYaml {
  public static final class Builder extends WorkflowYaml.Builder {
    private Builder() {}

    public static Builder aYaml() {
      return new Builder();
    }

    @Override
    protected MultiServiceWorkflowYaml getWorkflowYaml() {
      return new MultiServiceWorkflowYaml();
    }
  }
}