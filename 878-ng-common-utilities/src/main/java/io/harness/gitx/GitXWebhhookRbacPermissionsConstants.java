package io.harness.gitx;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface GitXWebhhookRbacPermissionsConstants {
  String GitXWebhhook_CREATE_AND_EDIT = "core_gitxWebhooks_edit";
  String GitXWebhhook_DELETE = "core_gitxWebhooks_delete";
  String GitXWebhhook_VIEW = "core_gitxWebhooks_view";
}
