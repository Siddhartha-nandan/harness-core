package io.harness.pms.sdk.core.recast;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

public class EphemeralCastedField extends CastedField {
  private ParameterizedType pType;
  private Object value;
  private CastedField parent;

  public EphemeralCastedField(final ParameterizedType t, final CastedField cf, final Recaster recaster) {
    super(cf.getField(), t, recaster);
    parent = cf;
    pType = t;
    final Class rawClass = (Class) t.getRawType();
    setIsSet(RecastReflectionUtils.implementsInterface(rawClass, Set.class));
    setIsMap(RecastReflectionUtils.implementsInterface(rawClass, Map.class));
    setMapKeyType(getMapKeyClass());
    setSubType(getSubType());
  }

  public EphemeralCastedField(final Type t, final CastedField cc, final Recaster recaster) {
    super(cc.getField(), t, recaster);
    parent = cc;
  }

  @Override
  public Class getMapKeyClass() {
    return (Class) (isMap() ? pType.getActualTypeArguments()[0] : null);
  }

  @Override
  public Type getSubType() {
    return pType != null ? pType.getActualTypeArguments()[isMap() ? 1 : 0] : null;
  }

  @Override
  public Class getSubClass() {
    return toClass(getSubType());
  }
}
