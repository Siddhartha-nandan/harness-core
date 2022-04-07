package io.harness.delegate.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EligibleDelegates {
  List<String> eligibleDelegateIds;
  List<String> errors;
}
