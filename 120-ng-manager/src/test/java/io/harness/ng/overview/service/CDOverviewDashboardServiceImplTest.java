package io.harness.ng.overview.service;

import static io.harness.rule.OwnerRule.ABHISHEK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.NgManagerTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.models.ActiveServiceInstanceInfoV2;
import io.harness.ng.overview.dto.InstanceGroupedByServiceList;
import io.harness.rule.Owner;
import io.harness.service.instancedashboardservice.InstanceDashboardServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@OwnedBy(HarnessTeam.CDC)
public class CDOverviewDashboardServiceImplTest extends NgManagerTestBase {
  @InjectMocks private CDOverviewDashboardServiceImpl cdOverviewDashboardService;
  @Mock private InstanceDashboardServiceImpl instanceDashboardService;

  InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution getSampleInstanceGroupedByPipelineExecution(
      String id, Long lastDeployedAt, int count, String name) {
    return new InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution(count, id, name, lastDeployedAt);
  }

  Map<String,
      Map<String,
          Map<String,
              Pair<Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>,
                  Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>>>>>
  getSampleServiceBuildEnvInfraMap() {
    Map<String,
        Map<String,
            Map<String,
                Pair<Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>,
                    Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>>>>>
        serviceBuildEnvInfraMap = new HashMap<>();

    Map<String,
        Map<String,
            Pair<Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>,
                Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>>>> buildEnvInfraMap =
        new HashMap<>();

    Map<String,
        Pair<Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>,
            Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>>> envInfraMap1 =
        new HashMap<>();
    Map<String,
        Pair<Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>,
            Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>>> envInfraMap2 =
        new HashMap<>();

    Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>> infraPipelineExecutionMap1 =
        new HashMap<>();
    Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>> infraPipelineExecutionMap2 =
        new HashMap<>();
    Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>> clusterPipelineExecutionMap1 =
        new HashMap<>();
    Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>> clusterPipelineExecutionMap2 =
        new HashMap<>();

    infraPipelineExecutionMap1.put("infra1", new ArrayList<>());
    infraPipelineExecutionMap1.get("infra1").add(getSampleInstanceGroupedByPipelineExecution("1", 1l, 1, "a"));
    infraPipelineExecutionMap1.get("infra1").add(getSampleInstanceGroupedByPipelineExecution("1", 2l, 2, "a"));
    infraPipelineExecutionMap1.get("infra1").add(getSampleInstanceGroupedByPipelineExecution("2", 1l, 1, "b"));
    infraPipelineExecutionMap1.put("infra2", new ArrayList<>());
    infraPipelineExecutionMap1.get("infra2").add(getSampleInstanceGroupedByPipelineExecution("1", 1l, 1, "a"));
    infraPipelineExecutionMap2.put("infra2", new ArrayList<>());
    infraPipelineExecutionMap2.get("infra2").add(getSampleInstanceGroupedByPipelineExecution("2", 1l, 1, "b"));

    clusterPipelineExecutionMap1.put("infra1", new ArrayList<>());
    clusterPipelineExecutionMap1.get("infra1").add(getSampleInstanceGroupedByPipelineExecution("1", 1l, 1, "a"));
    clusterPipelineExecutionMap1.get("infra1").add(getSampleInstanceGroupedByPipelineExecution("1", 2l, 2, "a"));
    clusterPipelineExecutionMap1.get("infra1").add(getSampleInstanceGroupedByPipelineExecution("2", 1l, 1, "b"));
    clusterPipelineExecutionMap1.put("infra2", new ArrayList<>());
    clusterPipelineExecutionMap1.get("infra2").add(getSampleInstanceGroupedByPipelineExecution("1", 1l, 1, "a"));
    clusterPipelineExecutionMap2.put("infra2", new ArrayList<>());
    clusterPipelineExecutionMap2.get("infra2").add(getSampleInstanceGroupedByPipelineExecution("2", 1l, 1, "b"));

    envInfraMap1.put("env1", new MutablePair<>(infraPipelineExecutionMap1, clusterPipelineExecutionMap1));
    envInfraMap2.put("env2", new MutablePair<>(infraPipelineExecutionMap2, clusterPipelineExecutionMap2));

    buildEnvInfraMap.put("1", envInfraMap1);
    buildEnvInfraMap.put("2", envInfraMap2);

    serviceBuildEnvInfraMap.put("svc1", buildEnvInfraMap);

    return serviceBuildEnvInfraMap;
  }

  List<InstanceGroupedByServiceList.InstanceGroupedByService> getSampleListInstanceGroupedByService() {
    InstanceGroupedByServiceList.InstanceGroupedByInfrastructure instanceGroupedByInfrastructure1 =
        InstanceGroupedByServiceList.InstanceGroupedByInfrastructure.builder()
            .infraIdentifier("infra1")
            .infraName("infra1")
            .instanceGroupedByPipelineExecutionList(
                Arrays.asList(getSampleInstanceGroupedByPipelineExecution("1", 2l, 3, "a"),
                    getSampleInstanceGroupedByPipelineExecution("2", 1l, 1, "b")))
            .build();
    InstanceGroupedByServiceList.InstanceGroupedByInfrastructure instanceGroupedByInfrastructure2 =
        InstanceGroupedByServiceList.InstanceGroupedByInfrastructure.builder()
            .infraIdentifier("infra2")
            .infraName("infra2")
            .instanceGroupedByPipelineExecutionList(
                Arrays.asList(getSampleInstanceGroupedByPipelineExecution("1", 1l, 1, "a")))
            .build();
    InstanceGroupedByServiceList.InstanceGroupedByInfrastructure instanceGroupedByInfrastructure3 =
        InstanceGroupedByServiceList.InstanceGroupedByInfrastructure.builder()
            .infraIdentifier("infra2")
            .infraName("infra2")
            .instanceGroupedByPipelineExecutionList(
                Arrays.asList(getSampleInstanceGroupedByPipelineExecution("2", 1l, 1, "b")))
            .build();
    InstanceGroupedByServiceList.InstanceGroupedByInfrastructure instanceGroupedByCluster1 =
        InstanceGroupedByServiceList.InstanceGroupedByInfrastructure.builder()
            .clusterIdentifier("infra1")
            .agentIdentifier("infra1")
            .instanceGroupedByPipelineExecutionList(
                Arrays.asList(getSampleInstanceGroupedByPipelineExecution("1", 2l, 3, "a"),
                    getSampleInstanceGroupedByPipelineExecution("2", 1l, 1, "b")))
            .build();
    InstanceGroupedByServiceList.InstanceGroupedByInfrastructure instanceGroupedByCluster2 =
        InstanceGroupedByServiceList.InstanceGroupedByInfrastructure.builder()
            .clusterIdentifier("infra2")
            .agentIdentifier("infra2")
            .instanceGroupedByPipelineExecutionList(
                Arrays.asList(getSampleInstanceGroupedByPipelineExecution("1", 1l, 1, "a")))
            .build();
    InstanceGroupedByServiceList.InstanceGroupedByInfrastructure instanceGroupedByCluster3 =
        InstanceGroupedByServiceList.InstanceGroupedByInfrastructure.builder()
            .clusterIdentifier("infra2")
            .agentIdentifier("infra2")
            .instanceGroupedByPipelineExecutionList(
                Arrays.asList(getSampleInstanceGroupedByPipelineExecution("2", 1l, 1, "b")))
            .build();

    InstanceGroupedByServiceList.InstanceGroupedByEnvironment instanceGroupedByEnvironment1 =
        InstanceGroupedByServiceList.InstanceGroupedByEnvironment.builder()
            .envId("env1")
            .envName("env1")
            .instanceGroupedByInfraList(
                Arrays.asList(instanceGroupedByInfrastructure2, instanceGroupedByInfrastructure1))
            .instanceGroupedByClusterList(Arrays.asList(instanceGroupedByCluster2, instanceGroupedByCluster1))
            .build();
    InstanceGroupedByServiceList.InstanceGroupedByEnvironment instanceGroupedByEnvironment2 =
        InstanceGroupedByServiceList.InstanceGroupedByEnvironment.builder()
            .envId("env2")
            .envName("env2")
            .instanceGroupedByInfraList(Arrays.asList(instanceGroupedByInfrastructure3))
            .instanceGroupedByClusterList(Arrays.asList(instanceGroupedByCluster3))
            .build();

    InstanceGroupedByServiceList.InstanceGroupedByArtifact instanceGroupedByArtifact1 =
        InstanceGroupedByServiceList.InstanceGroupedByArtifact.builder()
            .artifactVersion("1")
            .artifactPath("artifact1")
            .instanceGroupedByEnvironmentList(Arrays.asList(instanceGroupedByEnvironment1))
            .build();
    InstanceGroupedByServiceList.InstanceGroupedByArtifact instanceGroupedByArtifact2 =
        InstanceGroupedByServiceList.InstanceGroupedByArtifact.builder()
            .artifactVersion("2")
            .artifactPath("artifact2")
            .instanceGroupedByEnvironmentList(Arrays.asList(instanceGroupedByEnvironment2))
            .build();

    InstanceGroupedByServiceList.InstanceGroupedByService instanceGroupedByService1 =
        InstanceGroupedByServiceList.InstanceGroupedByService.builder()
            .serviceName("svcN1")
            .serviceId("svc1")
            .instanceGroupedByArtifactList(Arrays.asList(instanceGroupedByArtifact1, instanceGroupedByArtifact2))
            .build();
    return Arrays.asList(instanceGroupedByService1);
  }

  List<ActiveServiceInstanceInfoV2> getSampleListActiveServiceInstanceInfo() {
    List<ActiveServiceInstanceInfoV2> activeServiceInstanceInfo = new ArrayList<>();
    ActiveServiceInstanceInfoV2 instance1 = new ActiveServiceInstanceInfoV2(
        "svc1", "svcN1", "env1", "env1", "infra1", "infra1", null, null, "1", "a", 1l, "1", "artifact1:1", 1);
    activeServiceInstanceInfo.add(instance1);
    instance1 = new ActiveServiceInstanceInfoV2(
        "svc1", "svcN1", "env1", "env1", "infra1", "infra1", null, null, "1", "a", 2l, "1", "artifact1:1", 2);
    activeServiceInstanceInfo.add(instance1);
    instance1 = new ActiveServiceInstanceInfoV2(
        "svc1", "svcN1", "env1", "env1", "infra1", "infra1", null, null, "2", "b", 1l, "1", "artifact1:1", 1);
    activeServiceInstanceInfo.add(instance1);
    instance1 = new ActiveServiceInstanceInfoV2(
        "svc1", "svcN1", "env1", "env1", "infra2", "infra2", null, null, "1", "a", 1l, "1", "artifact1:1", 1);
    activeServiceInstanceInfo.add(instance1);
    instance1 = new ActiveServiceInstanceInfoV2(
        "svc1", "svcN1", "env2", "env2", "infra2", "infra2", null, null, "2", "b", 1l, "2", "artifact2:2", 1);
    activeServiceInstanceInfo.add(instance1);
    return activeServiceInstanceInfo;
  }

  List<ActiveServiceInstanceInfoV2> getSampleListActiveServiceInstanceInfoGitOps() {
    List<ActiveServiceInstanceInfoV2> activeServiceInstanceInfo = new ArrayList<>();
    ActiveServiceInstanceInfoV2 instance1 = new ActiveServiceInstanceInfoV2(
        "svc1", "svcN1", "env1", "env1", null, null, "infra1", "infra1", "1", "a", 1l, "1", "artifact1:1", 1);
    activeServiceInstanceInfo.add(instance1);
    instance1 = new ActiveServiceInstanceInfoV2(
        "svc1", "svcN1", "env1", "env1", null, null, "infra1", "infra1", "1", "a", 2l, "1", "artifact1:1", 2);
    activeServiceInstanceInfo.add(instance1);
    instance1 = new ActiveServiceInstanceInfoV2(
        "svc1", "svcN1", "env1", "env1", null, null, "infra1", "infra1", "2", "b", 1l, "1", "artifact1:1", 1);
    activeServiceInstanceInfo.add(instance1);
    instance1 = new ActiveServiceInstanceInfoV2(
        "svc1", "svcN1", "env1", "env1", null, null, "infra2", "infra2", "1", "a", 1l, "1", "artifact1:1", 1);
    activeServiceInstanceInfo.add(instance1);
    instance1 = new ActiveServiceInstanceInfoV2(
        "svc1", "svcN1", "env2", "env2", null, null, "infra2", "infra2", "2", "b", 1l, "2", "artifact2:2", 1);
    activeServiceInstanceInfo.add(instance1);
    return activeServiceInstanceInfo;
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_groupByServices() {
    Map<String,
        Map<String,
            Map<String,
                Pair<Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>,
                    Map<String, List<InstanceGroupedByServiceList.InstanceGroupedByPipelineExecution>>>>>>
        serviceBuildEnvInfraMap = getSampleServiceBuildEnvInfraMap();
    Map<String, String> serviceIdToServiceNameMap = new HashMap<>();
    Map<String, String> buildIdToArtifactPathMap = new HashMap<>();
    Map<String, String> envIdToEnvNameMap = new HashMap<>();
    Map<String, String> infraIdToInfraNameMap = new HashMap<>();

    serviceIdToServiceNameMap.put("svc1", "svcN1");
    serviceIdToServiceNameMap.put("svc2", "svcN2");

    buildIdToArtifactPathMap.put("1", "artifact1");
    buildIdToArtifactPathMap.put("2", "artifact2");

    envIdToEnvNameMap.put("env1", "env1");
    envIdToEnvNameMap.put("env2", "env2");

    infraIdToInfraNameMap.put("infra1", "infra1");
    infraIdToInfraNameMap.put("infra2", "infra2");

    List<InstanceGroupedByServiceList.InstanceGroupedByService> instanceGroupedByServices =
        getSampleListInstanceGroupedByService();

    List<InstanceGroupedByServiceList.InstanceGroupedByService> instanceGroupedByServices1 =
        cdOverviewDashboardService.groupedByServices(serviceBuildEnvInfraMap, envIdToEnvNameMap, infraIdToInfraNameMap,
            serviceIdToServiceNameMap, infraIdToInfraNameMap, buildIdToArtifactPathMap, false);

    assertThat(instanceGroupedByServices1).isEqualTo(instanceGroupedByServices);
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_getInstanceGroupedByServiceListHelper() {
    List<ActiveServiceInstanceInfoV2> activeServiceInstanceInfoV2List = getSampleListActiveServiceInstanceInfo();
    activeServiceInstanceInfoV2List.addAll(getSampleListActiveServiceInstanceInfoGitOps());
    InstanceGroupedByServiceList instanceGroupedByServiceList =
        cdOverviewDashboardService.getInstanceGroupedByServiceListHelper(activeServiceInstanceInfoV2List);
    assertThat(instanceGroupedByServiceList)
        .isEqualTo(InstanceGroupedByServiceList.builder()
                       .instanceGroupedByServiceList(getSampleListInstanceGroupedByService())
                       .build());
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_getInstanceGroupedByServiceList() {
    Mockito
        .when(instanceDashboardService.getActiveServiceInstanceInfo(
            "accountId", "orgId", "projectId", null, null, null, false))
        .thenReturn(getSampleListActiveServiceInstanceInfo());
    Mockito
        .when(instanceDashboardService.getActiveServiceInstanceInfo(
            "accountId", "orgId", "projectId", null, null, null, true))
        .thenReturn(getSampleListActiveServiceInstanceInfoGitOps());
    InstanceGroupedByServiceList instanceGroupedByServiceList =
        InstanceGroupedByServiceList.builder()
            .instanceGroupedByServiceList(getSampleListInstanceGroupedByService())
            .build();
    assertThat(instanceGroupedByServiceList)
        .isEqualTo(cdOverviewDashboardService.getInstanceGroupedByServiceList(
            "accountId", "orgId", "projectId", null, null, null));
  }
}
