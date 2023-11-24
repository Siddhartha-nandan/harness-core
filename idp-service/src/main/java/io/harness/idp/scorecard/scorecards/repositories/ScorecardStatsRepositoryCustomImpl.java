/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.scorecards.repositories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.backstagebeans.BackstageCatalogEntityTypes;
import io.harness.idp.scorecard.scorecards.entity.ScorecardStatsEntity.ScorecardStatsKeys;
import io.harness.idp.scorecard.scorecards.entity.ScorecardStatsEntity;
import io.harness.idp.scorecard.scores.beans.StatsMetadata;
import io.harness.idp.scorecard.scores.entity.ScoreEntity;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import static io.harness.idp.common.DateUtils.yesterdayInMilliseconds;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ScorecardStatsRepositoryCustomImpl implements ScorecardStatsRepositoryCustom {
  private MongoTemplate mongoTemplate;
  @Override
  public ScorecardStatsEntity saveOrUpdate(ScoreEntity scoreEntity, BackstageCatalogEntity backstageCatalog) {
    Criteria criteria = Criteria.where(ScorecardStatsKeys.accountIdentifier)
            .is(scoreEntity.getAccountIdentifier())
            .and(ScorecardStatsKeys.entityIdentifier)
            .is(scoreEntity.getEntityIdentifier())
            .and(ScorecardStatsKeys.scorecardIdentifier)
            .is(scoreEntity.getScorecardIdentifier());
    ScorecardStatsEntity entity = mongoTemplate.findOne(Query.query(criteria), ScorecardStatsEntity.class);
    if (entity == null) {
      return ScorecardStatsEntity.builder()
              .accountIdentifier(scoreEntity.getAccountIdentifier())
              .entityIdentifier(scoreEntity.getEntityIdentifier())
              .scorecardIdentifier(scoreEntity.getScorecardIdentifier())
              .score(scoreEntity.getScore())
              .metadata(buildMetadata(backstageCatalog))
              .build();
    }
    if (entity.getLastUpdatedAt() == yesterdayInMilliseconds()){
      // Throwing exception on purpose to avoid DB writes by multiple pods
      throw new InvalidRequestException("Scorecard stats already computed for yesterday");
    }
    entity.setScore(scoreEntity.getScore());
    entity.setMetadata(buildMetadata(backstageCatalog));
    entity.setLastUpdatedAt(yesterdayInMilliseconds());
    return entity;
  }

  private StatsMetadata buildMetadata(BackstageCatalogEntity backstageCatalog) {
    return StatsMetadata.builder()
            .kind(backstageCatalog.getKind())
            .namespace(backstageCatalog.getMetadata().getNamespace())
            .name(backstageCatalog.getMetadata().getName())
            .type(BackstageCatalogEntityTypes.getEntityType(backstageCatalog))
            .owner(BackstageCatalogEntityTypes.getEntityOwner(backstageCatalog))
            .system(BackstageCatalogEntityTypes.getEntitySystem(backstageCatalog))
            .build();
  }
}
