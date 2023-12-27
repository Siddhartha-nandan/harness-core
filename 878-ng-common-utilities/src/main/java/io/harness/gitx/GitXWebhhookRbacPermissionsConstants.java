/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitx;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface GitXWebhhookRbacPermissionsConstants {
  String GitXWebhhook_CREATE_AND_EDIT = "core_gitxWebhooks_edit";
  String GitXWebhhook_DELETE = "core_gitxWebhooks_delete";
  String GitXWebhhook_VIEW = "core_gitxWebhooks_view";
}
