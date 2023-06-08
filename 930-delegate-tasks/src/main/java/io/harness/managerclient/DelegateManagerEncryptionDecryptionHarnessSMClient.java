/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.managerclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptedRecord;
import io.harness.beans.EncryptData;
import io.harness.beans.EncryptedSMData;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptedRecordData;

import javax.ws.rs.Consumes;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.CDP)
public interface DelegateManagerEncryptionDecryptionHarnessSMClient {
  @POST("encryption/encrypt-harness-sm-secret")
  @Consumes({"application/x-protobuf"})
  Call<RestResponse<EncryptedRecordData>> encryptHarnessSMSecret(
      @Query("accountId") String accountId, @Body EncryptData encryptData);

  @POST("encryption/decrypt-harness-sm-secret")
  @Consumes({"application/x-protobuf"})
  Call<RestResponse<DecryptedRecord>> decryptHarnessSMSecret(
      @Query("accountId") String accountId, @Body EncryptedSMData encryptedSMData);
}
