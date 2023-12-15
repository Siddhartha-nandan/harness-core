package io.harness.repositories.ngsettings.custom;

import com.google.inject.Inject;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject}))

public class UserSettingConfigurationRepositoryCustomImpl implements UserSettingConfigurationRepositoryCustom{

    private final MongoTemplate mongoTemplate;

    @Override
    public List<UserSettingConfiguration> findAll(Criteria criteria) {
        Query query = new Query(criteria);
        return mongoTemplate.find(query, UserSettingConfiguration.class);
    }

}
