/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.http.v1;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.http.HttpHeaderConfig;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.internal.v1.PmsAbstractStepNodeV1;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepSpecTypeConstantsV1;
import io.harness.yaml.utils.v1.NGVariablesUtilsV1;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(PIPELINE)
@JsonTypeName(StepSpecTypeConstantsV1.HTTP)
@Builder
public class HttpStepNodeV1 extends PmsAbstractStepNodeV1 {
  @Getter String type = StepSpecTypeConstantsV1.HTTP;

  @Getter @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) HttpStepInfoV1 spec;

  @Override
  public SpecParameters getSpecParameters() {
    return HttpStepParameters.infoBuilder()
        .assertion(spec.getAssertion())
        .headers(EmptyPredicate.isEmpty(spec.getHeaders()) ? Collections.emptyMap()
                                                           : spec.getHeaders().stream().collect(Collectors.toMap(
                                                               HttpHeaderConfig::getKey, HttpHeaderConfig::getValue)))
        .cert(spec.getCert())
        .cert_key(spec.getCert_key())
        .method(spec.getMethod())
        .output_vars(NGVariablesUtilsV1.getMapOfVariables(
            spec.getOutput_vars() != null ? spec.getOutput_vars().getMap() : null, 0L))
        .input_vars(NGVariablesUtilsV1.getMapOfVariables(
            spec.getInput_vars() != null ? spec.getInput_vars().getMap() : null, 0L))
        .body(spec.getBody())
        .delegates(ParameterField.createValueField(
            CollectionUtils.emptyIfNull(spec.getDelegates() != null ? spec.getDelegates().getValue() : null)))
        .url(spec.getUrl())
        .build();
  }
}
