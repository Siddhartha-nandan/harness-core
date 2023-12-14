package io.harness.delegate.beans.perpetualtask;


import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import io.harness.annotations.SecondaryStoreIn;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@FieldNameConstants(innerTypeName = "PerpetualTaskConfigKeys")
@StoreIn(DbAliases.HARNESS)
@SecondaryStoreIn(DbAliases.DMS)
@Entity(value = "perpetualTaskConfig", noClassnameStored = true)
@OwnedBy(HarnessTeam.PL)
public class PerpetualTaskConfig {
    public static List<MongoIndex> mongoIndexes() {
        return ImmutableList.<MongoIndex>builder()
                .add(CompoundMongoIndex.builder()
                        .unique(true)
                        .name("unique_perpetualTaskScheduleConfig_index1")
                        .field(PerpetualTaskConfigKeys.accountId)
                        .field(PerpetualTaskConfigKeys.perpetualTaskType)
                        .build())
                .build();
    }
    @Id @NotNull private String uuid;
    private String accountId;
    private String perpetualTaskType;
}
