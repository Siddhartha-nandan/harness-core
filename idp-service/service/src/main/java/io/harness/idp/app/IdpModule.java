/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.app;

import static io.harness.authorization.AuthorizationServiceHeader.IDP_SERVICE;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.SECRET_ENTITY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorResourceClientModule;
import io.harness.events.EventsFrameworkModule;
import io.harness.idp.config.resource.ConfigManagerResource;
import io.harness.idp.config.resources.ConfigManagerResourceImpl;
import io.harness.idp.config.service.AppConfigService;
import io.harness.idp.config.service.AppConfigServiceImpl;
import io.harness.idp.gitintegration.factory.ConnectorProcessorFactory;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.idp.namespace.service.NamespaceServiceImpl;
import io.harness.idp.secret.eventlisteners.SecretCrudListener;
import io.harness.idp.secret.resources.EnvironmentSecretApiImpl;
import io.harness.idp.secret.service.EnvironmentSecretService;
import io.harness.idp.secret.service.EnvironmentSecretServiceImpl;
import io.harness.idp.status.k8s.HealthCheck;
import io.harness.idp.status.k8s.PodHealthCheck;
import io.harness.idp.status.resources.StatusInfoApiImpl;
import io.harness.idp.status.service.StatusInfoService;
import io.harness.idp.status.service.StatusInfoServiceImpl;
import io.harness.k8s.client.K8sApiClient;
import io.harness.k8s.client.K8sClient;
import io.harness.metrics.modules.MetricsModule;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.event.MessageListener;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.queue.QueueController;
import io.harness.secrets.SecretNGManagerClientModule;
import io.harness.serializer.IdpServiceRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.spec.server.idp.v1.EnvironmentSecretApi;
import io.harness.spec.server.idp.v1.StatusInfoApi;
import io.harness.threading.ThreadPool;
import io.harness.version.VersionModule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import dev.morphia.converters.TypeConverter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;

@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class IdpModule extends AbstractModule {
  private final IdpConfiguration appConfig;
  public IdpModule(IdpConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    install(VersionModule.getInstance());
    install(new IdpPersistenceModule());
    install(new AbstractMongoModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(IdpServiceRegistrars.kryoRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(IdpServiceRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
      }

      @Provides
      @Singleton
      List<Class<? extends Converter<?, ?>>> springConverters() {
        return ImmutableList.<Class<? extends Converter<?, ?>>>builder().build();
      }

      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }

      @Provides
      @Singleton
      @Named("dbAliases")
      public List<String> getDbAliases() {
        return appConfig.getDbAliases();
      }

      @Provides
      @Singleton
      @Named("morphiaClasses")
      Map<Class, String> morphiaCustomCollectionNames() {
        return ImmutableMap.<Class, String>builder().build();
      }
    });
    install(new MetricsModule());
    install(new EventsFrameworkModule(appConfig.getEventsFrameworkConfiguration()));
    install(new AbstractModule() {
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
    install(new SecretNGManagerClientModule(appConfig.getNgManagerServiceHttpClientConfig(),
        appConfig.getNgManagerServiceSecret(), IDP_SERVICE.getServiceId()));
    install(new ConnectorResourceClientModule(appConfig.getNgManagerServiceHttpClientConfig(),
        appConfig.getNgManagerServiceSecret(), IDP_SERVICE.getServiceId()));

    bind(IdpConfiguration.class).toInstance(appConfig);
    // Keeping it to 1 thread to start with. Assuming executor service is used only to
    // serve health checks. If it's being used for other tasks also, max pool size should be increased.
    bind(ExecutorService.class)
        .toInstance(ThreadPool.create(1, 2, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("default-idp-service-executor-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
    bind(HPersistence.class).to(MongoPersistence.class).in(Singleton.class);
    bind(AppConfigService.class).to(AppConfigServiceImpl.class);
    bind(EnvironmentSecretService.class).to(EnvironmentSecretServiceImpl.class);
    bind(StatusInfoService.class).to(StatusInfoServiceImpl.class);
    bind(NamespaceService.class).to(NamespaceServiceImpl.class);
    bind(ConfigManagerResource.class).to(ConfigManagerResourceImpl.class);
    bind(EnvironmentSecretApi.class).to(EnvironmentSecretApiImpl.class);
    bind(StatusInfoApi.class).to(StatusInfoApiImpl.class);
    bind(K8sClient.class).to(K8sApiClient.class);
    bind(HealthCheck.class).to(PodHealthCheck.class);
    bind(MessageListener.class).annotatedWith(Names.named(SECRET_ENTITY + ENTITY_CRUD)).to(SecretCrudListener.class);
    bind(ConnectorProcessorFactory.class);
  }

  @Provides
  @Singleton
  public MongoConfig mongoConfig() {
    return appConfig.getMongoConfig();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
