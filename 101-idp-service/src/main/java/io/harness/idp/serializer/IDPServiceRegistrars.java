/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.idp.serializer;

import com.google.common.collect.ImmutableSet;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.serializer.kryo.IDPServiceKryoRegistrar;
import io.harness.idp.serializer.morphia.IDPServiceMorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.KryoRegistrar;
import lombok.experimental.UtilityClass;

import static io.harness.annotations.dev.HarnessTeam.IDP;

@UtilityClass
@OwnedBy(IDP)
public class IDPServiceRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
          ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(IDPServiceKryoRegistrar.class).build();
  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
          ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(IDPServiceMorphiaRegistrar.class).build();
}
