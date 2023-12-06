/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.acl.iterators;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.harness.accesscontrol.commons.iterators.AccessControlIteratorsConfig;
import io.harness.accesscontrol.commons.iterators.IteratorConfig;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.aggregator.consumers.ACLGeneratorService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofSeconds;

@OwnedBy(PL)
@Singleton
@Slf4j
public class ACLReconciliationIterator implements MongoPersistenceIterator.Handler<RoleAssignmentDBO> {
    private final PersistenceIteratorFactory persistenceIteratorFactory;
    private final MongoTemplate mongoTemplate;
    private final IteratorConfig iteratorConfig;
    private final ACLGeneratorService aclGeneratorService;

    @Inject
    public ACLReconciliationIterator(AccessControlIteratorsConfig iteratorsConfig, PersistenceIteratorFactory persistenceIteratorFactory,
                                     @Named("mongoTemplate") MongoTemplate mongoTemplate, ACLGeneratorService aclGeneratorService) {
        this.persistenceIteratorFactory = persistenceIteratorFactory;
        this.mongoTemplate = mongoTemplate;
        this.iteratorConfig = iteratorsConfig.getAclIteratorConfig();
        this.aclGeneratorService = aclGeneratorService;
    }

    public void registerIterators() {
        Duration reconciliationInterval = Duration.ofSeconds(iteratorConfig.getTargetIntervalInSeconds());
        if (iteratorConfig.isEnabled()) {
            persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
                    PersistenceIteratorFactory.PumpExecutorOptions.builder()
                            .name("ACLReconciliationIterator")
                            .poolSize(5)
                            .interval(ofSeconds(5))
                            .build(),
                    RoleAssignmentDBO.class,
                    MongoPersistenceIterator.<RoleAssignmentDBO, SpringFilterExpander>builder()
                            .clazz(RoleAssignmentDBO.class)
                            .fieldName(RoleAssignmentDBO.RoleAssignmentDBOKeys.nextReconciliationIterationAt)
                            .targetInterval(reconciliationInterval.plus(Duration.ofMinutes(1)))
                            .acceptableNoAlertDelay(reconciliationInterval.plus(reconciliationInterval))
                            .handler(this)
                            .schedulingType(REGULAR)
                            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
                            .redistribute(true));
        }
    }

    @Override
    public void handle(RoleAssignmentDBO roleAssignmentDBO) {
        //aclGeneratorService
        // aclGeneratorService
    }
}
