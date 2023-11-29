package io.harness.ngsettings.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.inject.Inject;
import dev.morphia.annotations.Entity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PL)
@Data
@FieldNameConstants(innerTypeName = "SettingKeys")
@StoreIn(DbAliases.NG_MANAGER)
@Entity(value = "settings", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("settings")
@Persistent
@TypeAlias("NGSetting")
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(onConstructor = @__({ @Inject}))

public class AccountSetting extends Setting{

    @NotNull Boolean allowOverrides;
    @NotNull SettingValueType valueType;
    @NotNull String value;

    @Builder
    public AccountSetting(String id, String identifier,String accountIdentifier,String orgIdentifier,String projectIdentifier
    , SettingCategory category, String groupIdentifier, Long lastModifiedAt, Boolean allowOverrides, SettingValueType valueType,String value) {
super(id, identifier, accountIdentifier, orgIdentifier, projectIdentifier, category, groupIdentifier, lastModifiedAt);
this.allowOverrides=allowOverrides;
this.valueType = valueType;
this.value =value;
    }
}
