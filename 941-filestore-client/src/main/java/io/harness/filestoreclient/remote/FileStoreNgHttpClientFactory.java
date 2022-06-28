package io.harness.filestoreclient.remote;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class FileStoreNgHttpClientFactory extends AbstractHttpClientFactory implements Provider<FileStoreClient> {
  public FileStoreNgHttpClientFactory(ServiceHttpClientConfig fileStoreClientConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId,
      ClientMode clientMode) {
    super(fileStoreClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId, false, clientMode);
  }

  @Override
  public FileStoreClient get() {
    return getRetrofit().create(FileStoreClient.class);
  }
}
