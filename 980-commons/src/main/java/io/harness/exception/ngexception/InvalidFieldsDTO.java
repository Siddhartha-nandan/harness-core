/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception.ngexception;

import io.harness.exception.ngexception.metadata.ErrorMetadataConstants;
import io.harness.exception.ngexception.metadata.ErrorMetadataDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;

@Builder
@JsonTypeName(ErrorMetadataConstants.INVALID_FIELDS_ERROR)
public class InvalidFieldsDTO implements ErrorMetadataDTO {
  Map<String, String> expectedValues;
  @Override
  public String getType() {
    return ErrorMetadataConstants.INVALID_FIELDS_ERROR;
  }
}
