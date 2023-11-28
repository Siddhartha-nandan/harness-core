/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */
package io.harness.shell;

import io.harness.logging.LogLevel;

import java.time.Instant;
import lombok.Data;

@Data
public class ShellScriptLog {
  Instant timeStamp;
  String text;
  LogLevel level;

  public ShellScriptLog(Instant timeStamp, String text, LogLevel level) {
    this.timeStamp = timeStamp;
    this.text = text;
    this.level = level;
  }
}
