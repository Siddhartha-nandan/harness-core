package io.harness.gitsync;

import io.harness.gitsync.interceptor.GitSyncThreadDecorator;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.dropwizard.setup.Environment;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class GitSyncSdkInitHelper {
  public static void initGitSyncSdk(Injector injector, Environment environment, GitSyncSdkConfiguration config) {
    String serviceName = config.getMicroservice().name();
    initializeServiceManager(injector, serviceName);
    registerInterceptor(environment);
  }

  private static void registerInterceptor(Environment environment) {
    environment.jersey().register(new GitSyncThreadDecorator());
  }

  private static void initializeServiceManager(Injector injector, String serviceName) {
    log.info("Initializing GMS SDK for service: {}", serviceName);
    ServiceManager serviceManager =
        injector.getInstance(Key.get(ServiceManager.class, Names.named("gitsync-sdk-service-manager"))).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
  }
}
