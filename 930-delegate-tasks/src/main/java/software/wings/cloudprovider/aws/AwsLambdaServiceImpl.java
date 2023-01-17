/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.cloudprovider.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import software.wings.cloudprovider.aws.ifc.AwsLambdaService;

@Singleton
@OwnedBy(CDP)
public class AwsLambdaServiceImpl implements AwsLambdaService {}
