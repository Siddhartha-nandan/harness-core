/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.awscdk;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_INFRA_PROVISIONERS})
@OwnedBy(HarnessTeam.CDP)
public interface AwsCdkEnvironmentVariables {
  String PLUGIN_AWS_CDK_APP_PATH = "PLUGIN_AWS_CDK_APP_PATH";
  String PLUGIN_AWS_CDK_COMMAND_OPTIONS = "PLUGIN_AWS_CDK_COMMAND_OPTIONS";
  String PLUGIN_AWS_CDK_EXPORT_SYNTH_TEMPLATE = "PLUGIN_AWS_CDK_EXPORT_SYNTH_TEMPLATE";
  String PLUGIN_AWS_CDK_STACK_NAMES = "PLUGIN_AWS_CDK_STACK_NAMES";
  String PLUGIN_AWS_CDK_PARAMETERS = "PLUGIN_AWS_CDK_PARAMETERS";
  String PLUGIN_AWS_CDK_ACTION = "PLUGIN_AWS_CDK_ACTION";
  String BOOTSTRAP = "BOOTSTRAP";
  String DIFF = "DIFF";
  String SYNTH = "SYNTH";
  String DEPLOY = "DEPLOY";
  String DESTROY = "DESTROY";
}
