/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.ngsettings.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ngsettings.SettingConstants.TYPE_ALIAS_FOR_USER_SETTING;
import static io.harness.ngsettings.SettingConstants._CLASS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.entities.Setting.SettingKeys;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.ngsettings.entities.UserSetting.UserSettingKeys;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(PL)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))

public class UserSettingRepositoryCustomImpl implements UserSettingRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public UserSetting getUserSettingByIdentifier(String accountIdentifier, String userId, String identifier) {
    Criteria criteria = Criteria.where(SettingKeys.identifier)
                            .is(identifier)
                            .and(SettingKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(UserSettingKeys.userID)
                            .is(userId);
    criteria.and(_CLASS).is(TYPE_ALIAS_FOR_USER_SETTING);
    Query query = new Query(criteria);
    return mongoTemplate.findOne(query, UserSetting.class);
  }

  @Override
  public List<UserSetting> listUserSettingForAccount(
      String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier) {
    Criteria criteria = Criteria.where(SettingKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(UserSettingKeys.userID)
                            .is(userIdentifier);
    if (category != null) {
      criteria.and(SettingKeys.category).is(category);
    }
    if (groupIdentifier != null) {
      criteria.and(SettingKeys.groupIdentifier).is(groupIdentifier);
    }
    criteria.and(_CLASS).is(TYPE_ALIAS_FOR_USER_SETTING);
    Query query = new Query(criteria);
    return mongoTemplate.find(query, UserSetting.class);
  }
}
