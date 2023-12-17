package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.perpetualtask.PerpetualTaskConfig;
import io.harness.delegate.beans.perpetualtask.PerpetualTaskScheduleConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.perpetualtask.PerpetualTaskConfigService;
import io.harness.rest.RestResponse;

import software.wings.security.UserThreadLocal;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.annotations.Api;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api("delegate-background-job")
@Path("/delegate-background-job")
@Produces("application/json")
@Singleton
@Slf4j
@OwnedBy(PL)
public class PerpetualTaskConfigResource {
  private PerpetualTaskConfigService perpetualTaskConfigService;

  @Inject
  public PerpetualTaskConfigResource(PerpetualTaskConfigService perpetualTaskConfigService) {
    this.perpetualTaskConfigService = perpetualTaskConfigService;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> save(
      @QueryParam("accountId") String accountId, @QueryParam("perpetualTaskType") String perpetualTaskType) {
    PerpetualTaskConfig perpetualTaskConfig =
        perpetualTaskConfigService.disableAccountPerpetualTask(accountId, perpetualTaskType);
    if (perpetualTaskConfig != null) {
      return new RestResponse<>("Disabled background job for the account");
    }
    throw new InvalidRequestException("Unable to disable background jobs");
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public RestResponse<String> resetByAccountIdAndPerpetualTaskType(
      @QueryParam("accountId") String accountId, @QueryParam("perpetualTaskType") String perpetualTaskType) {
    PerpetualTaskConfig perpetualTaskConfig =
        perpetualTaskConfigService.resumeAccountPerpetualTask(accountId, perpetualTaskType);
    if (perpetualTaskConfig != null) {
      return new RestResponse<>("Enabled background job for the account");
    }
    throw new InvalidRequestException("Unable to enable background jobs");
  }
}
