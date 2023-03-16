/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.configmanager.repositories;

import io.harness.idp.configmanager.beans.entity.AppConfigEntity;
import io.harness.idp.configmanager.beans.entity.AppConfigEntity.AppConfigEntityKeys;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class AppConfigRepositoryCustomImpl implements AppConfigRepositoryCustom {
  private MongoTemplate mongoTemplate;

  @Override
  public AppConfigEntity updateConfig(AppConfigEntity appConfigEntity) {
    Criteria criteria = getCriteriaForPlugin(appConfigEntity.getAccountIdentifier(), appConfigEntity.getPluginId());
    Query query = new Query(criteria);
    Update update = new Update();
    update.set(AppConfigEntityKeys.configs, appConfigEntity.getConfigs());
    update.set(AppConfigEntityKeys.lastModifiedAt, System.currentTimeMillis());
    FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
    return mongoTemplate.findAndModify(query, update, options, AppConfigEntity.class);
  }

  @Override
  public AppConfigEntity updatePluginEnablement(String accountIdentifier, String pluginId, Boolean enabled) {
    Criteria criteria = getCriteriaForPlugin(accountIdentifier, pluginId);
    Query query = new Query(criteria);
    Update update = new Update();
    update.set(AppConfigEntityKeys.enabled, enabled);
    update.set(AppConfigEntityKeys.enabledDisabledAt, System.currentTimeMillis());
    FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
    return mongoTemplate.findAndModify(query, update, options, AppConfigEntity.class);
  }

  private Criteria getCriteriaForPlugin(String accountIdentifier, String pluginId) {
    return Criteria.where(AppConfigEntityKeys.accountIdentifier)
        .is(accountIdentifier)
        .and(AppConfigEntityKeys.pluginId)
        .is(pluginId);
  }
}
