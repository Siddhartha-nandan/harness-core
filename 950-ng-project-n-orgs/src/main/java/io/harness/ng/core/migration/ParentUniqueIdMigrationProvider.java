package io.harness.ng.core.migration;

import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;
import io.harness.ng.core.migration.schema.UniqueIdParentIdSchema;

import java.util.ArrayList;
import java.util.List;

public class ParentUniqueIdMigrationProvider implements MigrationProvider {
    @Override
    public String getServiceName() {
        return "parentUniqueIdEntities";
    }

    @Override
    public Class<? extends NGSchema> getSchemaClass() {
        return UniqueIdParentIdSchema.class;
    }

    @Override
    public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
        return new ArrayList<Class<? extends MigrationDetails>>() {
            { add(ParentUniqueIdBackgroundMigration.class); }
        };
    }
}
