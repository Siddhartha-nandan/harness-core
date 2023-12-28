/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.manifest.yaml.kinds;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.SwaggerConstants.BOOLEAN_CLASSPATH;
import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.beans.SwaggerConstants.STRING_LIST_CLASSPATH;
import static io.harness.cdng.manifest.yaml.harness.HarnessStoreConstants.HARNESS_STORE_TYPE;
import static io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper.StoreConfigWrapperParameters;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.bool;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.expression;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.HelmManifestCommandFlag;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfigHelper;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.visitor.helpers.manifest.HelmChartManifestVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.validator.EntityIdentifier;
import io.harness.k8s.model.HelmVersion;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@OwnedBy(CDC)
@Data
@Builder
@FieldNameConstants(innerTypeName = "HelmChartManifestKeys")
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.HelmChart)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = HelmChartManifestVisitorHelper.class)
@TypeAlias("helmChartManifest")
@RecasterAlias("io.harness.cdng.manifest.yaml.kinds.HelmChartManifest")
public class HelmChartManifest implements ManifestAttributes, Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;

  @EntityIdentifier String identifier;
  @Wither
  @JsonProperty("store")
  @ApiModelProperty(dataType = "io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper")
  @SkipAutoEvaluation
  ParameterField<StoreConfigWrapper> store;
  @Wither @ApiModelProperty(dataType = STRING_CLASSPATH) @SkipAutoEvaluation ParameterField<String> chartName;
  @Wither @ApiModelProperty(dataType = STRING_CLASSPATH) @SkipAutoEvaluation ParameterField<String> chartVersion;
  @Wither HelmVersion helmVersion;
  @Wither
  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH)
  @YamlSchemaTypes({expression})
  @SkipAutoEvaluation
  @JsonProperty("valuesPaths")
  ParameterField<List<String>> valuesPaths;
  @Wither
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH)
  @YamlSchemaTypes({string})
  @SkipAutoEvaluation
  ParameterField<Boolean> skipResourceVersioning;

  @Wither @YamlSchemaTypes({string, bool}) @SkipAutoEvaluation ParameterField<Boolean> enableDeclarativeRollback;
  @Wither List<HelmManifestCommandFlag> commandFlags;
  @Wither @ApiModelProperty(dataType = STRING_CLASSPATH) @SkipAutoEvaluation ParameterField<String> subChartPath;
  @Wither
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH)
  @YamlSchemaTypes({expression})
  ParameterField<Boolean> fetchHelmChartMetadata;

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    HelmChartManifest helmChartManifest = (HelmChartManifest) overrideConfig;
    HelmChartManifest resultantManifest = this;
    if (helmChartManifest.getStore() != null && helmChartManifest.getStore().getValue() != null) {
      StoreConfigWrapper storeConfigOverride = helmChartManifest.getStore().getValue();
      resultantManifest = resultantManifest.withStore(
          ParameterField.createValueField(store.getValue().applyOverrides(storeConfigOverride)));
    }

    if (!ParameterField.isNull(helmChartManifest.getChartName())) {
      resultantManifest = resultantManifest.withChartName(helmChartManifest.getChartName());
    }

    if (!ParameterField.isNull(helmChartManifest.getChartVersion())) {
      resultantManifest = resultantManifest.withChartVersion(helmChartManifest.getChartVersion());
    }

    if (helmChartManifest.getHelmVersion() != null) {
      resultantManifest = resultantManifest.withHelmVersion(helmChartManifest.getHelmVersion());
    }

    if (!ParameterField.isNull(helmChartManifest.getValuesPaths())) {
      resultantManifest = resultantManifest.withValuesPaths(helmChartManifest.getValuesPaths());
    }

    if (helmChartManifest.getSkipResourceVersioning() != null) {
      resultantManifest = resultantManifest.withSkipResourceVersioning(helmChartManifest.getSkipResourceVersioning());
    }

    if (helmChartManifest.getCommandFlags() != null) {
      resultantManifest = resultantManifest.withCommandFlags(new ArrayList<>(helmChartManifest.getCommandFlags()));
    }

    if (helmChartManifest.getSubChartPath() != null) {
      resultantManifest = resultantManifest.withSubChartPath(helmChartManifest.getSubChartPath());
    }

    if (helmChartManifest.getEnableDeclarativeRollback() != null) {
      resultantManifest =
          resultantManifest.withEnableDeclarativeRollback(helmChartManifest.getEnableDeclarativeRollback());
    }

    return resultantManifest;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add(YAMLFieldNameConstants.STORE, store.getValue());
    return children;
  }

  @Override
  public String getKind() {
    return ManifestType.HelmChart;
  }

  @Override
  public StoreConfig getStoreConfig() {
    return this.store.getValue().getSpec();
  }

  @Override
  public ManifestAttributeStepParameters getManifestAttributeStepParameters() {
    return new HelmChartManifestStepParameters(identifier,
        StoreConfigWrapperParameters.fromStoreConfigWrapper(store.getValue()), chartName, chartVersion, helmVersion,
        valuesPaths, skipResourceVersioning, commandFlags, subChartPath, enableDeclarativeRollback);
  }

  @Value
  public static class HelmChartManifestStepParameters implements ManifestAttributeStepParameters {
    String identifier;
    StoreConfigWrapperParameters store;
    ParameterField<String> chartName;
    ParameterField<String> chartVersion;
    HelmVersion helmVersion;
    ParameterField<List<String>> valuesPaths;
    ParameterField<Boolean> skipResourceVersioning;
    List<HelmManifestCommandFlag> commandFlags;
    ParameterField<String> subChartPath;
    ParameterField<Boolean> enableDeclarativeRollback;
  }

  @Override
  public Set<String> validateAtRuntime() {
    Set<String> invalidParameters = new HashSet<>();
    StoreConfig storeConfig = getStoreConfig();
    if (storeConfig != null) {
      invalidParameters.addAll(storeConfig.validateAtRuntime());
      String storeKind = storeConfig.getKind();
      if (EmptyPredicate.isNotEmpty(storeKind)) {
        validateHelmChartNameValueAtRuntime(invalidParameters, storeKind);
      }
    }
    return invalidParameters;
  }

  private void validateHelmChartNameValueAtRuntime(Set<String> invalidParameters, String storeKind) {
    if (!(ManifestStoreType.isInGitSubset(storeKind)) && !(HARNESS_STORE_TYPE.equals(storeKind))) {
      if (StoreConfigHelper.checkStringParameterNullOrInput(chartName)) {
        invalidParameters.add(HelmChartManifestKeys.chartName);
      }
    }
  }
}
