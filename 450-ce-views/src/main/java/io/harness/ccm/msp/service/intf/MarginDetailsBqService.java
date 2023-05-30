/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.msp.service.intf;

import io.harness.ccm.msp.entities.MarginDetails;

public interface MarginDetailsBqService {
  void insertMarginDetailsInBQ(MarginDetails marginDetails);
  void updateMarginDetailsInBQ(MarginDetails marginDetails);
}
