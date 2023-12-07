/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.entitydetail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.matcher.Matchers;
import io.harness.CategoryTest;
import io.harness.beans.EntityReference;
import io.harness.beans.IdentifierRef;
import io.harness.beans.InfraDefReference;
import io.harness.beans.InputSetReference;
import io.harness.beans.NGTemplateReference;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.filter.ApiResponseFilter;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.spec.server.commons.v1.model.ErrorResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.UriInfo;

import static io.serializer.HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EntityDetailDeserializerTest extends CategoryTest {

  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    this.objectMapper = NG_DEFAULT_OBJECT_MAPPER;
  }

  @Test
  @Owner(developers = OwnerRule.LOVISH_BANSAL)
  @Category(UnitTests.class)
  public void testObjectMapperSuccess() throws JsonProcessingException {
     String entityRefNodeWithoutScope = "{\"accountIdentifier\":\"kmpySmUISimoRrJL6NL73w\",\"orgIdentifier\":\"Ng_Pipelines_K8s_Organisations\",\"projectIdentifier\":\"NGPipeAutoTasIcjyZn4Qpo\",\"envIdentifier\":\"e1\",\"identifier\":\"i1\",\"repoIdentifier\":null,\"branch\":null,\"isDefault\":true,\"default\":true,\"metadata\":{}}";
    String entityRefNodeWithScope = "{\"accountIdentifier\":\"kmpySmUISimoRrJL6NL73w\",\"orgIdentifier\":\"Ng_Pipelines_K8s_Organisations\",\"projectIdentifier\":\"NGPipeAutoTasIcjyZn4Qpo\",\"envIdentifier\":\"e1\",\"identifier\":\"i1\",\"repoIdentifier\":null,\"branch\":null,\"isDefault\":true,\"scope\":\"project\",\"default\":true,\"metadata\":{}}";

    Integer flag = 1;
    EntityReference reference;
     if(flag.equals(1)) {
       reference = objectMapper.readValue(entityRefNodeWithScope, InfraDefReference.class);
       reference = objectMapper.readValue(entityRefNodeWithoutScope, InfraDefReference.class);
     }
    if(flag.equals(2)) {
      reference = objectMapper.readValue(entityRefNodeWithoutScope, IdentifierRef.class);
    }
    if(flag.equals(3)) {
      reference = objectMapper.readValue(entityRefNodeWithoutScope, NGTemplateReference.class);
    }
    if(flag.equals(4)) {
      reference = objectMapper.readValue(entityRefNodeWithoutScope, InputSetReference.class);
    }
    ;
  }

}
