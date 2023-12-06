/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.utils;

import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.harness.HarnessApiAccessDTO;
import io.harness.delegate.beans.connector.scm.harness.HarnessApiAccessType;
import io.harness.delegate.beans.connector.scm.harness.HarnessConnectorDTO;
import io.harness.delegate.beans.connector.scm.harness.HarnessJWTTokenSpecDTO;
import io.harness.encryption.SecretRefData;
import io.harness.git.GitClientHelper;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;

@Singleton
public class HarnessCodeConnectorUtils {
  @Inject ServiceTokenGenerator tokenGenerator;

  public HarnessConnectorDTO getDummyHarnessCodeConnectorWithJwtAuth(String repoName, String accountId, String orgId,
      String projectId, String serviceSecret, String harnessCodeApiBaseUrl) {
    SecretRefData token = SecretRefData.builder().decryptedValue(getToken(serviceSecret).toCharArray()).build();
    HarnessJWTTokenSpecDTO jwtTokenSpecDTO = HarnessJWTTokenSpecDTO.builder().tokenRef(token).build();
    return HarnessConnectorDTO.builder()
        .connectionType(GitConnectionType.REPO)
        .apiAccess(HarnessApiAccessDTO.builder().spec(jwtTokenSpecDTO).type(HarnessApiAccessType.JWT_TOKEN).build())
        .connectionType(GitConnectionType.REPO)
        .executeOnDelegate(false)
        .slug(GitClientHelper.convertToHarnessRepoName(accountId, orgId, projectId, repoName) + "/+")
        .apiUrl(harnessCodeApiBaseUrl)
        .build();
  }

  private String getToken(String serviceSecret) {
    return tokenGenerator.getServiceTokenWithDuration(serviceSecret, Duration.ofHours(1));
  }
}
