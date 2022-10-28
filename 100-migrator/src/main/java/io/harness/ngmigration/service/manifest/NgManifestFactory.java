/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CDC)
public class NgManifestFactory {
  @Inject K8sManifestRemoteStoreService k8sManifestRemoteStoreService;
  @Inject K8sManifestHelmSourceRepoStoreService k8sManifestHelmSourceRepoStoreService;
  @Inject K8sManifestHelmChartRepoStoreService k8sManifestHelmChartRepoStoreService;
  @Inject ValuesManifestRemoteStoreService valuesManifestRemoteStoreService;
  @Inject ValuesManifestLocalStoreService valuesManifestLocalStoreService;
  @Inject OpenshiftParamRemoteStoreService openshiftParamRemoteStoreService;
  @Inject OpenshiftParamLocalStoreService openshiftParamLocalStoreService;
  @Inject K8sManifestLocalStoreService k8sManifestLocalStoreService;
  @Inject KustomizeSourceRepoStoreService kustomizeSourceRepoStoreService;
  @Inject OpenshiftSourceRepoStoreService openshiftSourceRepoStoreService;

  private static String ERROR_STRING = "%s storetype is currently not supported for %s appManifestKind";

  public NgManifestService getNgManifestService(ApplicationManifest applicationManifest) {
    if (applicationManifest.getKind() == null) {
      throw new InvalidRequestException("Empty appManifest kind is not supported for migration");
    }
    AppManifestKind appManifestKind = applicationManifest.getKind();
    StoreType storeType = applicationManifest.getStoreType();

    switch (appManifestKind) {
      case K8S_MANIFEST:
        switch (storeType) {
          case Local:
            return k8sManifestLocalStoreService;
          case Remote:
            return k8sManifestRemoteStoreService;
          case HelmSourceRepo:
            return k8sManifestHelmSourceRepoStoreService;
          case HelmChartRepo:
            return k8sManifestHelmChartRepoStoreService;
          case KustomizeSourceRepo:
            return kustomizeSourceRepoStoreService;
          case OC_TEMPLATES:
            return openshiftSourceRepoStoreService;
          default:
            throw new InvalidRequestException(String.format(ERROR_STRING, storeType, appManifestKind));
        }
      case VALUES:
        switch (storeType) {
          case Remote:
            return valuesManifestRemoteStoreService;
          case Local:
            return valuesManifestLocalStoreService;
          default:
            throw new InvalidRequestException(String.format(ERROR_STRING, storeType, appManifestKind));
        }
      case OC_PARAMS:
        switch (storeType) {
          case Remote:
            return openshiftParamRemoteStoreService;
          case Local:
            return openshiftParamLocalStoreService;
          default:
            throw new InvalidRequestException(String.format(ERROR_STRING, storeType, appManifestKind));
        }
      default:
        throw new InvalidRequestException(
            String.format("%s appManifestKind is currently not supported", appManifestKind));
    }
  }
}
