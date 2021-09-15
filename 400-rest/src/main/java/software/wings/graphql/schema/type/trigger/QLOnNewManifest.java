/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

@Getter
@OwnedBy(CDC)
@Builder
@FieldNameConstants(innerTypeName = "QLOnNewManifestKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLOnNewManifest implements QLTriggerCondition {
  private QLTriggerConditionType triggerConditionType;
  private String appManifestId;
  private String appManifestName;
  private String serviceId;
  private String versionRegex;
}
