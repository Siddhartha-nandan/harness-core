package io.harness.delegate.beans.perpetualtask;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import io.harness.annotations.SecondaryStoreIn;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "PerpetualTaskConfigKeys")
@StoreIn(DbAliases.HARNESS)
@SecondaryStoreIn(DbAliases.DMS)
@Entity(value = "perpetualTaskConfig", noClassnameStored = true)
@OwnedBy(HarnessTeam.PL)
public class PerpetualTaskConfig {
    @Id @NotNull private String uuid;
    private String accountId;
    private String perpetualTaskType;
    private boolean disabled;
}
