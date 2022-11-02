package io.harness.event.reconciliation.deployment;

import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;

@Singleton
@Slf4j
public class DeploymentReconRecordRepository {
  @Inject HPersistence persistence;

  public DeploymentReconRecord getLatestDeploymentReconRecord(@NotNull String accountId) {
    try (HIterator<DeploymentReconRecord> iterator =
             new HIterator<>(persistence.createQuery(DeploymentReconRecord.class)
                                 .field(DeploymentReconRecord.DeploymentReconRecordKeys.accountId)
                                 .equal(accountId)
                                 .order(Sort.descending(DeploymentReconRecord.DeploymentReconRecordKeys.durationEndTs))
                                 .fetch())) {
      if (!iterator.hasNext()) {
        return null;
      }
      return iterator.next();
    }
  }
}
