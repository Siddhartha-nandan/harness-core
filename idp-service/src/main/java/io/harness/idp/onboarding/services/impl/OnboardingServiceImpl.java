/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.onboarding.services.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.idp.onboarding.utils.Constants.ACCOUNT_SCOPED;
import static io.harness.idp.onboarding.utils.Constants.BACKSTAGE_ALL_LOCATION_FILE_NAME;
import static io.harness.idp.onboarding.utils.Constants.BACKSTAGE_LOCATION_URL_TYPE;
import static io.harness.idp.onboarding.utils.Constants.ENTITY_REQUIRED_ERROR_MESSAGE;
import static io.harness.idp.onboarding.utils.Constants.ORGANIZATION;
import static io.harness.idp.onboarding.utils.Constants.PAGE_LIMIT_FOR_ENTITY_FETCH;
import static io.harness.idp.onboarding.utils.Constants.PROJECT;
import static io.harness.idp.onboarding.utils.Constants.SERVICE;
import static io.harness.idp.onboarding.utils.Constants.SLASH_DELIMITER;
import static io.harness.idp.onboarding.utils.Constants.SOURCE_FORMAT;
import static io.harness.idp.onboarding.utils.Constants.STATUS_UPDATE_REASON_FOR_ONBOARDING_COMPLETED;
import static io.harness.idp.onboarding.utils.Constants.SUCCESS_RESPONSE_STRING;
import static io.harness.idp.onboarding.utils.Constants.YAML_FILE_EXTENSION;
import static io.harness.idp.onboarding.utils.FileUtils.cleanUpDirectories;
import static io.harness.idp.onboarding.utils.FileUtils.createDirectories;
import static io.harness.idp.onboarding.utils.FileUtils.writeObjectAsYamlInFile;
import static io.harness.remote.client.NGRestUtils.getGeneralResponse;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.clients.BackstageCatalogLocationCreateRequest;
import io.harness.clients.BackstageResourceClient;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.idp.common.GsonUtils;
import io.harness.idp.gitintegration.beans.CatalogInfraConnectorType;
import io.harness.idp.gitintegration.beans.CatalogRepositoryDetails;
import io.harness.idp.gitintegration.entities.CatalogConnector;
import io.harness.idp.gitintegration.mappers.CatalogConnectorMapper;
import io.harness.idp.gitintegration.processor.base.ConnectorProcessor;
import io.harness.idp.gitintegration.processor.factory.ConnectorProcessorFactory;
import io.harness.idp.gitintegration.repositories.CatalogConnectorRepository;
import io.harness.idp.gitintegration.service.GitIntegrationService;
import io.harness.idp.onboarding.beans.BackstageCatalogComponentEntity;
import io.harness.idp.onboarding.beans.BackstageCatalogDomainEntity;
import io.harness.idp.onboarding.beans.BackstageCatalogEntity;
import io.harness.idp.onboarding.beans.BackstageCatalogLocationEntity;
import io.harness.idp.onboarding.beans.BackstageCatalogSystemEntity;
import io.harness.idp.onboarding.config.OnboardingModuleConfig;
import io.harness.idp.onboarding.mappers.HarnessEntityToBackstageEntity;
import io.harness.idp.onboarding.mappers.HarnessOrgToBackstageDomain;
import io.harness.idp.onboarding.mappers.HarnessProjectToBackstageSystem;
import io.harness.idp.onboarding.mappers.HarnessServiceToBackstageComponent;
import io.harness.idp.onboarding.services.OnboardingService;
import io.harness.idp.status.enums.StatusType;
import io.harness.idp.status.service.StatusInfoService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ng.core.service.dto.ServiceResponse;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.organization.remote.OrganizationClient;
import io.harness.project.remote.ProjectClient;
import io.harness.service.remote.ServiceResourceClient;
import io.harness.spec.server.idp.v1.model.CatalogConnectorInfo;
import io.harness.spec.server.idp.v1.model.EntitiesForImport;
import io.harness.spec.server.idp.v1.model.HarnessBackstageEntities;
import io.harness.spec.server.idp.v1.model.HarnessEntitiesResponse;
import io.harness.spec.server.idp.v1.model.ImportEntitiesResponse;
import io.harness.spec.server.idp.v1.model.ImportHarnessEntitiesRequest;
import io.harness.spec.server.idp.v1.model.ManualImportEntityRequest;
import io.harness.spec.server.idp.v1.model.StatusInfo;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class OnboardingServiceImpl implements OnboardingService {
  static final String BEARER_TOKEN_FORMAT = "Bearer %s";
  @Inject @Named("onboardingModuleConfig") OnboardingModuleConfig onboardingModuleConfig;
  @Inject @Named("PRIVILEGED") OrganizationClient organizationClient;
  @Inject @Named("PRIVILEGED") ProjectClient projectClient;
  @Inject ServiceResourceClient serviceResourceClient;
  @Inject HarnessOrgToBackstageDomain harnessOrgToBackstageDomain;
  @Inject HarnessProjectToBackstageSystem harnessProjectToBackstageSystem;
  @Inject HarnessServiceToBackstageComponent harnessServiceToBackstageComponent;
  @Inject ConnectorProcessorFactory connectorProcessorFactory;
  @Inject CatalogConnectorRepository catalogConnectorRepository;
  @Inject BackstageResourceClient backstageResourceClient;
  @Inject GitIntegrationService gitIntegrationService;
  @Inject StatusInfoService statusInfoService;
  @Inject @Named("backstageServiceSecret") private String backstageServiceSecret;

  @Override
  public HarnessEntitiesResponse getHarnessEntities(
      String accountIdentifier, int page, int limit, String sort, String order, String searchTerm) {
    List<OrganizationDTO> organizationDTOS = getOrganizations(accountIdentifier);
    List<ProjectDTO> projectDTOS = getProjects(accountIdentifier);
    List<ServiceResponseDTO> serviceResponseDTOS = getServices(accountIdentifier);
    log.info("Found {} organizations, {} projects, {} services for IDP onboarding import", organizationDTOS.size(),
        projectDTOS.size(), serviceResponseDTOS.size());

    List<BackstageCatalogDomainEntity> catalogDomains = harnessOrgToBackstageDomain(organizationDTOS);
    List<BackstageCatalogSystemEntity> catalogSystems = harnessProjectToBackstageSystem(projectDTOS);
    List<BackstageCatalogComponentEntity> catalogComponents = harnessServiceToBackstageComponent(serviceResponseDTOS);
    log.info("Mapped harness entities to backstage entities for IDP onboarding import");

    HarnessEntitiesResponse harnessEntitiesResponse = new HarnessEntitiesResponse();

    harnessEntitiesResponse.setOrgCount(organizationDTOS.size());
    harnessEntitiesResponse.setProjectCount(projectDTOS.size());
    harnessEntitiesResponse.setServiceCount(serviceResponseDTOS.size());

    List<HarnessBackstageEntities> harnessBackstageEntities = new ArrayList<>();

    harnessBackstageEntities.addAll(BackstageCatalogDomainEntity.map(catalogDomains));
    harnessBackstageEntities.addAll(BackstageCatalogSystemEntity.map(catalogSystems));
    harnessBackstageEntities.addAll(BackstageCatalogComponentEntity.map(catalogComponents));
    harnessEntitiesResponse.setHarnessBackstageEntities(harnessBackstageEntities);
    log.info("Converted harness backstage entities to response view");

    PageResponse<HarnessBackstageEntities> harnessBackstageEntitiesResponse =
        PageUtils.offsetAndLimit(harnessBackstageEntities, page, limit);

    harnessEntitiesResponse.setHarnessBackstageEntities(harnessBackstageEntitiesResponse.getContent());

    return harnessEntitiesResponse;
  }

  @Override
  public ImportEntitiesResponse importHarnessEntities(
      String accountIdentifier, ImportHarnessEntitiesRequest importHarnessEntitiesRequest) {
    CatalogConnectorInfo catalogConnectorInfo = importHarnessEntitiesRequest.getCatalogConnectorInfo();

    Triple<List<OrganizationDTO>, List<ProjectDTO>, List<ServiceResponseDTO>> orgProjectService =
        getOrgProjectService(accountIdentifier, importHarnessEntitiesRequest);

    List<BackstageCatalogDomainEntity> catalogDomains = harnessOrgToBackstageDomain(orgProjectService.getLeft());
    List<BackstageCatalogSystemEntity> catalogSystems = harnessProjectToBackstageSystem(orgProjectService.getMiddle());
    List<BackstageCatalogComponentEntity> catalogComponents =
        harnessServiceToBackstageComponent(orgProjectService.getRight());
    log.info("Mapped harness entities to backstage entities for IDP onboarding import");

    replaceAccountScopedValuesInCatalogConnectorInfo(catalogConnectorInfo);

    ConnectorProcessor connectorProcessor = connectorProcessorFactory.getConnectorProcessor(
        ConnectorType.fromString(catalogConnectorInfo.getSourceConnector().getType()));
    log.info("IDP onboarding import - connector processor initialized for type = {}",
        catalogConnectorInfo.getSourceConnector().getType());

    String catalogInfraConnectorType = connectorProcessor.getInfraConnectorType(
        accountIdentifier, catalogConnectorInfo.getInfraConnector().getIdentifier());

    saveCatalogConnector(accountIdentifier, catalogConnectorInfo, catalogInfraConnectorType);

    String tmpPathForCatalogInfoYamlStore =
        onboardingModuleConfig.getTmpPathForCatalogInfoYamlStore() + SLASH_DELIMITER + accountIdentifier;
    String entitiesFolderPath = getEntitiesFolderPath(catalogConnectorInfo);
    String catalogInfoLocationParentPath = tmpPathForCatalogInfoYamlStore + entitiesFolderPath + SLASH_DELIMITER;
    String orgYamlPath = catalogInfoLocationParentPath + ORGANIZATION + SLASH_DELIMITER;
    String projectYamlPath = catalogInfoLocationParentPath + PROJECT + SLASH_DELIMITER;
    String serviceYamlPath = catalogInfoLocationParentPath + SERVICE + SLASH_DELIMITER;

    createDirectories(orgYamlPath, projectYamlPath, serviceYamlPath);
    log.info("Initialized directories to write yaml files for IDP onboarding import");

    String entityTargetParentPath = catalogConnectorInfo.getRepo() + SLASH_DELIMITER + SOURCE_FORMAT + SLASH_DELIMITER
        + catalogConnectorInfo.getBranch() + SLASH_DELIMITER + accountIdentifier + entitiesFolderPath + SLASH_DELIMITER;
    String orgEntityTargetPath = entityTargetParentPath + ORGANIZATION + SLASH_DELIMITER;
    String projectEntityTargetPath = entityTargetParentPath + PROJECT + SLASH_DELIMITER;
    String serviceEntityTargetPath = entityTargetParentPath + SERVICE + SLASH_DELIMITER;

    Pair<BackstageCatalogEntity, Pair<String, String>> backstageCatalogEntityInitial = getFirstAmongAll(
        catalogInfoLocationParentPath, entityTargetParentPath, catalogDomains, catalogSystems, catalogComponents);
    log.info("Fetched {} for initial import in IDP onboarding flow", backstageCatalogEntityInitial);
    List<String> filesToPush =
        writeEntityAsYamlInFile(Collections.singletonList(backstageCatalogEntityInitial.getFirst()),
            backstageCatalogEntityInitial.getSecond().getFirst());
    connectorProcessor.performPushOperation(accountIdentifier, catalogConnectorInfo,
        onboardingModuleConfig.getTmpPathForCatalogInfoYamlStore(), entitiesFolderPath, filesToPush);
    log.info("Finished operation of yaml generation, pushing to source for one initial entity");

    log.info("Starting async operations for remaining entities import");
    new Thread(() -> {
      List<String> locationTargets = new ArrayList<>();

      List<String> targets;

      filesToPush.addAll(writeEntityAsYamlInFile(catalogDomains, orgYamlPath));
      targets = prepareEntitiesTarget(catalogDomains, orgEntityTargetPath);
      locationTargets.addAll(targets);

      filesToPush.addAll(writeEntityAsYamlInFile(catalogSystems, projectYamlPath));
      targets = prepareEntitiesTarget(catalogSystems, projectEntityTargetPath);
      locationTargets.addAll(targets);

      filesToPush.addAll(writeEntityAsYamlInFile(catalogComponents, serviceYamlPath));
      targets = prepareEntitiesTarget(catalogComponents, serviceEntityTargetPath);
      locationTargets.addAll(targets);

      BackstageCatalogLocationEntity backstageCatalogLocationEntity =
          buildBackstageCatalogLocationEntity(onboardingModuleConfig.getBackstageLocationEntityAllHarnessEntitiesName(),
              onboardingModuleConfig.getBackstageLocationEntityAllHarnessEntitiesDesc(), BACKSTAGE_LOCATION_URL_TYPE,
              locationTargets);
      writeObjectAsYamlInFile(backstageCatalogLocationEntity,
          catalogInfoLocationParentPath + BACKSTAGE_ALL_LOCATION_FILE_NAME + YAML_FILE_EXTENSION);
      filesToPush.add(catalogInfoLocationParentPath + BACKSTAGE_ALL_LOCATION_FILE_NAME + YAML_FILE_EXTENSION);
      String allTargetLocation = catalogConnectorInfo.getRepo() + SLASH_DELIMITER + SOURCE_FORMAT + SLASH_DELIMITER
          + catalogConnectorInfo.getBranch() + SLASH_DELIMITER + accountIdentifier + entitiesFolderPath
          + SLASH_DELIMITER + BACKSTAGE_ALL_LOCATION_FILE_NAME + YAML_FILE_EXTENSION;
      locationTargets.add(allTargetLocation);

      connectorProcessor.performPushOperation(accountIdentifier, catalogConnectorInfo,
          onboardingModuleConfig.getTmpPathForCatalogInfoYamlStore(), entitiesFolderPath, filesToPush);

      registerLocationInBackstage(accountIdentifier, BACKSTAGE_LOCATION_URL_TYPE, locationTargets);
      onboardingModuleConfig.getSampleEntities().forEach(sampleEntity
          -> registerLocationInBackstage(
              accountIdentifier, BACKSTAGE_LOCATION_URL_TYPE, Collections.singletonList(sampleEntity)));

      createCatalogInfraConnectorSecretInBackstageK8S(accountIdentifier, catalogConnectorInfo);

      saveStatusInfo(accountIdentifier, StatusType.ONBOARDING.name(), StatusInfo.CurrentStatusEnum.COMPLETED,
          STATUS_UPDATE_REASON_FOR_ONBOARDING_COMPLETED);

      log.info("Finished operation of yaml generation, pushing to source, registering in backstage, "
          + "creating connector secret in K8S, saving status info for all entities");

      log.info("Cleaning up directories created during IDP onboarding");
      cleanUpDirectories(tmpPathForCatalogInfoYamlStore);
    }).start();

    return new ImportEntitiesResponse().status(SUCCESS_RESPONSE_STRING);
  }

  @Override
  public ImportEntitiesResponse manualImportEntity(
      String harnessAccount, ManualImportEntityRequest manualImportEntityRequest) {
    CatalogConnector catalogConnector = getCatalogConnector(harnessAccount);
    CatalogConnectorInfo catalogConnectorInfo = CatalogConnectorMapper.toDTO(catalogConnector);

    String tmpPathForCatalogInfoYamlStore =
        onboardingModuleConfig.getTmpPathForCatalogInfoYamlStore() + SLASH_DELIMITER + harnessAccount;
    String entitiesFolderPath = !catalogConnectorInfo.getPath().isEmpty()
        ? catalogConnectorInfo.getPath()
        : onboardingModuleConfig.getCatalogInfoLocationDefaultPath();
    String catalogInfoLocationParentPath = tmpPathForCatalogInfoYamlStore + entitiesFolderPath + SLASH_DELIMITER;
    String catalogInfoLocationFilePath =
        catalogInfoLocationParentPath + manualImportEntityRequest.getEntityName() + YAML_FILE_EXTENSION;
    String entityTargetParentPath = catalogConnectorInfo.getRepo() + SLASH_DELIMITER + SOURCE_FORMAT + SLASH_DELIMITER
        + catalogConnectorInfo.getBranch() + SLASH_DELIMITER + harnessAccount + entitiesFolderPath + SLASH_DELIMITER;
    String entityTargetFilePath =
        entityTargetParentPath + manualImportEntityRequest.getEntityName() + YAML_FILE_EXTENSION;

    createDirectories(catalogInfoLocationParentPath);
    log.info("Initialized directory to write yaml files for IDP manual entity import");

    writeObjectAsYamlInFile(GsonUtils.convertJsonStringToObject(manualImportEntityRequest.getYaml(), Object.class),
        catalogInfoLocationFilePath);

    ConnectorProcessor connectorProcessor = connectorProcessorFactory.getConnectorProcessor(
        ConnectorType.fromString(catalogConnector.getSourceConnector().getType()));
    connectorProcessor.performPushOperation(harnessAccount, catalogConnectorInfo,
        onboardingModuleConfig.getTmpPathForCatalogInfoYamlStore(), entitiesFolderPath,
        Collections.singletonList(catalogInfoLocationFilePath));

    registerLocationInBackstage(
        harnessAccount, BACKSTAGE_LOCATION_URL_TYPE, Collections.singletonList(entityTargetFilePath));

    log.info(
        "Finished operation of yaml generation, pushing to source, registering in backstage for manual import entity");

    return new ImportEntitiesResponse().status(SUCCESS_RESPONSE_STRING);
  }

  private List<OrganizationDTO> getOrganizations(String accountIdentifier) {
    PageResponse<OrganizationResponse> organizationResponse =
        getResponse(organizationClient.listAllOrganizations(accountIdentifier, new ArrayList<>()));
    return organizationResponse.getContent()
        .stream()
        .map(OrganizationResponse::getOrganization)
        .collect(Collectors.toList());
  }

  private List<ProjectDTO> getProjects(String accountIdentifier) {
    return getResponse(projectClient.getProjectList(accountIdentifier));
  }

  private List<ServiceResponseDTO> getServices(String accountIdentifier) {
    List<ServiceResponseDTO> serviceResponseDTOS = new ArrayList<>();
    PageResponse<ServiceResponse> services;
    int page = 0;
    do {
      services = getResponse(serviceResourceClient.getAllServicesList(
          accountIdentifier, null, null, page, PAGE_LIMIT_FOR_ENTITY_FETCH, null));
      if (services != null && isNotEmpty(services.getContent())) {
        serviceResponseDTOS.addAll(
            services.getContent().stream().map(ServiceResponse::getService).collect(Collectors.toList()));
      }
      page++;
    } while (services != null && isNotEmpty(services.getContent()));
    return serviceResponseDTOS;
  }

  private List<BackstageCatalogDomainEntity> harnessOrgToBackstageDomain(List<OrganizationDTO> organizationDTOList) {
    HarnessOrgToBackstageDomain harnessOrgToBackstageDomainMapper =
        (HarnessOrgToBackstageDomain) getMapperByType(ORGANIZATION);
    return organizationDTOList.stream().map(harnessOrgToBackstageDomainMapper::map).collect(Collectors.toList());
  }

  private List<BackstageCatalogSystemEntity> harnessProjectToBackstageSystem(List<ProjectDTO> projectDTOList) {
    HarnessProjectToBackstageSystem harnessProjectToBackstageSystemMapper =
        (HarnessProjectToBackstageSystem) getMapperByType(PROJECT);
    return projectDTOList.stream().map(harnessProjectToBackstageSystemMapper::map).collect(Collectors.toList());
  }

  private List<BackstageCatalogComponentEntity> harnessServiceToBackstageComponent(
      List<ServiceResponseDTO> serviceResponseDTOList) {
    HarnessServiceToBackstageComponent harnessServiceToBackstageComponentMapper =
        (HarnessServiceToBackstageComponent) getMapperByType(SERVICE);
    return serviceResponseDTOList.stream()
        .map(harnessServiceToBackstageComponentMapper::map)
        .collect(Collectors.toList());
  }

  private HarnessEntityToBackstageEntity<?, ? extends BackstageCatalogEntity> getMapperByType(String type) {
    switch (type) {
      case ORGANIZATION:
        return harnessOrgToBackstageDomain;
      case PROJECT:
        return harnessProjectToBackstageSystem;
      case SERVICE:
        return harnessServiceToBackstageComponent;
      default:
        throw new UnsupportedOperationException(type + " type not supported for harness to backstage entity mapping");
    }
  }

  private Triple<List<OrganizationDTO>, List<ProjectDTO>, List<ServiceResponseDTO>> getOrgProjectService(
      String accountIdentifier, ImportHarnessEntitiesRequest importHarnessEntitiesRequest) {
    List<EntitiesForImport> idpSaveHarnessEntities = importHarnessEntitiesRequest.getEntities();
    boolean allImport = importHarnessEntitiesRequest.isAllImport();

    List<String> orgToImport = getEntitiesByType(idpSaveHarnessEntities, ORGANIZATION);
    List<String> projectToImport = getEntitiesByType(idpSaveHarnessEntities, PROJECT);
    List<String> serviceToImport = getEntitiesByType(idpSaveHarnessEntities, SERVICE);
    log.info("Found {} organizations, {} projects, {} services for IDP onboarding import", orgToImport.size(),
        projectToImport.size(), serviceToImport.size());
    throwExceptionIfNothingToImport(orgToImport, projectToImport, serviceToImport, allImport);

    Map<String, List<String>> orgProjectsMapping = getOrgProjectsMapping(projectToImport);
    Map<String, Map<String, List<String>>> orgProjectsServicesMapping = getOrgProjectsServicesMapping(serviceToImport);

    List<OrganizationDTO> organizationDTOS;
    List<ProjectDTO> projectDTOS;
    List<ServiceResponseDTO> serviceDTOS;
    if (allImport) {
      organizationDTOS = getOrganizations(accountIdentifier);
      projectDTOS = getProjects(accountIdentifier);
      serviceDTOS = getServices(accountIdentifier);
    } else {
      organizationDTOS = getOrganizationDTOS(accountIdentifier, orgToImport);
      projectDTOS = getProjectDTOS(accountIdentifier, orgProjectsMapping);
      serviceDTOS = getServiceDTOS(accountIdentifier, orgProjectsServicesMapping);
    }
    log.info("Fetched {} organizations, {} projects, {} services for IDP onboarding import", organizationDTOS.size(),
        projectDTOS.size(), serviceDTOS.size());
    throwExceptionIfMismatchBetweenFoundAndProvided(
        orgToImport, projectToImport, serviceToImport, organizationDTOS, projectDTOS, serviceDTOS, allImport);

    return Triple.of(organizationDTOS, projectDTOS, serviceDTOS);
  }

  private List<String> getEntitiesByType(List<EntitiesForImport> idpSaveHarnessEntities, String type) {
    return idpSaveHarnessEntities.stream()
        .filter(entitiesForImport -> entitiesForImport.getEntityType().equals(type))
        .map(EntitiesForImport::getIdentifier)
        .collect(Collectors.toList());
  }

  private void throwExceptionIfNothingToImport(
      List<String> orgToImport, List<String> projectToImport, List<String> serviceToImport, boolean allImport) {
    if ((orgToImport.size() + projectToImport.size() + serviceToImport.size() == 0) && !allImport) {
      throw new InvalidRequestException(ENTITY_REQUIRED_ERROR_MESSAGE);
    }
  }

  private Map<String, List<String>> getOrgProjectsMapping(List<String> harnessEntitiesProjects) {
    Map<String, List<String>> projectIdentifiers = new HashMap<>();
    harnessEntitiesProjects.forEach(project -> {
      String[] orgProject = project.split("\\|");
      if (projectIdentifiers.containsKey(orgProject[0])) {
        List<String> existingProjects = projectIdentifiers.get(orgProject[0]);
        existingProjects.add(orgProject[1]);
        projectIdentifiers.put(orgProject[0], existingProjects);
      } else {
        projectIdentifiers.put(orgProject[0], Collections.singletonList(orgProject[1]));
      }
    });
    return projectIdentifiers;
  }

  private Map<String, Map<String, List<String>>> getOrgProjectsServicesMapping(List<String> harnessEntitiesServices) {
    Map<String, Map<String, List<String>>> serviceIdentifiers = new HashMap<>();
    harnessEntitiesServices.forEach(service -> {
      String[] orgProjectService = service.split("\\|");
      if (serviceIdentifiers.containsKey(orgProjectService[0])) {
        Map<String, List<String>> existingProjectsServices =
            new HashMap<>(serviceIdentifiers.get(orgProjectService[0]));
        if (existingProjectsServices.containsKey(orgProjectService[1])) {
          List<String> existingServices = new ArrayList<>(existingProjectsServices.get(orgProjectService[1]));
          existingServices.add(orgProjectService[2]);
          existingProjectsServices.put(orgProjectService[1], existingServices);
          serviceIdentifiers.put(orgProjectService[0], existingProjectsServices);
        } else {
          existingProjectsServices.put(orgProjectService[1], Collections.singletonList(orgProjectService[2]));
          serviceIdentifiers.put(orgProjectService[0], existingProjectsServices);
        }
      } else {
        serviceIdentifiers.put(
            orgProjectService[0], Map.of(orgProjectService[1], Collections.singletonList(orgProjectService[2])));
      }
    });
    return serviceIdentifiers;
  }

  private List<OrganizationDTO> getOrganizationDTOS(String accountIdentifier, List<String> orgToImport) {
    return !orgToImport.isEmpty() ? getOrganizations(accountIdentifier, orgToImport) : new ArrayList<>();
  }

  private List<ProjectDTO> getProjectDTOS(String accountIdentifier, Map<String, List<String>> orgProjectsMapping) {
    return orgProjectsMapping.size() > 0 ? getProjects(accountIdentifier, orgProjectsMapping.keySet(),
               orgProjectsMapping.values().stream().flatMap(Collection::stream).collect(Collectors.toList()))
                                         : new ArrayList<>();
  }

  private List<ServiceResponseDTO> getServiceDTOS(
      String accountIdentifier, Map<String, Map<String, List<String>>> orgProjectsServicesMapping) {
    return orgProjectsServicesMapping.size() > 0 ? getServices(accountIdentifier, orgProjectsServicesMapping)
                                                 : new ArrayList<>();
  }

  private void throwExceptionIfMismatchBetweenFoundAndProvided(List<String> orgToImport, List<String> projectToImport,
      List<String> serviceToImport, List<OrganizationDTO> organizationDTOS, List<ProjectDTO> projectDTOS,
      List<ServiceResponseDTO> serviceResponseDTOS, boolean allImport) {
    if ((organizationDTOS.size() != orgToImport.size() || projectDTOS.size() != projectToImport.size()
            || serviceResponseDTOS.size() != serviceToImport.size())
        && !allImport) {
      throw new UnexpectedException("Mismatch between provided and found harness entities for IDP import");
    }
  }

  private List<OrganizationDTO> getOrganizations(String accountIdentifier, List<String> identifiers) {
    List<OrganizationDTO> organizationDTOS = new ArrayList<>();
    PageResponse<OrganizationResponse> organizations;
    int page = 0;
    do {
      organizations = getResponse(organizationClient.listOrganization(
          accountIdentifier, identifiers, null, page, PAGE_LIMIT_FOR_ENTITY_FETCH, null));
      if (organizations != null && isNotEmpty(organizations.getContent())) {
        organizationDTOS.addAll(organizations.getContent()
                                    .stream()
                                    .map(OrganizationResponse::getOrganization)
                                    .collect(Collectors.toList()));
      }
      page++;
    } while (organizations != null && isNotEmpty(organizations.getContent()));
    return organizationDTOS;
  }

  private List<ProjectDTO> getProjects(
      String accountIdentifier, Set<String> organizationIdentifiers, List<String> identifiers) {
    List<ProjectDTO> projectDTOS = new ArrayList<>();
    PageResponse<ProjectResponse> projects;
    int page = 0;
    do {
      projects = getResponse(projectClient.listWithMultiOrg(accountIdentifier, organizationIdentifiers, false,
          identifiers, null, null, page, PAGE_LIMIT_FOR_ENTITY_FETCH, null));
      if (projects != null && isNotEmpty(projects.getContent())) {
        projectDTOS.addAll(
            projects.getContent().stream().map(ProjectResponse::getProject).collect(Collectors.toList()));
      }
      page++;
    } while (projects != null && isNotEmpty(projects.getContent()));
    return projectDTOS;
  }

  private List<ServiceResponseDTO> getServices(
      String accountIdentifier, Map<String, Map<String, List<String>>> orgProjectsServicesMapping) {
    List<ServiceResponseDTO> serviceResponseDTOS = new ArrayList<>();
    for (var serviceIdentifier : orgProjectsServicesMapping.entrySet()) {
      String org = serviceIdentifier.getKey();
      for (var projectService : serviceIdentifier.getValue().entrySet()) {
        PageResponse<ServiceResponse> services;
        int page = 0;
        do {
          services = getResponse(serviceResourceClient.listServicesForProject(page, PAGE_LIMIT_FOR_ENTITY_FETCH,
              accountIdentifier, org, projectService.getKey(), projectService.getValue(), null));
          if (services != null && isNotEmpty(services.getContent())) {
            serviceResponseDTOS.addAll(
                services.getContent().stream().map(ServiceResponse::getService).collect(Collectors.toList()));
          }
          page++;
        } while (services != null && isNotEmpty(services.getContent()));
      }
    }
    return serviceResponseDTOS;
  }

  private void replaceAccountScopedValuesInCatalogConnectorInfo(CatalogConnectorInfo catalogConnectorInfo) {
    catalogConnectorInfo.getInfraConnector().setIdentifier(
        catalogConnectorInfo.getInfraConnector().getIdentifier().replace(ACCOUNT_SCOPED, ""));
    catalogConnectorInfo.getSourceConnector().setIdentifier(
        catalogConnectorInfo.getSourceConnector().getIdentifier().replace(ACCOUNT_SCOPED, ""));
  }

  private void saveCatalogConnector(
      String accountIdentifier, CatalogConnectorInfo catalogConnectorInfo, String catalogInfraConnectorType) {
    CatalogConnector catalogConnector = new CatalogConnector();

    catalogConnector.setAccountIdentifier(accountIdentifier);
    catalogConnector.setIdentifier(catalogConnectorInfo.getInfraConnector().getIdentifier() + "_"
        + catalogConnectorInfo.getSourceConnector().getIdentifier());
    catalogConnector.setType(CatalogInfraConnectorType.valueOf(catalogInfraConnectorType));
    catalogConnector.setInfraConnector(catalogConnectorInfo.getInfraConnector());
    catalogConnector.setSourceConnector(catalogConnectorInfo.getSourceConnector());
    catalogConnector.setCatalogRepositoryDetails(new CatalogRepositoryDetails(
        catalogConnectorInfo.getRepo(), catalogConnectorInfo.getBranch(), catalogConnectorInfo.getPath()));

    catalogConnectorRepository.save(catalogConnector);
    log.info("Saved catalogConnector to DB. Account = {}", accountIdentifier);
  }

  private String getEntitiesFolderPath(CatalogConnectorInfo catalogConnectorInfo) {
    String entitiesFolderPath = !catalogConnectorInfo.getPath().isEmpty()
        ? catalogConnectorInfo.getPath()
        : onboardingModuleConfig.getCatalogInfoLocationDefaultPath();
    entitiesFolderPath =
        !entitiesFolderPath.startsWith(SLASH_DELIMITER) ? (SLASH_DELIMITER + entitiesFolderPath) : entitiesFolderPath;
    return entitiesFolderPath;
  }

  private Pair<BackstageCatalogEntity, Pair<String, String>> getFirstAmongAll(String catalogInfoLocationParentPath,
      String entityTargetParentPath, List<? extends BackstageCatalogEntity>... backstageCatalogEntities) {
    for (List<? extends BackstageCatalogEntity> backstageCatalogEntity : backstageCatalogEntities) {
      for (BackstageCatalogEntity catalogEntity : backstageCatalogEntity) {
        if (catalogEntity instanceof BackstageCatalogDomainEntity) {
          return new Pair<>(catalogEntity,
              new Pair<>(catalogInfoLocationParentPath + ORGANIZATION + SLASH_DELIMITER,
                  entityTargetParentPath + ORGANIZATION + SLASH_DELIMITER));
        } else if (catalogEntity instanceof BackstageCatalogSystemEntity) {
          return new Pair<>(catalogEntity,
              new Pair<>(catalogInfoLocationParentPath + PROJECT + SLASH_DELIMITER,
                  entityTargetParentPath + PROJECT + SLASH_DELIMITER));
        } else if (catalogEntity instanceof BackstageCatalogComponentEntity) {
          return new Pair<>(catalogEntity,
              new Pair<>(catalogInfoLocationParentPath + SERVICE + SLASH_DELIMITER,
                  entityTargetParentPath + SERVICE + SLASH_DELIMITER));
        } else {
          throw new UnexpectedException("Entity should be off one among domain / system / component");
        }
      }
    }
    throw new UnexpectedException("Found invalid entity to import in IDP onboarding flow");
  }

  private List<String> writeEntityAsYamlInFile(List<? extends BackstageCatalogEntity> entities, String prefixPath) {
    List<String> files = new ArrayList<>();
    entities.forEach(entity -> {
      entity.getMetadata().setName(entity.getMetadata().getAbsoluteIdentifier());
      writeObjectAsYamlInFile(entity, prefixPath + entity.getMetadata().getAbsoluteIdentifier() + YAML_FILE_EXTENSION);
      files.add(prefixPath + entity.getMetadata().getAbsoluteIdentifier() + YAML_FILE_EXTENSION);
    });
    return files;
  }

  private List<String> prepareEntitiesTarget(List<? extends BackstageCatalogEntity> entities, String prefixPath) {
    List<String> targets = new ArrayList<>();
    entities.forEach(
        entity -> targets.add(prefixPath + entity.getMetadata().getAbsoluteIdentifier() + YAML_FILE_EXTENSION));
    return targets;
  }

  private BackstageCatalogLocationEntity buildBackstageCatalogLocationEntity(
      String name, String description, String type, List<String> targets) {
    BackstageCatalogLocationEntity backstageCatalogLocationEntity = new BackstageCatalogLocationEntity();

    BackstageCatalogEntity.Metadata metadata = new BackstageCatalogEntity.Metadata();
    metadata.setName(name);
    metadata.setDescription(description);
    backstageCatalogLocationEntity.setMetadata(metadata);

    BackstageCatalogLocationEntity.Spec spec = new BackstageCatalogLocationEntity.Spec();
    spec.setType(type);
    spec.setTargets(targets);
    backstageCatalogLocationEntity.setSpec(spec);

    return backstageCatalogLocationEntity;
  }

  private void registerLocationInBackstage(String accountIdentifier, String type, List<String> targets) {
    try {
      targets.forEach(target
          -> getGeneralResponse(backstageResourceClient.createCatalogLocation(accountIdentifier,
              String.format(BEARER_TOKEN_FORMAT, backstageServiceSecret),
              new BackstageCatalogLocationCreateRequest(type, target))));
    } catch (Exception e) {
      log.error("Unable to register target of type = {} with locations = {} in backstage, ex = {}", type, targets,
          e.getMessage(), e);
    }
  }

  private void createCatalogInfraConnectorSecretInBackstageK8S(
      String accountIdentifier, CatalogConnectorInfo catalogConnectorInfo) {
    try {
      gitIntegrationService.createConnectorSecretsEnvVariable(accountIdentifier, null, null,
          catalogConnectorInfo.getInfraConnector().getIdentifier(),
          ConnectorType.fromString(catalogConnectorInfo.getInfraConnector().getType()));
    } catch (Exception e) {
      log.error("Unable to create infra connector secrets in backstage k8s, ex = {}", e.getMessage(), e);
    }
  }

  private void saveStatusInfo(
      String accountIdentifier, String type, StatusInfo.CurrentStatusEnum currentStatus, String reason) {
    StatusInfo statusInfo = new StatusInfo();
    statusInfo.setCurrentStatus(currentStatus);
    statusInfo.setReason(reason);
    statusInfoService.save(statusInfo, accountIdentifier, type);
  }

  private CatalogConnector getCatalogConnector(String accountIdentifier) {
    Optional<CatalogConnector> catalogConnector = catalogConnectorRepository.findByAccountIdentifier(accountIdentifier);
    if (catalogConnector.isEmpty()) {
      throw new InvalidRequestException(
          String.format("Catalog connector not found for accountIdentifier: [%s]]", accountIdentifier));
    }
    return catalogConnector.get();
  }
}
