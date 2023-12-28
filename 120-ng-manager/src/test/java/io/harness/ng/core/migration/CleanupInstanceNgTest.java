/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.migration;

import static io.harness.mongo.MongoConfig.NO_LIMIT;
import static io.harness.rule.OwnerRule.BUHA;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.NgManagerTestBase;
import io.harness.account.utils.AccountUtils;
import io.harness.category.element.UnitTests;
import io.harness.entities.Instance;
import io.harness.entities.Instance.InstanceKeys;
import io.harness.ng.core.migration.background.CleanupInstanceNg;
import io.harness.repositories.instance.InstanceRepository;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class CleanupInstanceNgTest extends NgManagerTestBase {
  @Mock private AccountUtils accountUtils;
  @Mock private MongoTemplate mongoTemplate;
  @Mock private InstanceRepository repository;
  @InjectMocks CleanupInstanceNg cleanupInstanceNg;

  String accountId1 = "accountId1";
  String accountId2 = "accountId2";
  String accountId3 = "accountId3";
  String deletedAccount1 = "deletedAccount1";
  String deletedAccount2 = "deletedAccount2";
  ArrayList<String> accountIds = new ArrayList<>(Arrays.asList(accountId1, accountId2, accountId3));

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCleanupMigration() {
    when(accountUtils.getAllNGAccountIds()).thenReturn(accountIds);
    when(mongoTemplate.stream(getQuery(), Instance.class))
        .thenReturn(
            createStream(new ArrayList<>(Arrays.asList(Instance.builder().id("1").accountIdentifier(accountId1).build(),
                                             Instance.builder().id("2").accountIdentifier(accountId2).build(),
                                             Instance.builder().id("3").accountIdentifier(accountId3).build(),
                                             Instance.builder().id("4").accountIdentifier(deletedAccount1).build(),
                                             Instance.builder().id("5").accountIdentifier(deletedAccount2).build()))
                             .iterator()));

    cleanupInstanceNg.migrate();

    verify(repository, times(2)).deleteById(anyString());
    verify(repository, times(1)).deleteById("4");
    verify(repository, times(1)).deleteById("5");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testNoEntryEligibleForDeletion() {
    when(accountUtils.getAllNGAccountIds()).thenReturn(accountIds);
    when(mongoTemplate.stream(getQuery(), Instance.class))
        .thenReturn(
            createStream(new ArrayList<>(Arrays.asList(Instance.builder().id("1").accountIdentifier(accountId1).build(),
                                             Instance.builder().id("2").accountIdentifier(accountId2).build(),
                                             Instance.builder().id("3").accountIdentifier(accountId3).build(),
                                             Instance.builder().id("4").accountIdentifier(accountId1).build(),
                                             Instance.builder().id("5").accountIdentifier(accountId2).build()))
                             .iterator()));

    cleanupInstanceNg.migrate();

    verifyNoInteractions(repository);
  }

  private <T> Stream<T> createStream(Iterator<T> iterator) {
    return new Stream<T>() {
      @NotNull
      @Override
      public Iterator<T> iterator() {
        return iterator;
      }

      @NotNull
      @Override
      public Spliterator<T> spliterator() {
        return null;
      }

      @Override
      public boolean isParallel() {
        return false;
      }

      @NotNull
      @Override
      public Stream<T> sequential() {
        return null;
      }

      @NotNull
      @Override
      public Stream<T> parallel() {
        return null;
      }

      @NotNull
      @Override
      public Stream<T> unordered() {
        return null;
      }

      @NotNull
      @Override
      public Stream<T> onClose(Runnable closeHandler) {
        return null;
      }

      @Override
      public void close() {}

      @Override
      public Stream<T> filter(Predicate<? super T> predicate) {
        return null;
      }

      @Override
      public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return null;
      }

      @Override
      public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return null;
      }

      @Override
      public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return null;
      }

      @Override
      public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return null;
      }

      @Override
      public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return null;
      }

      @Override
      public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return null;
      }

      @Override
      public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return null;
      }

      @Override
      public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return null;
      }

      @Override
      public Stream<T> distinct() {
        return null;
      }

      @Override
      public Stream<T> sorted() {
        return null;
      }

      @Override
      public Stream<T> sorted(Comparator<? super T> comparator) {
        return null;
      }

      @Override
      public Stream<T> peek(Consumer<? super T> action) {
        return null;
      }

      @Override
      public Stream<T> limit(long maxSize) {
        return null;
      }

      @Override
      public Stream<T> skip(long n) {
        return null;
      }

      @Override
      public void forEach(Consumer<? super T> action) {}

      @Override
      public void forEachOrdered(Consumer<? super T> action) {}

      @NotNull
      @Override
      public Object[] toArray() {
        return new Object[0];
      }

      @NotNull
      @Override
      public <A> A[] toArray(IntFunction<A[]> generator) {
        return null;
      }

      @Override
      public T reduce(T identity, BinaryOperator<T> accumulator) {
        return null;
      }

      @NotNull
      @Override
      public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return Optional.empty();
      }

      @Override
      public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return null;
      }

      @Override
      public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return null;
      }

      @Override
      public <R, A> R collect(Collector<? super T, A, R> collector) {
        return null;
      }

      @NotNull
      @Override
      public Optional<T> min(Comparator<? super T> comparator) {
        return Optional.empty();
      }

      @NotNull
      @Override
      public Optional<T> max(Comparator<? super T> comparator) {
        return Optional.empty();
      }

      @Override
      public long count() {
        return 0;
      }

      @Override
      public boolean anyMatch(Predicate<? super T> predicate) {
        return false;
      }

      @Override
      public boolean allMatch(Predicate<? super T> predicate) {
        return false;
      }

      @Override
      public boolean noneMatch(Predicate<? super T> predicate) {
        return false;
      }

      @NotNull
      @Override
      public Optional<T> findFirst() {
        return Optional.empty();
      }

      @NotNull
      @Override
      public Optional<T> findAny() {
        return Optional.empty();
      }
    };
  }

  private Query getQuery() {
    Query query = new Query(new Criteria()).limit(NO_LIMIT).cursorBatchSize(10000);
    query.fields().include("_id", InstanceKeys.accountIdentifier, InstanceKeys.instanceType,
        InstanceKeys.lastDeployedAt, InstanceKeys.isDeleted, InstanceKeys.deletedAt);
    return query;
  }
}