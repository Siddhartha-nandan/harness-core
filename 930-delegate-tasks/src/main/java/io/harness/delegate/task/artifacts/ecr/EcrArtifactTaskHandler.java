package io.harness.delegate.task.artifacts.ecr;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.WingsException.USER;
import static io.harness.utils.FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef;

import static software.wings.helpers.ext.ecr.EcrService.MAX_NO_OF_TAGS_PER_IMAGE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorDescending;
import io.harness.artifacts.ecr.beans.EcrInternalConfig;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import io.harness.delegate.task.artifacts.mappers.EcrRequestResponseMapper;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;

import software.wings.helpers.ext.ecr.EcrService;
import software.wings.service.impl.AwsApiHelperService;
import software.wings.service.impl.delegate.AwsEcrApiHelperServiceDelegate;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@OwnedBy(CDP)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class EcrArtifactTaskHandler extends DelegateArtifactTaskHandler<EcrArtifactDelegateRequest> {
  private final EcrService ecrService;
  private final AwsApiHelperService awsApiHelperService;
  @Inject AwsEcrApiHelperServiceDelegate awsEcrApiHelperServiceDelegate;

  @Override
  public ArtifactTaskExecutionResponse getBuilds(EcrArtifactDelegateRequest attributesRequest) {
    List<BuildDetailsInternal> builds;
    AwsInternalConfig awsInternalConfig = getAwsInternalConfig(attributesRequest);
    String ecrimageUrl = awsEcrApiHelperServiceDelegate.getEcrImageUrl(
        awsInternalConfig, attributesRequest.getRegion(), attributesRequest.getImagePath());
    builds = ecrService.getBuilds(awsInternalConfig, ecrimageUrl, attributesRequest.getRegion(),
        attributesRequest.getImagePath(), MAX_NO_OF_TAGS_PER_IMAGE);
    List<EcrArtifactDelegateResponse> ecrArtifactDelegateResponseList =
        builds.stream()
            .sorted(new BuildDetailsInternalComparatorDescending())
            .map(build -> EcrRequestResponseMapper.toEcrResponse(build, attributesRequest))
            .collect(Collectors.toList());
    return getSuccessTaskExecutionResponse(ecrArtifactDelegateResponseList);
  }

  @Override
  public ArtifactTaskExecutionResponse validateArtifactServer(EcrArtifactDelegateRequest attributesRequest) {
    AwsInternalConfig awsInternalConfig = getAwsInternalConfig(attributesRequest);
    String ecrimageUrl = awsEcrApiHelperServiceDelegate.getEcrImageUrl(
        awsInternalConfig, attributesRequest.getRegion(), attributesRequest.getImagePath());
    boolean validateCredentials = ecrService.validateCredentials(
        awsInternalConfig, ecrimageUrl, attributesRequest.getRegion(), attributesRequest.getImagePath());
    return ArtifactTaskExecutionResponse.builder()
        .isArtifactServerValid(validateCredentials)
        .artifactDelegateResponse(EcrArtifactDelegateResponse.builder().imageUrl(ecrimageUrl).build())
        .build();
  }

  @Override
  public ArtifactTaskExecutionResponse validateArtifactImage(EcrArtifactDelegateRequest attributesRequest) {
    AwsInternalConfig awsInternalConfig = getAwsInternalConfig(attributesRequest);
    String ecrimageUrl = awsEcrApiHelperServiceDelegate.getEcrImageUrl(
        awsInternalConfig, attributesRequest.getRegion(), attributesRequest.getImagePath());
    boolean verifyImageName = ecrService.verifyImageName(
        awsInternalConfig, ecrimageUrl, attributesRequest.getRegion(), attributesRequest.getImagePath());
    return ArtifactTaskExecutionResponse.builder().isArtifactSourceValid(verifyImageName).build();
  }

  private ArtifactTaskExecutionResponse getSuccessTaskExecutionResponse(
      List<EcrArtifactDelegateResponse> responseList) {
    return ArtifactTaskExecutionResponse.builder()
        .artifactDelegateResponses(responseList)
        .isArtifactSourceValid(true)
        .isArtifactServerValid(true)
        .build();
  }

  @Override
  public ArtifactTaskExecutionResponse getLastSuccessfulBuild(EcrArtifactDelegateRequest attributesRequest) {
    BuildDetailsInternal lastSuccessfulBuild;
    AwsInternalConfig awsInternalConfig = getAwsInternalConfig(attributesRequest);
    String ecrimageUrl = awsEcrApiHelperServiceDelegate.getEcrImageUrl(
        awsInternalConfig, attributesRequest.getRegion(), attributesRequest.getImagePath());
    if (EmptyPredicate.isNotEmpty(attributesRequest.getTagRegex())) {
      lastSuccessfulBuild = ecrService.getLastSuccessfulBuildFromRegex(awsInternalConfig, ecrimageUrl,
          attributesRequest.getRegion(), attributesRequest.getImagePath(), attributesRequest.getTagRegex());
    } else {
      lastSuccessfulBuild = ecrService.verifyBuildNumber(awsInternalConfig, ecrimageUrl, attributesRequest.getRegion(),
          attributesRequest.getImagePath(), attributesRequest.getTag());
    }
    return getSuccessTaskExecutionResponse(
        Collections.singletonList(EcrRequestResponseMapper.toEcrResponse(lastSuccessfulBuild, attributesRequest)));
  }

  public ArtifactTaskExecutionResponse getEcrImageUrl(EcrArtifactDelegateRequest attributesRequest) {
    AwsInternalConfig awsInternalConfig = getAwsInternalConfig(attributesRequest);
    String ecrImageUrl = awsEcrApiHelperServiceDelegate.getEcrImageUrl(
        awsInternalConfig, attributesRequest.getRegion(), attributesRequest.getImagePath());

    return getSuccessTaskExecutionResponse(
        Collections.singletonList(EcrArtifactDelegateResponse.builder().imageUrl(ecrImageUrl).build()));
  }

  public ArtifactTaskExecutionResponse getAmazonEcrAuthToken(EcrArtifactDelegateRequest attributesRequest) {
    AwsInternalConfig awsInternalConfig = getAwsInternalConfig(attributesRequest);
    String ecrImageUrl = awsEcrApiHelperServiceDelegate.getEcrImageUrl(
        awsInternalConfig, attributesRequest.getRegion(), attributesRequest.getImagePath());
    String authToken = awsEcrApiHelperServiceDelegate.getAmazonEcrAuthToken(
        awsInternalConfig, ecrImageUrl.substring(0, ecrImageUrl.indexOf('.')), attributesRequest.getRegion());

    List<EcrArtifactDelegateResponse> ecrArtifactDelegateResponseList = new ArrayList<>();
    ecrArtifactDelegateResponseList.add(EcrArtifactDelegateResponse.builder().authToken(authToken).build());
    return getSuccessTaskExecutionResponse(ecrArtifactDelegateResponseList);
  }

  private EcrInternalConfig getEcrInternalConfig(EcrArtifactDelegateRequest attributesRequest) throws IOException {
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    if (attributesRequest.getAwsConnectorDTO() != null) {
      AwsCredentialDTO credential = attributesRequest.getAwsConnectorDTO().getCredential();
      AwsManualConfigSpecDTO awsManualConfigSpecDTO = (AwsManualConfigSpecDTO) credential.getConfig();
      String accessKey = getSecretAsStringFromPlainTextOrSecretRef(
          awsManualConfigSpecDTO.getAccessKey(), awsManualConfigSpecDTO.getAccessKeyRef());
      if (accessKey == null) {
        throw new InvalidArgumentsException(Pair.of("accessKey", "Missing or empty"));
      }

      awsInternalConfig =
          AwsInternalConfig.builder()
              .accessKey(accessKey.toCharArray())
              .secretKey(SecretRefHelper.getSecretConfigString(awsManualConfigSpecDTO.getSecretKeyRef()).toCharArray())
              .build();
    }
    AmazonCloudWatchClientBuilder builder =
        AmazonCloudWatchClientBuilder.standard().withRegion(attributesRequest.getRegion());
    awsApiHelperService.attachCredentialsAndBackoffPolicy(builder, awsInternalConfig);
    return EcrRequestResponseMapper.toEcrInternalConfig(attributesRequest, attributesRequest.getAwsConnectorDTO());
  }

  private AwsInternalConfig getAwsInternalConfig(EcrArtifactDelegateRequest attributesRequest) {
    EcrInternalConfig ecrInternalConfig;
    try {
      ecrInternalConfig = getEcrInternalConfig(attributesRequest);
    } catch (IOException e) {
      log.error("Could not get secret keys", e);
      throw new InvalidRequestException("Could not get secret keys - " + e.getMessage(), USER);
    }
    return EcrRequestResponseMapper.toAwsInternalConfig(ecrInternalConfig);
  }
}
