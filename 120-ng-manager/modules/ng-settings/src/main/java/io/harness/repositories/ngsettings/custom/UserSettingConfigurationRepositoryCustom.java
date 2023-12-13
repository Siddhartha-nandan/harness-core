package io.harness.repositories.ngsettings.custom;


import io.harness.ngsettings.entities.UserSettingConfiguration;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

public interface UserSettingConfigurationRepositoryCustom {
    List<UserSettingConfiguration> findAll(Criteria criteria);
}
