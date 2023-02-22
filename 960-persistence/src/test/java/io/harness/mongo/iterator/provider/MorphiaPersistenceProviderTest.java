/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.mongo.iterator.provider;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PersistenceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.iterator.TestIterableEntity;
import io.harness.iterator.TestIterableEntity.TestIterableEntityKeys;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import dev.morphia.query.Query;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MorphiaPersistenceProviderTest extends PersistenceTestBase {
  @Inject private MorphiaPersistenceProvider<TestIterableEntity> persistenceProvider;

  @SuppressWarnings("checkstyle:RepetitiveName")
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCreateQueryWithNoFilter() {
    final Query<TestIterableEntity> query = persistenceProvider.createQuery(
        TestIterableEntity.class, TestIterableEntityKeys.nextIterations, null, false, false);

    assertThat(query.toString().replace(" ", "")).isEqualTo("{{}}");
    assertThat(persistenceProvider
                   .createQuery(5, TestIterableEntity.class, TestIterableEntityKeys.nextIterations, null, false, false)
                   .toString()
                   .replace(" ", ""))
        .isEqualTo("{{\"$or\":[{\"nextIterations\":{\"$lt\":5}},{\"nextIterations\":{\"$exists\":false}}]}}");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCreateQueryWithSimpleFilter() {
    MorphiaFilterExpander<TestIterableEntity> filterExpander =
        query -> query.filter(TestIterableEntityKeys.name, "foo");

    final Query<TestIterableEntity> query = persistenceProvider.createQuery(
        TestIterableEntity.class, TestIterableEntityKeys.nextIterations, filterExpander, false, false);

    assertThat(query.toString().replace(" ", "")).isEqualTo("{{\"name\":\"foo\"}}");
    assertThat(persistenceProvider
                   .createQuery(
                       5, TestIterableEntity.class, TestIterableEntityKeys.nextIterations, filterExpander, false, false)
                   .toString()
                   .replace(" ", ""))
        .isEqualTo(
            "{{\"name\":\"foo\",\"$or\":[{\"nextIterations\":{\"$lt\":5}},{\"nextIterations\":{\"$exists\":false}}]}}");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCreateQueryWithAndFilter() {
    MorphiaFilterExpander<TestIterableEntity> filterExpander = query
        -> query.and(query.criteria(TestIterableEntityKeys.name).exists(),
            query.criteria(TestIterableEntityKeys.name).equal("foo"));

    final Query<TestIterableEntity> query = persistenceProvider.createQuery(
        TestIterableEntity.class, TestIterableEntityKeys.nextIterations, filterExpander, false, false);

    assertThat(query.toString().replace(" ", ""))
        .isEqualTo("{{\"$and\":[{\"name\":{\"$exists\":true}},{\"name\":\"foo\"}]}}");

    final String stringQuery = persistenceProvider
                                   .createQuery(5, TestIterableEntity.class, TestIterableEntityKeys.nextIterations,
                                       filterExpander, false, false)
                                   .toString()
                                   .replace(" ", "");

    assertThat(stringQuery)
        .isEqualTo(
            "{{\"$and\":[{\"name\":{\"$exists\":true}},{\"name\":\"foo\"}],\"$or\":[{\"nextIterations\":{\"$lt\":5}},{\"nextIterations\":{\"$exists\":false}}]}}");
  }
}
