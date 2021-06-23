package io.harness.batch.processing.writer;

import static io.harness.beans.FeatureName.CE_AZURE_BILLING_CONNECTOR_DETAIL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import static software.wings.beans.SettingAttribute.SettingCategory.CE_CONNECTOR;
import static software.wings.settings.SettingVariableTypes.CE_AZURE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.ccm.AzureStorageSyncRecord;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.service.impl.AzureStorageSyncServiceImpl;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ceazure.BillingExportSpecDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.ff.FeatureFlagService;
import io.harness.ng.beans.PageResponse;

import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAzureConfig;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CE)
public class AzureStorageSyncEventWriter extends EventWriter implements ItemWriter<SettingAttribute> {
  @Autowired private AzureStorageSyncServiceImpl azureStorageSyncService;
  @Autowired private ConnectorResourceClient connectorResourceClient;
  @Autowired private FeatureFlagService featureFlagService;
  private JobParameters parameters;

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
  }

  @Override
  public void write(List<? extends SettingAttribute> dummySettingAttributeList) {
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    syncCurrentGenAzureContainers(accountId);
    if (featureFlagService.isEnabled(CE_AZURE_BILLING_CONNECTOR_DETAIL, accountId)) {
      syncNextGenContainers(accountId);
    }
  }

  public void syncCurrentGenAzureContainers(String accountId) {
    List<ConnectorResponseDTO> currentGenConnectorResponses = new ArrayList<>();
    List<SettingAttribute> ceConnectorsList =
        cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(accountId, CE_CONNECTOR, CE_AZURE);
    log.info("Processing batch size of {} in AzureStorageSyncEventWriter", ceConnectorsList.size());
    ceConnectorsList.forEach(settingAttribute -> {
      if (settingAttribute.getValue() instanceof CEAzureConfig) {
        CEAzureConfig ceAzureConfig = (CEAzureConfig) settingAttribute.getValue();
        BillingExportSpecDTO billingExportDetails = BillingExportSpecDTO.builder()
                                                        .containerName(ceAzureConfig.getContainerName())
                                                        .directoryName(ceAzureConfig.getDirectoryName())
                                                        .storageAccountName(ceAzureConfig.getStorageAccountName())
                                                        .build();

        ConnectorConfigDTO connectorConfig = CEAzureConnectorDTO.builder()
                                                 .billingExportSpec(billingExportDetails)
                                                 .subscriptionId(ceAzureConfig.getSubscriptionId())
                                                 .tenantId(ceAzureConfig.getTenantId())
                                                 .build();
        ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                                .connectorConfig(connectorConfig)
                                                .connectorType(ConnectorType.CE_AZURE)
                                                .identifier(settingAttribute.getUuid())
                                                .name(settingAttribute.getName())
                                                .build();
        ConnectorResponseDTO connectorResponse = ConnectorResponseDTO.builder().connector(connectorInfoDTO).build();
        currentGenConnectorResponses.add(connectorResponse);
      }
    });
    syncAzureContainers(currentGenConnectorResponses, accountId);
  }

  public void syncNextGenContainers(String accountId) {
    List<ConnectorResponseDTO> nextGenConnectors = new ArrayList<>();
    PageResponse<ConnectorResponseDTO> response = null;
    int page = 0;
    int size = 100;
    do {
      response = execute(connectorResourceClient.listConnectors(accountId, null, null, page, size,
          ConnectorFilterPropertiesDTO.builder()
              .types(Arrays.asList(ConnectorType.CE_AZURE))
              .ccmConnectorFilter(
                  CcmConnectorFilter.builder().featuresEnabled(Arrays.asList(CEFeatures.BILLING)).build())
              .build(),
          false));
      if (response != null && isNotEmpty(response.getContent())) {
        nextGenConnectors.addAll(response.getContent());
      }
      page++;
    } while (response != null && isNotEmpty(response.getContent()));
    log.info("Processing batch size of {} in AzureStorageSyncEventWriter (From NG)", nextGenConnectors.size());
    syncAzureContainers(nextGenConnectors, accountId);
  }

  public void syncAzureContainers(List<ConnectorResponseDTO> connectorResponses, String accountId) {
    connectorResponses.forEach(connector -> {
      CEAzureConnectorDTO ceAzureConnectorDTO = (CEAzureConnectorDTO) connector.getConnector().getConnectorConfig();
      if (ceAzureConnectorDTO != null && ceAzureConnectorDTO.getBillingExportSpec() != null) {
        AzureStorageSyncRecord azureStorageSyncRecord =
            AzureStorageSyncRecord.builder()
                .accountId(accountId)
                .settingId(connector.getConnector().getIdentifier())
                .containerName(ceAzureConnectorDTO.getBillingExportSpec().getContainerName())
                .directoryName(ceAzureConnectorDTO.getBillingExportSpec().getDirectoryName())
                .subscriptionId(ceAzureConnectorDTO.getSubscriptionId())
                .storageAccountName(ceAzureConnectorDTO.getBillingExportSpec().getStorageAccountName())
                .tenantId(ceAzureConnectorDTO.getTenantId())
                .build();
        azureStorageSyncService.syncContainer(azureStorageSyncRecord);
      }
    });
  }
}
