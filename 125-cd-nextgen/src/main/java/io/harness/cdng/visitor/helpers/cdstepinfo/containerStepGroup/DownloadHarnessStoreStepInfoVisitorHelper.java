/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.visitor.helpers.cdstepinfo.containerStepGroup;

import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class DownloadHarnessStoreStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return DownloadHarnessStoreStepInfo.infoBuilder().build();
  }

  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // nothing to validate
  }
}
