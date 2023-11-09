/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng;

import static io.harness.authorization.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.cache.CacheBackend.CAFFEINE;
import static io.harness.cache.CacheBackend.NOOP;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;

import static io.serializer.HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;
import static org.mockito.Mockito.mock;

import io.harness.ModuleType;
import io.harness.PluginConfiguration;
import io.harness.PluginModule;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.NoOpAccessControlClientImpl;
import io.harness.account.AccountClient;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cache.CacheConfig;
import io.harness.cache.CacheConfig.CacheConfigBuilder;
import io.harness.cache.CacheModule;
import io.harness.callback.DelegateCallbackToken;
import io.harness.cdng.orchestration.NgStepRegistrar;
import io.harness.cdng.plugininfoproviders.PluginExecutionConfig;
import io.harness.cdng.service.steps.helpers.serviceoverridesv2.services.ServiceOverrideV2MigrationService;
import io.harness.cdng.service.steps.helpers.serviceoverridesv2.services.ServiceOverrideV2MigrationServiceImpl;
import io.harness.cdng.service.steps.helpers.serviceoverridesv2.services.ServiceOverrideV2SettingsUpdateService;
import io.harness.cdng.service.steps.helpers.serviceoverridesv2.services.ServiceOverrideV2SettingsUpdateServiceImpl;
import io.harness.cdng.service.steps.helpers.serviceoverridesv2.services.ServiceOverridesServiceV2Impl;
import io.harness.cdng.service.steps.helpers.serviceoverridesv2.validators.ServiceOverrideValidatorService;
import io.harness.cdng.service.steps.helpers.serviceoverridesv2.validators.ServiceOverrideValidatorServiceImpl;
import io.harness.cf.AbstractCfModule;
import io.harness.cf.CfClientConfig;
import io.harness.cf.CfMigrationConfig;
import io.harness.connector.ConnectorResourceClientModule;
import io.harness.connector.services.ConnectorService;
import io.harness.delay.DelayEventListener;
import io.harness.delegate.DelegateServiceGrpc;
import io.harness.enforcement.client.services.EnforcementClientService;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.factory.ClosingFactory;
import io.harness.ff.FeatureFlagConfig;
import io.harness.ff.FeatureFlagService;
import io.harness.ff.FeatureFlagServiceImpl;
import io.harness.filestore.service.FileStoreService;
import io.harness.gitsync.persistance.testing.GitSyncablePersistenceTestModule;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.licensing.services.DefaultLicenseServiceImpl;
import io.harness.licensing.services.LicenseService;
import io.harness.lock.DistributedLockImplementation;
import io.harness.lock.PersistentLockModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.api.NGEncryptedDataService;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.entitysetupusage.EntitySetupUsageModule;
import io.harness.ng.core.serviceoverride.services.ServiceOverrideService;
import io.harness.ng.core.serviceoverride.services.impl.ServiceOverrideServiceImpl;
import io.harness.ng.core.serviceoverridev2.service.ServiceOverridesServiceV2;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ngsettings.client.remote.NGSettingsClient;
import io.harness.outbox.api.OutboxService;
import io.harness.outbox.api.impl.OutboxDaoImpl;
import io.harness.outbox.api.impl.OutboxServiceImpl;
import io.harness.persistence.HPersistence;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.expression.NoopEngineExpressionServiceImpl;
import io.harness.pms.sdk.PmsSdkConfiguration;
import io.harness.pms.sdk.PmsSdkModule;
import io.harness.pms.sdk.core.SdkDeployMode;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListenerController;
import io.harness.redis.RedisConfig;
import io.harness.registrars.CDServiceAdviserRegistrar;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.repositories.outbox.OutboxEventRepository;
import io.harness.rule.Cache;
import io.harness.rule.InjectorRuleMixin;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.secrets.services.PrivilegedSecretNGManagerClientServiceImpl;
import io.harness.serializer.CDNGRegistrars;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.springdata.HTransactionTemplate;
import io.harness.template.remote.TemplateResourceClient;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.timescaledb.TimeScaleDBService;
import io.harness.timescaledb.TimeScaleDBServiceImpl;
import io.harness.user.remote.UserClient;
import io.harness.waiter.AbstractWaiterModule;
import io.harness.waiter.WaiterConfiguration;
import io.harness.yaml.YamlSdkModule;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.schema.client.YamlSchemaClientModule;
import io.harness.yaml.schema.client.config.YamlSchemaClientConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import dev.morphia.converters.TypeConverter;
import io.dropwizard.jackson.Jackson;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.serializer.HObjectMapper;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class CDNGTestRule implements InjectorRuleMixin, MethodRule, MongoRuleMixin {
  ClosingFactory closingFactory;

  public CDNGTestRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(KryoModule.getInstance());
    modules.add(YamlSdkModule.getInstance());
    modules.add(new GitSyncablePersistenceTestModule());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(CDNGRegistrars.kryoRegistrars).build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(CDNGRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(ManagerRegistrars.morphiaConverters)
            .build();
      }

      @Provides
      @Singleton
      List<Class<? extends Converter<?, ?>>> springConverters() {
        return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
            .addAll(ManagerRegistrars.springConverters)
            .build();
      }

      @Provides
      @Singleton
      List<YamlSchemaRootClass> yamlSchemaRootClass() {
        return ImmutableList.<YamlSchemaRootClass>builder().addAll(CDNGRegistrars.yamlSchemaRegistrars).build();
      }

      @Provides
      @Named(OUTBOX_TRANSACTION_TEMPLATE)
      @Singleton
      TransactionTemplate getTransactionTemplate(MongoTransactionManager mongoTransactionManager) {
        return new HTransactionTemplate(mongoTransactionManager, false);
      }

      @Provides
      @Named("yaml-schema-mapper")
      @Singleton
      public ObjectMapper getYamlSchemaObjectMapper() {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        HObjectMapper.configureObjectMapperForNG(objectMapper);
        return objectMapper;
      }

      @Provides
      @Singleton
      OutboxService getOutboxService(OutboxEventRepository outboxEventRepository) {
        return new OutboxServiceImpl(new OutboxDaoImpl(outboxEventRepository), NG_DEFAULT_OBJECT_MAPPER);
      }

      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return MongoConfig.builder().build();
      }

      @Provides
      @Named("yaml-schema-subtypes")
      @Singleton
      public Map<Class<?>, Set<Class<?>>> yamlSchemaSubtypes() {
        return Mockito.mock(Map.class);
      }

      @Provides
      @Named("disableDeserialization")
      @Singleton
      public boolean getSerializationForDelegate() {
        return false;
      }

      @Provides
      @Named("TimeScaleDBConfig")
      @Singleton
      public TimeScaleDBConfig getTimeScaleDBConfig() {
        return TimeScaleDBConfig.builder().build();
      }

      @Provides
      @Singleton
      TemplateResourceClient getTemplateResourceClient() {
        return mock(TemplateResourceClient.class);
      }
    });
    modules.add(new AbstractModule() {
      @SneakyThrows
      @Override
      protected void configure() {
        bind(TimeScaleDBService.class)
            .toConstructor(TimeScaleDBServiceImpl.class.getConstructor(TimeScaleDBConfig.class));
        bind(HPersistence.class).to(MongoPersistence.class);
        bind(ConnectorService.class)
            .annotatedWith(Names.named(DEFAULT_CONNECTOR_SERVICE))
            .toInstance(Mockito.mock(ConnectorService.class));
        bind(SecretManagerClientService.class).toInstance(mock(SecretManagerClientService.class));
        bind(SecretManagerClientService.class)
            .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
            .toInstance(mock(PrivilegedSecretNGManagerClientServiceImpl.class));
        bind(DSLContext.class).toInstance(mock(DSLContext.class));
        bind(DelegateServiceGrpcClient.class).toInstance(mock(DelegateServiceGrpcClient.class));
        bind(DelegateSyncService.class).toInstance(mock(DelegateSyncService.class));
        bind(DelegateAsyncService.class).toInstance(mock(DelegateAsyncService.class));
        bind(UserClient.class).toInstance(mock(UserClient.class));
        bind(EntitySetupUsageClient.class).toInstance(mock(EntitySetupUsageClient.class));
        bind(EnforcementClientService.class).toInstance(mock(EnforcementClientService.class));
        bind(AccountClient.class).toInstance(mock(AccountClient.class));
        bind(AccountClient.class).annotatedWith(Names.named("PRIVILEGED")).toInstance(mock(AccountClient.class));
        bind(EngineExpressionService.class).toInstance(mock(NoopEngineExpressionServiceImpl.class));
        bind(new TypeLiteral<Supplier<DelegateCallbackToken>>() {
        }).toInstance(Suppliers.ofInstance(DelegateCallbackToken.newBuilder().build()));
        bind(new TypeLiteral<DelegateServiceGrpc.DelegateServiceBlockingStub>() {
        }).toInstance(DelegateServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName(generateUuid()).build()));
        bind(Producer.class)
            .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
            .toInstance(mock(NoOpProducer.class));
        bind(Producer.class)
            .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
            .toInstance(mock(NoOpProducer.class));
        bind(Producer.class)
            .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
            .toInstance(mock(NoOpProducer.class));
        bind(SecretNGManagerClient.class)
            .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
            .toInstance(mock(SecretNGManagerClient.class));
        bind(FileStoreService.class).toInstance(mock(FileStoreService.class));
        bind(NGEncryptedDataService.class).toInstance(mock(NGEncryptedDataService.class));
        bind(NGSettingsClient.class).toInstance(mock(NGSettingsClient.class));
        bind(ServiceOverridesServiceV2.class).to(ServiceOverridesServiceV2Impl.class);
        bind(ServiceOverrideService.class).to(ServiceOverrideServiceImpl.class);
        bind(ServiceOverrideV2MigrationService.class).to(ServiceOverrideV2MigrationServiceImpl.class);
        bind(ServiceOverrideV2SettingsUpdateService.class).to(ServiceOverrideV2SettingsUpdateServiceImpl.class);
        bind(ServiceOverrideValidatorService.class).to(ServiceOverrideValidatorServiceImpl.class);
        bind(AccessControlClient.class).to(NoOpAccessControlClientImpl.class).in(Scopes.SINGLETON);
        bind(OrganizationService.class).toInstance(mock(OrganizationService.class));
        bind(ProjectService.class).toInstance(mock(ProjectService.class));
        bind(FeatureFlagService.class).toInstance(mock(FeatureFlagServiceImpl.class));
        bind(CDStepHelper.class).toProvider(() -> mock(CDStepHelper.class)).asEagerSingleton();
        bind(LicenseService.class).toInstance(mock(DefaultLicenseServiceImpl.class));
        bind(AccessControlClient.class)
            .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
            .to(NoOpAccessControlClientImpl.class);
        bind(ExecutorService.class)
            .annotatedWith(Names.named("service-gitx-executor"))
            .toInstance(mock(ExecutorService.class));
        bind(ExecutorService.class)
            .annotatedWith(Names.named("environment-gitx-executor"))
            .toInstance(mock(ExecutorService.class));
        bind(SecretCrudService.class).toProvider(() -> mock(SecretCrudService.class)).asEagerSingleton();
        bind(ExecutorService.class)
            .annotatedWith(Names.named("deployment-stage-plan-creation-info-executor"))
            .toInstance(mock(ExecutorService.class));
      }
    });

    modules.add(new AbstractCfModule() {
      @Override
      public CfClientConfig cfClientConfig() {
        return CfClientConfig.builder().build();
      }

      @Override
      public CfMigrationConfig cfMigrationConfig() {
        return CfMigrationConfig.builder().build();
      }

      @Override
      public FeatureFlagConfig featureFlagConfig() {
        return FeatureFlagConfig.builder().build();
      }
    });

    modules.add(TimeModule.getInstance());
    modules.add(NGModule.getInstance());
    modules.add(
        PluginModule.getInstance(PluginConfiguration.builder()
                                     .pluginExecutionConfig(PluginExecutionConfig.builder().apiUrl("apiUrl").build())
                                     .build()));

    modules.add(new ConnectorResourceClientModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:3457/").build(), "test_secret", "CI"));

    modules.add(TestMongoModule.getInstance());
    modules.add(mongoTypeModule(annotations));
    modules.add(new EntitySetupUsageModule());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      DistributedLockImplementation distributedLockImplementation() {
        return DistributedLockImplementation.NOOP;
      }

      @Provides
      @Named("lock")
      @Singleton
      RedisConfig redisConfig() {
        return RedisConfig.builder().build();
      }
    });
    modules.add(PersistentLockModule.getInstance());
    modules.add(new AbstractWaiterModule() {
      @Override
      public WaiterConfiguration waiterConfiguration() {
        return WaiterConfiguration.builder().persistenceLayer(WaiterConfiguration.PersistenceLayer.MORPHIA).build();
      }
    });

    CacheConfigBuilder cacheConfigBuilder =
        CacheConfig.builder().disabledCaches(new HashSet<>()).cacheNamespace("harness-cache");
    if (annotations.stream().anyMatch(annotation -> annotation instanceof Cache)) {
      cacheConfigBuilder.cacheBackend(CAFFEINE);
    } else {
      cacheConfigBuilder.cacheBackend(NOOP);
    }
    CacheModule cacheModule = new CacheModule(cacheConfigBuilder.build());
    modules.add(cacheModule);

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
      }
    });

    modules.add(PmsSdkModule.getInstance(getPmsSdkConfiguration()));
    modules.add(YamlSchemaClientModule.getInstance(getYamlSchemaClientConfig(), NG_MANAGER.getServiceId()));
    return modules;
  }
  private YamlSchemaClientConfig getYamlSchemaClientConfig() {
    return YamlSchemaClientConfig.builder().build();
  }

  private PmsSdkConfiguration getPmsSdkConfiguration() {
    return PmsSdkConfiguration.builder()
        .deploymentMode(SdkDeployMode.LOCAL)
        .moduleType(ModuleType.CD)
        .engineSteps(NgStepRegistrar.getEngineSteps())
        .engineAdvisers(CDServiceAdviserRegistrar.getEngineAdvisers())
        .build();
  }

  @Override
  public void initialize(Injector injector, List<Module> modules) {
    for (Module module : modules) {
      if (module instanceof ServersModule) {
        for (Closeable server : ((ServersModule) module).servers(injector)) {
          closingFactory.addServer(server);
        }
      }
    }

    final QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    queueListenerController.register(injector.getInstance(DelayEventListener.class), 1);
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return applyInjector(log, base, method, target);
  }
}
