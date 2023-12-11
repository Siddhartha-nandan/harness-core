package io.harness.ngsettings.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeLevel;
import io.harness.licensing.Edition;
import io.harness.ng.DbAliases;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingPlanConfig;
import io.harness.ngsettings.SettingValueType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.PL)
@Getter
@Setter
@FieldNameConstants(innerTypeName = "UserSettingConfigurationKeys")
@StoreIn(DbAliases.NG_MANAGER)
@Entity(value = "settingConfigurations", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("settingConfigurations")
@Persistent
@TypeAlias("UserSettingConfiguration")
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserSettingConfiguration extends  SettingConfiguration{

    String defaultValue;
    @NotNull SettingValueType valueType;
    Set<String> allowedValues;
    @Builder
    public UserSettingConfiguration(String id, String identifier,String name, SettingCategory category,
                                       String groupIdentifier,String defaultValue, SettingValueType valueType, Set<String> allowedValues,Set<ScopeLevel> allowedScopes
            , Map<Edition, SettingPlanConfig> allowedPlans){
        super(id,identifier,name,category,groupIdentifier,allowedScopes,allowedPlans);
        this.defaultValue =defaultValue;
        this.valueType =valueType;
        this.allowedValues =allowedValues;
    }
}
