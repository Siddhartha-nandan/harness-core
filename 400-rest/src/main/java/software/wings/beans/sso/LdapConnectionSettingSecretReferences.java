package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.common.beans.Generation;
import io.harness.persistence.AccountAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.Base;

@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "LdapConnectionSettingSecretReferencesKeys")
@StoreIn(DbAliases.HARNESS)
@Entity(value = "ldapConnectionSettingSecretReferences", noClassnameStored = true)
@HarnessEntity(exportable = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapConnectionSettingSecretReferences extends Base implements AccountAccess {

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
            .name("accountIdLdapSsoIdIdx")
            .field(LdapConnectionSettingSecretReferencesKeys.accountId)
            .field(LdapConnectionSettingSecretReferencesKeys.ldapSsoId)
            .unique(true)
            .build())
        .build();
  }


  @NotBlank String accountId;
  @NotBlank String ldapSsoId;

  @Getter
  @Setter
  private Map<Generation, String> connectionSettingSecretReferences = new HashMap<>();

}
