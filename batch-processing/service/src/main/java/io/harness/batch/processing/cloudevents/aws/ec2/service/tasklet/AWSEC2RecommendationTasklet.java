package io.harness.batch.processing.cloudevents.aws.ec2.service.tasklet;

import com.amazonaws.services.costexplorer.model.EC2ResourceDetails;
import com.amazonaws.services.costexplorer.model.RecommendationTarget;
import com.amazonaws.services.costexplorer.model.RightsizingRecommendation;
import com.google.inject.Singleton;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.cloudevents.aws.ec2.service.AWSEC2RecommendationService;
import io.harness.batch.processing.cloudevents.aws.ec2.service.helper.AWSEC2Details;
import io.harness.batch.processing.cloudevents.aws.ec2.service.helper.AWSRegionRegistry;
import io.harness.batch.processing.cloudevents.aws.ec2.service.helper.EC2MetricHelper;
import io.harness.batch.processing.cloudevents.aws.ec2.service.request.EC2RecommendationRequest;
import io.harness.batch.processing.cloudevents.aws.ec2.service.response.EC2RecommendationResponse;
import io.harness.batch.processing.cloudevents.aws.ec2.service.response.Ec2UtilzationData;
import io.harness.batch.processing.cloudevents.aws.ec2.service.response.MetricValue;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.ng.NGConnectorHelper;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.dao.recommendation.EC2RecommendationDAO;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.commons.entities.ec2.recommendation.EC2Recommendation;
import io.harness.ccm.commons.entities.ec2.recommendation.EC2RecommendationDetail;
import io.harness.ccm.setup.CECloudAccountDao;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.harness.batch.processing.ccm.UtilizationInstanceType.EC2_INSTANCE;
import static software.wings.beans.SettingAttribute.SettingCategory.CE_CONNECTOR;
import static software.wings.settings.SettingVariableTypes.CE_AWS;

@Slf4j
@Singleton
public class AWSEC2RecommendationTasklet  implements Tasklet {
    @Autowired private EC2MetricHelper ec2MetricHelper;
    @Autowired private AWSEC2RecommendationService awsec2RecommendationService;
    @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
    @Autowired private CECloudAccountDao ceCloudAccountDao;
    @Autowired private NGConnectorHelper ngConnectorHelper;
    @Autowired private UtilizationDataServiceImpl utilizationDataService;
    @Autowired private EC2RecommendationDAO ec2RecommendationDAO;
    private static final String TERMINATE = "TERMINATE";
    private static final String MODIFY = "Modify";

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        final JobConstants jobConstants = CCMJobConstants.fromContext(chunkContext);
        String accountId = jobConstants.getAccountId();
        Instant startTime = Instant.ofEpochMilli(jobConstants.getJobStartTime());
        log.info("accountId = {}", accountId);
        // call aws get-metric-data to get the cpu & memory utilisation data
        Map<String, AwsCrossAccountAttributes> infraAccCrossArnMap = getCrossAccountAttributes(accountId);

        if (!infraAccCrossArnMap.isEmpty()) {
            for (Map.Entry<String, AwsCrossAccountAttributes> infraAccCrossArn : infraAccCrossArnMap.entrySet()) {
                Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);
                // fetching the aws ec2 recommendations
                EC2RecommendationResponse ec2RecommendationResponse =
                        awsec2RecommendationService.getRecommendations(EC2RecommendationRequest.builder()
                                .awsCrossAccountAttributes(infraAccCrossArn.getValue())
                                .build());
                log.info("Ec2RecommendationResponse = {}", ec2RecommendationResponse);

                if (Objects.nonNull(ec2RecommendationResponse) &&
                        !ec2RecommendationResponse.getRecommendationMap().isEmpty()) {
                    for (Map.Entry<RecommendationTarget, List<RightsizingRecommendation>> rightsizingRecommendations
                            : ec2RecommendationResponse.getRecommendationMap().entrySet()) {
                        if (!rightsizingRecommendations.getValue().isEmpty()) {
                            rightsizingRecommendations.getValue().forEach(rightsizingRecommendation -> {
                                EC2Recommendation recommendation;
                                if (rightsizingRecommendation.getRightsizingType().equalsIgnoreCase(MODIFY)) {
                                    recommendation = buildRecommendationObjectFromModifyType(rightsizingRecommendation);
                                    recommendation.setRecommendationType(rightsizingRecommendations.getKey().name());
                                } else {
                                    recommendation = buildRecommendationForTerminationType(rightsizingRecommendation);
                                    recommendation.setRecommendationType(TERMINATE);
                                }
                                recommendation.setAccountId(accountId);
                                recommendation.setLastUpdatedTime(startTime);
                                // Save the ec2 recommendation to mongo and timescale
                                EC2Recommendation ec2Recommendation = ec2RecommendationDAO.saveRecommendation(recommendation);
                                log.info("ec2Recommendation saved to mongoDB = {}", ec2Recommendation);
                                saveRecommendationInTimeScaleDB(ec2Recommendation);
                            });
                        }
                    }

                    List<AWSEC2Details> instances = extractEC2InstanceDetails(ec2RecommendationResponse);
                    List<Ec2UtilzationData> utilizationData =
                            ec2MetricHelper.getUtilizationMetrics(infraAccCrossArn.getValue(), Date.from(now.minus(1, ChronoUnit.DAYS)),
                                    Date.from(now), instances);
                    if (!utilizationData.isEmpty()) {
                        saveUtilDataToTimescaleDB(accountId, utilizationData);
                    }
                }
            }
        }

        return null;
    }

    private void saveUtilDataToTimescaleDB(String accountId, List<Ec2UtilzationData> utilizationMetricsList) {
        List<InstanceUtilizationData> instanceUtilizationDataList = new ArrayList<>();
        utilizationMetricsList.forEach(utilizationMetrics -> {
            String instanceId;
            String instanceType;
            instanceId = utilizationMetrics.getInstanceId();
            instanceType = EC2_INSTANCE;

            long startTime = 0L;
            long oneDayMillis = Duration.ofDays(1).toMillis();
            boolean utilDataPresent = false;
            List<Double> cpuUtilizationAvgList = new ArrayList<>();
            List<Double> cpuUtilizationMaxList = new ArrayList<>();
            List<Double> memoryUtilizationAvgList = new ArrayList<>();
            List<Double> memoryUtilizationMaxList = new ArrayList<>();

            for (MetricValue utilizationMetric : utilizationMetrics.getMetricValues()) {
                if (!utilizationMetric.getTimestamps().isEmpty()) {
                    startTime = utilizationMetric.getTimestamps().get(0).toInstant().toEpochMilli();
                }

                List<Double> metricsList = utilizationMetric.getValues();
                switch (utilizationMetric.getStatistic()) {
                    case "Maximum":
                        switch (utilizationMetric.getMetricName()) {
                            case "MemoryUtilization":
                                memoryUtilizationMaxList = metricsList;
                                utilDataPresent = true;
                                break;
                            case "CPUUtilization":
                                cpuUtilizationMaxList = metricsList;
                                utilDataPresent = true;
                                break;
                            default:
                                throw new InvalidRequestException("Invalid Utilization metric name");
                        }
                        break;
                    case "Average":
                        switch (utilizationMetric.getMetricName()) {
                            case "MemoryUtilization":
                                memoryUtilizationAvgList = metricsList;
                                utilDataPresent = true;
                                break;
                            case "CPUUtilization":
                                cpuUtilizationAvgList = metricsList;
                                utilDataPresent = true;
                                break;
                            default:
                                throw new InvalidRequestException("Invalid Utilization metric name");
                        }
                        break;
                    default:
                        throw new InvalidRequestException("Invalid Utilization metric Statistic");
                }
            }

            InstanceUtilizationData utilizationData =
                    InstanceUtilizationData.builder()
                            .accountId(accountId)
                            .instanceId(instanceId)
                            .instanceType(instanceType)
                            .settingId(instanceId)
                            .clusterId(instanceId)
                            .cpuUtilizationMax((!cpuUtilizationMaxList.isEmpty()) ? getScaledUtilValue(cpuUtilizationMaxList.get(0)) : 0.0)
                            .cpuUtilizationAvg((!cpuUtilizationAvgList.isEmpty()) ?getScaledUtilValue(cpuUtilizationAvgList.get(0)) : 0.0)
                            .memoryUtilizationMax((!memoryUtilizationMaxList.isEmpty()) ?getScaledUtilValue(memoryUtilizationMaxList.get(0)) : 0.0)
                            .memoryUtilizationAvg((!memoryUtilizationAvgList.isEmpty()) ?getScaledUtilValue(memoryUtilizationAvgList.get(0)) : 0.0)
                            .startTimestamp(startTime)
                            .endTimestamp(startTime + oneDayMillis)
                            .build();
            if (utilDataPresent) {
                instanceUtilizationDataList.add(utilizationData);
            }
        });

        if (!instanceUtilizationDataList.isEmpty()) {
            utilizationDataService.create(instanceUtilizationDataList);
        }
    }

    private double getScaledUtilValue(double value) {
        return value / 100;
    }

    private List<AWSEC2Details> extractEC2InstanceDetails(EC2RecommendationResponse response) {
        List<AWSEC2Details> awsec2Details = new ArrayList<>();
        for (Map.Entry<RecommendationTarget, List<RightsizingRecommendation>> rightsizingRecommendations
                : response.getRecommendationMap().entrySet()) {
            awsec2Details.addAll(rightsizingRecommendations.getValue().stream()
                    .map(rightsizingRecommendation -> {
                        String instanceId = rightsizingRecommendation.getCurrentInstance().getResourceId();
                        String region = rightsizingRecommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getRegion();
                        return new AWSEC2Details(instanceId, AWSRegionRegistry.getRegionNameFromDisplayName(region));
            }).collect(Collectors.toList()));
        }
        return awsec2Details;
    }

    private EC2Recommendation buildRecommendationObjectFromModifyType(RightsizingRecommendation recommendation) {
        return EC2Recommendation.builder()
                .awsAccountId(recommendation.getAccountId())
                .currentMaxCPU(recommendation.getCurrentInstance().getResourceUtilization().getEC2ResourceUtilization().getMaxCpuUtilizationPercentage())
                .currentMaxMemory(recommendation.getCurrentInstance().getResourceUtilization().getEC2ResourceUtilization().getMaxMemoryUtilizationPercentage())
                .currentMonthlyCost(recommendation.getCurrentInstance().getMonthlyCost())
                .currencyCode(recommendation.getCurrentInstance().getCurrencyCode())
                .instanceId(recommendation.getCurrentInstance().getResourceId())
                .instanceName(recommendation.getCurrentInstance().getInstanceName())
                .instanceType(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getInstanceType())
                .memory(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getMemory())
                .platform(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getPlatform())
                .region(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getRegion())
                .sku(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getSku())
                .vcpu(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getVcpu())
                .expectedMaxCPU(recommendation.getModifyRecommendationDetail().getTargetInstances().get(0).getExpectedResourceUtilization().getEC2ResourceUtilization().getMaxCpuUtilizationPercentage())
                .expectedMaxMemory(recommendation.getModifyRecommendationDetail().getTargetInstances().get(0).getExpectedResourceUtilization().getEC2ResourceUtilization().getMaxMemoryUtilizationPercentage())
                .recommendationInfo(buildRecommendationInfo(recommendation))
                .expectedMonthlyCost(recommendation.getModifyRecommendationDetail().getTargetInstances().get(0).getEstimatedMonthlyCost())
                .expectedMonthlySaving(recommendation.getModifyRecommendationDetail().getTargetInstances().get(0).getEstimatedMonthlySavings())
                .rightsizingType(recommendation.getRightsizingType())
                .build();
    }

    private EC2Recommendation buildRecommendationForTerminationType(RightsizingRecommendation recommendation) {
        return EC2Recommendation.builder()
                .awsAccountId(recommendation.getAccountId())
                .currentMaxCPU(recommendation.getCurrentInstance().getResourceUtilization().getEC2ResourceUtilization().getMaxCpuUtilizationPercentage())
                .currentMaxMemory(recommendation.getCurrentInstance().getResourceUtilization().getEC2ResourceUtilization().getMaxMemoryUtilizationPercentage())
                .currentMonthlyCost(recommendation.getCurrentInstance().getMonthlyCost())
                .currencyCode(recommendation.getCurrentInstance().getCurrencyCode())
                .instanceId(recommendation.getCurrentInstance().getResourceId())
                .instanceName(recommendation.getCurrentInstance().getInstanceName())
                .instanceType(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getInstanceType())
                .memory(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getMemory())
                .platform(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getPlatform())
                .region(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getRegion())
                .sku(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getSku())
                .vcpu(recommendation.getCurrentInstance().getResourceDetails().getEC2ResourceDetails().getVcpu())
                .expectedMonthlySaving(recommendation.getTerminateRecommendationDetail().getEstimatedMonthlySavings())
                .rightsizingType(recommendation.getRightsizingType())
                .build();
    }

    private EC2RecommendationDetail buildRecommendationInfo(RightsizingRecommendation recommendation) {
        EC2ResourceDetails ec2ResourceDetails =
                recommendation.getModifyRecommendationDetail().getTargetInstances().get(0).getResourceDetails().getEC2ResourceDetails();
        return EC2RecommendationDetail.builder()
                .instanceType(ec2ResourceDetails.getInstanceType())
                .hourlyOnDemandRate(ec2ResourceDetails.getHourlyOnDemandRate())
                .memory(ec2ResourceDetails.getMemory())
                .platform(ec2ResourceDetails.getPlatform())
                .region(ec2ResourceDetails.getRegion())
                .sku(ec2ResourceDetails.getSku())
                .vcpu(ec2ResourceDetails.getVcpu())
                .build();
    }

    private void saveRecommendationInTimeScaleDB(EC2Recommendation ec2Recommendation) {
        Double currentMonthCost = Double.parseDouble(
                ec2Recommendation.getCurrentMonthlyCost().isEmpty() ? "0.0" : ec2Recommendation.getCurrentMonthlyCost());
        Double monthlySaving = Double.parseDouble(
                ec2Recommendation.getExpectedMonthlySaving().isEmpty() ? "0.0" : ec2Recommendation.getExpectedMonthlySaving());
        ec2RecommendationDAO.upsertCeRecommendation(ec2Recommendation.getUuid(),
                ec2Recommendation.getAccountId(), ec2Recommendation.getInstanceId(), ec2Recommendation.getAwsAccountId(),
                ec2Recommendation.getInstanceName(), currentMonthCost, monthlySaving, ec2Recommendation.getLastUpdatedTime());
    }

    private Map<String, AwsCrossAccountAttributes> getCrossAccountAttributes(String accountId) {
        List<SettingAttribute> ceConnectorsList =
                cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(accountId, CE_CONNECTOR, CE_AWS);
        Map<String, AwsCrossAccountAttributes> crossAccountAttributesMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(ceConnectorsList)) {
            List<CECloudAccount> ceCloudAccountList =
                    ceCloudAccountDao.getBySettingId(accountId, ceConnectorsList.get(0).getUuid());
            ceCloudAccountList.forEach(ceCloudAccount
                    -> crossAccountAttributesMap.put(
                    ceCloudAccount.getInfraAccountId(), ceCloudAccount.getAwsCrossAccountAttributes()));
            List<SettingAttribute> ceConnectorList =
                    cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(accountId, CE_CONNECTOR, CE_AWS);
            ceConnectorList.forEach(ceConnector -> {
                CEAwsConfig ceAwsConfig = (CEAwsConfig) ceConnector.getValue();
                crossAccountAttributesMap.put(ceAwsConfig.getAwsMasterAccountId(), ceAwsConfig.getAwsCrossAccountAttributes());
            });
        }
        List<ConnectorResponseDTO> nextGenConnectors =
                ngConnectorHelper.getNextGenConnectors(accountId, Arrays.asList(ConnectorType.CE_AWS),
                        Arrays.asList(CEFeatures.VISIBILITY), Arrays.asList(ConnectivityStatus.SUCCESS));
        for (ConnectorResponseDTO connector : nextGenConnectors) {
            ConnectorInfoDTO connectorInfo = connector.getConnector();
            CEAwsConnectorDTO ceAwsConnectorDTO = (CEAwsConnectorDTO) connectorInfo.getConnectorConfig();
            if (ceAwsConnectorDTO != null && ceAwsConnectorDTO.getCrossAccountAccess() != null) {
                AwsCrossAccountAttributes crossAccountAttributes =
                        AwsCrossAccountAttributes.builder()
                                .crossAccountRoleArn(ceAwsConnectorDTO.getCrossAccountAccess().getCrossAccountRoleArn())
                                .externalId(ceAwsConnectorDTO.getCrossAccountAccess().getExternalId())
                                .build();
                crossAccountAttributesMap.put(ceAwsConnectorDTO.getAwsAccountId(), crossAccountAttributes);
            }
        }
        return crossAccountAttributesMap;
    }
}
