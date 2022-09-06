package io.harness.ldap.service.impl.errors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExplanationException;
import io.harness.exception.GeneralException;
import io.harness.exception.HintException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.ldaptive.ResultCode;

@UtilityClass
@Slf4j
@OwnedBy(PL)
public class LdapErrorHandler {
  public void handleError(ResultCode resultCode, String errorMessage) throws WingsException {
    switch (resultCode) {
      case INVALID_CREDENTIALS:
        throw NestedExceptionUtils.hintWithExplanationException(HintException.CHECK_LDAP_CONNECTION,
            ExplanationException.INVALID_LDAP_CREDENTIALS, new GeneralException(errorMessage));
      case INAPPROPRIATE_MATCHING:
        throw NestedExceptionUtils.hintWithExplanationException(HintException.LDAP_ATTRIBUTES_INCORRECT,
            ExplanationException.LDAP_ATTRIBUTES_INCORRECT, new GeneralException(errorMessage));
      default:
        log.error("NGLDAP: Unhandled exception. Should not have reached here.");
        throw NestedExceptionUtils.hintWithExplanationException(HintException.HINT_UNEXPECTED_ERROR,
            ExplanationException.EXPLANATION_UNEXPECTED_ERROR, new GeneralException(errorMessage));
    }
  }
}
