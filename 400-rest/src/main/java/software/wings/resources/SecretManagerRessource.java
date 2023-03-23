package software.wings.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.InternalApi;

import software.wings.service.intfc.security.SecretManager;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/secret-manager")
@Path("/secret-manager")
@Produces("application/json")
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SecretManagerRessource {
  private final SecretManager secretManager;

  @GET
  @Path("/fetchSecret")
  @Timed
  @ExceptionMetered
  @InternalApi
  public RestResponse<String> getDelegateImageTag(@QueryParam("accountIdentifier") @NotEmpty String accountId,
      @QueryParam("secretRecordId") @NotEmpty String secretRecordId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(secretManager.fetchSecretValue(accountId, secretRecordId));
    }
  }
}
