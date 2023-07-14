/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.eventmapper.filters.impl.buildtrigger;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.beans.response.TriggerEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_FOR_EVENT_CONDITION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.UnMatchedTriggerInfo;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse.WebhookEventMappingResponseBuilder;
import io.harness.ngtriggers.beans.response.TriggerEventResponse;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.ManifestTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.MultiRegionArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.conditionchecker.ConditionEvaluator;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.helpers.TriggerEventResponseHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(PIPELINE)
public class BuildTriggerEventConditionsFilter implements TriggerFilter {
  private final BuildTriggerHelper buildTriggerHelper;

  @Override
  public WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData) {
    WebhookEventMappingResponseBuilder mappingResponseBuilder = initWebhookEventMappingResponse(filterRequestData);
    List<TriggerDetails> matchedTriggers = new ArrayList<>();
    List<UnMatchedTriggerInfo> unMatchedTriggersInfoList = new ArrayList<>();

    for (TriggerDetails trigger : filterRequestData.getDetails()) {
      try {
        if (checkTriggerEligibility(filterRequestData, trigger)) {
          matchedTriggers.add(trigger);
        } else {
          UnMatchedTriggerInfo unMatchedTriggerInfo =
              UnMatchedTriggerInfo.builder()
                  .unMatchedTriggers(trigger)
                  .finalStatus(TriggerEventResponse.FinalStatus.TRIGGER_DID_NOT_MATCH_EVENT_CONDITION)
                  .message(trigger.getNgTriggerEntity().getIdentifier() + " didn't match conditions for payload event")
                  .build();
          unMatchedTriggersInfoList.add(unMatchedTriggerInfo);
        }
      } catch (Exception e) {
        log.error(getTriggerSkipMessage(trigger.getNgTriggerEntity()), e);
      }
    }

    mappingResponseBuilder.unMatchedTriggerInfoList(unMatchedTriggersInfoList);

    if (isEmpty(matchedTriggers)) {
      log.info("No trigger matched polling event after event condition evaluation:");
      mappingResponseBuilder.failedToFindTrigger(true)
          .webhookEventResponse(TriggerEventResponseHelper.toResponse(NO_MATCHING_TRIGGER_FOR_FOR_EVENT_CONDITION,
              filterRequestData.getWebhookPayloadData().getOriginalEvent(), null, null,
              "No Trigger matched conditions for payload event for Event: "
                  + buildTriggerHelper.generatePollingDescriptor(filterRequestData.getPollingResponse()),
              null))
          .build();
    } else {
      addDetails(mappingResponseBuilder, filterRequestData, matchedTriggers);
    }
    return mappingResponseBuilder.build();
  }

  boolean checkTriggerEligibility(FilterRequestData filterRequestData, TriggerDetails triggerDetails) {
    String version = filterRequestData.getPollingResponse().getBuildInfo().getVersions(0);

    List<TriggerEventDataCondition> triggerEventConditions = null;

    NGTriggerConfigV2 ngTriggerConfigV2 = triggerDetails.getNgTriggerConfigV2();
    NGTriggerSourceV2 source = ngTriggerConfigV2.getSource();
    NGTriggerSpecV2 spec = source.getSpec();
    if (ManifestTriggerConfig.class.isAssignableFrom(spec.getClass())) {
      ManifestTriggerConfig manifestTriggerConfig = (ManifestTriggerConfig) spec;
      triggerEventConditions = manifestTriggerConfig.getSpec().fetchEventDataConditions();
    } else if (ArtifactTriggerConfig.class.isAssignableFrom(spec.getClass())) {
      ArtifactTriggerConfig artifactTriggerConfig = (ArtifactTriggerConfig) spec;
      triggerEventConditions = artifactTriggerConfig.getSpec().fetchEventDataConditions();
    } else if (MultiRegionArtifactTriggerConfig.class.isAssignableFrom(spec.getClass())) {
      MultiRegionArtifactTriggerConfig multiRegionArtifactTriggerConfig = (MultiRegionArtifactTriggerConfig) spec;
      triggerEventConditions = multiRegionArtifactTriggerConfig.getEventConditions();
    }

    if (isEmpty(triggerEventConditions)) {
      return true;
    }

    TriggerEventDataCondition condition = triggerEventConditions.get(0);

    return ("version".equals(condition.getKey()) || "build".equals(condition.getKey()))
        && ConditionEvaluator.evaluate(version, condition.getValue(), condition.getOperator().getValue());
  }
}
