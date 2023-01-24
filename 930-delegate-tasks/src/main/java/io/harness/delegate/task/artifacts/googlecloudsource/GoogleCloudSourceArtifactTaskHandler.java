package io.harness.delegate.task.artifacts.googlecloudsource;

import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import io.harness.delegate.task.artifacts.mappers.GoogleCloudSourceRequestResponseMapper;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class GoogleCloudSourceArtifactTaskHandler
    extends DelegateArtifactTaskHandler<GoogleCloudSourceArtifactDelegateRequest> {
  private final SecretDecryptionService secretDecryptionService;

  @Override
  public ArtifactTaskExecutionResponse getLastSuccessfulBuild(
      GoogleCloudSourceArtifactDelegateRequest artifactDelegateRequest) {
    if (StringUtils.isBlank(artifactDelegateRequest.getProject())) {
      throw new InvalidRequestException("Please specify the project for the GCS artifact source.");
    }
    if (StringUtils.isBlank(artifactDelegateRequest.getRepository())) {
      throw new InvalidRequestException("Please specify the repository for the GCS artifact source.");
    }
    if (StringUtils.isBlank(artifactDelegateRequest.getSourceDirectory())) {
      throw new InvalidRequestException("Please specify the sourceDirectory path for the GCS artifact source.");
    }
    return ArtifactTaskExecutionResponse.builder()
        .artifactDelegateResponse(
            GoogleCloudSourceRequestResponseMapper.toGoogleCloudSourceResponse(artifactDelegateRequest))
        .build();
  }

  @Override
  public void decryptRequestDTOs(GoogleCloudSourceArtifactDelegateRequest artifactDelegateRequest) {
    if (artifactDelegateRequest.getGcpConnectorDTO() != null
        && artifactDelegateRequest.getGcpConnectorDTO().getCredential() != null
        && artifactDelegateRequest.getGcpConnectorDTO().getCredential().getConfig() != null) {
      secretDecryptionService.decrypt(artifactDelegateRequest.getGcpConnectorDTO().getCredential().getConfig(),
          artifactDelegateRequest.getEncryptedDataDetails());
      ExceptionMessageSanitizer.storeAllSecretsForSanitizing(
          artifactDelegateRequest.getGcpConnectorDTO().getCredential().getConfig(),
          artifactDelegateRequest.getEncryptedDataDetails());
    }
  }
}
