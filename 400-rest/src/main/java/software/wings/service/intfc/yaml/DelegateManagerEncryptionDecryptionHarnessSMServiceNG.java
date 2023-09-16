/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.yaml;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.security.encryption.EncryptedRecordData;

import software.wings.DelegateFileEncryptedRecordDataPackage;
import software.wings.beans.DelegateFileMetadata;
import software.wings.beans.record.DecryptedRecord;

import java.io.IOException;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_INFRA_PROVISIONERS})
@OwnedBy(CDP)
public interface DelegateManagerEncryptionDecryptionHarnessSMServiceNG {
  EncryptedRecordData encryptDataNG(String accountId, byte[] content);
  DecryptedRecord decryptDataNG(String accountId, EncryptedRecordData record);
  DelegateFileEncryptedRecordDataPackage encryptDataNGWithFileUpload(
      String accountId, byte[] content, DelegateFileMetadata delegateFile) throws IOException;
}
