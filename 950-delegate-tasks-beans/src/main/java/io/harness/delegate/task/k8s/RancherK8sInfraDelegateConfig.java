/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.beans.connector.rancher.RancherConnectorDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.taskcontext.infra.InfraContext;
import io.harness.taskcontext.infra.RancherK8sInfraContext;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@Value
@Builder
@OwnedBy(CDP)
@RecasterAlias("io.harness.delegate.task.k8s.RancherK8sInfraDelegateConfig")
@Slf4j
public class RancherK8sInfraDelegateConfig implements K8sInfraDelegateConfig {
  String namespace;
  String cluster;
  RancherConnectorDTO rancherConnectorDTO;
  List<EncryptedDataDetail> encryptionDataDetails;

  @Override
  public InfraContext toInfraContext(String delegateId) {
    return RancherK8sInfraContext.builder().delegateId(delegateId).build();
  }
}
