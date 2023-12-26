/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datapointsdata.dsldataprovider.factory;

import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.BITBUCKET_FILE_CONTAINS;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.BITBUCKET_FILE_CONTENTS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.datapointsdata.dsldataprovider.base.DataSourceDsl;
import io.harness.idp.scorecard.datapointsdata.dsldataprovider.base.DslDataProvider;
import io.harness.idp.scorecard.datapointsdata.dsldataprovider.impl.BitbucketContentDsl;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class BitbucketDslFactory implements DataSourceDsl {
  BitbucketContentDsl bitbucketContentDsl;

  @Override
  public DslDataProvider getDslDataProvider(String dslIdentifier) {
    switch (dslIdentifier) {
      case BITBUCKET_FILE_CONTENTS:
      case BITBUCKET_FILE_CONTAINS:
        return bitbucketContentDsl;
      default:
        throw new UnsupportedOperationException(
            String.format("For data source - Bitbucket, Dsl is not supported - %s", dslIdentifier));
    }
  }
}
