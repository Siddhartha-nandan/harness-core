package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Service.APP_ID_KEY;
import static software.wings.beans.Service.NAME_KEY;
import static software.wings.beans.Service.ServiceBuilder;
import static software.wings.beans.Service.builder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.generator.ApplicationGenerator.Applications;
import software.wings.generator.OwnerManager.Owners;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;

@Singleton
public class ServiceGenerator {
  @Inject ApplicationGenerator applicationGenerator;

  @Inject ServiceResourceService serviceResourceService;
  @Inject WingsPersistence wingsPersistence;

  public enum Services {
    GENERIC_TEST,
  }

  public Service ensurePredefined(Randomizer.Seed seed, Owners owners, Services predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest(seed, owners);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private Service ensureGenericTest(Randomizer.Seed seed, Owners owners) {
    Application application = owners.obtainApplication();
    if (application != null) {
      application = applicationGenerator.ensurePredefined(seed, Applications.GENERIC_TEST);
      owners.add(application);
    }
    return ensureService(seed, owners, builder().name("Test Service").artifactType(ArtifactType.WAR).build());
  }

  public Service ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);

    Services predefined = random.nextObject(Services.class);

    return ensurePredefined(seed, owners, predefined);
  }

  public Service exists(Service service) {
    return wingsPersistence.createQuery(Service.class)
        .filter(APP_ID_KEY, service.getAppId())
        .filter(NAME_KEY, service.getName())
        .get();
  }

  public Service ensureService(Randomizer.Seed seed, Owners owners, Service service) {
    EnhancedRandom random = Randomizer.instance(seed);

    ServiceBuilder builder = Service.builder();

    if (service != null && service.getAppId() != null) {
      builder.appId(service.getAppId());
    } else {
      final Application application = owners.obtainApplication();
      builder.appId(application.getUuid());
    }

    if (service != null && service.getName() != null) {
      builder.name(service.getName());
    } else {
      builder.name(random.nextObject(String.class));
    }

    Service existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    if (service != null && service.getArtifactType() != null) {
      builder.artifactType(service.getArtifactType());
    } else {
      builder.artifactType(random.nextObject(ArtifactType.class));
    }

    return serviceResourceService.save(builder.build());
  }
}
