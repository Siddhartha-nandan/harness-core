package io.harness.ng.core.migration.tasks.parentid;

import static io.harness.mongo.MongoConfig.NO_LIMIT;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;

import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import io.harness.ng.core.entities.Organization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.CloseableIterator;

@Slf4j
public class RenameParentIdtoParentUniqueIdMigration implements NGMigration {
  private static final int BATCH_SIZE = 500;
  private static final String RENAME_LOG_CONST = "[NGRenameParentIdtoParentUniqueId]:";

  private final MongoTemplate mongoTemplate;

  @Inject
  public RenameParentIdtoParentUniqueIdMigration(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }
  @Override
  public void migrate() {

    int renamedCounter = 0;
    int totalCounter = 0;
    int batchSizeCounter = 0;

    Query documentQuery = new Query(new Criteria());
    log.info(format("%s Entity Type: [%s], total count: [%s]", RENAME_LOG_CONST, "Organization",
            mongoTemplate.count(documentQuery, Organization.class)));

    BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Organization.class);

    String idValue = null;
    try (CloseableIterator<Organization> iterator =
                 mongoTemplate.stream(documentQuery.limit(NO_LIMIT).maxTimeMsec(MAX_VALUE), Organization.class)) {
      while (iterator.hasNext()) {
        totalCounter++;
        Organization nextOrg = iterator.next();
        if (null != nextOrg ) {
          idValue = nextOrg.getId();
          batchSizeCounter++;
          Update update = new Update().rename("parentId", "parentUniqueId");
          bulkOperations.updateOne(new Query(Criteria.where("_id").is(idValue)), update);
          if (batchSizeCounter == BATCH_SIZE) {
            renamedCounter += bulkOperations.execute().getModifiedCount();
            bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Organization.class);
            batchSizeCounter = 0;
          }
        }
      }
      if (batchSizeCounter > 0) { // for the last remaining batch of entities
        renamedCounter += bulkOperations.execute().getModifiedCount();
      }
    } catch (Exception exc) {
      log.error(format("%s job failed for Entity Type [%s], for entity Id: [%s]", RENAME_LOG_CONST,
                      "Organization", idValue),
              exc);
    }
    log.info(format(
            "%s Field rename migration for Entity Type: [%s]. Total documents: [%d], Successful: [%d], Failed: [%d]",
            RENAME_LOG_CONST, "Organization", totalCounter, renamedCounter,
            totalCounter - renamedCounter));
  }
}



