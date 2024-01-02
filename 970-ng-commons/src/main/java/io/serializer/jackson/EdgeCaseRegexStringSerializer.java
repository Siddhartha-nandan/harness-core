/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.serializer.jackson;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.util.regex.Pattern;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PIPELINE)
// This serializer handles the edge cases when we need a string to be surrounded by quotes in the compiled yaml.
// UnitTests for these cases are added in YamlUtilsTest.java
public class EdgeCaseRegexStringSerializer extends StdSerializer<String> {
  // The string is an SHA digest. If it's something like 97e0087, it matches the scientific notation and hence without
  // quotes, it gets converted to a number 9.7E88, to avoid this, we are adding quotes around the string which matches
  // following regex.
  // The scientific notation has 2 components. Before e/E and after e/E. Before e can be any Number(Negative/Positive
  // and can include decimal). And after e component can be any Integer(Negative/Zero/Positive).
  private final Pattern shaRegex = Pattern.compile("^[-+]?(?:\\d+\\.\\d*|\\.\\d+|\\d+)[eE][-+]?\\d+$");
  // When string is of type +1234.23, it also needs to be wrapped around quotes, else, it'll be considered a number and
  // user won't be able to save a string variable with this value
  private final Pattern positiveNumberRegex = Pattern.compile("[+][0-9]*(\\.[0-9]*)?");

  // When string is of type 123_321, it also needs to be wrapped around quotes, else, it'll be considered a number and
  // user will be able to save that value but, upon reloading UI will remove underscores from it. The string should
  // start with a number and have at-least 1 underscore in it.
  private final Pattern numbersWithUnderscoresRegex = Pattern.compile("^[0-9][0-9_]*_[0-9_]*$");
  private final Pattern dateWithTimezoneFormatRegex =
      Pattern.compile("^[0-9][0-9][0-9][0-9][-/][0-1][0-9][-/][0-3][0-9][T ][0-2][0-9]:[0-5][0-9]:[0-5][0-9]Z?$",
          Pattern.CASE_INSENSITIVE);

  // When string is of type 00:00:00.100, it also needs to be wrapped around quotes, else, it'll be considered a number
  // and User will be able to save the pipeline, but will get error while running the pipeline as quotes are removed
  // while merging the Yaml.
  private final Pattern timePatternWithMilliseconds = Pattern.compile("[\\d+:]+\\d+\\.\\d+$");
  public EdgeCaseRegexStringSerializer() {
    super(String.class);
  }

  @Override
  public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    if (value != null) {
      if (shaRegex.matcher(value).matches() || positiveNumberRegex.matcher(value).matches()
          || numbersWithUnderscoresRegex.matcher(value).matches()
          || dateWithTimezoneFormatRegex.matcher(value).matches()
          || timePatternWithMilliseconds.matcher(value).matches()) {
        YAMLGenerator yamlGenerator = (YAMLGenerator) gen;
        yamlGenerator.disable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        yamlGenerator.writeString(value);
        yamlGenerator.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
      } else {
        gen.writeString(value);
      }
    }
  }
}
