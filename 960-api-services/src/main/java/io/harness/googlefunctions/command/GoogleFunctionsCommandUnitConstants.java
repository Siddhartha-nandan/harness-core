/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.googlefunctions.command;

public enum GoogleFunctionsCommandUnitConstants {
  fetchManifests {
    @Override
    public String toString() {
      return "Fetch Manifests";
    }
  },
  prepareRollbackData {
    @Override
    public String toString() {
      return "Prepare Rollback Data";
    }
  },
  deploy {
    @Override
    public String toString() {
      return "Deploy";
    }
  },
  rollback {
    @Override
    public String toString() {
      return "Rollback";
    }
  },
  trafficShift {
    @Override
    public String toString() {
      return "Traffic Shift";
    }
  }
}
