package io.harness.migrations.accountpermission;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.MongoUtils.setUnset;

import static software.wings.security.PermissionAttribute.PermissionType.ACCESS_NEXTGEN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute;

import com.google.inject.Inject;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(PL)
@Slf4j
public class AddAccessNextGenPermissionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  private static final String debugMessage = "AddAccessNextGenPermissionMigration: ";

  private void runMigration() {
    try (HIterator<UserGroup> userGroupHIterator = new HIterator<>(
             wingsPersistence.createQuery(UserGroup.class).field(UserGroupKeys.accountPermissions).exists().fetch())) {
      while (userGroupHIterator.hasNext()) {
        UserGroup userGroup = userGroupHIterator.next();
        try {
          Set<PermissionAttribute.PermissionType> accountPermissions =
              userGroup.getAccountPermissions().getPermissions();
          if (accountPermissions.contains(ACCESS_NEXTGEN)) {
            accountPermissions.remove(ACCESS_NEXTGEN);
          }

          UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
          setUnset(operations, UserGroupKeys.accountPermissions,
              AccountPermissions.builder().permissions(accountPermissions).build());
          wingsPersistence.update(userGroup, operations);
        } catch (Exception e) {
          log.error(debugMessage + "Error occurred while updating userGroup:[{}]", userGroup.getUuid(), e);
        }
      }
    }
  }

  @Override
  public void migrate() {
    log.info(debugMessage + "Starting Migration");
    try {
      runMigration();
    } catch (Exception e) {
      log.error(debugMessage + "Error on running migration", e);
    }
    log.info(debugMessage + "Completed Migration");
  }
}
