/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.secret;

import io.harness.delegate.core.beans.SecretManagerType;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;
import org.mapstruct.factory.Mappers;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedSourcePolicy = ReportingPolicy.ERROR,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SecretManagerTypeProtoPojoMapper {
  SecretManagerTypeProtoPojoMapper INSTANCE = Mappers.getMapper(SecretManagerTypeProtoPojoMapper.class);

  @ValueMappings({
    @ValueMapping(source = "SM_NOT_SET", target = MappingConstants.NULL)
    , @ValueMapping(source = "UNKNOWN_SM", target = MappingConstants.THROW_EXCEPTION)
  })
  io.harness.security.encryption.SecretManagerType
  map(SecretManagerType type);
}
