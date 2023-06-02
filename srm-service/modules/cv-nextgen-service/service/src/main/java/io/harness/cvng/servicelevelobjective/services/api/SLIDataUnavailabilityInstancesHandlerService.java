/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.services.api;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.entities.SLIRecordParam;

import java.time.Instant;
import java.util.List;

public interface SLIDataUnavailabilityInstancesHandlerService {
  List<SLIRecordParam> filterSLIRecordsToSkip(List<SLIRecordParam> sliRecordList, ProjectParams projectParams,
      Instant startTime, Instant endTime, String monitoredServiceIdentifier, String sliId);
}
