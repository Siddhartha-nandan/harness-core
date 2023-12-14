/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.data.validator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.BHAVYA;
import static io.harness.rule.OwnerRule.VIKAS;
import static io.harness.utils.UuidAndIdentifierUtils.base64StrToUuid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.validator.uuid.Uuid;
import io.harness.rule.Owner;
import io.harness.utils.UuidAndIdentifierUtils;

import java.util.UUID;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class UuidValidatorTest extends CategoryTest {
  @Builder
  static class UuidValidatorTestStructure {
    @Uuid String str;
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testUuid() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    final Validator validator = factory.getValidator();

    // Some random string
    assertThat(validator.validate(UuidValidatorTestStructure.builder().str("abcd").build())).isNotEmpty();

    // Random UUID
    String accountId = UUID.randomUUID().toString();
    assertThat(validator.validate(UuidValidatorTestStructure.builder().str(accountId).build())).isEmpty();

    // Specific UUID
    assertThat(
        validator.validate(UuidValidatorTestStructure.builder().str("cdaed56d-8712-414d-b346-01905d0026fe").build()))
        .isEmpty();

    // Specific Base64 encoded UUID
    String base64Str = "za7VbYcSQU2zRgGQXQAm/g"; // a base64 encoded UUID
    String decodedUUIDStr = base64StrToUuid(base64Str);
    assertThat(decodedUUIDStr).isEqualTo("cdaed56d-8712-414d-b346-01905d0026fe");
    assertThat(validator.validate(UuidValidatorTestStructure.builder().str(base64Str).build())).isEmpty();

    // Random Base64 encoded UUID that is URL-safe
    String base64encodedUuid = generateUuid();
    assertThat(validator.validate(UuidValidatorTestStructure.builder().str(base64encodedUuid).build())).isEmpty();

    // Specific Base64 encoded UUID that is URL-safe
    String base64encodedUrlSafeUuid = "sXfoYJRPTOiIaqpICi_aUg";
    assertThat(validator.validate(UuidValidatorTestStructure.builder().str(base64encodedUrlSafeUuid).build()))
        .isEmpty();

    String uuidType1 = "efee4cba-9d5f-11e9-a2a3-2a2ae2dbcce4";
    assertThat(UuidAndIdentifierUtils.isValidUuidStr(uuidType1)).isTrue();

    String uuidType4 = "3bcd1e59-1dab-4f6f-a374-17b8e2339f64";
    assertThat(UuidAndIdentifierUtils.isValidUuidStr(uuidType4)).isTrue();
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void test_harnessUIFormatUUID() {
    String name = "$%^abc.ksxjx_+$++&UII J";
    assertThat(UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier(name)).isEqualTo("abcksxjx_$UII_J");
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_BasicCase() {
    assertEquals("example_Name", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier("example Name"));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_EmptyString() {
    assertEquals("", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier(""));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_StringWithSpecialCharacters() {
    assertEquals("user123", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier("@user#123"));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_StringWithLeadingDigitsDashesAndDollarSigns() {
    assertEquals("test", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier("123-$test"));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_StringWithVariousSpecialCharacters() {
    assertEquals("my_test_is_cool", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier("my@_test.is-cool"));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_StringWithAccentedCharacters() {
    assertEquals("Cafe", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier("Café"));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_StringWithMultipleSpaces() {
    assertEquals("multiple_spaces_here",
        uidAndIdentifierUtils.generateHarnessUIFormatIdentifier("  multiple   spaces  here  "));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_StringWithLeadingAndTrailingSpaces() {
    assertEquals("space_test", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier("  space_test  "));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_StringWithOnlyDigitsAndSpecialCharacters() {
    assertEquals("_12345", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier("$#12345"));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_StringWithMultipleConsecutiveDotsAndDashes() {
    assertEquals("test_case_here", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier("test...case---here"));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_StringWithNullInput() {
    assertEquals("", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier(null));
  }

  @Test
  public void testGenerateHarnessUIFormatIdentifier_StringWithSpecialCharactersAndSpaces() {
    assertEquals("
        my_test_case_is_here", UuidAndIdentifierUtils.generateHarnessUIFormatIdentifier("my!@ test. case - is $here"));
  }

  @Test
  public void testGenerateFormattedIdentifier_BasicCase() {
    assertEquals("example_Name", UuidAndIdentifierUtils.generateFormattedIdentifier("example Name"));
  }

  @Test
  public void testGenerateFormattedIdentifier_EmptyString() {
    assertEquals("", UuidAndIdentifierUtils.generateFormattedIdentifier(""));
  }

  @Test
  public void testGenerateFormattedIdentifier_StringWithAccentedCharacters() {
    assertEquals("Cafe", UuidAndIdentifierUtils.generateFormattedIdentifier("Café"));
  }

  @Test
  public void testGenerateFormattedIdentifier_StringWithMultipleSpaces() {
    assertEquals("m
        ultiple_spaces_here", UuidAndIdentifierUtils.generateFormattedIdentifier("  multiple   spaces  here  "));
  }

  @Test
  public void testGenerateFormattedIdentifier_StringWithLeadingAndTrailingSpaces() {
    assertEquals("space_test", UuidAndIdentifierUtils.generateFormattedIdentifier("  space_test  "));
  }

  @Test
  public void testGenerateFormattedIdentifier_StringWithMultipleConsecutiveDotsAndDashes() {
    assertEquals("test_case_here", UuidAndIdentifierUtils.generateFormattedIdentifier("test...case---here"));
  }

  @Test
  public void testGenerateFormattedIdentifier_StringWithNullInput() {
    assertEquals("", UuidAndIdentifierUtils.generateFormattedIdentifier(null));
  }
}
