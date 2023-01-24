package io.harness.delegate.googlefunction;

import com.google.cloud.functions.v2.Function;
import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.exception.GoogleFunctionException;
import io.harness.delegate.task.googlefunction.GoogleFunctionCommandTaskHelper;
import io.harness.delegate.task.googlefunctionbeans.GcpGoogleFunctionInfraConfig;
import io.harness.delegate.task.googlefunctionbeans.GoogleFunction;
import io.harness.delegate.task.googlefunctionbeans.request.GoogleFunctionCommandRequest;
import io.harness.delegate.task.googlefunctionbeans.request.GoogleFunctionDeployWithoutTrafficRequest;
import io.harness.delegate.task.googlefunctionbeans.response.GoogleFunctionCommandResponse;
import io.harness.delegate.task.googlefunctionbeans.response.GoogleFunctionDeployWithoutTrafficResponse;
import io.harness.googlefunctions.command.GoogleFunctionsCommandUnitConstants;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.lang.String.format;

@OwnedBy(HarnessTeam.CDP)
@NoArgsConstructor
@Slf4j
public class GoogleFunctionDeployWithoutTrafficCommandTaskHandler extends GoogleFunctionCommandTaskHandler {
    @Inject private GoogleFunctionCommandTaskHelper googleFunctionCommandTaskHelper;

    @Override
    protected GoogleFunctionCommandResponse executeTaskInternal(GoogleFunctionCommandRequest
                                                                            googleFunctionCommandRequest,
                                                                ILogStreamingTaskClient iLogStreamingTaskClient,
                                                                CommandUnitsProgress commandUnitsProgress) throws Exception {
        GoogleFunctionDeployWithoutTrafficRequest googleFunctionDeployWithoutTrafficRequest =
                (GoogleFunctionDeployWithoutTrafficRequest) googleFunctionCommandRequest;
        GcpGoogleFunctionInfraConfig googleFunctionInfraConfig =
                (GcpGoogleFunctionInfraConfig) googleFunctionDeployWithoutTrafficRequest.getGoogleFunctionInfraConfig();
        try{
            LogCallback executionLogCallback = new NGDelegateLogCallback(iLogStreamingTaskClient,
                    GoogleFunctionsCommandUnitConstants.deploy.toString(),
                    true, commandUnitsProgress);
            executionLogCallback.saveExecutionLog(format("Deploying..%n%n"), LogLevel.INFO);
            Function function = googleFunctionCommandTaskHelper.deployFunction(googleFunctionInfraConfig,
                    googleFunctionDeployWithoutTrafficRequest.getGoogleFunctionDeployManifestContent(),
                    googleFunctionDeployWithoutTrafficRequest.getUpdateFieldMaskContent(),
                    googleFunctionDeployWithoutTrafficRequest.getGoogleFunctionArtifactConfig(), false,
                    executionLogCallback);

            GoogleFunction googleFunction = googleFunctionCommandTaskHelper.getGoogleFunction(function,
                    googleFunctionInfraConfig);

            executionLogCallback.saveExecutionLog(
                    format("Done"), LogLevel.INFO, CommandExecutionStatus.SUCCESS);
            return GoogleFunctionDeployWithoutTrafficResponse.builder()
                    .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                    .function(googleFunction)
                    .build();
        }
        catch (Exception exception) {
            throw new GoogleFunctionException(exception);
        }
    }
}
