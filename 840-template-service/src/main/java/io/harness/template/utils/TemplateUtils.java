/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.utils;

import io.harness.beans.Scope;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.template.entity.TemplateEntity;

import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
public class TemplateUtils {
  public Scope buildScope(TemplateEntity templateEntity) {
    return Scope.of(templateEntity.getAccountIdentifier(), templateEntity.getOrgIdentifier(),
        templateEntity.getProjectIdentifier());
  }

  public boolean isInlineEntity(GitEntityInfo gitEntityInfo) {
    return StoreType.INLINE.equals(gitEntityInfo.getStoreType());
  }

  public boolean isRemoteEntity(GitEntityInfo gitEntityInfo) {
    return StoreType.REMOTE.equals(gitEntityInfo.getStoreType());
  }

  public Update getUpdateOperations(TemplateEntity templateEntity, long timestamp) {
    Update update = new Update();
    update.set(TemplateEntity.TemplateEntityKeys.yaml, templateEntity.getYaml());
    update.set(TemplateEntity.TemplateEntityKeys.lastUpdatedAt, timestamp);
    update.set(TemplateEntity.TemplateEntityKeys.deleted, false);
    update.set(TemplateEntity.TemplateEntityKeys.name, templateEntity.getName());
    update.set(TemplateEntity.TemplateEntityKeys.description, templateEntity.getDescription());
    update.set(TemplateEntity.TemplateEntityKeys.tags, templateEntity.getTags());
    return update;
  }

  public TemplateEntity updateFieldsInDBEntry(
      TemplateEntity entityFromDB, TemplateEntity fieldsToUpdate, long timeOfUpdate) {
    return entityFromDB.withYaml(fieldsToUpdate.getYaml())
        .withName(fieldsToUpdate.getName())
        .withLastUpdatedAt(timeOfUpdate)
        .withDescription(fieldsToUpdate.getDescription())
        .withTags(fieldsToUpdate.getTags());
  }

  public Update getUpdateOperationsForOnboardingToInline() {
    Update update = new Update();
    update.set(TemplateEntity.TemplateEntityKeys.storeType, StoreType.INLINE);
    return update;
  }
}
