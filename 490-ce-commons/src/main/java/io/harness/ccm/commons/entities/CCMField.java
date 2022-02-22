/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.entities;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Field", description = "List of all possible Fields")
public enum CCMField {
  PERSPECTIVE_ID,
  WORKLOAD,
  WORKLOAD_TYPE,
  CLUSTER_ID,
  CLUSTER_NAME,
  CLUSTER_NAMESPACE,
  CLUSTER_NAMESPACE_ID,
  CLUSTER_WORKLOAD,
  CLUSTER_WORKLOAD_ID,
  CLUSTER_NODE,
  CLUSTER_STORAGE,
  CLUSTER_APPLICATION,
  CLUSTER_ENVIRONMENT,
  CLUSTER_SERVICE,
  CLUSTER_CLOUD_PROVIDER,
  CLUSTER_ECS_SERVICE,
  CLUSTER_ECS_SERVICE_ID,
  CLUSTER_ECS_TASK,
  CLUSTER_ECS_TASK_ID,
  CLUSTER_ECS_LAUNCH_TYPE,
  CLUSTER_ECS_LAUNCH_TYPE_ID,
  NAMESPACE,
  GCP_PRODUCT,
  GCP_PROJECT,
  GCP_SKU_ID,
  GCP_SKU_DESCRIPTION,
  AWS_ACCOUNT,
  AWS_SERVICE,
  AWS_INSTANCE_TYPE,
  AWS_USAGE_TYPE,
  AZURE_SUBSCRIPTION_GUID,
  AZURE_METER_NAME,
  AZURE_METER_CATEGORY,
  AZURE_METER_SUBCATEGORY,
  AZURE_RESOURCE_ID,
  AZURE_RESOURCE_GROUP_NAME,
  AZURE_RESOURCE_TYPE,
  AZURE_RESOURCE,
  AZURE_SERVICE_NAME,
  AZURE_SERVICE_TIER,
  AZURE_INSTANCE_ID,
  AZURE_SUBSCRIPTION_NAME,
  AZURE_PUBLISHER_NAME,
  AZURE_PUBLISHER_TYPE,
  AZURE_RESERVATION_ID,
  AZURE_RESERVATION_NAME,
  AZURE_FREQUENCY,
  COMMON_PRODUCT,
  COMMON_REGION,
  COMMON_NONE,
  CLOUD_PROVIDER,
  STATUS,
  ANOMALY_TIME,
  ACTUAL_COST,
  EXPECTED_COST,
  COST_IMPACT,
  ALL
}
