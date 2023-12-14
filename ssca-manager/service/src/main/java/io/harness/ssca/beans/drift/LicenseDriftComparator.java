/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.drift;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Comparator;

@OwnedBy(HarnessTeam.SSCA)
public class LicenseDriftComparator implements Comparator<LicenseDrift> {
  @Override
  public int compare(LicenseDrift o1, LicenseDrift o2) {
    // Assuming either of o1 or o2 is not null.
    return o1.getName().compareTo(o2.getName());
  }
}
