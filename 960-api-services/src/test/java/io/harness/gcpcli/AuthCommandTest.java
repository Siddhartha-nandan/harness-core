/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gcpcli;

import static io.harness.rule.OwnerRule.PRATYUSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AuthCommandTest extends CategoryTest {
  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void servicePrincipalWithCertAuthTest() {
    GcpCliClient client = GcpCliClient.client("gcloud");
    AuthCommand authCommand = client.auth().keyFile("/path/to/key/file");
    assertThat(authCommand.command())
        .isEqualTo("gcloud auth activate-service-account --key-file /path/to/key/file --quiet");
  }
}
