package io.harness.validation;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OneOfFieldValidatorTest extends CategoryTest {
  OneOfFieldValidator oneOfFieldValidator = new OneOfFieldValidator();

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testIsValid() {
    Set<String> oneOfs = new HashSet<>();
    oneOfs.add("a");
    oneOfs.add("c");
    oneOfFieldValidator.fields = oneOfs;

    testIsValidClass(TestClass.builder().a("1").b("2").c(InnerClass.builder().build()).build(), false);
    testIsValidClass(TestClass.builder().b("2").c(InnerClass.builder().build()).build(), true);
    testIsValidClass(TestClass.builder().a("1").b("2").build(), true);
    testIsValidClass(TestClass.builder().b("2").build(), false);

    oneOfFieldValidator.nullable = true;
    testIsValidClass(TestClass.builder().a("1").b("2").c(InnerClass.builder().build()).build(), false);
    testIsValidClass(TestClass.builder().b("2").c(InnerClass.builder().build()).build(), true);
    testIsValidClass(TestClass.builder().a("1").b("2").build(), true);
    testIsValidClass(TestClass.builder().b("2").build(), true);
  }

  private void testIsValidClass(TestClass testClass, boolean expected) {
    final boolean valid = oneOfFieldValidator.isValid(testClass, null);
    assertThat(valid).isEqualTo(expected);
  }

  @Builder
  public static class TestClass {
    String a;
    String b;
    InnerClass c;
  }

  @Builder
  public static class InnerClass {
    String c;
  }
}