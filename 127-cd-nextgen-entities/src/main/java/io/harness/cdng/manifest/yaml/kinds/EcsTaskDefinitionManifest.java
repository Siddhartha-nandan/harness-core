package io.harness.cdng.manifest.yaml.kinds;

import static io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper.StoreConfigWrapperParameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.visitor.helpers.manifest.EcsTaskDefinitionManifestVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.EcsTaskDefinition)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = EcsTaskDefinitionManifestVisitorHelper.class)
@TypeAlias("ecsTaskDefinitionManifest")
@RecasterAlias("io.harness.cdng.manifest.yaml.kinds.EcsTaskDefinitionManifest")
public class EcsTaskDefinitionManifest implements ManifestAttributes, Visitable {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    private String uuid;

    @EntityIdentifier
    String identifier;

    @Wither
    @JsonProperty("store")
    @ApiModelProperty(dataType = "io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper")
    @SkipAutoEvaluation
    ParameterField<StoreConfigWrapper> store;

    // For Visitor Framework Impl
    String metadata;

    @Override
    public String getKind() {
        return ManifestType.EcsTaskDefinition;
    }

    @Override
    public StoreConfig getStoreConfig() {
        return store.getValue().getSpec();
    }

    @Override
    public VisitableChildren getChildrenToWalk() {
        VisitableChildren children = VisitableChildren.builder().build();
        children.add(YAMLFieldNameConstants.STORE, store.getValue());
        return children;
    }

    @Override
    public ManifestAttributeStepParameters getManifestAttributeStepParameters() {
        return new EcsTaskDefinitionManifestStepParameters(
                identifier, StoreConfigWrapperParameters.fromStoreConfigWrapper(store.getValue()));
    }

    @Override
    public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
        EcsTaskDefinitionManifest ecsTaskDefinitionManifest =
                (EcsTaskDefinitionManifest) overrideConfig;
        EcsTaskDefinitionManifest resultantManifest = this;
        if (ecsTaskDefinitionManifest.getStore() != null && ecsTaskDefinitionManifest.getStore().getValue() != null) {
            resultantManifest = resultantManifest.withStore(ParameterField.createValueField(
                    store.getValue().applyOverrides(ecsTaskDefinitionManifest.getStore().getValue())));
        }
        return resultantManifest;
    }

    @Value
    public static class EcsTaskDefinitionManifestStepParameters implements ManifestAttributeStepParameters {
        String identifier;
        StoreConfigWrapperParameters store;
    }
}
