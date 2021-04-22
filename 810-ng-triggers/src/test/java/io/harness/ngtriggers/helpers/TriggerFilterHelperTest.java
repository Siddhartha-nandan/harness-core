package io.harness.ngtriggers.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.MATT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent.TriggerWebhookEventBuilder;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData.WebhookPayloadDataBuilder;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.EventActionTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.GitWebhookTriggerRepoFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.GithubIssueCommentTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.HeaderTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.JexlConditionsTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.PayloadConditionsTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.ProjectTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.SourceRepoTypeTriggerFilter;
import io.harness.ngtriggers.mapper.TriggerFilterHelper;
import io.harness.product.ci.scm.proto.Issue;
import io.harness.product.ci.scm.proto.IssueCommentHook;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class TriggerFilterHelperTest extends CategoryTest {
  @Mock GitWebhookTriggerRepoFilter gitWebhookTriggerRepoFilter;
  @Mock ProjectTriggerFilter projectTriggerFilter;
  @Mock SourceRepoTypeTriggerFilter sourceRepoTypeTriggerFilter;
  @Mock EventActionTriggerFilter eventActionTriggerFilter;
  @Mock PayloadConditionsTriggerFilter payloadConditionsTriggerFilter;
  @Mock GithubIssueCommentTriggerFilter githubIssueCommentTriggerFilter;
  @Mock HeaderTriggerFilter headerTriggerFilter;
  @Mock JexlConditionsTriggerFilter jexlConditionsTriggerFilter;
  @Inject @InjectMocks TriggerFilterStore triggerFilterStore;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetWebhookTriggerFilters() {
    TriggerWebhookEventBuilder originalEventBuilder = TriggerWebhookEvent.builder().sourceRepoType("CUSTOM");
    WebhookPayloadDataBuilder webhookPayloadDataBuilder = WebhookPayloadData.builder();

    List<TriggerFilter> webhookTriggerFilters = triggerFilterStore.getWebhookTriggerFilters(
        webhookPayloadDataBuilder.originalEvent(originalEventBuilder.build()).build());
    assertThat(webhookTriggerFilters).isNotNull();
    assertThat(webhookTriggerFilters)
        .containsExactlyInAnyOrder(projectTriggerFilter, payloadConditionsTriggerFilter, jexlConditionsTriggerFilter);

    TriggerFilter[] triggerFiltersDefaultGit = new TriggerFilter[] {projectTriggerFilter, sourceRepoTypeTriggerFilter,
        eventActionTriggerFilter, payloadConditionsTriggerFilter, gitWebhookTriggerRepoFilter, headerTriggerFilter,
        jexlConditionsTriggerFilter};

    webhookTriggerFilters = triggerFilterStore.getWebhookTriggerFilters(
        webhookPayloadDataBuilder.parseWebhookResponse(ParseWebhookResponse.newBuilder().build())
            .originalEvent(originalEventBuilder.sourceRepoType("GITLAB").build())
            .build());
    assertThat(webhookTriggerFilters).isNotNull();
    assertThat(webhookTriggerFilters).containsExactlyInAnyOrder(triggerFiltersDefaultGit);

    webhookTriggerFilters = triggerFilterStore.getWebhookTriggerFilters(
        webhookPayloadDataBuilder.parseWebhookResponse(ParseWebhookResponse.newBuilder().build())
            .originalEvent(originalEventBuilder.sourceRepoType("BITBUCKET").build())
            .build());
    assertThat(webhookTriggerFilters).isNotNull();
    assertThat(webhookTriggerFilters).containsExactlyInAnyOrder(triggerFiltersDefaultGit);

    webhookTriggerFilters = triggerFilterStore.getWebhookTriggerFilters(
        webhookPayloadDataBuilder.parseWebhookResponse(ParseWebhookResponse.newBuilder().build())
            .originalEvent(originalEventBuilder.sourceRepoType("GITHUB").build())
            .build());
    assertThat(webhookTriggerFilters).isNotNull();
    assertThat(webhookTriggerFilters).containsExactlyInAnyOrder(triggerFiltersDefaultGit);

    webhookTriggerFilters = triggerFilterStore.getWebhookTriggerFilters(
        webhookPayloadDataBuilder
            .parseWebhookResponse(
                ParseWebhookResponse.newBuilder()
                    .setComment(
                        IssueCommentHook.newBuilder()
                            .setIssue(Issue.newBuilder().setPr(PullRequest.newBuilder().setNumber(1).build()).build())
                            .build())
                    .build())
            .originalEvent(originalEventBuilder.sourceRepoType("GITHUB").build())
            .build());
    assertThat(webhookTriggerFilters).isNotNull();
    assertThat(webhookTriggerFilters)
        .containsExactlyInAnyOrder(projectTriggerFilter, sourceRepoTypeTriggerFilter, eventActionTriggerFilter,
            gitWebhookTriggerRepoFilter, githubIssueCommentTriggerFilter, headerTriggerFilter);
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testGetCronTriggerUpdateOperations() {
    NGTriggerEntity updateEntity = NGTriggerEntity.builder()
                                       .accountId("accountId")
                                       .name("name")
                                       .identifier("identifier")
                                       .description("description")
                                       .nextIterations(Arrays.asList(1L, 2L, 3L, 4L))
                                       .build();
    assertThat(TriggerFilterHelper.getUpdateOperations(updateEntity).modifies(NGTriggerEntityKeys.nextIterations))
        .isTrue();
  }
}
