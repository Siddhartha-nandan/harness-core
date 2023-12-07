/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.repositories;

import io.harness.idp.plugin.entities.PluginInfoEntity;
import io.harness.idp.plugin.entities.PluginInfoEntity.PluginInfoEntityKeys;
import io.harness.spec.server.idp.v1.model.PluginInfo;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class PluginInfoRepositoryCustomImpl implements PluginInfoRepositoryCustom {
  private MongoTemplate mongoTemplate;

  @Override
  public PluginInfoEntity saveOrUpdate(PluginInfoEntity pluginInfoEntity) {
    Criteria criteria = getCriteria(pluginInfoEntity.getIdentifier(), pluginInfoEntity.getAccountIdentifier());
    PluginInfoEntity entity = findByIdentifier(criteria);
    if (entity == null) {
      return mongoTemplate.save(pluginInfoEntity);
    }
    return update(criteria, pluginInfoEntity);
  }

  @Override
  public PluginInfoEntity update(String pluginIdentifier, String accountIdentifier, PluginInfoEntity pluginInfoEntity) {
    Criteria criteria = getCriteria(pluginIdentifier, accountIdentifier);
    return update(criteria, pluginInfoEntity);
  }

  private Criteria getCriteria(String pluginIdentifier, String accountIdentifier) {
    return Criteria.where(PluginInfoEntityKeys.identifier)
        .is(pluginIdentifier)
        .and(PluginInfoEntityKeys.accountIdentifier)
        .is(accountIdentifier);
  }

  private PluginInfoEntity update(Criteria criteria, PluginInfoEntity pluginInfoEntity) {
    Query query = new Query(criteria);
    Update update = buildUpdateQuery(pluginInfoEntity);
    FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
    return mongoTemplate.findAndModify(query, update, options, PluginInfoEntity.class);
  }

  private PluginInfoEntity findByIdentifier(Criteria criteria) {
    return mongoTemplate.findOne(Query.query(criteria), PluginInfoEntity.class);
  }

  private Update buildUpdateQuery(PluginInfoEntity pluginInfoEntity) {
    Update update = new Update();
    update.set(PluginInfoEntityKeys.name, pluginInfoEntity.getName());
    if (!PluginInfo.PluginTypeEnum.CUSTOM.equals(pluginInfoEntity.getType())) {
      update.set(PluginInfoEntityKeys.accountIdentifier, pluginInfoEntity.getAccountIdentifier());
    }
    update.set(PluginInfoEntityKeys.description, pluginInfoEntity.getDescription());
    update.set(PluginInfoEntityKeys.creator, pluginInfoEntity.getCreator());
    update.set(PluginInfoEntityKeys.category, pluginInfoEntity.getCategory());
    update.set(PluginInfoEntityKeys.source, pluginInfoEntity.getSource());
    update.set(PluginInfoEntityKeys.iconUrl, pluginInfoEntity.getIconUrl());
    update.set(PluginInfoEntityKeys.documentation, pluginInfoEntity.getDocumentation());
    update.set(PluginInfoEntityKeys.exports, pluginInfoEntity.getExports());
    update.set(PluginInfoEntityKeys.config, pluginInfoEntity.getConfig());
    update.set(PluginInfoEntityKeys.envVariables, pluginInfoEntity.getEnvVariables());
    return update;
  }
}
