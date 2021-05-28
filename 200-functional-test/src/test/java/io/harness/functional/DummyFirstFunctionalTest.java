package io.harness.functional;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;

import com.google.inject.Inject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class DummyFirstFunctionalTest extends AbstractFunctionalTest {
  @Inject private AccountSetupService accountSetupService;

  @Before
  @Override
  public void testSetup() throws IOException {
    account = accountSetupService.ensureAccount();
    adminUser = Setup.loginUser(ADMIN_USER, "admin");
    bearerToken = adminUser.getToken();
    log.info("Basic setup completed.");
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(FunctionalTests.class)
  public void accessManagementPermissionTestForList() {
    assertThat(true).isTrue();
  }
}
