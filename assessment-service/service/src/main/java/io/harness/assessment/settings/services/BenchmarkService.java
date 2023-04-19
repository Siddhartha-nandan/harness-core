/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.assessment.settings.services;

import io.harness.assessment.settings.beans.dto.BenchmarkDTO;
import io.harness.assessment.settings.beans.dto.BenchmarksListRequest;
import io.harness.assessment.settings.beans.dto.upload.BenchmarkUploadResponse;

import java.util.List;

public interface BenchmarkService {
  BenchmarkUploadResponse uploadBenchmark(BenchmarksListRequest benchmarksListRequest, String assessmentId);

  List<BenchmarkDTO> getBenchmarks(String assessmentId, Long version);
}
