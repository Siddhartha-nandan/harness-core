/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.expressions;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ngmigration.dto.Flag.LONG_RELEASE_NAME;
import static io.harness.ngmigration.utils.MigratorUtility.isEnabled;
import static io.harness.reflection.CodeUtils.isHarnessClass;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.EncryptedData;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.NotExpression;
import io.harness.ngmigration.beans.InputDefaults;
import io.harness.ngmigration.beans.MigExpressionOverrides;
import io.harness.ngmigration.beans.MigrationContext;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.utils.CaseFormat;
import io.harness.ngmigration.utils.MigratorUtility;

import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.template.Template;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.NGMigrationEntityType;

import com.google.inject.Singleton;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
@Slf4j
@Singleton
public class MigratorExpressionUtils {
  private static final int MAX_DEPTH = 32;
  private static final int MAX_RECURSION_DEPTH = 250;

  public static Object render(MigrationContext context, Object object, Map<String, Object> customExpressions) {
    return render(context, object, MigExpressionOverrides.builder().customExpressions(customExpressions).build());
  }

  public static Object render(MigrationContext context, Object object, MigExpressionOverrides overrides) {
    if (context.getInputDTO().getReplace().size() > 0) {
      replace(object, context.getInputDTO().getReplace());
    }
    try {
      Map<CgEntityId, CgEntityNode> cgEntities = context.getEntities();
      Map<CgEntityId, NGYamlFile> migratedEntities = context.getMigratedEntities();
      // Generate the secret map
      Map<String, String> secretRefMap = new HashMap<>();
      if (isNotEmpty(cgEntities) && isNotEmpty(migratedEntities)) {
        Set<CgEntityId> secretIds = migratedEntities.keySet()
                                        .stream()
                                        .filter(cgEntityId -> NGMigrationEntityType.SECRET.equals(cgEntityId.getType()))
                                        .filter(cgEntities::containsKey)
                                        .collect(Collectors.toSet());
        for (CgEntityId secretId : secretIds) {
          EncryptedData encryptedData = (EncryptedData) cgEntities.get(secretId).getEntity();
          NGYamlFile ngYamlFile = migratedEntities.get(secretId);
          if (StringUtils.isNotBlank(encryptedData.getName())) {
            secretRefMap.put(
                encryptedData.getName(), MigratorUtility.getIdentifierWithScope(ngYamlFile.getNgEntityDetail()));
          }
        }
      }

      Map<String, Object> ctx = prepareContextMap(context, secretRefMap, overrides);
      return ExpressionEvaluatorUtils.updateExpressions(object, new MigratorResolveFunctor(ctx));
    } catch (Exception e) {
      log.error("There was an error rendering the expressions", e);
      return object;
    }
  }

  @NotNull
  static Map<String, Object> prepareContextMap(
      MigrationContext migrationContext, Map<String, String> secretRefMap, MigExpressionOverrides overrides) {
    boolean asPipelineVariables = overrides != null && overrides.isWorkflowVarsAsPipeline();
    CaseFormat identifierCaseFormat = getCaseFormat(migrationContext);
    Map<String, Object> context = new HashMap<>();

    context.put("deploymentTriggeredBy", "<+pipeline.triggeredBy.name>");
    context.put("currentStep.name", "<+step.name>");
    context.put("deploymentUrl", "<+pipeline.execution.url>");

    // Infra Expressions
    context.put("infra.kubernetes.namespace", "<+infra.namespace>");

    if (isEnabled(LONG_RELEASE_NAME)) {
      context.put("infra.kubernetes.infraId", "<+INFRA_KEY>");
    } else {
      context.put("infra.kubernetes.infraId", "<+INFRA_KEY_SHORT_ID>");
    }

    context.put("infra.helm.releaseName", "<+infra.releaseName>");
    context.put("infra.helm.shortId", "<+INFRA_KEY_SHORT_ID>");
    context.put("infra.name", "<+infra.name>");
    context.put("infra.cloudProvider.name", "<+infra.connectorRef>");

    // Env Expressions
    context.put("env.name", "<+env.name>");
    context.put("env.description", "<+env.description>");
    context.put("env.environmentType", "<+env.type>");
    context.put("env.uuid", "<+env.identifier>");
    context.put("env.accountId", "<+account.identifier>");

    // Service Expressions
    context.put("service.name", "<+service.name>");
    context.put("service.Name", "<+service.name>");
    context.put("Service.name", "<+service.name>");
    context.put("service.tag", "<+service.tags>");
    context.put("service.uuid", "<+service.identifier>");
    context.put("service.description", "<+service.description>");
    context.put("service.accountId", "<+account.identifier>");
    context.put("service.manifest", "<+manifest.name>");
    context.put("service.manifest.repoRoot", "<+manifest.repoName>");

    Map<String, String> artifactExpressions = new HashMap<>();
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.image", "<+ARTIFACT_PLACEHOLDER.image>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.tag", "<+ARTIFACT_PLACEHOLDER.tag>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.source.dockerconfig", "<+ARTIFACT_PLACEHOLDER.imagePullSecret>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.fileName", "<+ARTIFACT_PLACEHOLDER.metadata.fileName>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.format", "<+ARTIFACT_PLACEHOLDER.repositoryFormat>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.getSHA()", "<+ARTIFACT_PLACEHOLDER.metadata.SHA>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.groupId", "<+ARTIFACT_PLACEHOLDER.groupId>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.package", "<+ARTIFACT_PLACEHOLDER.metadata.package>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.region", "<+ARTIFACT_PLACEHOLDER.metadata.region>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.chartName", "<+ARTIFACT_PLACEHOLDER.metadata.chartName>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.repository", "<+ARTIFACT_PLACEHOLDER.repository>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.repositoryName", "<+ARTIFACT_PLACEHOLDER.repositoryName>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.url", "<+ARTIFACT_PLACEHOLDER.url>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.URL", "<+ARTIFACT_PLACEHOLDER.URL>");
    artifactExpressions.put(
        "ARTIFACT_PLACEHOLDER.metadata.artifactFileName", "<+ARTIFACT_PLACEHOLDER.metadata.fileName>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.buildFullDisplayName", "<+ARTIFACT_PLACEHOLDER.uiDisplayName>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.displayName", "<+ARTIFACT_PLACEHOLDER.displayName>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.artifactId", "<+ARTIFACT_PLACEHOLDER.metadata.artifactId>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.metadata.version", "<+ARTIFACT_PLACEHOLDER.metadata.version>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.revision", "<+ARTIFACT_PLACEHOLDER.tag>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.buildNo", "<+ARTIFACT_PLACEHOLDER.tag>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.source.registryUrl", "<+ARTIFACT_PLACEHOLDER.registryUrl>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.URL", "<+ARTIFACT_PLACEHOLDER.url>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.url", "<+ARTIFACT_PLACEHOLDER.metadata.url>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.artifactPath", "<+ARTIFACT_PLACEHOLDER.artifactPath>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.fileName", "<+ARTIFACT_PLACEHOLDER.metadata.fileName>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.key", "<+artifact.metadata.key>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.bucketName", "<+ARTIFACT_PLACEHOLDER.metadata.bucketName>");
    artifactExpressions.put("ARTIFACT_PLACEHOLDER.source.repositoryName", "<+ARTIFACT_PLACEHOLDER.imagePath>");

    artifactExpressions.forEach((k, v) -> {
      // Artifact Expressions
      context.put(k.replace("ARTIFACT_PLACEHOLDER", "artifact"), v.replace("ARTIFACT_PLACEHOLDER", "artifact"));
      // Rollback Artifact Expressions
      context.put(
          k.replace("ARTIFACT_PLACEHOLDER", "rollbackArtifact"), v.replace("ARTIFACT_PLACEHOLDER", "rollbackArtifact"));
    });

    context.put("artifact.metadata.s3_bucket", "<+artifact.metadata.get(\"s3_bucket\")>");
    context.put("artifact.metadata.s3_key", "<+artifact.metadata.get(\"s3_key\")>");
    context.put("artifact.metadata.s3_secret", "<+artifact.metadata.get(\"s3_secret\")>");

    context.put("artifact.label", new ArtifactLabelMigratorFunctor());
    context.put("rollbackArtifact.label", new ArtifactLabelMigratorFunctor());
    context.put("artifact.metadata.getSHA()", "artifact.metadata.SHA");

    // Application Expressions
    context.put("app.name", "<+project.name>");
    context.put("app.description", "<+project.description>");
    context.put("app.accountId", "<+account.identifier>");
    context.put("pipeline.name", "<+pipeline.name>");
    context.put("pipeline.description", "<+pipeline.description>");
    context.put("workflow.name", "<+stage.name>");
    context.put("workflow.description", "<+stage.description>");
    context.put("workflow.releaseNo", "<+pipeline.sequenceId>");
    context.put("workflow.pipelineResumeUuid", "<+pipeline.executionId>");
    context.put("workflow.pipelineDeploymentUuid", "<+pipeline.executionId>");
    context.put("workflow.startTs", "<+pipeline.startTs>");

    // Variables
    context.put("workflow.variables",
        asPipelineVariables ? new PipelineVariablesMigratorFunctor() : new WorkflowVariablesMigratorFunctor());
    context.put("pipeline.variables", new PipelineVariablesMigratorFunctor());
    context.put("serviceVariable",
        new ServiceVariablesMigratorFunctor(extractContextFromOverrides("serviceVariable", migrationContext)));
    context.put("serviceVariables",
        new ServiceVariablesMigratorFunctor(extractContextFromOverrides("serviceVariables", migrationContext)));
    context.put("service.variables",
        new ServiceVariablesMigratorFunctor(extractContextFromOverrides("serviceVariables", migrationContext)));
    context.put("servicevariable",
        new ServiceVariablesMigratorFunctor(extractContextFromOverrides("servicevariable", migrationContext)));
    context.put("environmentVariable", new EnvVariablesMigratorFunctor());
    context.put("environmentVariables", new EnvVariablesMigratorFunctor());
    context.put("configFile", new ConfigFileMigratorFunctor(identifierCaseFormat));

    // Secrets
    context.put(
        "secrets", new SecretMigratorFunctor(secretRefMap, identifierCaseFormat, getSecretScope(migrationContext)));

    // App
    context.put("app.defaults", new AppVariablesMigratorFunctor(identifierCaseFormat));

    // Account
    context.put("account.defaults", new AccountVariablesMigratorFunctor(identifierCaseFormat));

    // Http Step
    context.put("httpResponseCode", "<+httpResponseCode>");
    context.put("httpResponseBody", "<+httpResponseBody>");
    context.put("httpMethod", "<+httpMethod>");
    context.put("httpUrl", "<+httpUrl>");

    // Instance
    context.put("instance.hostName", "<+instance.hostName>");
    context.put("instance.name", "<+instance.name>");
    context.put("instance.host.hostName", "<+instance.host.hostName>");
    context.put("instance.host.ip", "<+instance.host.privateIp>");

    // PCF
    context.put("pcf.finalRoutes", "<+pcf.finalRoutes>");
    context.put("pcf.oldAppRoutes", "<+pcf.oldAppRoutes>");
    context.put("pcf.tempRoutes", "<+pcf.tempRoutes>");
    context.put("pcf.newAppRoutes", "<+pcf.newAppRoutes>");
    context.put("pcf.newAppRoutes[0]", "<+pcf.newAppRoutes[0]>");
    context.put("pcf.newAppName", "<+pcf.newAppName>");
    context.put("pcf.newAppGuid", "<+pcf.newAppGuid>");
    context.put("pcf.oldAppName", "<+pcf.oldAppName>");
    context.put("pcf.activeAppName", "<+pcf.activeAppName>");
    context.put("pcf.inActiveAppName", "<+pcf.inActiveAppName>");
    context.put("pcf.oldAppGuid", "<+pcf.oldAppGuid>");
    context.put("pcf.oldAppRoutes[0]", "<+pcf.oldAppRoutes[0]>");
    context.put("infra.pcf.cloudProvider.name", "<+infra.connector.name>");
    context.put("infra.pcf.organization", "<+infra.organization>");
    context.put("infra.pcf.space", "<+infra.space>");
    context.put("host.pcfElement.applicationId", "<+pcf.newAppGuid>");
    context.put("host.pcfElement.displayName", "<+pcf.newAppName>");

    context.put("BACKUP_PATH", "<+variable.backupPath>");
    context.put("RUNTIME_PATH", "<+variable.runtimePath>");
    context.put("STAGING_PATH", "<+variable.stagingPath>");
    context.put("WINGS_BACKUP_PATH", "<+variable.backupPath>");
    context.put("WINGS_RUNTIME_PATH", "<+variable.runtimePath>");
    context.put("WINGS_STAGING_PATH", "<+variable.stagingPath>");

    if (overrides != null && isNotEmpty(overrides.getCustomExpressions())) {
      context.putAll(overrides.getCustomExpressions());
    }

    if (migrationContext.getInputDTO().getReplace() != null
        && isNotEmpty(migrationContext.getInputDTO().getReplace())) {
      context.putAll(migrationContext.getInputDTO().getReplace());
    }

    if (migrationContext.getInputDTO() != null && isNotEmpty(migrationContext.getInputDTO().getCustomExpressions())) {
      context.putAll(migrationContext.getInputDTO().getCustomExpressions());
    }

    return context;
  }

  private static Map<String, String> extractContextFromOverrides(String key, MigrationContext migrationContext) {
    Map<String, String> result = new HashMap<>();
    if (migrationContext.getInputDTO() != null && isNotEmpty(migrationContext.getInputDTO().getCustomExpressions())) {
      Map<String, Object> customExpressions = migrationContext.getInputDTO().getCustomExpressions();
      customExpressions.forEach((k, v) -> {
        try {
          if (k.startsWith(key + ".") && v instanceof String) {
            result.put(k.substring(key.length() + 1), (String) v);
          }
        } catch (Exception e) {
          log.error("Failed to extract service context", e);
        }
      });
    }
    return result;
  }

  public static Set<String> extractAll(String source) {
    /*
      Should start with `${`
      Should end with `}`
      Between the curly braces it can match any word character(a-z, A-Z, 0-9, _)
    */
    Set<String> allMatches = new HashSet<>();
    allMatches.addAll(extractAll(source, Pattern.compile("\\$\\{[\\w-.\"()]+}")));
    allMatches.addAll(extractAll(source, Pattern.compile("\\$\\{secrets.getValue\\([^{}]+\\)}")));
    return allMatches;
  }

  private static Set<String> extractAll(String source, Pattern compiled) {
    if (StringUtils.isBlank(source)) {
      return Collections.emptySet();
    }
    Set<String> matches = new HashSet<>();
    final Matcher matcher = compiled.matcher(source);
    while (matcher.find()) {
      String match = matcher.group();
      if (!match.isEmpty()) {
        matches.add(match);
      }
    }
    // We only want to return what is within the curly braces.
    // ${artifact.image} -> artifact.image
    return matches.stream().map(str -> str.substring(2, str.length() - 1)).collect(Collectors.toSet());
  }

  public static Set<String> getExpressions(Object object) {
    return getExpressions(object, MAX_DEPTH);
  }

  private static Set<String> getExpressions(Object object, int depth) {
    if (depth <= 0) {
      return Collections.emptySet();
    }
    if (object == null) {
      return Collections.emptySet();
    }
    // If it is number or boolean etc
    if (object.getClass().isPrimitive()) {
      return Collections.emptySet();
    }

    if (object instanceof String) {
      return extractAll((String) object);
    }

    Class c = object.getClass();
    if (c.isArray()) {
      if (c.getComponentType().isPrimitive()) {
        return Collections.emptySet();
      }
      int length = Array.getLength(object);
      Set<String> all = new HashSet<>();
      for (int i = 0; i < length; i++) {
        all.addAll(getExpressions(Array.get(object, i), depth - 1));
      }
      return all;
    }
    // Handle Maps
    if (object instanceof Map) {
      Map<Object, Object> map = (Map) object;
      return map.values().stream().flatMap(val -> getExpressions(val, depth - 1).stream()).collect(Collectors.toSet());
    }
    // Handle Lists
    if (object instanceof List) {
      List<Object> list = (List) object;
      return list.stream().flatMap(val -> getExpressions(val, depth - 1).stream()).collect(Collectors.toSet());
    }
    // Handle Sets
    if (object instanceof Set) {
      Set<Object> set = (Set) object;
      return set.stream().flatMap(val -> getExpressions(val, depth - 1).stream()).collect(Collectors.toSet());
    }

    Set<String> all = new HashSet<>();
    while (c.getSuperclass() != null) {
      for (Field f : c.getDeclaredFields()) {
        // Ignore field if skipPredicate returns true or if the field is static.
        if ((f.isAnnotationPresent(NotExpression.class)) || Modifier.isStatic(f.getModifiers())) {
          continue;
        }
        boolean isAccessible = f.isAccessible();
        f.setAccessible(true);
        try {
          Object obj = f.get(object);
          all.addAll(getExpressions(obj, depth - 1));
        } catch (IllegalAccessException ignored) {
          log.warn("Expressions Field [{}] is not accessible", f.getName());
        }
        f.setAccessible(isAccessible);
      }
      c = c.getSuperclass();
    }
    return all;
  }

  private static Scope getSecretScope(MigrationContext context) {
    Scope scope = Scope.PROJECT;
    if (context == null || context.getInputDTO() == null
        || EmptyPredicate.isEmpty(context.getInputDTO().getDefaults())) {
      return scope;
    }
    InputDefaults inputDefaults = context.getInputDTO().getDefaults().getOrDefault(NGMigrationEntityType.SECRET, null);
    if (inputDefaults == null) {
      return scope;
    }
    return inputDefaults.getScope() == null ? scope : inputDefaults.getScope();
  }

  private static CaseFormat getCaseFormat(MigrationContext context) {
    if (context == null || context.getInputDTO() == null || context.getInputDTO().getIdentifierCaseFormat() == null) {
      return CaseFormat.CAMEL_CASE;
    }
    return context.getInputDTO().getIdentifierCaseFormat();
  }

  public static void replace(Object object, Map<String, Object> replace) {
    if (replace.size() == 0) {
      return;
    }

    try {
      replaceStringInObject(object, replace, 0);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static void replaceStringInObject(Object obj, Map<String, Object> replace, int depth)
      throws IllegalAccessException {
    if (depth >= MAX_RECURSION_DEPTH) {
      return; // Stop recursion if maximum depth is reached
    }

    if (obj == null) {
      return;
    }

    Class<?> clazz = obj.getClass();

    // For non-collection objects and strings
    if ((isHarnessClass(clazz)) && !clazz.isArray() && !clazz.isPrimitive() && !clazz.isEnum()
        && !(obj instanceof CharSequence)) {
      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields) {
        try {
          field.setAccessible(true);
          Object fieldValue = field.get(obj);
          if (fieldValue instanceof String) {
            String strValue = (String) fieldValue;
            for (Map.Entry<String, Object> entry : replace.entrySet()) {
              strValue = strValue.replace(entry.getKey(), entry.getValue().toString());
              field.set(obj, strValue);
            }
          } else {
            replaceStringInObject(fieldValue, replace, depth + 1); // Recursively check non-string fields
          }
        } catch (Exception e) {
          // Handle the exception (e.g., log it or take appropriate action)
          e.printStackTrace(); // Example: print the stack trace
        }
      }
    } else if (obj instanceof Set) {
      Set<Object> set = (Set<Object>) obj;
      Set<Object> elementsToRemove = new HashSet<>();

      for (Object element : set) {
        if (element instanceof String) {
          String strValue = (String) element;
          for (Map.Entry<String, Object> entryMap : replace.entrySet()) {
            strValue = strValue.replace(entryMap.getKey(), entryMap.getValue().toString());
          }
          elementsToRemove.add(element);
          elementsToRemove.add(strValue);
        } else if (element instanceof Map) {
          replaceStringInObject(element, replace, depth + 1);
        } else {
          replaceStringInObject(element, replace, depth + 1); // Recursively check other types of elements
        }
      }

      // Remove old elements from the set
      set.removeAll(elementsToRemove);

      // Add new elements to the set
      set.addAll(elementsToRemove);
    } else if (obj instanceof List) {
      List<Object> list = (List<Object>) obj;
      List<Object> elementsToRemove = new ArrayList<>();

      for (int i = 0; i < list.size(); i++) {
        Object element = list.get(i);

        if (element instanceof String) {
          String strValue = (String) element;
          for (Map.Entry<String, Object> entryMap : replace.entrySet()) {
            strValue = strValue.replace(entryMap.getKey(), entryMap.getValue().toString());
          }
          elementsToRemove.add(element);
          elementsToRemove.add(strValue);
        } else if (element instanceof Map) {
          replaceStringInObject(element, replace, depth + 1);
        } else {
          replaceStringInObject(element, replace, depth + 1); // Recursively check other types of elements
        }
      }

      // Remove old elements from the list
      list.removeAll(elementsToRemove);

      // Add new elements to the list
      list.addAll(elementsToRemove);
    } else if (obj instanceof Map) {
      Map<Object, Object> map = (Map<Object, Object>) obj;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getValue() instanceof String) {
          String strValue = (String) entry.getValue();
          for (Map.Entry<String, Object> entryMap : replace.entrySet()) {
            strValue = strValue.replace(entryMap.getKey(), entryMap.getValue().toString());
          }
          map.put(entry.getKey(), strValue);
        } else {
          replaceStringInObject(entry.getKey(), replace, depth + 1); // Recursively check map keys
          replaceStringInObject(entry.getValue(), replace, depth + 1); // Recursively check map values
        }
      }
    }
  }
}
