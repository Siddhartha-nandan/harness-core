package io.harness.delegate.task.ecs.response;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.ecs.EcsBlueGreenRollbackResult;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.logging.CommandExecutionStatus;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class EcsBlueGreenRollbackResponse implements EcsCommandResponse {
    @NonFinal
    DelegateMetaInfo delegateMetaInfo;
    @NonFinal
    UnitProgressData unitProgressData;
    CommandExecutionStatus commandExecutionStatus;
    String errorMessage;
    EcsBlueGreenRollbackResult ecsBlueGreenRollbackResult;

    @Override
    public void setDelegateMetaInfo(DelegateMetaInfo metaInfo) {
        this.delegateMetaInfo = metaInfo;
    }

    @Override
    public void setCommandUnitsProgress(UnitProgressData unitProgressData) {
        this.unitProgressData = unitProgressData;
    }
}
