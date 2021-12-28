package io.harness.expression;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.provision.TerraformConstants.TF_APPLY_VAR_NAME;
import static io.harness.provision.TerraformConstants.TF_DESTROY_VAR_NAME;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.terraform.TerraformPlanParam;
import io.harness.exception.FunctorException;
import io.harness.terraform.expression.TerraformPlanExpressionInterface;

import java.util.function.Function;
import lombok.Builder;

@OwnedBy(CDP)
public class TerraformPlanExpressionFunctor implements ExpressionFunctor, TerraformPlanExpressionInterface {
  private final transient String planSweepingOutputName;
  private final transient Function<String, TerraformPlanParam> obtainTfPlanFunction;
  private final transient int expressionFunctorToken;
  private transient TerraformPlanParam cachedTerraformPlanParam;

  public final TerraformPlanExpressionFunctor destroy;

  @Builder
  public TerraformPlanExpressionFunctor(
      Function<String, TerraformPlanParam> obtainTfPlanFunction, int expressionFunctorToken) {
    this.planSweepingOutputName = TF_APPLY_VAR_NAME;
    this.obtainTfPlanFunction = obtainTfPlanFunction;
    this.expressionFunctorToken = expressionFunctorToken;

    this.destroy =
        new TerraformPlanExpressionFunctor(TF_DESTROY_VAR_NAME, obtainTfPlanFunction, expressionFunctorToken);
  }

  protected TerraformPlanExpressionFunctor(String planSweepingOutputName,
      Function<String, TerraformPlanParam> obtainTfPlanFunction, int expressionFunctorToken) {
    this.planSweepingOutputName = planSweepingOutputName;
    this.obtainTfPlanFunction = obtainTfPlanFunction;
    this.expressionFunctorToken = expressionFunctorToken;
    this.destroy = null;
  }

  @Override
  public String jsonFilePath() {
    if (cachedTerraformPlanParam == null) {
      this.cachedTerraformPlanParam = findTerraformPlanParam();
    }

    return format(
        DELEGATE_EXPRESSION, cachedTerraformPlanParam.getTfPlanJsonFileId(), expressionFunctorToken, "jsonFilePath");
  }

  private TerraformPlanParam findTerraformPlanParam() {
    TerraformPlanParam terraformPlanParam = obtainTfPlanFunction.apply(this.planSweepingOutputName);
    if (terraformPlanParam == null) {
      throw new FunctorException(format("Terraform plan '%s' is not available in current context. "
              + "Terraform plan is available only after terraform step with run plan only option",
          this.planSweepingOutputName));
    }

    if (terraformPlanParam.getTfPlanJsonFileId() == null) {
      throw new FunctorException(
          format("Invalid usage of terraform plan functor. Missing tfPlanJsonFileId in terraform plan param '%s'",
              this.planSweepingOutputName));
    }

    return terraformPlanParam;
  }
}
