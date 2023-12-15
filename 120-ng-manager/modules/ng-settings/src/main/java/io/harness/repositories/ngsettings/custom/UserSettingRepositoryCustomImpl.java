package io.harness.repositories.ngsettings.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.entities.Setting;
import io.harness.ngsettings.entities.Setting.SettingKeys;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.ngsettings.entities.UserSetting.UserSettingKeys;

import com.google.inject.Inject;
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
  public UserSetting getUserSettingForAccount(String accountIdentifier, String userId, String identifier) {
    Criteria criteria = Criteria.where(SettingKeys.identifier)
                            .is(identifier)
                            .and(SettingKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(UserSettingKeys.userID)
                            .is(userId);
    Query query = new Query(criteria);
      return  mongoTemplate.findOne(query, UserSetting.class);
  }
}
