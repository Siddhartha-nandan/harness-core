/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.ng.core.filestore.FileUsage;
import io.harness.ngmigration.beans.BaseProvidedInput;
import io.harness.ngmigration.beans.FileYamlDTO;
import io.harness.ngmigration.beans.InputDefaults;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.ngmigration.expressions.MigratorExpressionUtils;
import io.harness.ngmigration.secrets.SecretFactory;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.SecretNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;

import software.wings.beans.GitFileConfig;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariableType;
import software.wings.beans.Variable;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.NGMigrationEntityType;

import com.google.common.collect.ImmutableMap;
import io.serializer.HObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class MigratorUtility {
  public static final ParameterField<String> RUNTIME_INPUT = ParameterField.createValueField("<+input>");
  public static final Pattern cgPattern = Pattern.compile("\\$\\{[\\w-.\"()]+}");
  public static final Pattern ngPattern = Pattern.compile("<\\+[\\w-.\"()]+>");

  private static final int APPLICATION = 0;
  private static final int SECRET_MANAGER = 1;
  private static final int SECRET = 5;
  private static final int TEMPLATE = 7;
  private static final int CONNECTOR = 10;
  private static final int CONTAINER_TASK = 13;
  private static final int ECS_SERVICE_SPEC = 14;
  private static final int MANIFEST = 15;
  private static final int CONFIG_FILE = 16;
  private static final int AMI_STARTUP_SCRIPT = 17;
  private static final int ELASTIGROUP_CONFIGURATION = 18;
  private static final int SERVICE = 20;
  private static final int INFRA_PROVISIONER = 23;
  private static final int ENVIRONMENT = 25;
  private static final int INFRA = 35;
  private static final int SERVICE_VARIABLE = 40;
  private static final int WORKFLOW = 70;
  private static final int PIPELINE = 100;

  private static final int TRIGGER = 150;

  private static final Map<NGMigrationEntityType, Integer> MIGRATION_ORDER =
      ImmutableMap.<NGMigrationEntityType, Integer>builder()
          .put(NGMigrationEntityType.APPLICATION, APPLICATION)
          .put(NGMigrationEntityType.SECRET_MANAGER, SECRET_MANAGER)
          .put(NGMigrationEntityType.TEMPLATE, TEMPLATE)
          .put(NGMigrationEntityType.CONNECTOR, CONNECTOR)
          .put(NGMigrationEntityType.CONTAINER_TASK, CONTAINER_TASK)
          .put(NGMigrationEntityType.ECS_SERVICE_SPEC, ECS_SERVICE_SPEC)
          .put(NGMigrationEntityType.AMI_STARTUP_SCRIPT, AMI_STARTUP_SCRIPT)
          .put(NGMigrationEntityType.ELASTIGROUP_CONFIGURATION, ELASTIGROUP_CONFIGURATION)
          .put(NGMigrationEntityType.MANIFEST, MANIFEST)
          .put(NGMigrationEntityType.CONFIG_FILE, CONFIG_FILE)
          .put(NGMigrationEntityType.SERVICE, SERVICE)
          .put(NGMigrationEntityType.INFRA_PROVISIONER, INFRA_PROVISIONER)
          .put(NGMigrationEntityType.ENVIRONMENT, ENVIRONMENT)
          .put(NGMigrationEntityType.INFRA, INFRA)
          .put(NGMigrationEntityType.SERVICE_VARIABLE, SERVICE_VARIABLE)
          .put(NGMigrationEntityType.WORKFLOW, WORKFLOW)
          .put(NGMigrationEntityType.PIPELINE, PIPELINE)
          .put(NGMigrationEntityType.TRIGGER, TRIGGER)
          .build();

  private MigratorUtility() {}

  public static <T> T getRestClient(ServiceHttpClientConfig ngClientConfig, Class<T> clazz) {
    OkHttpClient okHttpClient = Http.getOkHttpClient(ngClientConfig.getBaseUrl(), false);
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(ngClientConfig.getBaseUrl())
                            .addConverterFactory(JacksonConverterFactory.create(HObjectMapper.NG_DEFAULT_OBJECT_MAPPER))
                            .build();
    return retrofit.create(clazz);
  }

  public static String generateManifestIdentifier(String name) {
    return generateIdentifier(name);
  }

  public static String generateIdentifier(String name) {
    if (StringUtils.isBlank(name)) {
      return "";
    }
    name = StringUtils.stripAccents(name);
    String generated = CaseUtils.toCamelCase(name.replaceAll("[^A-Za-z0-9]", " ").trim(), false, ' ');
    return Character.isDigit(generated.charAt(0)) ? "_" + generated : generated;
  }

  public static ParameterField<Timeout> getTimeout(Integer timeoutInMillis) {
    if (timeoutInMillis == null) {
      return ParameterField.createValueField(Timeout.builder().timeoutString("10m").build());
    }
    long t = timeoutInMillis / 1000;
    String timeoutString = t + "s";
    return ParameterField.createValueField(Timeout.builder().timeoutString(timeoutString).build());
  }

  public static ParameterField<String> getParameterField(String value) {
    if (StringUtils.isBlank(value)) {
      return ParameterField.createValueField("");
    }
    return ParameterField.createValueField(value);
  }

  public static void sort(List<NGYamlFile> files) {
    files.sort(Comparator.comparingInt(MigratorUtility::toInt));
  }

  public static ParameterField<List<TaskSelectorYaml>> getDelegateSelectors(List<String> strings) {
    return EmptyPredicate.isEmpty(strings)
        ? ParameterField.createValueField(Collections.emptyList())
        : ParameterField.createValueField(strings.stream().map(TaskSelectorYaml::new).collect(Collectors.toList()));
  }

  // This is for sorting entities while creating
  private static int toInt(NGYamlFile file) {
    if (NGMigrationEntityType.SECRET == file.getType()) {
      return SecretFactory.isStoredInHarnessSecretManager(file) ? Integer.MIN_VALUE : SECRET;
    }
    if (MIGRATION_ORDER.containsKey(file.getType())) {
      return MIGRATION_ORDER.get(file.getType());
    }
    throw new InvalidArgumentsException("Unknown type found: " + file.getType());
  }

  public static Scope getDefaultScope(MigrationInputDTO inputDTO, CgEntityId entityId, Scope defaultScope) {
    NGMigrationEntityType entityType = entityId.getType();
    return getDefaultScope(inputDTO, entityId, defaultScope, entityType);
  }

  public static Scope getDefaultScope(MigrationInputDTO inputDTO, CgEntityId entityId, Scope defaultScope,
      NGMigrationEntityType destinationEntityType) {
    if (inputDTO == null) {
      return defaultScope;
    }
    Scope scope = defaultScope;
    Map<NGMigrationEntityType, InputDefaults> defaults = inputDTO.getDefaults();
    if (defaults != null && defaults.containsKey(destinationEntityType)
        && defaults.get(destinationEntityType).getScope() != null) {
      scope = defaults.get(destinationEntityType).getScope();
    }
    Map<CgEntityId, BaseProvidedInput> inputs = inputDTO.getOverrides();
    if (inputs != null && inputs.containsKey(entityId) && inputs.get(entityId).getScope() != null) {
      scope = inputs.get(entityId).getScope();
    }
    return scope;
  }

  public static Scope getScope(NgEntityDetail entityDetail) {
    String orgId = entityDetail.getOrgIdentifier();
    String projectId = entityDetail.getProjectIdentifier();
    if (StringUtils.isAllBlank(orgId, projectId)) {
      return Scope.ACCOUNT;
    }
    if (StringUtils.isNotBlank(projectId)) {
      return Scope.PROJECT;
    }
    return Scope.ORG;
  }

  public static SecretRefData getSecretRefDefaultNull(Map<CgEntityId, NGYamlFile> migratedEntities, String secretId) {
    if (StringUtils.isBlank(secretId)) {
      return null;
    }
    CgEntityId secretEntityId = CgEntityId.builder().id(secretId).type(NGMigrationEntityType.SECRET).build();
    if (!migratedEntities.containsKey(secretEntityId)) {
      return null;
    }
    return getSecretRef(migratedEntities, secretId);
  }

  public static SecretRefData getSecretRef(Map<CgEntityId, NGYamlFile> migratedEntities, String secretId) {
    return getSecretRef(migratedEntities, secretId, NGMigrationEntityType.SECRET);
  }

  public static SecretRefData getSecretRef(
      Map<CgEntityId, NGYamlFile> migratedEntities, String entityId, NGMigrationEntityType entityType) {
    if (entityId == null) {
      return SecretRefData.builder().identifier("__PLEASE_FIX_ME__").scope(Scope.PROJECT).build();
    }
    CgEntityId secretEntityId = CgEntityId.builder().id(entityId).type(entityType).build();
    if (!migratedEntities.containsKey(secretEntityId)) {
      return SecretRefData.builder().identifier("__PLEASE_FIX_ME__").scope(Scope.PROJECT).build();
    }
    NgEntityDetail migratedSecret = migratedEntities.get(secretEntityId).getNgEntityDetail();
    return SecretRefData.builder()
        .identifier(migratedSecret.getIdentifier())
        .scope(MigratorUtility.getScope(migratedSecret))
        .build();
  }

  public static String getIdentifierWithScopeDefaults(
      Map<CgEntityId, NGYamlFile> migratedEntities, String entityId, NGMigrationEntityType entityType) {
    NGYamlFile detail = migratedEntities.get(CgEntityId.builder().type(entityType).id(entityId).build());
    if (detail == null) {
      return "__PLEASE_FIX_ME__";
    }
    return getIdentifierWithScope(detail.getNgEntityDetail());
  }

  public static String getIdentifierWithScope(
      Map<CgEntityId, NGYamlFile> migratedEntities, String entityId, NGMigrationEntityType entityType) {
    NgEntityDetail detail =
        migratedEntities.get(CgEntityId.builder().type(entityType).id(entityId).build()).getNgEntityDetail();
    return getIdentifierWithScope(detail);
  }

  public static String getIdentifierWithScope(NgEntityDetail entityDetail) {
    String orgId = entityDetail.getOrgIdentifier();
    String projectId = entityDetail.getProjectIdentifier();
    String identifier = entityDetail.getIdentifier();
    if (StringUtils.isAllBlank(orgId, projectId)) {
      return "account." + identifier;
    }
    if (StringUtils.isNotBlank(projectId)) {
      return identifier;
    }
    return "org." + identifier;
  }

  public static List<NGVariable> getVariables(List<Variable> cgVariables) {
    List<NGVariable> variables = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(cgVariables)) {
      cgVariables.forEach(serviceVariable -> variables.add(getNGVariable(serviceVariable)));
    }
    return variables;
  }

  public static List<NGVariable> getVariables(
      List<ServiceVariable> serviceVariables, Map<CgEntityId, NGYamlFile> migratedEntities) {
    List<NGVariable> variables = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(serviceVariables)) {
      serviceVariables.forEach(serviceVariable -> variables.add(getNGVariable(serviceVariable, migratedEntities)));
    }
    return variables;
  }

  public static NGVariable getNGVariable(Variable variable) {
    String value = "<+input>";
    if (EmptyPredicate.isNotEmpty(variable.getValue())) {
      value = String.valueOf(MigratorExpressionUtils.render(variable.getValue(), new HashMap<>()));
    }
    String name = variable.getName();
    name = name.replace('-', '_');
    return StringNGVariable.builder()
        .type(NGVariableType.STRING)
        .name(name)
        .value(ParameterField.createValueField(value))
        .build();
  }

  public static NGVariable getNGVariable(
      ServiceVariable serviceVariable, Map<CgEntityId, NGYamlFile> migratedEntities) {
    if (serviceVariable.getType().equals(ServiceVariableType.ENCRYPTED_TEXT)) {
      return SecretNGVariable.builder()
          .type(NGVariableType.SECRET)
          .value(ParameterField.createValueField(
              MigratorUtility.getSecretRef(migratedEntities, serviceVariable.getEncryptedValue())))
          .name(serviceVariable.getName())
          .build();
    } else {
      String value = "";
      if (EmptyPredicate.isNotEmpty(serviceVariable.getValue())) {
        value =
            String.valueOf(MigratorExpressionUtils.render(String.valueOf(serviceVariable.getValue()), new HashMap<>()));
      }
      String name = serviceVariable.getName();
      name = name.replace('-', '_');
      return StringNGVariable.builder()
          .type(NGVariableType.STRING)
          .name(name)
          .value(ParameterField.createValueField(value))
          .build();
    }
  }

  public static String generateIdentifier(
      Map<CgEntityId, BaseProvidedInput> inputs, CgEntityId entityId, String defaultIdentifier) {
    if (inputs == null || !inputs.containsKey(entityId) || StringUtils.isBlank(inputs.get(entityId).getIdentifier())) {
      return defaultIdentifier;
    }
    return inputs.get(entityId).getIdentifier();
  }

  public static String generateIdentifierDefaultName(
      Map<CgEntityId, BaseProvidedInput> inputs, CgEntityId entityId, String name) {
    if (inputs == null || !inputs.containsKey(entityId) || StringUtils.isBlank(inputs.get(entityId).getIdentifier())) {
      return generateIdentifier(name);
    }
    return inputs.get(entityId).getIdentifier();
  }

  public static String generateName(
      Map<CgEntityId, BaseProvidedInput> inputs, CgEntityId entityId, String defaultName) {
    if (inputs == null || !inputs.containsKey(entityId) || StringUtils.isBlank(inputs.get(entityId).getName())) {
      return generateName(defaultName);
    }
    return inputs.get(entityId).getName();
  }

  public static String getOrgIdentifier(Scope scope, MigrationInputDTO inputDTO) {
    if (Scope.ACCOUNT.equals(scope)) {
      return null;
    }
    if (StringUtils.isBlank(inputDTO.getOrgIdentifier())) {
      throw new InvalidRequestException("Trying to scope entity to Org but no org identifier provided in input");
    }
    return inputDTO.getOrgIdentifier();
  }

  public static String getProjectIdentifier(Scope scope, MigrationInputDTO inputDTO) {
    if (Scope.ACCOUNT.equals(scope) || Scope.ORG.equals(scope)) {
      return null;
    }
    if (StringUtils.isAllBlank(inputDTO.getOrgIdentifier(), inputDTO.getProjectIdentifier())) {
      throw new InvalidRequestException("Trying to scope entity to Project but org/project identifier(s) are missing");
    }
    return inputDTO.getProjectIdentifier();
  }

  public static boolean endsWithIgnoreCase(String str, String arg, String... args) {
    if (str.toLowerCase().endsWith(arg)) {
      return true;
    }
    for (String arg1 : args) {
      if (str.toLowerCase().endsWith(arg1)) {
        return true;
      }
    }
    return false;
  }

  public static ParameterField<List<String>> getFileStorePaths(List<NGYamlFile> files) {
    if (EmptyPredicate.isEmpty(files)) {
      return ParameterField.ofNull();
    }
    return ParameterField.createValueField(
        files.stream().map(file -> "/" + ((FileYamlDTO) file.getYaml()).getName()).collect(Collectors.toList()));
  }

  public static ParameterField<List<String>> splitWithComma(String str) {
    return ParameterField.createValueField(
        Arrays.stream(str.split(",")).map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList()));
  }

  public static ParameterField<String> getIdentifierWithScopeDefaultsRuntime(
      Map<CgEntityId, NGYamlFile> migratedEntities, String entityId, NGMigrationEntityType entityType) {
    NGYamlFile ngYamlFile = migratedEntities.get(CgEntityId.builder().type(entityType).id(entityId).build());
    if (ngYamlFile == null) {
      return RUNTIME_INPUT;
    }
    NgEntityDetail detail = ngYamlFile.getNgEntityDetail();
    return ParameterField.createValueField(getIdentifierWithScope(detail));
  }

  public static String generateName(String str) {
    if (StringUtils.isBlank(str)) {
      return str;
    }
    str = StringUtils.stripAccents(str);
    Pattern p = Pattern.compile("[^-0-9a-zA-Z_\\s]", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(str);
    String generated = m.replaceAll("_");
    return Character.isDigit(generated.charAt(0)) ? "_" + generated : generated;
  }

  @Nullable
  public static NGYamlFile getYamlConfigFile(MigrationInputDTO inputDTO, byte[] content, String identifier) {
    if (isEmpty(content)) {
      return null;
    }
    String fileUsage = FileUsage.CONFIG.name();
    String projectIdentifier = MigratorUtility.getProjectIdentifier(Scope.PROJECT, inputDTO);
    String orgIdentifier = MigratorUtility.getOrgIdentifier(Scope.PROJECT, inputDTO);

    return NGYamlFile.builder()
        .type(NGMigrationEntityType.CONFIG_FILE)
        .yaml(FileYamlDTO.builder()
                  .identifier(identifier)
                  .fileUsage(fileUsage)
                  .name(identifier)
                  .content(new String(content))
                  .orgIdentifier(orgIdentifier)
                  .projectIdentifier(projectIdentifier)
                  .build())
        .ngEntityDetail(NgEntityDetail.builder()
                            .identifier(identifier)
                            .orgIdentifier(orgIdentifier)
                            .projectIdentifier(projectIdentifier)
                            .build())
        .cgBasicInfo(null)
        .build();
  }

  public static boolean containsExpressions(String str) {
    return ngPattern.matcher(str).find() || cgPattern.matcher(str).find();
  }

  public static NgEntityDetail getGitConnector(
      Map<CgEntityId, NGYamlFile> migratedEntities, GitFileConfig gitFileConfig) {
    CgEntityId cgEntityId =
        CgEntityId.builder().id(gitFileConfig.getConnectorId()).type(NGMigrationEntityType.CONNECTOR).build();
    if (!migratedEntities.containsKey(cgEntityId)) {
      log.error(String.format("Could not find GitConnector %s", gitFileConfig.getConnectorId()));
      return null;
    }
    return migratedEntities.get(cgEntityId).getNgEntityDetail();
  }

  public static String generateFileIdentifier(String fileName) {
    String prefix = fileName + ' ';
    return MigratorUtility.generateManifestIdentifier(prefix);
  }
}
