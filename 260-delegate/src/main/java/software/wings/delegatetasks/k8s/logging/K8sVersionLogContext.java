/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.k8s.logging;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.logcontext.AutoLogContext;

import com.google.common.collect.ImmutableMap;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class K8sVersionLogContext extends AutoLogContext {
  public static final String CLOUD_PROVIDER = "cloudProvider";
  public static final String VERSION = "version";
  public static final String CC_ENABLED = "ccEnabled";

  public K8sVersionLogContext(String cloudProvider, String version, boolean ccEnabled, OverrideBehavior behavior) {
    super(ImmutableMap.of(CLOUD_PROVIDER, cloudProvider, VERSION, version, CC_ENABLED, String.valueOf(ccEnabled)),
        behavior);
  }
}
