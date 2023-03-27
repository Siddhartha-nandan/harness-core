/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.assessment.settings.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.assessment.settings.beans.dto.BenchmarkDTO;
import io.harness.assessment.settings.beans.entities.Benchmark;
import io.harness.assessment.settings.beans.entities.Score;

import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.SEI)
@UtilityClass
public class BenchmarkMapper {
  public Benchmark fromDTO(BenchmarkDTO benchmarkDTO) {
    return Benchmark.builder()
        .benchmarkId(benchmarkDTO.getBenchmarkId())
        .benchmarkName(benchmarkDTO.getBenchmarkName())
        .scores(benchmarkDTO.getScores()
                    .stream()
                    .map(scoreDTO
                        -> Score.builder()
                               .score(scoreDTO.getScore())
                               .scoreType(scoreDTO.getScoreType())
                               .entityId(scoreDTO.getEntityId())
                               .build())
                    .collect(Collectors.toList()))
        .build();
  }
}
