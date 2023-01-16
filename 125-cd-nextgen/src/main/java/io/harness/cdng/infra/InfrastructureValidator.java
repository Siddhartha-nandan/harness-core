/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.infra;

import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.common.ParameterFieldHelper.hasValueListOrExpression;
import static io.harness.common.ParameterFieldHelper.hasValueOrExpression;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.yaml.AsgInfrastructure;
import io.harness.cdng.infra.yaml.AzureWebAppInfrastructure;
import io.harness.cdng.infra.yaml.EcsInfrastructure;
import io.harness.cdng.infra.yaml.ElastigroupInfrastructure;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8sAzureInfrastructure;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.cdng.infra.yaml.PdcInfrastructure;
import io.harness.cdng.infra.yaml.ServerlessAwsLambdaInfrastructure;
import io.harness.cdng.infra.yaml.SshWinRmAwsInfrastructure;
import io.harness.cdng.infra.yaml.SshWinRmAzureInfrastructure;
import io.harness.cdng.infra.yaml.TanzuApplicationServiceInfrastructure;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.pms.yaml.ParameterField;

import com.google.inject.Singleton;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class InfrastructureValidator {
  private static final String CANNOT_BE_EMPTY_ERROR_MSG = "cannot be empty";

  public void validate(Infrastructure infrastructure) {
    switch (infrastructure.getKind()) {
      case InfrastructureKind.KUBERNETES_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        validateK8sDirectInfrastructure(k8SDirectInfrastructure);
        break;

      case InfrastructureKind.KUBERNETES_GCP:
        K8sGcpInfrastructure k8sGcpInfrastructure = (K8sGcpInfrastructure) infrastructure;
        validateK8sGcpInfrastructure(k8sGcpInfrastructure);
        break;

      case InfrastructureKind.SERVERLESS_AWS_LAMBDA:
        validateServerlessAwsInfrastructure((ServerlessAwsLambdaInfrastructure) infrastructure);
        break;

      case InfrastructureKind.KUBERNETES_AZURE:
        K8sAzureInfrastructure k8sAzureInfrastructure = (K8sAzureInfrastructure) infrastructure;
        validateK8sAzureInfrastructure(k8sAzureInfrastructure);
        break;

      case InfrastructureKind.PDC:
        validatePdcInfrastructure((PdcInfrastructure) infrastructure);
        break;

      case InfrastructureKind.SSH_WINRM_AWS:
        validateSshWinRmAwsInfrastructure((SshWinRmAwsInfrastructure) infrastructure);
        break;

      case InfrastructureKind.SSH_WINRM_AZURE:
        validateSshWinRmAzureInfrastructure((SshWinRmAzureInfrastructure) infrastructure);
        break;

      case InfrastructureKind.AZURE_WEB_APP:
        validateAzureWebAppInfrastructure((AzureWebAppInfrastructure) infrastructure);
        break;

      case InfrastructureKind.ECS:
        validateEcsInfrastructure((EcsInfrastructure) infrastructure);
        break;

      case InfrastructureKind.ELASTIGROUP:
        validateElastigroupInfrastructure((ElastigroupInfrastructure) infrastructure);
        break;

      case InfrastructureKind.CUSTOM_DEPLOYMENT:
        break;

      case InfrastructureKind.TAS:
        validateTanzuApplicationServiceInfrastructure((TanzuApplicationServiceInfrastructure) infrastructure);
        break;

      case InfrastructureKind.ASG:
        validateAsgInfrastructure((AsgInfrastructure) infrastructure);
        break;

      default:
        throw new InvalidArgumentsException(
            String.format("Unknown Infrastructure Kind : [%s]", infrastructure.getKind()));
    }
  }

  private void validateK8sDirectInfrastructure(K8SDirectInfrastructure infrastructure) {
    if (ParameterField.isNull(infrastructure.getNamespace())
        || isEmpty(getParameterFieldValue(infrastructure.getNamespace()))) {
      throw new InvalidArgumentsException(Pair.of("namespace", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (!hasValueOrExpression(infrastructure.getReleaseName())) {
      throw new InvalidArgumentsException(Pair.of("releaseName", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }

  private void validateK8sGcpInfrastructure(K8sGcpInfrastructure infrastructure) {
    if (ParameterField.isNull(infrastructure.getNamespace())
        || isEmpty(getParameterFieldValue(infrastructure.getNamespace()))) {
      throw new InvalidArgumentsException(Pair.of("namespace", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (!hasValueOrExpression(infrastructure.getReleaseName())) {
      throw new InvalidArgumentsException(Pair.of("releaseName", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (ParameterField.isNull(infrastructure.getCluster())
        || isEmpty(getParameterFieldValue(infrastructure.getCluster()))) {
      throw new InvalidArgumentsException(Pair.of("cluster", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }

  private void validateK8sAzureInfrastructure(K8sAzureInfrastructure infrastructure) {
    if (ParameterField.isNull(infrastructure.getNamespace())
        || isEmpty(getParameterFieldValue(infrastructure.getNamespace()))) {
      throw new InvalidArgumentsException(Pair.of("namespace", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (!hasValueOrExpression(infrastructure.getReleaseName())) {
      throw new InvalidArgumentsException(Pair.of("releaseName", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (ParameterField.isNull(infrastructure.getCluster())
        || isEmpty(getParameterFieldValue(infrastructure.getCluster()))) {
      throw new InvalidArgumentsException(Pair.of("cluster", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (ParameterField.isNull(infrastructure.getSubscriptionId())
        || isEmpty(getParameterFieldValue(infrastructure.getSubscriptionId()))) {
      throw new InvalidArgumentsException(Pair.of("subscription", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (ParameterField.isNull(infrastructure.getResourceGroup())
        || isEmpty(getParameterFieldValue(infrastructure.getResourceGroup()))) {
      throw new InvalidArgumentsException(Pair.of("resourceGroup", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }

  private void validateAzureWebAppInfrastructure(AzureWebAppInfrastructure infrastructure) {
    if (ParameterField.isNull(infrastructure.getConnectorRef())
        || isEmpty(getParameterFieldValue(infrastructure.getConnectorRef()))) {
      throw new InvalidArgumentsException(Pair.of("connectorRef", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (ParameterField.isNull(infrastructure.getSubscriptionId())
        || isEmpty(getParameterFieldValue(infrastructure.getSubscriptionId()))) {
      throw new InvalidArgumentsException(Pair.of("subscription", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (ParameterField.isNull(infrastructure.getResourceGroup())
        || isEmpty(getParameterFieldValue(infrastructure.getResourceGroup()))) {
      throw new InvalidArgumentsException(Pair.of("resourceGroup", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }

  private void validatePdcInfrastructure(PdcInfrastructure infrastructure) {
    if (!hasValueOrExpression(infrastructure.getCredentialsRef())) {
      throw new InvalidArgumentsException(Pair.of("credentialsRef", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (infrastructure.isDynamicallyProvisioned()) {
      validateDynamicPdcInfrastructure(infrastructure);
    } else {
      validatePdcInfrastructure(infrastructure.getHosts(), infrastructure.getConnectorRef());
    }
  }

  private void validatePdcInfrastructure(ParameterField<List<String>> hosts, ParameterField<String> connectorRef) {
    if (!hasValueListOrExpression(hosts) && !hasValueOrExpression(connectorRef)) {
      throw new InvalidArgumentsException(Pair.of("hosts", CANNOT_BE_EMPTY_ERROR_MSG),
          Pair.of("connectorRef", CANNOT_BE_EMPTY_ERROR_MSG),
          new IllegalArgumentException("hosts and connectorRef are not defined"));
    }
  }

  private void validateDynamicPdcInfrastructure(PdcInfrastructure infrastructure) {
    if (!hasValueOrExpression(infrastructure.getHostObjectArray(), false)) {
      throw new InvalidArgumentsException(Pair.of("hostObjectArray", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (!hasValueOrExpression(infrastructure.getHostAttributes(), false)) {
      throw new InvalidArgumentsException(Pair.of("hostAttributes", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }

  private void validateServerlessAwsInfrastructure(ServerlessAwsLambdaInfrastructure infrastructure) {
    if (ParameterField.isNull(infrastructure.getRegion())
        || isEmpty(getParameterFieldValue(infrastructure.getRegion()))) {
      throw new InvalidArgumentsException(Pair.of("region", CANNOT_BE_EMPTY_ERROR_MSG));
    }
    if (!hasValueOrExpression(infrastructure.getStage())) {
      throw new InvalidArgumentsException(Pair.of("stage", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }

  private void validateSshWinRmAzureInfrastructure(SshWinRmAzureInfrastructure infrastructure) {
    if (!hasValueOrExpression(infrastructure.getConnectorRef())) {
      throw new InvalidArgumentsException(Pair.of("connectorRef", CANNOT_BE_EMPTY_ERROR_MSG));
    }
    if (!hasValueOrExpression(infrastructure.getSubscriptionId())) {
      throw new InvalidArgumentsException(Pair.of("subscriptionId", CANNOT_BE_EMPTY_ERROR_MSG));
    }
    if (!hasValueOrExpression(infrastructure.getResourceGroup())) {
      throw new InvalidArgumentsException(Pair.of("resourceGroup", CANNOT_BE_EMPTY_ERROR_MSG));
    }
    if (!hasValueOrExpression(infrastructure.getCredentialsRef())) {
      throw new InvalidArgumentsException(Pair.of("credentialsRef", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }

  private void validateSshWinRmAwsInfrastructure(SshWinRmAwsInfrastructure infrastructure) {
    if (!hasValueOrExpression(infrastructure.getCredentialsRef())) {
      throw new InvalidArgumentsException(Pair.of("credentialsRef", CANNOT_BE_EMPTY_ERROR_MSG));
    }
    if (!hasValueOrExpression(infrastructure.getConnectorRef())) {
      throw new InvalidArgumentsException(Pair.of("connectorRef", CANNOT_BE_EMPTY_ERROR_MSG));
    }
    if (!hasValueOrExpression(infrastructure.getRegion())) {
      throw new InvalidArgumentsException(Pair.of("region", CANNOT_BE_EMPTY_ERROR_MSG));
    }
    if (!hasValueOrExpression(infrastructure.getHostConnectionType())) {
      throw new InvalidArgumentsException(Pair.of("hostConnectionType", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (infrastructure.getAwsInstanceFilter() == null) {
      throw new InvalidArgumentsException(Pair.of("awsInstanceFilter", "cannot be null"));
    }
  }

  private void validateEcsInfrastructure(EcsInfrastructure infrastructure) {
    if (!hasValueOrExpression(infrastructure.getConnectorRef())) {
      throw new InvalidArgumentsException(Pair.of("connectorRef", CANNOT_BE_EMPTY_ERROR_MSG));
    }
    if (!hasValueOrExpression(infrastructure.getCluster())) {
      throw new InvalidArgumentsException(Pair.of("cluster", CANNOT_BE_EMPTY_ERROR_MSG));
    }
    if (!hasValueOrExpression(infrastructure.getRegion())) {
      throw new InvalidArgumentsException(Pair.of("region", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }

  private void validateElastigroupInfrastructure(ElastigroupInfrastructure infrastructure) {
    if (!hasValueOrExpression(infrastructure.getConnectorRef())) {
      throw new InvalidArgumentsException(Pair.of("connectorRef", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (null == infrastructure.getConfiguration()) {
      throw new InvalidArgumentsException(Pair.of("configuration", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }

  private void validateTanzuApplicationServiceInfrastructure(TanzuApplicationServiceInfrastructure infrastructure) {
    if (!hasValueOrExpression(infrastructure.getConnectorRef())) {
      throw new InvalidArgumentsException(Pair.of("connectorRef", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (!hasValueOrExpression(infrastructure.getOrganization())) {
      throw new InvalidArgumentsException(Pair.of("Organization", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (!hasValueOrExpression(infrastructure.getSpace())) {
      throw new InvalidArgumentsException(Pair.of("Space", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }

  private void validateAsgInfrastructure(AsgInfrastructure infrastructure) {
    if (!hasValueOrExpression(infrastructure.getConnectorRef())) {
      throw new InvalidArgumentsException(Pair.of("connectorRef", CANNOT_BE_EMPTY_ERROR_MSG));
    }

    if (!hasValueOrExpression(infrastructure.getRegion())) {
      throw new InvalidArgumentsException(Pair.of("region", CANNOT_BE_EMPTY_ERROR_MSG));
    }
  }
}
