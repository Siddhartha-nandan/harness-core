/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.aws.asg.request;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awsconnector.AwsCapabilityHelper;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.aws.asg.AsgCommandTypeNG;
import io.harness.delegate.task.aws.asg.AsgInfraConfig;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.CDP)
public interface AsgCommandRequest extends TaskParameters, ExecutionCapabilityDemander {
    String getAccountId();
    @NotEmpty AsgCommandTypeNG getAsgCommandType();
    String getCommandName();
    CommandUnitsProgress getCommandUnitsProgress();
    AsgInfraConfig getAsgInfraConfig();
    Integer getTimeoutIntervalInMin();

    @Override
    default List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
        AsgInfraConfig asgInfraConfig = getAsgInfraConfig();
        List<EncryptedDataDetail> infraConfigEncryptionDataDetails = asgInfraConfig.getEncryptionDataDetails();

        List<ExecutionCapability> capabilities =
                new ArrayList<>(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
                        infraConfigEncryptionDataDetails, maskingEvaluator));

        AwsConnectorDTO awsConnectorDTO = asgInfraConfig.getAwsConnectorDTO();
        capabilities.addAll(AwsCapabilityHelper.fetchRequiredExecutionCapabilities(awsConnectorDTO, maskingEvaluator));
        return capabilities;
    }
}
