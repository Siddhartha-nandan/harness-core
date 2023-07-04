/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.utils;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.Boolean.parseBoolean;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.entities.AWSViewPreferenceCost;
import io.harness.ccm.views.entities.AWSViewPreferences;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.GCPViewPreferences;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewPreferences;
import io.harness.ccm.views.graphql.QLCEViewAggregateArithmeticOperation;
import io.harness.ccm.views.graphql.QLCEViewAggregateOperation;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewPreferenceAggregation;
import io.harness.ccm.views.helper.ViewParametersHelper;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingIdentifiers;
import io.harness.ngsettings.client.remote.NGSettingsClient;
import io.harness.ngsettings.dto.SettingDTO;
import io.harness.ngsettings.dto.SettingResponseDTO;
import io.harness.remote.client.NGRestUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CE)
public class CEViewPreferenceUtils {
  // Columns
  private static final String COST = "cost";
  private static final String CLOUD_PROVIDER = "cloudProvider";
  private static final String AWS_BLENDED_COST = "awsBlendedCost";
  private static final String AWS_AMORTISED_COST = "awsAmortisedCost";
  private static final String AWS_NET_AMORTISED_COST = "awsNetAmortisedCost";
  private static final String AWS_EFFECTIVE_COST = "awsEffectiveCost";
  private static final String AWS_UNBLENDED_COST = "awsUnblendedCost";
  private static final String AWS_LINE_ITEM_TYPE = "awsLineItemType";
  private static final String GCP_DISCOUNT = "discount";
  private static final String GCP_COST_TYPE = "gcpCostType";

  // AWS and GCP different type values
  private static final String AWS_LINE_ITEM_TYPE_DISCOUNT = "Discount";
  private static final String AWS_LINE_ITEM_TYPE_CREDIT = "Credit";
  private static final String AWS_LINE_ITEM_TYPE_REFUND = "Refund";
  private static final String AWS_LINE_ITEM_TYPE_TAX = "Tax";
  private static final String GCP_COST_TYPE_TAX = "tax";

  // Field names
  private static final String GCP_DISCOUNT_FIELD_NAME = "Discount";
  private static final String CLOUD_PROVIDER_FIELD_NAME = "Cloud Provider";
  private static final String AWS_LINE_ITEM_TYPE_FIELD_NAME = "AWS Line Item Type";
  private static final String GCP_COST_TYPE_FIELD_NAME = "GCP Cost Type";

  @Inject @Named("PRIVILEGED") private NGSettingsClient settingsClient;
  @Inject private ViewParametersHelper viewParametersHelper;

  public ViewPreferences getCEViewPreferencesForMigration(final CEView ceView) {
    return getCEViewPreferences(ceView, true);
  }

  public ViewPreferences getCEViewPreferences(
      final CEView ceView, final boolean updateWithViewPreferenceDefaultSettings) {
    ViewPreferences viewPreferences = ceView.getViewPreferences();
    final List<SettingResponseDTO> settingsResponse = getDefaultSettingResponse(ceView.getAccountId());
    if (!isEmpty(settingsResponse)) {
      final List<SettingDTO> settingsDTO =
          settingsResponse.stream().map(SettingResponseDTO::getSetting).collect(Collectors.toList());
      final Map<String, String> settingsMap =
          settingsDTO.stream().collect(Collectors.toMap(SettingDTO::getIdentifier, SettingDTO::getValue));
      viewPreferences = getViewPreferences(ceView, settingsMap, updateWithViewPreferenceDefaultSettings);
    } else {
      log.warn(
          "Unable to fetch perspective preferences account default settings for account: {}", ceView.getAccountId());
    }
    return viewPreferences;
  }

  private List<SettingResponseDTO> getDefaultSettingResponse(final String accountId) {
    List<SettingResponseDTO> settings = null;
    try {
      settings = NGRestUtils.getResponse(settingsClient.listSettings(
          accountId, null, null, SettingCategory.CE, SettingIdentifiers.PERSPECTIVE_PREFERENCES_GROUP_IDENTIFIER));
    } catch (final Exception exception) {
      log.error("Error when getting perspective preference list settings for account: {}", accountId, exception);
    }
    return settings;
  }

  private ViewPreferences getViewPreferences(final CEView ceView, final Map<String, String> settingsMap,
      final boolean updateWithViewPreferenceDefaultSettings) {
    ViewPreferences viewPreferences;
    final Set<ViewFieldIdentifier> dataSources = viewParametersHelper.getDataSourcesFromCEView(ceView);
    if (Objects.nonNull(ceView.getViewPreferences())) {
      viewPreferences =
          getViewPreferencesFromCEView(ceView, settingsMap, dataSources, updateWithViewPreferenceDefaultSettings);
    } else {
      viewPreferences = getDefaultViewPreferences(ceView, settingsMap, dataSources);
    }
    return viewPreferences;
  }

  private ViewPreferences getViewPreferencesFromCEView(final CEView ceView, final Map<String, String> settingsMap,
      final Set<ViewFieldIdentifier> dataSources, final boolean updateWithViewPreferenceDefaultSettings) {
    return ViewPreferences.builder()
        .includeOthers(getBooleanSettingValue(ceView.getViewPreferences().getIncludeOthers(), settingsMap,
            SettingIdentifiers.SHOW_OTHERS_IDENTIFIER, updateWithViewPreferenceDefaultSettings))
        .includeUnallocatedCost(
            getBooleanSettingValue(ceView.getViewPreferences().getIncludeOthers(), settingsMap,
                SettingIdentifiers.SHOW_UNALLOCATED_CLUSTER_COST_IDENTIFIER, updateWithViewPreferenceDefaultSettings)
            && viewParametersHelper.isClusterDataSources(dataSources))
        .showAnomalies(getBooleanSettingValue(ceView.getViewPreferences().getShowAnomalies(), settingsMap,
            SettingIdentifiers.SHOW_ANOMALIES_IDENTIFIER, updateWithViewPreferenceDefaultSettings))
        .awsPreferences(
            getAWSViewPreferences(ceView, settingsMap, dataSources, updateWithViewPreferenceDefaultSettings))
        .gcpPreferences(
            getGCPViewPreferences(ceView, settingsMap, dataSources, updateWithViewPreferenceDefaultSettings))
        .build();
  }

  private ViewPreferences getDefaultViewPreferences(
      final CEView ceView, final Map<String, String> settingsMap, final Set<ViewFieldIdentifier> dataSources) {
    return ViewPreferences.builder()
        .includeOthers(getBooleanSettingValue(settingsMap, SettingIdentifiers.SHOW_OTHERS_IDENTIFIER))
        .includeUnallocatedCost(
            getBooleanSettingValue(settingsMap, SettingIdentifiers.SHOW_UNALLOCATED_CLUSTER_COST_IDENTIFIER)
            && viewParametersHelper.isClusterDataSources(new HashSet<>(dataSources)))
        .showAnomalies(getBooleanSettingValue(settingsMap, SettingIdentifiers.SHOW_ANOMALIES_IDENTIFIER))
        .awsPreferences(getAWSViewPreferences(ceView, settingsMap, dataSources, true))
        .gcpPreferences(getGCPViewPreferences(ceView, settingsMap, dataSources, true))
        .build();
  }

  private GCPViewPreferences getGCPViewPreferences(final CEView ceView, final Map<String, String> settingsMap,
      final Set<ViewFieldIdentifier> dataSources, final boolean updateWithViewPreferenceDefaultSettings) {
    GCPViewPreferences gcpViewPreferences = null;
    if (Objects.nonNull(ceView) && Objects.nonNull(dataSources) && dataSources.contains(ViewFieldIdentifier.GCP)) {
      if (Objects.nonNull(ceView.getViewPreferences())
          && Objects.nonNull(ceView.getViewPreferences().getGcpPreferences())) {
        gcpViewPreferences =
            getGCPViewPreferencesFromCEView(ceView, settingsMap, updateWithViewPreferenceDefaultSettings);
      } else {
        gcpViewPreferences = getDefaultGCPViewPreferences(settingsMap);
      }
    }
    return gcpViewPreferences;
  }

  private GCPViewPreferences getGCPViewPreferencesFromCEView(final CEView ceView, final Map<String, String> settingsMap,
      final boolean updateWithViewPreferenceDefaultSettings) {
    final GCPViewPreferences gcpViewPreferences = ceView.getViewPreferences().getGcpPreferences();
    return GCPViewPreferences.builder()
        .includeDiscounts(getBooleanSettingValue(gcpViewPreferences.getIncludeDiscounts(), settingsMap,
            SettingIdentifiers.INCLUDE_GCP_DISCOUNTS_IDENTIFIER, updateWithViewPreferenceDefaultSettings))
        .includeTaxes(getBooleanSettingValue(gcpViewPreferences.getIncludeDiscounts(), settingsMap,
            SettingIdentifiers.INCLUDE_GCP_TAXES_IDENTIFIER, updateWithViewPreferenceDefaultSettings))
        .build();
  }

  private GCPViewPreferences getDefaultGCPViewPreferences(final Map<String, String> settingsMap) {
    return GCPViewPreferences.builder()
        .includeDiscounts(getBooleanSettingValue(settingsMap, SettingIdentifiers.INCLUDE_GCP_DISCOUNTS_IDENTIFIER))
        .includeTaxes(getBooleanSettingValue(settingsMap, SettingIdentifiers.INCLUDE_GCP_TAXES_IDENTIFIER))
        .build();
  }

  private AWSViewPreferences getAWSViewPreferences(final CEView ceView, final Map<String, String> settingsMap,
      final Set<ViewFieldIdentifier> dataSources, final boolean updateWithViewPreferenceDefaultSettings) {
    AWSViewPreferences awsViewPreferences = null;
    if (Objects.nonNull(ceView) && Objects.nonNull(dataSources) && dataSources.contains(ViewFieldIdentifier.AWS)) {
      if (Objects.nonNull(ceView.getViewPreferences())
          && Objects.nonNull(ceView.getViewPreferences().getAwsPreferences())) {
        awsViewPreferences =
            getAWSViewPreferencesFromCEView(ceView, settingsMap, updateWithViewPreferenceDefaultSettings);
      } else {
        awsViewPreferences = getDefaultAWSViewPreferences(settingsMap);
      }
    }
    return awsViewPreferences;
  }

  private AWSViewPreferences getAWSViewPreferencesFromCEView(final CEView ceView, final Map<String, String> settingsMap,
      final boolean updateWithViewPreferenceDefaultSettings) {
    final AWSViewPreferences awsViewPreferences = ceView.getViewPreferences().getAwsPreferences();
    return AWSViewPreferences.builder()
        .includeDiscounts(getBooleanSettingValue(awsViewPreferences.getIncludeDiscounts(), settingsMap,
            SettingIdentifiers.INCLUDE_AWS_DISCOUNTS_IDENTIFIER, updateWithViewPreferenceDefaultSettings))
        .includeCredits(getBooleanSettingValue(awsViewPreferences.getIncludeCredits(), settingsMap,
            SettingIdentifiers.INCLUDE_AWS_CREDIT_IDENTIFIER, updateWithViewPreferenceDefaultSettings))
        .includeRefunds(getBooleanSettingValue(awsViewPreferences.getIncludeRefunds(), settingsMap,
            SettingIdentifiers.INCLUDE_AWS_REFUNDS_IDENTIFIER, updateWithViewPreferenceDefaultSettings))
        .includeTaxes(getBooleanSettingValue(awsViewPreferences.getIncludeTaxes(), settingsMap,
            SettingIdentifiers.INCLUDE_AWS_TAXES_IDENTIFIER, updateWithViewPreferenceDefaultSettings))
        .awsCost(getAWSCostSettingValue(awsViewPreferences.getAwsCost(), settingsMap,
            SettingIdentifiers.SHOW_AWS_COST_AS_IDENTIFIER, updateWithViewPreferenceDefaultSettings))
        .build();
  }

  private AWSViewPreferences getDefaultAWSViewPreferences(final Map<String, String> settingsMap) {
    return AWSViewPreferences.builder()
        .includeDiscounts(getBooleanSettingValue(settingsMap, SettingIdentifiers.INCLUDE_AWS_DISCOUNTS_IDENTIFIER))
        .includeCredits(getBooleanSettingValue(settingsMap, SettingIdentifiers.INCLUDE_AWS_CREDIT_IDENTIFIER))
        .includeRefunds(getBooleanSettingValue(settingsMap, SettingIdentifiers.INCLUDE_AWS_REFUNDS_IDENTIFIER))
        .includeTaxes(getBooleanSettingValue(settingsMap, SettingIdentifiers.INCLUDE_AWS_TAXES_IDENTIFIER))
        .awsCost(getAWSCostSettingValue(settingsMap, SettingIdentifiers.SHOW_AWS_COST_AS_IDENTIFIER))
        .build();
  }

  private Boolean getBooleanSettingValue(final Boolean value, final Map<String, String> settingsMap,
      final String settingIdentifier, final boolean updateWithViewPreferenceDefaultSettings) {
    return Objects.nonNull(value) && !updateWithViewPreferenceDefaultSettings
        ? value
        : getBooleanSettingValue(settingsMap, settingIdentifier);
  }

  private AWSViewPreferenceCost getAWSCostSettingValue(final AWSViewPreferenceCost value,
      final Map<String, String> settingsMap, final String settingIdentifier,
      final boolean updateWithViewPreferenceDefaultSettings) {
    return Objects.nonNull(value) && !updateWithViewPreferenceDefaultSettings
        ? value
        : getAWSCostSettingValue(settingsMap, settingIdentifier);
  }

  private Boolean getBooleanSettingValue(final Map<String, String> settingsMap, final String settingIdentifier) {
    final String settingValue = settingsMap.get(settingIdentifier);
    if (Objects.isNull(settingValue)) {
      log.warn("Unable to get perspective preference default setting value. settingsMap: {}, settingIdentifier: {}",
          settingsMap, settingIdentifier);
    }
    return parseBoolean(settingValue);
  }

  private AWSViewPreferenceCost getAWSCostSettingValue(
      final Map<String, String> settingsMap, final String settingIdentifier) {
    final String settingValue = settingsMap.get(settingIdentifier);
    if (Objects.isNull(settingValue)) {
      log.warn("Unable to get perspective preference default setting value. settingsMap: {}, settingIdentifier: {}",
          settingsMap, settingIdentifier);
    }
    return AWSViewPreferenceCost.fromString(settingValue);
  }

  public List<QLCEViewPreferenceAggregation> getViewPreferenceAggregations(
      final CEView ceView, final ViewPreferences viewPreferences) {
    if (Objects.isNull(ceView) || Objects.isNull(viewPreferences)) {
      log.warn("View preferences are not set for view: {}", ceView);
      return Collections.emptyList();
    }
    final List<QLCEViewPreferenceAggregation> viewPreferenceAggregations = new ArrayList<>();
    final Set<ViewFieldIdentifier> dataSources = viewParametersHelper.getDataSourcesFromCEView(ceView);
    if (Objects.isNull(dataSources)) {
      // All cloud providers are present
      viewPreferenceAggregations.add(getQLCEOthersViewPreferenceAggregation());
      viewPreferenceAggregations.addAll(getQLCEAWSViewPreferenceAggregation(viewPreferences));
      viewPreferenceAggregations.addAll(getQLCEGCPViewPreferenceAggregation(viewPreferences));
    } else {
      if (dataSources.contains(ViewFieldIdentifier.AWS)) {
        viewPreferenceAggregations.addAll(getQLCEAWSViewPreferenceAggregation(viewPreferences));
      }
      if (dataSources.contains(ViewFieldIdentifier.GCP)) {
        viewPreferenceAggregations.addAll(getQLCEGCPViewPreferenceAggregation(viewPreferences));
      }
      dataSources.removeAll(Set.of(ViewFieldIdentifier.AWS, ViewFieldIdentifier.GCP));
      if (!dataSources.isEmpty()) {
        viewPreferenceAggregations.add(getQLCEOthersViewPreferenceAggregation());
      }
    }
    return viewPreferenceAggregations;
  }

  private QLCEViewPreferenceAggregation getQLCEOthersViewPreferenceAggregation() {
    return getQLCEViewPreferenceAggregation(COST, QLCEViewAggregateArithmeticOperation.ADD,
        getCloudProviderFilter(
            new String[] {ViewFieldIdentifier.AWS.getDisplayName(), ViewFieldIdentifier.GCP.getDisplayName()},
            QLCEViewFilterOperator.NOT_IN));
  }

  private List<QLCEViewPreferenceAggregation> getQLCEAWSViewPreferenceAggregation(
      final ViewPreferences viewPreferences) {
    if (Objects.isNull(viewPreferences.getAwsPreferences())) {
      log.warn("AWS preferences are not set for view preference: {}", viewPreferences);
      return Collections.emptyList();
    }
    List<QLCEViewPreferenceAggregation> awsViewPreferenceAggregations = new ArrayList<>();
    final AWSViewPreferences awsViewPreferences = viewPreferences.getAwsPreferences();
    switch (awsViewPreferences.getAwsCost()) {
      case BLENDED:
        // [TODO]: Add/Subtract discounts and other values for blended cost
        awsViewPreferenceAggregations.addAll(getAWSBlendedViewPreferenceAggregations(awsViewPreferences));
        break;
      case AMORTISED:
        // [TODO]: Add/Subtract discounts and other values for blended cost
        awsViewPreferenceAggregations.addAll(getAWSAmortisedViewPreferenceAggregations(awsViewPreferences));
        break;
      case NET_AMORTISED:
        // [TODO]: Add/Subtract discounts and other values for blended cost
        awsViewPreferenceAggregations.addAll(getAWSNetAmortisedViewPreferenceAggregations(awsViewPreferences));
        break;
      case EFFECTIVE:
        // [TODO]: Add/Subtract discounts and other values for blended cost
        awsViewPreferenceAggregations.addAll(getAWSEffectiveViewPreferenceAggregations());
        break;
      case UNBLENDED:
        awsViewPreferenceAggregations.addAll(getAWSUnblendedViewPreferenceAggregations(awsViewPreferences));
        break;
      default:
        log.warn("AWS Cost Type: {} is not supported", awsViewPreferences.getAwsCost());
        break;
    }

    return awsViewPreferenceAggregations;
  }

  private List<QLCEViewPreferenceAggregation> getAWSCommonViewPreferenceAggregations(
      final AWSViewPreferences awsViewPreferences, final String awsCostTypeColumnName) {
    final List<QLCEViewPreferenceAggregation> awsViewPreferenceAggregations = new ArrayList<>();
    awsViewPreferenceAggregations.add(getQLCEViewPreferenceAggregation(awsCostTypeColumnName,
        QLCEViewAggregateArithmeticOperation.ADD,
        getCloudProviderFilter(new String[] {ViewFieldIdentifier.AWS.getDisplayName()}, QLCEViewFilterOperator.IN)));
    if (!Boolean.TRUE.equals(awsViewPreferences.getIncludeCredits())) {
      awsViewPreferenceAggregations.add(
          getQLCEViewPreferenceAggregation(awsCostTypeColumnName, QLCEViewAggregateArithmeticOperation.SUBTRACT,
              getAWSLineItemTypeFilter(new String[] {AWS_LINE_ITEM_TYPE_CREDIT})));
    }
    if (!Boolean.TRUE.equals(awsViewPreferences.getIncludeRefunds())) {
      awsViewPreferenceAggregations.add(
          getQLCEViewPreferenceAggregation(awsCostTypeColumnName, QLCEViewAggregateArithmeticOperation.SUBTRACT,
              getAWSLineItemTypeFilter(new String[] {AWS_LINE_ITEM_TYPE_REFUND})));
    }
    if (!Boolean.TRUE.equals(awsViewPreferences.getIncludeTaxes())) {
      awsViewPreferenceAggregations.add(
          getQLCEViewPreferenceAggregation(awsCostTypeColumnName, QLCEViewAggregateArithmeticOperation.SUBTRACT,
              getAWSLineItemTypeFilter(new String[] {AWS_LINE_ITEM_TYPE_TAX})));
    }
    return awsViewPreferenceAggregations;
  }

  private List<QLCEViewPreferenceAggregation> getAWSBlendedViewPreferenceAggregations(
      final AWSViewPreferences awsViewPreferences) {
    return getAWSCommonViewPreferenceAggregations(awsViewPreferences, AWS_BLENDED_COST);
  }

  private List<QLCEViewPreferenceAggregation> getAWSAmortisedViewPreferenceAggregations(
      final AWSViewPreferences awsViewPreferences) {
    return getAWSCommonViewPreferenceAggregations(awsViewPreferences, AWS_AMORTISED_COST);
  }

  private List<QLCEViewPreferenceAggregation> getAWSNetAmortisedViewPreferenceAggregations(
      final AWSViewPreferences awsViewPreferences) {
    return getAWSCommonViewPreferenceAggregations(awsViewPreferences, AWS_NET_AMORTISED_COST);
  }

  private List<QLCEViewPreferenceAggregation> getAWSEffectiveViewPreferenceAggregations() {
    final List<QLCEViewPreferenceAggregation> awsEffectiveViewPreferenceAggregations = new ArrayList<>();
    awsEffectiveViewPreferenceAggregations.add(getQLCEViewPreferenceAggregation(AWS_EFFECTIVE_COST,
        QLCEViewAggregateArithmeticOperation.ADD,
        getCloudProviderFilter(new String[] {ViewFieldIdentifier.AWS.getDisplayName()}, QLCEViewFilterOperator.IN)));
    return awsEffectiveViewPreferenceAggregations;
  }

  private List<QLCEViewPreferenceAggregation> getAWSUnblendedViewPreferenceAggregations(
      final AWSViewPreferences awsViewPreferences) {
    final List<QLCEViewPreferenceAggregation> awsUnblendedViewPreferenceAggregations =
        new ArrayList<>(getAWSCommonViewPreferenceAggregations(awsViewPreferences, AWS_UNBLENDED_COST));
    if (!Boolean.TRUE.equals(awsViewPreferences.getIncludeDiscounts())) {
      awsUnblendedViewPreferenceAggregations.add(
          getQLCEViewPreferenceAggregation(AWS_UNBLENDED_COST, QLCEViewAggregateArithmeticOperation.SUBTRACT,
              getAWSLineItemTypeFilter(new String[] {AWS_LINE_ITEM_TYPE_DISCOUNT})));
    }
    return awsUnblendedViewPreferenceAggregations;
  }

  private List<QLCEViewPreferenceAggregation> getQLCEGCPViewPreferenceAggregation(
      final ViewPreferences viewPreferences) {
    if (Objects.isNull(viewPreferences.getGcpPreferences())) {
      log.warn("GCP preferences are not set for view preference: {}", viewPreferences);
      return Collections.emptyList();
    }
    final List<QLCEViewPreferenceAggregation> gcpViewPreferenceAggregations = new ArrayList<>();
    final GCPViewPreferences gcpViewPreferences = viewPreferences.getGcpPreferences();
    gcpViewPreferenceAggregations.add(getQLCEViewPreferenceAggregation(COST, QLCEViewAggregateArithmeticOperation.ADD,
        getCloudProviderFilter(new String[] {ViewFieldIdentifier.GCP.getDisplayName()}, QLCEViewFilterOperator.IN)));
    if (Boolean.TRUE.equals(gcpViewPreferences.getIncludeDiscounts())) {
      gcpViewPreferenceAggregations.add(getQLCEViewPreferenceAggregation(
          GCP_DISCOUNT, QLCEViewAggregateArithmeticOperation.ADD, getGCPDiscountNotNullFilter()));
    }
    if (!Boolean.TRUE.equals(gcpViewPreferences.getIncludeTaxes())) {
      gcpViewPreferenceAggregations.add(getQLCEViewPreferenceAggregation(
          COST, QLCEViewAggregateArithmeticOperation.SUBTRACT, getGCPCostTypeFilter(new String[] {GCP_COST_TYPE_TAX})));
    }

    return gcpViewPreferenceAggregations;
  }

  private QLCEViewPreferenceAggregation getQLCEViewPreferenceAggregation(final String columnName,
      final QLCEViewAggregateArithmeticOperation qlCEViewAggregateArithmeticOperation, final QLCEViewFilter filter) {
    return QLCEViewPreferenceAggregation.builder()
        .operationType(QLCEViewAggregateOperation.SUM)
        .columnName(columnName)
        .arithmeticOperationType(qlCEViewAggregateArithmeticOperation)
        .filter(filter)
        .build();
  }

  private QLCEViewFilter getGCPCostTypeFilter(final String[] values) {
    return QLCEViewFilter.builder()
        .field(QLCEViewFieldInput.builder()
                   .fieldId(GCP_COST_TYPE)
                   .fieldName(GCP_COST_TYPE_FIELD_NAME)
                   .identifier(ViewFieldIdentifier.GCP)
                   .identifierName(ViewFieldIdentifier.GCP.getDisplayName())
                   .build())
        .operator(QLCEViewFilterOperator.IN)
        .values(values)
        .build();
  }

  private QLCEViewFilter getAWSLineItemTypeFilter(final String[] values) {
    return QLCEViewFilter.builder()
        .field(QLCEViewFieldInput.builder()
                   .fieldId(AWS_LINE_ITEM_TYPE)
                   .fieldName(AWS_LINE_ITEM_TYPE_FIELD_NAME)
                   .identifier(ViewFieldIdentifier.AWS)
                   .identifierName(ViewFieldIdentifier.AWS.getDisplayName())
                   .build())
        .operator(QLCEViewFilterOperator.IN)
        .values(values)
        .build();
  }

  private QLCEViewFilter getGCPDiscountNotNullFilter() {
    return QLCEViewFilter.builder()
        .field(QLCEViewFieldInput.builder()
                   .fieldId(GCP_DISCOUNT)
                   .fieldName(GCP_DISCOUNT_FIELD_NAME)
                   .identifier(ViewFieldIdentifier.GCP)
                   .identifierName(ViewFieldIdentifier.GCP.getDisplayName())
                   .build())
        .operator(QLCEViewFilterOperator.NOT_NULL)
        .build();
  }

  private QLCEViewFilter getCloudProviderFilter(
      final String[] values, final QLCEViewFilterOperator qlCEViewFilterOperator) {
    return QLCEViewFilter.builder()
        .field(QLCEViewFieldInput.builder()
                   .fieldId(CLOUD_PROVIDER)
                   .fieldName(CLOUD_PROVIDER_FIELD_NAME)
                   .identifier(ViewFieldIdentifier.COMMON)
                   .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
                   .build())
        .operator(qlCEViewFilterOperator)
        .values(values)
        .build();
  }
}