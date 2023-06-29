/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.helpers.EncryptDecryptHelperImpl.ON_FILE_STORAGE;
import static io.harness.security.SimpleEncryption.CHARSET;

import static java.lang.Boolean.TRUE;

import io.harness.beans.SecretManagerConfig;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.FileMetadata;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.DelegateFileEncryptedRecordDataPackage;
import software.wings.beans.DecryptedRecord;
import software.wings.beans.DelegateFileMetadata;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.yaml.DelegateManagerEncryptionDecryptionHarnessSMServiceNG;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReaderInputStream;

@Slf4j
public class DelegateManagerEncryptionDecryptionHarnessSMServiceNGImpl
    implements DelegateManagerEncryptionDecryptionHarnessSMServiceNG {
  @Inject private KmsEncryptorsRegistry kmsEncryptorsRegistry;
  @Inject private SecretManagerClientService secretManagerClientService;
  @Inject private FileService fileService;
  public static final String HARNESS_SECRET_MANAGER_IDENTIFIER = "harnessSecretManager";

  @Override
  public EncryptedRecordData encryptDataNG(String accountId, byte[] content) {
    String value = new String(CHARSET.decode(ByteBuffer.wrap(encodeBase64ToByteArray(content))).array());
    SecretManagerConfig secretManagerConfig = SecretManagerConfigMapper.fromDTO(
        secretManagerClientService.getSecretManager(accountId, null, null, HARNESS_SECRET_MANAGER_IDENTIFIER, false));
    return (EncryptedRecordData) encryptKmsSecret(value, secretManagerConfig);
  }

  @Override
  public DelegateFileEncryptedRecordDataPackage encryptDataNGWithFileUpload(
      String accountId, byte[] content, DelegateFileMetadata delegateFile) throws IOException {
    String value = new String(CHARSET.decode(ByteBuffer.wrap(encodeBase64ToByteArray(content))).array());
    SecretManagerConfig secretManagerConfig = SecretManagerConfigMapper.fromDTO(
        secretManagerClientService.getSecretManager(accountId, null, null, HARNESS_SECRET_MANAGER_IDENTIFIER, false));
    EncryptedRecordData encryptedRecordData = (EncryptedRecordData) encryptKmsSecret(value, secretManagerConfig);

    String fileId = uploadEncryptedValueToFileStorage(encryptedRecordData, delegateFile);

    encryptedRecordData.setEncryptedValue(fileId.toCharArray());
    encryptedRecordData.setAdditionalMetadata(AdditionalMetadata.builder().value(ON_FILE_STORAGE, TRUE).build());

    return DelegateFileEncryptedRecordDataPackage.builder()
        .encryptedRecordData(encryptedRecordData)
        .delegateFileId(fileId)
        .build();
  }

  @Override
  public DecryptedRecord decryptDataNG(String accountId, EncryptedRecordData encryptedRecord) {
    SecretManagerConfig secretManagerConfig = SecretManagerConfigMapper.fromDTO(
        secretManagerClientService.getSecretManager(accountId, null, null, HARNESS_SECRET_MANAGER_IDENTIFIER, false));
    return DecryptedRecord.builder().decryptedValue(decryptSecretValue(encryptedRecord, secretManagerConfig)).build();
  }

  private EncryptedRecord encryptKmsSecret(String value, EncryptionConfig encryptionConfig) {
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(encryptionConfig);
    return kmsEncryptor.encryptSecret(encryptionConfig.getAccountId(), value, encryptionConfig);
  }

  private char[] decryptSecretValue(EncryptedRecordData encryptedRecord, EncryptionConfig config) {
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(config);
    return kmsEncryptor.fetchSecretValue(config.getAccountId(), encryptedRecord, config);
  }

  private String uploadEncryptedValueToFileStorage(
      EncryptedRecordData encryptedRecordData, DelegateFileMetadata delegateFile) throws IOException {
    CharArrayReader charArrayReader = new CharArrayReader(encryptedRecordData.getEncryptedValue());
    try (InputStream inputStream = new ReaderInputStream(charArrayReader, Charsets.UTF_8)) {
      return upload(delegateFile, inputStream);
    }
  }

  private String upload(DelegateFileMetadata delegateFile, InputStream inputStream) {
    FileMetadata fileMetadata = FileMetadata.builder()
                                    .fileName(delegateFile.getFileName())
                                    .accountId(delegateFile.getAccountId())
                                    .fileUuid(UUIDGenerator.generateUuid())
                                    .build();
    String fileId = fileService.saveFile(fileMetadata, inputStream, delegateFile.getBucket());
    log.info("fileId: {} and fileName {}", fileId, fileMetadata.getFileName());
    return fileId;
  }
}
