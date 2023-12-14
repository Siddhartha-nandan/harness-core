package io.harness.notification.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class EventFrequency {
  int frequencyNumber;
  TimeUnit timeUnit;

  @Getter
  public enum TimeUnit {
    SECONDS("s", 1000),
    MINUTES("m", 60 * SECONDS.inMilliSeconds),
    HOURS("h", 60 * MINUTES.inMilliSeconds),
    DAYS("d", 24 * HOURS.inMilliSeconds),
    WEEKS("w", 7 * DAYS.inMilliSeconds);

    String unit;
    long inMilliSeconds; // in relation to milliseconds

    TimeUnit(String unit, long inMilliSeconds) {
      this.unit = unit;
      this.inMilliSeconds = inMilliSeconds;
    }
  }
}
