/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import io.harness.repositories.EnforcementResultRepo;
import io.harness.repositories.SBOMComponentRepo;
import io.harness.serializer.JsonUtils;
import io.harness.spec.server.ssca.v1.model.Artifact;
import io.harness.spec.server.ssca.v1.model.EnforceSbomRequestBody;
import io.harness.spec.server.ssca.v1.model.EnforceSbomResponseBody;
import io.harness.spec.server.ssca.v1.model.EnforcementSummaryResponse;
import io.harness.spec.server.ssca.v1.model.PolicyViolation;
import io.harness.ssca.enforcement.ExecutorRegistry;
import io.harness.ssca.entities.ArtifactEntity;
import io.harness.ssca.entities.EnforcementResultEntity;
import io.harness.ssca.entities.EnforcementSummaryEntity;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;

import com.google.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
public class EnforcementStepServiceImpl implements EnforcementStepService {
  @Inject ArtifactService artifactService;
  @Inject ExecutorRegistry executorRegistry;
  @Inject RuleEngineService ruleEngineService;
  @Inject EnforcementSummaryService enforcementSummaryService;
  @Inject EnforcementResultService enforcementResultService;
  @Inject SBOMComponentRepo sbomComponentRepo;
  @Inject EnforcementResultRepo enforcementResultRepo;

  @Override
  public EnforceSbomResponseBody enforceSbom(
      String accountId, String orgIdentifier, String projectIdentifier, EnforceSbomRequestBody body) {
    String artifactId =
        artifactService.generateArtifactId(body.getArtifact().getRegistryUrl(), body.getArtifact().getName());
    ArtifactEntity artifactEntity =
        artifactService
            .getArtifact(accountId, orgIdentifier, projectIdentifier, artifactId,
                Sort.by(ArtifactEntity.ArtifactEntityKeys.createdOn).descending())
            .orElseThrow(()
                             -> new NotFoundException(
                                 String.format("Artifact with image name [%s] and registry Url [%s] is not found",
                                     body.getArtifact().getName(), body.getArtifact().getRegistryUrl())));

    String regoPolicy = "package sbom\n"
        + "\n"
        + "import future.keywords.if\n"
        + "import future.keywords.in\n"
        + "\n"
        + "# Define a set of licenses that are denied\n"
        + "deny_list := fill_defaults([{\"name\": {\"value\": \"d.*\", \"operator\": \"~\"}},{\"name\": {\"value\": \"hyuiyuiuyuiuyuiuyui.*\", \"operator\": \"~\"}}])\n"
        + "\n"
        + "# Define a set of licenses that are allowed\n"
        + "allow_list := {\n"
        + "\t\"licenses\": [\n"
        + "\t\t{\"license\": {\n"
        + "\t\t\t\"value\": \"GPL-29999.0-only\",\n"
        + "\t\t\t\"operator\": \"==\",\n"
        + "\t\t}},\n"
        + "\t\t{\"license\": {\n"
        + "\t\t\t\"value\": \"GPL-299.0-only\",\n"
        + "\t\t\t\"operator\": \"==\",\n"
        + "\t\t}},\n"
        + "\t],\n"
        + "\t\"purls\": [{\"purl\": {\n"
        + "\t\t\"value\": \"golang\",\n"
        + "\t\t\"operator\": \"~\",\n"
        + "\t}}],\n"
        + "}\n"
        + "\n"
        + "# deny_rule_violations(deny_rule) :=violating_packages {\n"
        + "# \tsome pkg in input\n"
        + "# \tviolating_packages := [x |\n"
        + "# \t\tx := pkg.uuid\n"
        + "# \t\tdeny_compare(pkg, deny_rule)\n"
        + "# \t]\n"
        + "# \tcount(violating_packages) > 0\n"
        + "# }\n"
        + "\n"
        + "deny_list_violations[violations] {\n"
        + "\tsome deny_rule in deny_list\n"
        + "\tviolations := [x |\n"
        + "\t\tx := {\n"
        + "\t\t\t\"type\": \"deny\",\n"
        + "\t\t\t\"rule\": deny_rule,\n"
        + "\t\t\t\"violations\": [violating_id |\n"
        + "\t\t\t\tsome pkg in input\n"
        + "\t\t\t\tviolating_id := pkg.uuid\n"
        + "\t\t\t\tdeny_compare(pkg, deny_rule)\n"
        + "\t\t\t],\n"
        + "\t\t}\n"
        + "\t]\n"
        + "\tcount(violations) > 0\n"
        + "}\n"
        + "\n"
        + "does_violate_license(pkg, rule) if {\n"
        + "\tlicense_match := [x |\n"
        + "\t\tx := true\n"
        + "\t\tsome license, package_license in pkg.packageLicense\n"
        + "\t\tstr_compare(package_license, rule.license.operator, rule.license.value)\n"
        + "\t]\n"
        + "\tcount(license_match) == 0\n"
        + "}\n"
        + "\n"
        + "does_violate_purl(pkg, rule) if {\n"
        + "\tnot str_compare(pkg.purl, rule.purl.operator, rule.purl.value)\n"
        + "}\n"
        + "\n"
        + "is_empty(myList) if count(myList) == 0\n"
        + "\n"
        + "allow_rules_licenses_violations(allow_rules_licenses) := violating_packages if {\n"
        + "\tviolating_packages := {result |\n"
        + "\t\tsome pkg in input\n"
        + "\t\tsome allow_rules_license in allow_rules_licenses\n"
        + "\t\tdoes_violate_license(pkg, allow_rules_license)\n"
        + "\t\tresult = pkg.uuid\n"
        + "\t}\n"
        + "\tcount(violating_packages) > 0\n"
        + "}\n"
        + "\n"
        + "allow_rules_purls_violations(allow_rules_purls) := violating_packages if {\n"
        + "\tviolating_packages := {result |\n"
        + "\t\tsome pkg in input\n"
        + "\t\tsome allow_rules_purl in allow_rules_purls\n"
        + "\t\tdoes_violate_purl(pkg, allow_rules_purl)\n"
        + "\t\tresult = pkg.uuid\n"
        + "\t}\n"
        + "\tcount(violating_packages) > 0\n"
        + "}\n"
        + "\n"
        + "allow_list_violations[violations] {\n"
        + "\tallow_rules_licenses := object.get(allow_list, \"licenses\", [])\n"
        + "\tcount(allow_rules_licenses) > 0\n"
        + "\tviolations := [x |\n"
        + "\t\tx := {\n"
        + "\t\t\t\"type\": \"allow\",\n"
        + "\t\t\t\"rule\": allow_rules_licenses,\n"
        + "\t\t\t\"violations\": allow_rules_licenses_violations(allow_rules_licenses),\n"
        + "\t\t}\n"
        + "\t]\n"
        + "\tcount(violations) > 0\n"
        + "}\n"
        + "\n"
        + "allow_list_violations[violations] {\n"
        + "\tallow_rules_purls := object.get(allow_list, \"purls\", [])\n"
        + "\tcount(allow_rules_purls) > 0\n"
        + "\tviolations := [x |\n"
        + "\t\tx := {\n"
        + "\t\t\t\"type\": \"allow\",\n"
        + "\t\t\t\"rule\": allow_rules_purls,\n"
        + "\t\t\t\"violations\": allow_rules_purls_violations(allow_rules_purls),\n"
        + "\t\t}\n"
        + "\t]\n"
        + "\tcount(violations) > 0\n"
        + "}\n"
        + "\n"
        + "deny_compare(pkg, rule) if {\n"
        + "\tlicense_match := [x |\n"
        + "\t\tx := true\n"
        + "\t\tsome license, package_license in pkg.packageLicense\n"
        + "\t\tstr_compare(package_license, rule.license.operator, rule.license.value)\n"
        + "\t]\n"
        + "\tcount(license_match) != 0\n"
        + "\tstr_compare(pkg.packageName, rule.name.operator, rule.name.value)\n"
        + "\tstr_compare(pkg.purl, rule.purl.operator, rule.purl.value)\n"
        + "\tsemver_compare(pkg.packageVersion, rule.version.operator, rule.version.value)\n"
        + "}\n"
        + "\n"
        + "str_compare(a, \"==\", b) := a == b\n"
        + "\n"
        + "str_compare(a, \"!\", b) := a != b\n"
        + "\n"
        + "str_compare(a, \"~\", b) := regex.match(b, a)\n"
        + "\n"
        + "str_compare(a, null, b) := a == b if b != null\n"
        + "\n"
        + "str_compare(a, null, null) := true\n"
        + "\n"
        + "semver_compare(a, \"<=\", b) := semver.compare(b, a) <= 0\n"
        + "\n"
        + "semver_compare(a, \"<\", b) := semver.compare(b, a) < 0\n"
        + "\n"
        + "semver_compare(a, \"==\", b) := semver.compare(b, a) == 0\n"
        + "\n"
        + "semver_compare(a, \">\", b) := semver.compare(b, a) > 0\n"
        + "\n"
        + "semver_compare(a, \">=\", b) := semver.compare(b, a) >= 0\n"
        + "\n"
        + "semver_compare(a, \"!\", b) := semver.compare(b, a) == 0\n"
        + "\n"
        + "semver_compare(a, \"~\", b) := regex.match(b, a)\n"
        + "\n"
        + "semver_compare(a, null, b) := semver.compare(b, a) == 0 if b != null\n"
        + "\n"
        + "semver_compare(a, null, null) := true\n"
        + "\n"
        + "remove_null(obj) := {x |\n"
        + "\tsome x in obj\n"
        + "\tx.value != null\n"
        + "}\n"
        + "\n"
        + "fill_default_allow_rules(obj) := standard_obj if {\n"
        + "\tdefaults := {\n"
        + "\t\t\"licenses\": [],\n"
        + "\t\t\"suppliers\": [],\n"
        + "\t\t\"purls\": [],\n"
        + "\t}\n"
        + "\tstandard_obj := object.union(defaults, obj[_])\n"
        + "}\n"
        + "\n"
        + "fill_defaults(obj) := list if {\n"
        + "\tdefaults := {\n"
        + "\t\t\"name\": {\"value\": null, \"operator\": null},\n"
        + "\t\t\"license\": {\"value\": null, \"operator\": null},\n"
        + "\t\t\"version\": {\"value\": null, \"operator\": null},\n"
        + "\t\t\"supplier\": {\"value\": null, \"operator\": null},\n"
        + "\t\t\"purl\": {\"value\": null, \"operator\": null},\n"
        + "\t}\n"
        + "\tlist := [x | x := object.union(defaults, obj[_])]\n"
        + "}\n";
    //        ruleEngineService.getPolicy(accountId, orgIdentifier, projectIdentifier, body.getPolicyFileId());
    log.info("RegoPolicyLength {}", regoPolicy.length());
    Page<NormalizedSBOMComponentEntity> entities =
        sbomComponentRepo.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndOrchestrationId(accountId,
            orgIdentifier, projectIdentifier, artifactEntity.getOrchestrationId(),
            PageRequest.of(0, Integer.MAX_VALUE));
    log.info("NumberOfEntities {}", entities.getTotalElements());
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put("rego", regoPolicy);

    List<NormalizedSBOMComponentEntity> s = entities.get().collect(Collectors.toList());
    Map<String, NormalizedSBOMComponentEntity> artifactMap =
        s.stream().collect(Collectors.toMap(NormalizedSBOMComponentEntity::getUuid, en -> en, (u, v) -> v));
    requestMap.put("input", s);
    String requestBody = JsonUtils.asJson(requestMap);
    log.info("RequestBody {}", requestBody);
    log.info("RequestBody size{}", requestBody.length());
    String response = getHttpResponse(requestBody);
    log.info("Response {}", response);
    Map<String, Object> responseMap1 = JsonUtils.asMap(response);
    List<Map<String, Object>> outputMapList = (List<Map<String, Object>>) responseMap1.get("output");
    List<Map<String, Object>> expressionsMapList =
        (List<Map<String, Object>>) ((Map<String, Object>) outputMapList.get(0)).get("expressions");
    Map<String, Object> expressionsMap = (Map<String, Object>) (expressionsMapList.get(0));
    Map<String, Object> valueMap = (Map<String, Object>) expressionsMap.get("value");
    Map<String, Object> sbomMap = (Map<String, Object>) valueMap.get("sbom");
    List<List<Violation>> allowListViolationsList = (List<List<Violation>>) sbomMap.get("allow_list_violations");
    List<List<Violation>> denyListViolationsList = (List<List<Violation>>) sbomMap.get("deny_list_violations");
    List<EnforcementResultEntity> denyListResult = new ArrayList<>();
    List<EnforcementResultEntity> allowListResult = new ArrayList<>();
    for (List violations : allowListViolationsList) {
      Map<String, Object> currentViolation = (Map<String, Object>) violations.get(0);
      if (CollectionUtils.isEmpty((List<String>) currentViolation.get("violations"))) {
        continue;
      }
      Violation current = getViolation(currentViolation);
      current.violations.forEach(uuid -> {
        EnforcementResultEntity currentResult = EnforcementResultEntity.builder()
                                                    .artifactId(artifactMap.get(uuid).getArtifactId())
                                                    .enforcementID(body.getEnforcementId())
                                                    .accountId(accountId)
                                                    .projectIdentifier(projectIdentifier)
                                                    .orgIdentifier(orgIdentifier)
                                                    .imageName(artifactMap.get(uuid).getPackageName())
                                                    .supplierType(artifactMap.get(uuid).getPackageSourceInfo())
                                                    .supplier(artifactMap.get(uuid).getPackageSourceInfo())
                                                    .name(artifactMap.get(uuid).getPackageName())
                                                    .packageManager(artifactMap.get(uuid).getPackageManager())
                                                    .purl(artifactMap.get(uuid).getPurl())
                                                    .license(artifactMap.get(uuid).getPackageLicense())
                                                    .violationType(current.type)
                                                    .violationDetails(current.rule.toString())
                                                    .build();
        allowListResult.add(currentResult);
      });
    }
    for (List<Violation> violations : denyListViolationsList) {
      Violation current = getViolation((Map<String, Object>) violations.get(0));
      current.violations.forEach(uuid -> {
        EnforcementResultEntity currentResult = EnforcementResultEntity.builder()
                                                    .artifactId(artifactMap.get(uuid).getArtifactId())
                                                    .enforcementID(body.getEnforcementId())
                                                    .projectIdentifier(projectIdentifier)
                                                    .orgIdentifier(orgIdentifier)
                                                    .imageName(artifactMap.get(uuid).getPackageName())
                                                    .supplierType(artifactMap.get(uuid).getPackageSourceInfo())
                                                    .supplier(artifactMap.get(uuid).getPackageSourceInfo())
                                                    .name(artifactMap.get(uuid).getPackageName())
                                                    .packageManager(artifactMap.get(uuid).getPackageManager())
                                                    .purl(artifactMap.get(uuid).getPurl())
                                                    .license(artifactMap.get(uuid).getPackageLicense())
                                                    .accountId(accountId)
                                                    .violationType(current.type)
                                                    .violationDetails(current.rule.toString())
                                                    .build();
        denyListResult.add(currentResult);
      });
    }

    enforcementResultRepo.saveAll(allowListResult);
    enforcementResultRepo.saveAll(denyListResult);
    String status = enforcementSummaryService.persistEnforcementSummary(
        body.getEnforcementId(), denyListResult, allowListResult, artifactEntity, body.getPipelineExecutionId());

    EnforceSbomResponseBody responseBody = new EnforceSbomResponseBody();
    responseBody.setEnforcementId(body.getEnforcementId());
    responseBody.setStatus(status);

    return responseBody;
  }

  @Override
  public EnforcementSummaryResponse getEnforcementSummary(
      String accountId, String orgIdentifier, String projectIdentifier, String enforcementId) {
    EnforcementSummaryEntity enforcementSummary =
        enforcementSummaryService.getEnforcementSummary(accountId, orgIdentifier, projectIdentifier, enforcementId)
            .orElseThrow(()
                             -> new NotFoundException(String.format(
                                 "Enforcement with enforcementIdentifier [%s] is not found", enforcementId)));

    return new EnforcementSummaryResponse()
        .enforcementId(enforcementSummary.getEnforcementId())
        .artifact(new Artifact()
                      .id(enforcementSummary.getArtifact().getArtifactId())
                      .name(enforcementSummary.getArtifact().getName())
                      .type(enforcementSummary.getArtifact().getType())
                      .registryUrl(enforcementSummary.getArtifact().getUrl())
                      .tag(enforcementSummary.getArtifact().getTag())

                )
        .allowListViolationCount(enforcementSummary.getAllowListViolationCount())
        .denyListViolationCount(enforcementSummary.getDenyListViolationCount())
        .status(enforcementSummary.getStatus());
  }

  @Override
  public Page<PolicyViolation> getPolicyViolations(String accountId, String orgIdentifier, String projectIdentifier,
      String enforcementId, String searchText, Pageable pageable) {
    return enforcementResultService
        .getPolicyViolations(accountId, orgIdentifier, projectIdentifier, enforcementId, searchText, pageable)
        .map(enforcementResultEntity
            -> new PolicyViolation()
                   .enforcementId(enforcementResultEntity.getEnforcementID())
                   .account(enforcementResultEntity.getAccountId())
                   .org(enforcementResultEntity.getOrgIdentifier())
                   .project(enforcementResultEntity.getProjectIdentifier())
                   .artifactId(enforcementResultEntity.getArtifactId())
                   .imageName(enforcementResultEntity.getImageName())
                   .purl(enforcementResultEntity.getPurl())
                   .orchestrationId(enforcementResultEntity.getOrchestrationID())
                   .license(enforcementResultEntity.getLicense())
                   .tag(enforcementResultEntity.getTag())
                   .supplier(enforcementResultEntity.getSupplier())
                   .supplierType(enforcementResultEntity.getSupplierType())
                   .name(enforcementResultEntity.getName())
                   .version(enforcementResultEntity.getVersion())
                   .packageManager(enforcementResultEntity.getPackageManager())
                   .violationType(enforcementResultEntity.getViolationType())
                   .violationDetails(enforcementResultEntity.getViolationDetails()));
  }

  private String getHttpResponse(String requestPayload) {
    try {
      String base64EncodedKey = System.getenv("HARNESS_API_TOKEN");
      String harnessAPIKey = new String(Base64.getDecoder().decode(base64EncodedKey));
      HttpRequest request2 =
          HttpRequest.newBuilder()
              .uri(new URI(
                  "https://qa.harness.io/gateway/pm/api/v1/evaluate?accountIdentifier=-k53qRQAQ1O7DBLb9ACnjQ&orgIdentifier=default&projectIdentifier=CloudWatch"))
              .header("content-type", "application/json")
              .header("accept", "application/json")
              .header("X-API-KEY", harnessAPIKey)
              .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
              .build();
      Instant start = Instant.now();
      HttpResponse<String> response =
          HttpClient.newBuilder().build().send(request2, HttpResponse.BodyHandlers.ofString());
      log.info("Duration {} content {}", Duration.between(start, Instant.now()).toMillis(),
          request2.bodyPublisher().get().contentLength());
      return response.body();
    } catch (Exception ex) {
    }
    return null;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  public static class Violation {
    List<String> violations;
    Object rule;
    String type;
  }

  private static Violation getViolation(Map<String, Object> map) {
    return Violation.builder()
        .violations((List<String>) map.get("violations"))
        .type((String) map.get("type"))
        .rule(map.get("rule"))
        .build();
  }
}
