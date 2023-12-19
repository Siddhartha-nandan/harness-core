/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_ARTIFACTS})
@Value
@EqualsAndHashCode(callSuper = false)
public class S3ArtifactDelegateResponse extends ArtifactDelegateResponse {
  /** Buckets in repos*/
  String bucketName;

  /** filePath refers to exact tag number */
  String filePath;

  /** filePathRegex refers to tag regex */
  String filePathRegex;

  Map<String, String> metadata;

  @Builder
  public S3ArtifactDelegateResponse(ArtifactBuildDetailsNG buildDetails, ArtifactSourceType sourceType,
      String bucketName, String filePath, String filePathRegex, Map<String, String> metadata) {
    super(buildDetails, sourceType);

    this.bucketName = bucketName;
    this.filePath = filePath;
    this.filePathRegex = filePathRegex;
    this.metadata = metadata;
  }

  @Override
  public String describe() {
    String type = getSourceType() != null ? getSourceType().getDisplayName() : null;
    return String.format("type: %s \nbucketName: %s \nfilePath: %s \nfilePathRegex: %s", type, getBucketName(),
        StringUtils.defaultIfBlank(getFilePath(), null), StringUtils.defaultIfBlank(getFilePathRegex(), null));
  }
}
