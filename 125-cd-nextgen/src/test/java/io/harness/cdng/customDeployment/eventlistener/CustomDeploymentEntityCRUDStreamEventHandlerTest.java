/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.customDeployment.eventlistener;

import static io.harness.rule.OwnerRule.ANIL;
import static io.harness.rule.OwnerRule.SOURABH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.InfraDefReference;
import io.harness.category.element.UnitTests;
import io.harness.cdng.customdeploymentng.CustomDeploymentInfrastructureHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;
import io.harness.ng.core.entitysetupusage.mappers.EntitySetupUsageEntityToDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.ng.core.infrastructure.entity.InfrastructureEntity;
import io.harness.ng.core.infrastructure.services.InfrastructureEntityService;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class CustomDeploymentEntityCRUDStreamEventHandlerTest extends CategoryTest {
  private static final String ACCOUNT = "accIdentifier";
  private static final String ORG = "orgIdentifier";
  private static final String PROJECT = "projectIdentifier";
  private static final String INFRA = "infra";
  private static final String TEMP = "infra";
  private static final String ENV = "env";
  private static final String SECRET = "secret";
  private static final String NUMBER = "number";
  private static final String RESOURCE_PATH_PREFIX = "customdeployment/";
  private static final String INFRA_RESOURCE_PATH_PREFIX = "infrastructure/";
  private static final String TEMPLATE_RESOURCE_PATH_PREFIX = "template/";
  private List<EntitySetupUsage> entityList = new ArrayList<>();

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock EntitySetupUsageService entitySetupUsageService;
  @Mock CustomDeploymentInfrastructureHelper customDeploymentInfrastructureHelper;
  @Mock InfrastructureEntityService infrastructureEntityService;
  @Spy @InjectMocks CustomDeploymentEntityCRUDEventHandler customDeploymentEntityCRUDEventHandler;

  @Mock EntitySetupUsageEntityToDTO entitySetupUsageEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    InfraDefReference infraDefReference = InfraDefReference.builder()
                                              .identifier(INFRA)
                                              .accountIdentifier(ACCOUNT)
                                              .orgIdentifier(ORG)
                                              .projectIdentifier(PROJECT)
                                              .envIdentifier(ENV)
                                              .build();
    EntitySetupUsage entitySetupUsage =
        EntitySetupUsage.builder()
            .referredByEntity(EntityDetail.builder().entityRef(infraDefReference).build())
            .build();
    entityList.add(entitySetupUsage);
    EntitySetupUsageDTO entitySetupUsageDTO =
        EntitySetupUsageDTO.builder()
            .referredByEntity(EntityDetail.builder().entityRef(infraDefReference).build())
            .build();
    when(entitySetupUsageEntityToDTO.createEntityReferenceDTO(any())).thenReturn(entitySetupUsageDTO);
  }
  private String readFile(String filename, String folder) {
    String relativePath = RESOURCE_PATH_PREFIX + folder + filename;
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      return Resources.toString(Objects.requireNonNull(classLoader.getResource(relativePath)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not read resource file: " + filename);
    }
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testUpdateInfraAsObsolete() {
    String templateYaml = readFile("template.yaml", TEMPLATE_RESOURCE_PATH_PREFIX);

    when(customDeploymentInfrastructureHelper.checkIfInfraIsObsolete(any(), any(), any())).thenCallRealMethod();
    String infraYaml = readFile("infrastructure.yaml", INFRA_RESOURCE_PATH_PREFIX);
    InfrastructureEntity infrastructure = InfrastructureEntity.builder().accountId(ACCOUNT).yaml(infraYaml).build();
    when(infrastructureEntityService.get(any(), any(), any(), any(), any())).thenReturn(Optional.of(infrastructure));
    when(customDeploymentInfrastructureHelper.getTemplateYaml(any(), any(), any(), any(), any()))
        .thenReturn(templateYaml);
    when(entitySetupUsageService.streamAllEntityUsagePerReferredEntityScope(any(), any(), any(), any(), any()))
        .thenReturn(createStream(entityList.iterator()));
    boolean isObsolete = customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(ACCOUNT, ORG, PROJECT, TEMP, "1");
    ArgumentCaptor<ArrayList<String>> infraArgumentCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(customDeploymentEntityCRUDEventHandler, times(1))
        .updateInfras(infraArgumentCaptor.capture(), any(), any(), any());
    assertThat(infraArgumentCaptor.getAllValues().get(0).get(0)).isEqualTo(INFRA);
    assertThat(isObsolete).isEqualTo(true);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testUpdateInfraAsObsoleteNoEntityUsage() {
    String templateYaml = readFile("template.yaml", TEMPLATE_RESOURCE_PATH_PREFIX);
    when(customDeploymentInfrastructureHelper.checkIfInfraIsObsolete(any(), any(), any())).thenCallRealMethod();
    String infraYaml = readFile("infrastructure.yaml", INFRA_RESOURCE_PATH_PREFIX);
    InfrastructureEntity infrastructure = InfrastructureEntity.builder().accountId(ACCOUNT).yaml(infraYaml).build();
    when(infrastructureEntityService.get(any(), any(), any(), any(), any())).thenReturn(Optional.of(infrastructure));
    when(customDeploymentInfrastructureHelper.getTemplateYaml(any(), any(), any(), any(), any()))
        .thenReturn(templateYaml);
    when(entitySetupUsageService.streamAllEntityUsagePerReferredEntityScope(any(), any(), any(), any(), any()))
        .thenReturn(createStream(Collections.emptyIterator()));
    boolean isObsolete = customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(ACCOUNT, ORG, PROJECT, TEMP, "1");
    ArgumentCaptor<ArrayList<String>> infraArgumentCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(customDeploymentEntityCRUDEventHandler, times(0))
        .updateInfras(infraArgumentCaptor.capture(), any(), any(), any());
    assertThat(isObsolete).isEqualTo(true);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testUpdateInfraAsObsoleteInfraDoesNotExist() {
    String templateYaml = readFile("template.yaml", TEMPLATE_RESOURCE_PATH_PREFIX);
    when(customDeploymentInfrastructureHelper.checkIfInfraIsObsolete(any(), any(), any())).thenCallRealMethod();
    String infraYaml = readFile("infrastructure.yaml", INFRA_RESOURCE_PATH_PREFIX);
    InfrastructureEntity infrastructure = InfrastructureEntity.builder().accountId(ACCOUNT).yaml(infraYaml).build();

    when(infrastructureEntityService.get(eq(ACCOUNT), eq(ORG), eq(PROJECT), eq(ENV), eq(INFRA)))
        .thenReturn(Optional.empty());

    when(customDeploymentInfrastructureHelper.getTemplateYaml(any(), any(), any(), any(), any()))
        .thenReturn(templateYaml);
    when(entitySetupUsageService.streamAllEntityUsagePerReferredEntityScope(any(), any(), any(), any(), any()))
        .thenReturn(createStream(entityList.iterator()));

    boolean isObsolete = customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(ACCOUNT, ORG, PROJECT, TEMP, "1");
    ArgumentCaptor<ArrayList<String>> infraArgumentCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(customDeploymentEntityCRUDEventHandler, times(0))
        .updateInfras(infraArgumentCaptor.capture(), any(), any(), any());
    assertThat(isObsolete).isEqualTo(true);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testUpdateInfraAsObsoleteWithStableVersionAccountLevelTemplate() {
    String templateYaml = readFile("template.yaml", TEMPLATE_RESOURCE_PATH_PREFIX);
    String infraYaml = readFile("infrastructure.yaml", INFRA_RESOURCE_PATH_PREFIX);
    InfrastructureEntity infrastructure = InfrastructureEntity.builder().accountId(ACCOUNT).yaml(infraYaml).build();
    when(infrastructureEntityService.get(any(), any(), any(), any(), any())).thenReturn(Optional.of(infrastructure));
    when(customDeploymentInfrastructureHelper.getTemplateYaml(any(), any(), any(), any(), any()))
        .thenReturn(templateYaml);
    when(customDeploymentInfrastructureHelper.checkIfInfraIsObsolete(any(), any(), any())).thenCallRealMethod();
    when(entitySetupUsageService.streamAllEntityUsagePerReferredEntityScope(any(), any(), any(), any(), any()))
        .thenReturn(createStream(entityList.iterator()));
    boolean isObsolete = customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(ACCOUNT, null, null, TEMP, null);
    ArgumentCaptor<ArrayList<String>> infraArgumentCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(customDeploymentEntityCRUDEventHandler, times(1))
        .updateInfras(infraArgumentCaptor.capture(), any(), any(), any());
    assertThat(infraArgumentCaptor.getAllValues().get(0).get(0)).isEqualTo(INFRA);
    assertThat(isObsolete).isEqualTo(true);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testUpdateInfraAsObsoleteWithOrgLevelTemplate() {
    String templateYaml = readFile("template.yaml", TEMPLATE_RESOURCE_PATH_PREFIX);
    String infraYaml = readFile("infrastructure.yaml", INFRA_RESOURCE_PATH_PREFIX);
    InfrastructureEntity infrastructure = InfrastructureEntity.builder().accountId(ACCOUNT).yaml(infraYaml).build();
    when(infrastructureEntityService.get(any(), any(), any(), any(), any())).thenReturn(Optional.of(infrastructure));
    when(customDeploymentInfrastructureHelper.getTemplateYaml(any(), any(), any(), any(), any()))
        .thenReturn(templateYaml);
    when(customDeploymentInfrastructureHelper.checkIfInfraIsObsolete(any(), any(), any())).thenCallRealMethod();
    when(entitySetupUsageService.streamAllEntityUsagePerReferredEntityScope(any(), any(), any(), any(), any()))
        .thenReturn(createStream(entityList.iterator()));
    boolean isObsolete = customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(ACCOUNT, ORG, null, TEMP, null);
    ArgumentCaptor<ArrayList<String>> infraArgumentCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(customDeploymentEntityCRUDEventHandler, times(1))
        .updateInfras(infraArgumentCaptor.capture(), any(), any(), any());
    assertThat(infraArgumentCaptor.getAllValues().get(0).get(0)).isEqualTo(INFRA);
    assertThat(isObsolete).isEqualTo(true);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testUpdateInfraAsObsoleteNoUpdateRequired() {
    String templateYaml = readFile("template.yaml", TEMPLATE_RESOURCE_PATH_PREFIX);
    String infraYaml = readFile("infrastructureWithDiffVarType.yaml", INFRA_RESOURCE_PATH_PREFIX);
    InfrastructureEntity infrastructure = InfrastructureEntity.builder().accountId(ACCOUNT).yaml(infraYaml).build();
    when(infrastructureEntityService.get(any(), any(), any(), any(), any())).thenReturn(Optional.of(infrastructure));
    when(customDeploymentInfrastructureHelper.getTemplateYaml(any(), any(), any(), any(), any()))
        .thenReturn(templateYaml);
    when(entitySetupUsageService.streamAllEntityUsagePerReferredEntityScope(any(), any(), any(), any(), any()))
        .thenReturn(createStream(entityList.iterator()));
    boolean isObsolete = customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(ACCOUNT, ORG, null, TEMP, null);
    ArgumentCaptor<ArrayList<String>> infraArgumentCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(customDeploymentEntityCRUDEventHandler, times(0))
        .updateInfras(infraArgumentCaptor.capture(), any(), any(), any());
    assertThat(isObsolete).isEqualTo(true);
  }
  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testUpdateInfraAsObsoleteNoUpdateRequiredForIdentifierRefInfra() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("envID", ENV);
    String templateYaml = readFile("template.yaml", TEMPLATE_RESOURCE_PATH_PREFIX);
    String infraYaml = readFile("infrastructureWithDiffVarType.yaml", INFRA_RESOURCE_PATH_PREFIX);
    InfrastructureEntity infrastructure = InfrastructureEntity.builder().accountId(ACCOUNT).yaml(infraYaml).build();
    when(infrastructureEntityService.get(any(), any(), any(), any(), any())).thenReturn(Optional.of(infrastructure));
    when(customDeploymentInfrastructureHelper.getTemplateYaml(any(), any(), any(), any(), any()))
        .thenReturn(templateYaml);
    when(entitySetupUsageService.streamAllEntityUsagePerReferredEntityScope(any(), any(), any(), any(), any()))
        .thenReturn(createStream(entityList.iterator()));
    boolean isObsolete = customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(ACCOUNT, ORG, null, TEMP, null);
    ArgumentCaptor<ArrayList<String>> infraArgumentCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(customDeploymentEntityCRUDEventHandler, times(0))
        .updateInfras(infraArgumentCaptor.capture(), any(), any(), any());
    assertThat(isObsolete).isEqualTo(true);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testUpdateForInvalidFQNNullAccount() {
    assertThatThrownBy(() -> customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(null, ORG, null, TEMP, null))
        .hasMessage("No account identifier provided.");
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testUpdateForInvalidFQNNullOrg() {
    assertThatThrownBy(() -> customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(null, null, null, TEMP, null))
        .hasMessage("No account ID provided.");
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testUpdateForInvalidFQNNullIdentifier() {
    assertThatThrownBy(
        () -> customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(ACCOUNT, null, PROJECT, null, null))
        .hasMessage("No identifier provided.");
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testUpdateForInvalidFQNNullOrgIdentifier() {
    assertThatThrownBy(
        () -> customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(ACCOUNT, null, PROJECT, TEMP, null))
        .hasMessage("No org identifier provided.");
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testCheckIfUpdateRequiredForEmpty() {
    assertThatThrownBy(
        () -> customDeploymentEntityCRUDEventHandler.updateInfraAsObsolete(ACCOUNT, null, PROJECT, TEMP, null))
        .hasMessage("No org identifier provided.");
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
}
