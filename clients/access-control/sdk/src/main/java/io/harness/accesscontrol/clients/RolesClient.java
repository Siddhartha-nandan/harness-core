/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.clients;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;

import javax.ws.rs.BeanParam;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

@OwnedBy(HarnessTeam.CDC)
public interface RolesClient {
  String ROLES_GROUP_API = "/roles";

  @GET(ROLES_GROUP_API + "/{identifier}")
  Call<ResponseDTO<RoleResponseDTO>> get(@Path(value = NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @BeanParam HarnessScopeParams harnessScopeParams);
}
