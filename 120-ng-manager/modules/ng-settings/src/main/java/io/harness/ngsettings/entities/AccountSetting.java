package io.harness.ngsettings.entities;

import com.google.inject.Inject;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingValueType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import javax.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.PL)
@Getter
@FieldNameConstants(innerTypeName = "AccountSettingKeys")
@StoreIn(DbAliases.NG_MANAGER)
@Entity(value = "settings", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("settings")
@Persistent
@TypeAlias("NGSetting")
@EqualsAndHashCode(callSuper = true)
public class AccountSetting extends Setting {
  @NotNull Boolean allowOverrides;
  @NotNull SettingValueType valueType;
  @NotNull String value;

  @Builder
  public AccountSetting(String id, String identifier, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, SettingCategory category, String groupIdentifier, Long lastModifiedAt,
      Boolean allowOverrides, SettingValueType valueType, String value) {
    super(
        id, identifier, accountIdentifier, orgIdentifier, projectIdentifier, category, groupIdentifier, lastModifiedAt);
    this.allowOverrides = allowOverrides;
    this.valueType = valueType;
    this.value = value;
  }
}
