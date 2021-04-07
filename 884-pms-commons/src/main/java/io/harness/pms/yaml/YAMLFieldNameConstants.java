package io.harness.pms.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDC)
public class YAMLFieldNameConstants {
  public final String EXECUTION = "execution";
  public final String PIPELINE = "pipeline";
  public final String CI_CODE_BASE = "codebase";
  public final String PROPERTIES = "properties";
  public final String CI = "ci";
  public final String PARALLEL = "parallel";
  public final String SPEC = "spec";
  public final String STAGE = "stage";
  public final String STAGES = "stages";
  public final String STEP = "step";
  public final String STEPS = "steps";
  public final String STEP_GROUP = "stepGroup";
  public final String ROLLBACK_STEPS = "rollbackSteps";
  public final String FAILURE_STRATEGIES = "failureStrategies";
  public final String NAME = "name";
  public final String IDENTIFIER = "identifier";
  public final String DESCRIPTION = "description";
  public final String TAGS = "tags";
  public final String VARIABLES = "variables";
  public final String TYPE = "type";
  public final String KEY = "key";
  public final String VALUE = "value";
  public final String UUID = "uuid";
  public final String TIMEOUT = "timeout";
  public final String OUTPUT_VARIABLES = "outputVariables";
  public final String HEADERS = "headers";
  public final String OUTPUT = "output";
  public final String ENVIRONMENT = "environment";
  public final String PROVISIONER = "provisioner";
  public final String PIPELINE_INFRASTRUCTURE = "infrastructure";
  public final String USE_ROLLBACK_STRATEGY = "useRollbackStrategy";
  public final String PIPELINE_GROUP = "PIPELINE";
}
