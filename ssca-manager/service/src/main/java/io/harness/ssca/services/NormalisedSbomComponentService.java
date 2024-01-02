/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import io.harness.spec.server.ssca.v1.model.Artifact;
import io.harness.spec.server.ssca.v1.model.ArtifactComponentViewRequestBody;
import io.harness.spec.server.ssca.v1.model.ComponentFilter;
import io.harness.spec.server.ssca.v1.model.LicenseFilter;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;
import io.harness.ssca.entities.artifact.ArtifactEntity;

import java.util.List;
import javax.ws.rs.core.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;

public interface NormalisedSbomComponentService {
  List<NormalizedSBOMComponentEntity> getNormalizedSbomComponentsForOrchestrationId(String accountId,
      String orgIdentifier, String projectIdentifier, String orchestrationId, List<String> fieldsToBeIncluded);
  Response listNormalizedSbomComponent(
      String orgIdentifier, String projectIdentifier, Integer page, Integer limit, Artifact body, String accountId);

  Page<NormalizedSBOMComponentEntity> getNormalizedSbomComponents(String accountId, String orgIdentifier,
      String projectIdentifier, ArtifactEntity artifact, ArtifactComponentViewRequestBody filterBody,
      Pageable pageable);

  List<String> getOrchestrationIds(String accountId, String orgIdentifier, String projectIdentifier,
      LicenseFilter licenseFilter, List<ComponentFilter> componentFilter);

  <T> List<T> getComponentsByAggregation(Aggregation aggregation, Class<T> resultClass);

  List<NormalizedSBOMComponentEntity> getComponentsOfSbomByLicense(String orchestrationId, String license);
}
