/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.graphql.core.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.ANMOL;

import static com.google.cloud.bigquery.FieldValue.Attribute.PRIMITIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.commons.beans.recommendation.AzureVmMetricType;
import io.harness.ccm.commons.beans.recommendation.AzureVmUtilisationDTO;
import io.harness.ccm.commons.utils.BigQueryHelper;
import io.harness.rule.Owner;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@OwnedBy(CE)
public class AzureMetricsUtilisationServiceTest extends CategoryTest {
  @Mock private BigQueryService mockBigQueryService;
  @Mock private BigQueryHelper mockBigQueryHelper;
  @Mock TableResult tableResult;
  @Mock BigQuery bigQuery;
  @InjectMocks private AzureMetricsUtilisationService azureMetricsUtilisationServiceUnderTest;

  private List<AzureVmUtilisationDTO> expectedResultCpu;
  private List<AzureVmUtilisationDTO> expectedResultMemory;
  private List<FieldValue> vmCpuUtilisationData;
  private FieldValueList vmCpuUtilisationDataList;
  private List<FieldValue> vmAverageCpuUtilisationData;
  private FieldValueList vmAverageCpuUtilisationDataList;
  private List<FieldValue> vmMaximumCpuUtilisationData;
  private FieldValueList vmMaximumCpuUtilisationDataList;
  private List<FieldValue> vmMemoryUtilisationData;
  private FieldValueList vmMemoryUtilisationDataList;
  private List<FieldValue> vmAverageMemoryUtilisationData;
  private FieldValueList vmAverageMemoryUtilisationDataList;
  private List<FieldValue> vmMaximumMemoryUtilisationData;
  private FieldValueList vmMaximumMemoryUtilisationDataList;

  private static final String START_TIME = "startTime";
  private static final String END_TIME = "endTime";
  private static final String AVERAGE = "average";
  private static final String MAXIMUM = "maximum";
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String VM_ID = "vmId";
  private static final String ACCOUNT_ID = "accountId";
  private static final String AZURE_VM_INVENTORY_METRIC = "azureVMInventoryMetric";
  private static final String DUMMY_DS = "dummy";
  private static final String DUMMY_CLOUD_PROVIDER_TABLE_FORMAT = "%s.BillingReport_%s.%s";

  @Before
  public void setUp() throws InterruptedException {
    expectedResultCpu = List.of(AzureVmUtilisationDTO.builder()
                                    .vmId(VM_ID)
                                    .averageCpu(12.0)
                                    .maxCpu(20.0)
                                    .startTime(1684454400L)
                                    .endTime(168445440L)
                                    .build());
    when(mockBigQueryHelper.getCloudProviderTableName(ACCOUNT_ID, AZURE_VM_INVENTORY_METRIC))
        .thenReturn(String.format(DUMMY_CLOUD_PROVIDER_TABLE_FORMAT, DUMMY_DS, ACCOUNT_ID, AZURE_VM_INVENTORY_METRIC));
    when(mockBigQueryService.get()).thenReturn(bigQuery);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);

    FieldList fieldListVmCpuUtilisationData =
        FieldList.of(Field.newBuilder(AVERAGE, StandardSQLTypeName.FLOAT64).build(),
            Field.newBuilder(MAXIMUM, StandardSQLTypeName.FLOAT64).build(),
            Field.newBuilder(START_TIME, StandardSQLTypeName.TIMESTAMP).build(),
            Field.newBuilder(END_TIME, StandardSQLTypeName.TIMESTAMP).build());
    vmCpuUtilisationData = new ArrayList<>();
    vmCpuUtilisationData.add(FieldValue.of(PRIMITIVE, "12"));
    vmCpuUtilisationData.add(FieldValue.of(PRIMITIVE, "20"));
    vmCpuUtilisationData.add(FieldValue.of(PRIMITIVE, "1684454.400"));
    vmCpuUtilisationData.add(FieldValue.of(PRIMITIVE, "168445.4400"));
    vmCpuUtilisationDataList = FieldValueList.of(vmCpuUtilisationData, fieldListVmCpuUtilisationData);

    FieldList fieldListAverageVmCpuUtilisationData =
        FieldList.of(Field.newBuilder(AVERAGE, StandardSQLTypeName.FLOAT64).build());
    vmAverageCpuUtilisationData = new ArrayList<>();
    vmAverageCpuUtilisationData.add(FieldValue.of(PRIMITIVE, "19.9"));
    vmAverageCpuUtilisationDataList =
        FieldValueList.of(vmAverageCpuUtilisationData, fieldListAverageVmCpuUtilisationData);

    FieldList fieldListMaximumVmCpuUtilisationData =
        FieldList.of(Field.newBuilder(MAXIMUM, StandardSQLTypeName.FLOAT64).build());
    vmMaximumCpuUtilisationData = new ArrayList<>();
    vmMaximumCpuUtilisationData.add(FieldValue.of(PRIMITIVE, "39.8"));
    vmMaximumCpuUtilisationDataList =
        FieldValueList.of(vmMaximumCpuUtilisationData, fieldListMaximumVmCpuUtilisationData);

    expectedResultMemory = List.of(AzureVmUtilisationDTO.builder()
                                       .vmId(VM_ID)
                                       .averageMemory(12.0)
                                       .maxMemory(20.0)
                                       .startTime(1684454400L)
                                       .endTime(168445440L)
                                       .build());
    when(mockBigQueryHelper.getCloudProviderTableName(ACCOUNT_ID, AZURE_VM_INVENTORY_METRIC))
        .thenReturn(String.format(DUMMY_CLOUD_PROVIDER_TABLE_FORMAT, DUMMY_DS, ACCOUNT_ID, AZURE_VM_INVENTORY_METRIC));
    when(mockBigQueryService.get()).thenReturn(bigQuery);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);

    FieldList fieldListVmMemoryUtilisationData =
        FieldList.of(Field.newBuilder(AVERAGE, StandardSQLTypeName.FLOAT64).build(),
            Field.newBuilder(MAXIMUM, StandardSQLTypeName.FLOAT64).build(),
            Field.newBuilder(START_TIME, StandardSQLTypeName.TIMESTAMP).build(),
            Field.newBuilder(END_TIME, StandardSQLTypeName.TIMESTAMP).build());
    vmMemoryUtilisationData = new ArrayList<>();
    vmMemoryUtilisationData.add(FieldValue.of(PRIMITIVE, "12"));
    vmMemoryUtilisationData.add(FieldValue.of(PRIMITIVE, "20"));
    vmMemoryUtilisationData.add(FieldValue.of(PRIMITIVE, "1684454.400"));
    vmMemoryUtilisationData.add(FieldValue.of(PRIMITIVE, "168445.4400"));
    vmMemoryUtilisationDataList = FieldValueList.of(vmMemoryUtilisationData, fieldListVmMemoryUtilisationData);

    FieldList fieldListAverageMemoryUtilisationData =
        FieldList.of(Field.newBuilder(AVERAGE, StandardSQLTypeName.FLOAT64).build());
    vmAverageMemoryUtilisationData = new ArrayList<>();
    vmAverageMemoryUtilisationData.add(FieldValue.of(PRIMITIVE, "19.9"));
    vmAverageMemoryUtilisationDataList =
        FieldValueList.of(vmAverageMemoryUtilisationData, fieldListAverageMemoryUtilisationData);

    FieldList fieldListMaximumVmMemoryUtilisationData =
        FieldList.of(Field.newBuilder(MAXIMUM, StandardSQLTypeName.FLOAT64).build());
    vmMaximumMemoryUtilisationData = new ArrayList<>();
    vmMaximumMemoryUtilisationData.add(FieldValue.of(PRIMITIVE, "39.8"));
    vmMaximumMemoryUtilisationDataList =
        FieldValueList.of(vmMaximumMemoryUtilisationData, fieldListMaximumVmMemoryUtilisationData);
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetAzureVmMetricUtilisationData_CpuUtilisation() {
    final List<AzureVmUtilisationDTO> result = azureMetricsUtilisationServiceUnderTest.getAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 7, AzureVmMetricType.PERCENTAGE_CPU);
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetAzureVmMetricUtilisationData_MemoryUtilisation() {
    final List<AzureVmUtilisationDTO> result = azureMetricsUtilisationServiceUnderTest.getAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 7, AzureVmMetricType.PERCENTAGE_MEMORY);
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetAzureVmMetricUtilisationData_CpuUtilisation_TableResultNotEmpty() throws InterruptedException {
    Iterable<FieldValueList> fieldValueListIterator = Arrays.asList(vmCpuUtilisationDataList);
    when(tableResult.iterateAll()).thenReturn(fieldValueListIterator);
    when(tableResult.getTotalRows()).thenReturn(1L);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);
    final List<AzureVmUtilisationDTO> result = azureMetricsUtilisationServiceUnderTest.getAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 7, AzureVmMetricType.PERCENTAGE_CPU);
    assertThat(result).isEqualTo(expectedResultCpu);
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetAzureVmMetricUtilisationData_MemoryUtilisation_TableResultNotEmpty() throws InterruptedException {
    Iterable<FieldValueList> fieldValueListIterator = Arrays.asList(vmMemoryUtilisationDataList);
    when(tableResult.iterateAll()).thenReturn(fieldValueListIterator);
    when(tableResult.getTotalRows()).thenReturn(1L);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);
    final List<AzureVmUtilisationDTO> result = azureMetricsUtilisationServiceUnderTest.getAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 7, AzureVmMetricType.PERCENTAGE_MEMORY);
    assertThat(result).isEqualTo(expectedResultMemory);
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetAverageAzureVmMetricUtilisationData_CpuUtilisation() {
    final Double result = azureMetricsUtilisationServiceUnderTest.getAverageAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 1, AzureVmMetricType.PERCENTAGE_CPU);
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetAverageAzureVmCpuUtilisationData_MemoryUtilisation() {
    final Double result = azureMetricsUtilisationServiceUnderTest.getAverageAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 1, AzureVmMetricType.PERCENTAGE_MEMORY);
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetAverageAzureVmMetricUtilisationData_CpuUtilisation_TableResultNotEmpty()
      throws InterruptedException {
    Iterable<FieldValueList> fieldValueListIterator = Arrays.asList(vmAverageCpuUtilisationDataList);
    when(tableResult.iterateAll()).thenReturn(fieldValueListIterator);
    when(tableResult.getTotalRows()).thenReturn(1L);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);
    final Double result = azureMetricsUtilisationServiceUnderTest.getAverageAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 1, AzureVmMetricType.PERCENTAGE_CPU);
    assertThat(result).isEqualTo(19.9);
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetAverageAzureVmMetricUtilisationData_MemoryUtilisation_TableResultNotEmpty()
      throws InterruptedException {
    Iterable<FieldValueList> fieldValueListIterator = Arrays.asList(vmAverageMemoryUtilisationDataList);
    when(tableResult.iterateAll()).thenReturn(fieldValueListIterator);
    when(tableResult.getTotalRows()).thenReturn(1L);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);
    final Double result = azureMetricsUtilisationServiceUnderTest.getAverageAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 1, AzureVmMetricType.PERCENTAGE_MEMORY);
    assertThat(result).isEqualTo(19.9);
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetMaximumAzureVmMetricUtilisationData_CpuUtilisation() {
    final Double result = azureMetricsUtilisationServiceUnderTest.getMaximumAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 1, AzureVmMetricType.PERCENTAGE_CPU);
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetMaximumAzureVmMetricUtilisationData_MemoryUtilisation() {
    final Double result = azureMetricsUtilisationServiceUnderTest.getMaximumAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 1, AzureVmMetricType.PERCENTAGE_MEMORY);
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetMaximumAzureVmMetricUtilisationData_CpuUtilisation_TableResultNotEmpty()
      throws InterruptedException {
    Iterable<FieldValueList> fieldValueListIterator = Arrays.asList(vmMaximumCpuUtilisationDataList);
    when(tableResult.iterateAll()).thenReturn(fieldValueListIterator);
    when(tableResult.getTotalRows()).thenReturn(1L);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);
    final Double result = azureMetricsUtilisationServiceUnderTest.getMaximumAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 1, AzureVmMetricType.PERCENTAGE_CPU);
    assertThat(result).isEqualTo(39.8);
  }

  @Test
  @Owner(developers = ANMOL)
  @Category(UnitTests.class)
  public void testGetMaximumAzureVmMetricUtilisationData_MemoryUtilisation_TableResultNotEmpty()
      throws InterruptedException {
    Iterable<FieldValueList> fieldValueListIterator = Arrays.asList(vmMaximumMemoryUtilisationDataList);
    when(tableResult.iterateAll()).thenReturn(fieldValueListIterator);
    when(tableResult.getTotalRows()).thenReturn(1L);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);
    final Double result = azureMetricsUtilisationServiceUnderTest.getMaximumAzureVmMetricUtilisationData(
        VM_ID, ACCOUNT_IDENTIFIER, 1, AzureVmMetricType.PERCENTAGE_MEMORY);
    assertThat(result).isEqualTo(39.8);
  }
}
