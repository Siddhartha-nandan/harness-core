/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.businessmapping.service.intf;

import io.harness.ccm.commons.entities.CCMSortOrder;
import io.harness.ccm.views.businessmapping.entities.BusinessMapping;
import io.harness.ccm.views.businessmapping.entities.BusinessMappingListDTO;
import io.harness.ccm.views.businessmapping.entities.CostCategorySortType;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BusinessMappingService {
  BusinessMapping save(BusinessMapping businessMapping);
  BusinessMapping get(String uuid, String accountId);
  BusinessMapping get(String uuid);
  BusinessMapping update(BusinessMapping newBusinessMapping, BusinessMapping oldBusinessMapping);
  boolean delete(String uuid, String accountIdentifier);
  BusinessMappingListDTO list(String accountId, String searchKey, CostCategorySortType sortType, CCMSortOrder sortOrder,
      Integer limit, Integer offset);
  List<ViewField> getBusinessMappingViewFields(String accountId);
  Set<String> getBusinessMappingIds(String accountId);
  List<String> getCostTargetNames(String businessMappingId, String accountId, String searchString);
  List<ViewFieldIdentifier> getBusinessMappingDataSources(String accountId, String businessMappingId);
}
