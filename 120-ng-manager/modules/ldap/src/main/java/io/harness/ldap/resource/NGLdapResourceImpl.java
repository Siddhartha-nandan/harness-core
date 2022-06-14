/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ldap.resource;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class NGLdapResourceImpl implements NGLdapResource {
  @Override
  public Response test(String userIdentifier, String accountIdentifier) {
    log.info("NGLDAP: Received request: " + userIdentifier);
    return Response.status(Response.Status.OK).entity(userIdentifier).build();
  }
}
