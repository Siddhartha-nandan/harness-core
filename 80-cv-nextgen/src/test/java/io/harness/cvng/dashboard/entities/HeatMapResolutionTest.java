package io.harness.cvng.dashboard.entities;

import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.FIFTEEN_MINUTES;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.FIVE_MIN;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.FOUR_HOURS;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.THIRTY_MINUTES;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.TWELVE_HOURS;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class HeatMapResolutionTest extends CVNextGenBaseTest {
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHeatMapResolution() {
    Instant endTime = Instant.now();
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(10, ChronoUnit.MINUTES), endTime))
        .isEqualTo(FIVE_MIN);
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(4, ChronoUnit.HOURS), endTime)).isEqualTo(FIVE_MIN);
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(6, ChronoUnit.HOURS), endTime))
        .isEqualTo(FIFTEEN_MINUTES);
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(12, ChronoUnit.HOURS), endTime))
        .isEqualTo(FIFTEEN_MINUTES);
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(16, ChronoUnit.HOURS), endTime))
        .isEqualTo(THIRTY_MINUTES);
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(24, ChronoUnit.HOURS), endTime))
        .isEqualTo(THIRTY_MINUTES);
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(26, ChronoUnit.HOURS), endTime))
        .isEqualTo(FOUR_HOURS);
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(7, ChronoUnit.DAYS), endTime))
        .isEqualTo(FOUR_HOURS);
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(8, ChronoUnit.DAYS), endTime))
        .isEqualTo(TWELVE_HOURS);
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(30, ChronoUnit.DAYS), endTime))
        .isEqualTo(TWELVE_HOURS);
    assertThat(HeatMapResolution.getHeatMapResolution(endTime.minus(60, ChronoUnit.DAYS), endTime))
        .isEqualTo(TWELVE_HOURS);
  }
}
