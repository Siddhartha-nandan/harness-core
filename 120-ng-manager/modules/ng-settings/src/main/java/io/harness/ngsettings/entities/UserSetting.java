package io.harness.ngsettings.entities;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingValueType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.PL)
@Getter
@Setter
@FieldNameConstants(innerTypeName = "UserSettingKeys")
@StoreIn(DbAliases.NG_MANAGER)
@Entity(value = "settings", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("settings")
@Persistent
@TypeAlias("UserSetting")
@EqualsAndHashCode(callSuper = true)
public class UserSetting extends Setting {
  @NotNull String userID;
  @NotNull SettingValueType valueType;
  @NotNull String value;

  @Builder
  public UserSetting(String id, String identifier, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, SettingCategory category, String groupIdentifier, Long lastModifiedAt,
      SettingValueType valueType, String value, String userID) {
    super(
        id, identifier, accountIdentifier, orgIdentifier, projectIdentifier, category, groupIdentifier, lastModifiedAt);
    this.userID = userID;
    this.valueType = valueType;
    this.value = value;
  }
}
