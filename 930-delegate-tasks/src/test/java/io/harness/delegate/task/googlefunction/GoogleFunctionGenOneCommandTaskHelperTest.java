/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.googlefunction;

import com.google.cloud.functions.v1.CloudFunction;
import com.google.cloud.functions.v1.CloudFunctionStatus;
import com.google.cloud.functions.v1.CreateFunctionRequest;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.task.googlefunctionbeans.GcpGoogleFunctionInfraConfig;
import io.harness.googlefunctions.GoogleCloudFunctionGenOneClient;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.ExecutionException;

import static io.harness.rule.OwnerRule.PRAGYESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GoogleFunctionGenOneCommandTaskHelperTest extends CategoryTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    private final String PROJECT = "cd-play";
    private final String REGION = "us-east1";
    private final String BUCKET = "bucket";
    private final String FUNCTION = "function";
    private final Long TIMEOUT = 10L;

    @InjectMocks
    private GoogleFunctionGenOneCommandTaskHelper googleFunctionGenOneCommandTaskHelper;

    private GcpGoogleFunctionInfraConfig gcpGoogleFunctionInfraConfig;
    @Mock private LogCallback logCallback;
    @Mock private GoogleCloudFunctionGenOneClient googleCloudFunctionGenOneClient;
    private CloudFunction function;


    @Before
    public void setUp() throws Exception {
        gcpGoogleFunctionInfraConfig =
                GcpGoogleFunctionInfraConfig.builder()
                        .region(REGION)
                        .project(PROJECT)
                        .gcpConnectorDTO(GcpConnectorDTO.builder()
                                .credential(GcpConnectorCredentialDTO.builder()
                                        .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                                        .build())
                                .build())
                        .build();
        function = CloudFunction.newBuilder()
                .setName("FUNCTION")
                .setStatus(CloudFunctionStatus.ACTIVE)
                .build();
    }

    @Test
    @Owner(developers = PRAGYESH)
    @Category(UnitTests.class)
    public void createFunctionTest() throws ExecutionException, InterruptedException {
        when(googleCloudFunctionGenOneClient.getFunction(any(), any())).thenReturn(function);
        when(googleCloudFunctionGenOneClient.getFunction(any(), any(), any(), any()).thenReturn(function);
        CloudFunction function =
                googleFunctionGenOneCommandTaskHelper.createFunction(CreateFunctionRequest.newBuilder().build(),
                        gcpGoogleFunctionInfraConfig, logCallback, TIMEOUT);
        verify(googleCloudFunctionGenOneClient).createFunction(any(), any());
        verify(googleCloudFunctionGenOneClient).getOperation(any(), any());
        verify(googleCloudFunctionGenOneClient, times(2)).getFunction(any(), any());
        assertThat(function.getName()).isEqualTo(function.getName());
        assertThat(function.getStatus()).isEqualTo(function.getStatus());
    }


}
