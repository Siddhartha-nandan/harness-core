/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.app.resources;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.runner.DockerRunnerResource;
import io.harness.delegate.beans.DelegateType;
import io.harness.remote.client.CGRestUtils;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;

@Slf4j
@OwnedBy(CI)
@NextGenManagerAuth
public class DockerRunnerResourceImpl implements DockerRunnerResource {
  @Inject private AccountClient accountClient;

  public String get(String accountId, String os, String arch) throws Exception {
    Call<RestResponse<Map<String, String>>> req =
        accountClient.getInstallationCommand(accountId, DelegateType.DOCKER, os, arch);
    Map<String, String> res = CGRestUtils.getResponse(req);
    String command = res.get("command") + "\n\n";
    command +=
        "wget -q https://github.com/harness/harness-docker-runner/releases/latest/download/harness-docker-runner-linux-amd64 \n"
        + "sudo chmod +x harness-docker-runner-linux-amd64 \n"
        + "sudo ./harness-docker-runner-linux-amd64 server \n";

    return command;
  }
}
