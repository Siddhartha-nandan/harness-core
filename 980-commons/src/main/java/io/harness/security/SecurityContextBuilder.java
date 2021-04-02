package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.security.PrincipalContextData.PRINCIPAL_CONTEXT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContext;
import io.harness.govern.Switch;
import io.harness.manage.GlobalContextManager;
import io.harness.security.dto.ApiKeyPrincipal;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.ServicePrincipal;
import io.harness.security.dto.UserPrincipal;

import com.auth0.jwt.interfaces.Claim;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class SecurityContextBuilder {
  public static final String PRINCIPAL_TYPE = "type";
  public static final String PRINCIPAL_NAME = "name";
  public static final String ACCOUNT_ID = "accountId";
  public static final String USER_ID = "userId";

  public Principal getPrincipalFromClaims(Map<String, Claim> claimMap) {
    Principal principal = null;
    if (claimMap.get(PRINCIPAL_TYPE) != null) {
      PrincipalType type = claimMap.get(PRINCIPAL_TYPE).as(PrincipalType.class);
      switch (type) {
        case USER:
          principal = UserPrincipal.getPrincipal(claimMap);
          break;
        case API_KEY:
          principal = ApiKeyPrincipal.getPrincipal(claimMap);
          break;
        case SERVICE:
          principal = ServicePrincipal.getPrincipal(claimMap);
          break;
        default:
          Switch.unhandled(type);
      }
    }
    return principal;
  }

  public void setContext(Map<String, Claim> claimMap) {
    Principal principal = getPrincipalFromClaims(claimMap);
    setContext(principal);
  }

  public void setContext(Principal principal) {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(PrincipalContextData.builder().principal(principal).build());
  }

  public Principal getPrincipal() {
    PrincipalContextData principalContextData = GlobalContextManager.get(PRINCIPAL_CONTEXT);
    if (principalContextData == null) {
      return null;
    }
    return principalContextData.getPrincipal();
  }

  public void unsetPrincipalContext() {
    GlobalContextManager.unset(PRINCIPAL_CONTEXT);
  }

  public void unsetCompleteContext() {
    GlobalContextManager.unset();
  }
}
