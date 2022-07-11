package io.harness.cdng.service.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.configfile.ConfigFileWrapper;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.visitor.helpers.serviceconfig.EcsServiceSpecVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.yaml.core.variables.NGVariable;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;
import io.harness.cdng.service.ServiceSpec;
import io.harness.walktree.visitor.Visitable;


import java.util.List;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
@JsonTypeName(ServiceSpecType.ECS)
@SimpleVisitorHelper(helperClass = EcsServiceSpecVisitorHelper.class)
@TypeAlias("ecsServiceSpec")
@RecasterAlias("io.harness.cdng.service.beans.EcsServiceSpec")
public class EcsServiceSpec implements ServiceSpec, Visitable {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    String uuid;
    List<NGVariable> variables;
    ArtifactListConfig artifacts;
    List<ManifestConfigWrapper> manifests;
    List<ConfigFileWrapper> configFiles;

    // For Visitor Framework Impl
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

    @Override
    public String getType() {
        return ServiceDefinitionType.ECS.getYamlName();
    }

    @Override
    public VisitableChildren getChildrenToWalk() {
        VisitableChildren children = VisitableChildren.builder().build();
        if (EmptyPredicate.isNotEmpty(variables)) {
            variables.forEach(ngVariable -> children.add("variables", ngVariable));
        }

        children.add("artifacts", artifacts);
        if (EmptyPredicate.isNotEmpty(manifests)) {
            manifests.forEach(manifest -> children.add("manifests", manifest));
        }

        if (EmptyPredicate.isNotEmpty(configFiles)) {
            configFiles.forEach(configFile -> children.add("configFiles", configFile));
        }

        return children;
    }
}
