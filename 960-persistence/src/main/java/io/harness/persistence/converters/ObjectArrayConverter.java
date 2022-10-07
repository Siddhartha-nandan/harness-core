/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.persistence.converters;

import io.harness.serializer.KryoSerializerWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

@Singleton
public class ObjectArrayConverter extends TypeConverter implements SimpleValueConverter {
  @Inject private KryoSerializerWrapper kryoSerializerWrapper;

  public ObjectArrayConverter() {
    super(Object[].class);
  }

  @Override
  public Object encode(Object value, MappedField optionalExtraInfo) {
    if (value == null) {
      return null;
    }
    return kryoSerializerWrapper.asBytes(value);
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
    if (fromDBObject == null) {
      return null;
    }
    return kryoSerializerWrapper.asObject((byte[]) fromDBObject);
  }
}
