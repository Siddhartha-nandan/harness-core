package io.harness.ccm.remote.resources.policies;

import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@Singleton
public class PolicyStoreDao {
  @Inject private HPersistence hPersistence;
  public boolean save(PolicyStore policyStore) {
    return hPersistence.save(policyStore) != null;
  }

  public boolean delete(String accountId, String uuid) {
    Query<PolicyStore> query = hPersistence.createQuery(PolicyStore.class)
                                   .field(PolicyStore.PolicyId.accountId)
                                   .equal(accountId)
                                   .field(PolicyStore.PolicyId.uuid)
                                   .equal(uuid);
    return hPersistence.delete(query);
  }
  public List<PolicyStore> list(String accountId) {
    return hPersistence.createQuery(PolicyStore.class).field(PolicyStore.PolicyId.accountId).equal(accountId).asList();
  }
  public List<PolicyStore> findByResource(String resource, String accountId) {
    Query<PolicyStore> query = hPersistence.createQuery(PolicyStore.class)
                                   .filter(PolicyStore.PolicyId.resource, resource)
                                   .filter(PolicyStore.PolicyId.accountId, accountId);
    return query.asList();
  }

  public List<PolicyStore> findByTag(String tag, String accountId) {
    Query<PolicyStore> query = hPersistence.createQuery(PolicyStore.class)
                                   .filter(PolicyStore.PolicyId.tags, tag)
                                   .filter(PolicyStore.PolicyId.accountId, accountId);
    System.out.println(query);
    return query.asList();
  }

  public PolicyStore listid(String accountId, String uuid) {
    return hPersistence.createQuery(PolicyStore.class)
        .field(PolicyStore.PolicyId.accountId)
        .equal(accountId)
        .field(PolicyStore.PolicyId.uuid)
        .equal(uuid)
        .asList()
        .get(0);
  }

  public List<PolicyStore> findByTagAndResource(String resource, String tag, String accountId) {
    Query<PolicyStore> query = hPersistence.createQuery(PolicyStore.class)
                                   .filter(PolicyStore.PolicyId.resource, resource)
                                   .filter(PolicyStore.PolicyId.tags, tag)
                                   .filter(PolicyStore.PolicyId.accountId, accountId);
    System.out.println(query);

    return query.asList();
  }

  public PolicyStore update(PolicyStore policyStore) {
    Query query = hPersistence.createQuery(PolicyStore.class)
                      .field(PolicyStore.PolicyId.accountId)
                      .equal(policyStore.getAccountId())
                      .field(PolicyStore.PolicyId.uuid)
                      .equal(policyStore.getUuid());
    UpdateOperations<PolicyStore> updateOperations =
        hPersistence.createUpdateOperations(PolicyStore.class)
            .set(PolicyStore.PolicyId.resource, policyStore.getResource())
            .set(PolicyStore.PolicyId.name, policyStore.getName())
            .set(PolicyStore.PolicyId.description, policyStore.getDescription())
            .set(PolicyStore.PolicyId.policy, policyStore.getPolicy())
            .set(PolicyStore.PolicyId.tags, policyStore.getTags())
            .set(PolicyStore.PolicyId.lastUpdatedAt, policyStore.getLastUpdatedAt());

    hPersistence.update(query, updateOperations);
    System.out.println(updateOperations);
    System.out.println(query);

    return policyStore;
  }
}
