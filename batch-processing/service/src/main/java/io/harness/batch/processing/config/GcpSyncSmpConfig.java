package io.harness.batch.processing.config;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class GcpSyncSmpConfig {
  private String nextgenCeSecretName;
  private String batchProcessingSecretName;
  private String awsDestinationBucketKey;
  private String awsAccountIdKey;
  private String k8sJobContainerName;
  private String k8sJobName;
  private String k8sJobPythonImage;
  private String batchProcessingConfigMapName;
  private String hmacAccessKey;
  private String hmacSecretKey;
}
