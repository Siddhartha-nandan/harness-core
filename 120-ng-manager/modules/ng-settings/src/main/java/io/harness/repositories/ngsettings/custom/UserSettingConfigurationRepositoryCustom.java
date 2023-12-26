/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.ngsettings.custom;

import io.harness.ngsettings.entities.UserSettingConfiguration;

import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;

public interface UserSettingConfigurationRepositoryCustom {
  List<UserSettingConfiguration> findAll(Criteria criteria);
}
