package io.harness.cdng.infra.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.infra.beans.EcsInfraMapping;
import io.harness.cdng.infra.beans.GoogleFunctionsInfraMapping;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.filters.ConnectorRefExtractorHelper;
import io.harness.filters.WithConnectorRef;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
@JsonTypeName(InfrastructureKind.GOOGLE_CLOUD_FUNCTIONS)
@SimpleVisitorHelper(helperClass = ConnectorRefExtractorHelper.class)
@TypeAlias("googleFunctionsInfrastructure")
@RecasterAlias("io.harness.cdng.infra.yaml.GoogleFunctionsInfrastructure")
public class GoogleFunctionsInfrastructure
        extends InfrastructureDetailsAbstract implements Infrastructure, Visitable, WithConnectorRef {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    String uuid;

    @NotNull
    @NotEmpty
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    @Wither
    ParameterField<String> connectorRef;
    @NotNull
    @NotEmpty
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    @Wither
    ParameterField<String> project;
    @NotNull
    @NotEmpty
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    @Wither
    ParameterField<String> region;

    @Override
    public InfraMapping getInfraMapping() {
        return GoogleFunctionsInfraMapping.builder()
                .gcpConnector(connectorRef.getValue())
                .region(region.getValue())
                .project(project.getValue())
                .build();
    }

    @Override
    public String getKind() {
        return InfrastructureKind.GOOGLE_CLOUD_FUNCTIONS;
    }

    @Override
    public ParameterField<String> getConnectorReference() {
        return connectorRef;
    }

    @Override
    public String[] getInfrastructureKeyValues() {
        return new String[] {connectorRef.getValue(), project.getValue(), region.getValue()};
    }

    @Override
    public Map<String, ParameterField<String>> extractConnectorRefs() {
        Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
        connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
        return connectorRefMap;
    }

    @Override
    public Infrastructure applyOverrides(Infrastructure overrideConfig) {
        GoogleFunctionsInfrastructure config = (GoogleFunctionsInfrastructure) overrideConfig;
        GoogleFunctionsInfrastructure resultantInfra = this;
        if (!ParameterField.isNull(config.getConnectorRef())) {
            resultantInfra = resultantInfra.withConnectorRef(config.getConnectorRef());
        }
        if (!ParameterField.isNull(config.getRegion())) {
            resultantInfra = resultantInfra.withRegion(config.getRegion());
        }
        if (!ParameterField.isNull(config.getProject())) {
            resultantInfra = resultantInfra.withProject(config.getProject());
        }
        return resultantInfra;
    }
}
