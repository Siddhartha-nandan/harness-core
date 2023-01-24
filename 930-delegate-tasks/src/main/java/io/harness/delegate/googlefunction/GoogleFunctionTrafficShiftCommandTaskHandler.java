package io.harness.delegate.googlefunction;

import com.google.cloud.functions.v2.Function;
import com.google.cloud.run.v2.Service;
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
import io.harness.delegate.task.googlefunctionbeans.request.GoogleFunctionTrafficShiftRequest;
import io.harness.delegate.task.googlefunctionbeans.response.GoogleFunctionCommandResponse;
import io.harness.delegate.task.googlefunctionbeans.response.GoogleFunctionTrafficShiftResponse;
import io.harness.googlefunctions.command.GoogleFunctionsCommandUnitConstants;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.lang.String.format;
import static software.wings.beans.LogColor.Green;
import static software.wings.beans.LogHelper.color;

@OwnedBy(HarnessTeam.CDP)
@NoArgsConstructor
@Slf4j
public class GoogleFunctionTrafficShiftCommandTaskHandler extends GoogleFunctionCommandTaskHandler {
    @Inject
    private GoogleFunctionCommandTaskHelper googleFunctionCommandTaskHelper;

    @Override
    protected GoogleFunctionCommandResponse executeTaskInternal(GoogleFunctionCommandRequest googleFunctionCommandRequest,
                                                                ILogStreamingTaskClient iLogStreamingTaskClient,
                                                                CommandUnitsProgress commandUnitsProgress) throws Exception {
        GoogleFunctionTrafficShiftRequest googleFunctionTrafficShiftRequest =
                (GoogleFunctionTrafficShiftRequest) googleFunctionCommandRequest;
        GcpGoogleFunctionInfraConfig googleFunctionInfraConfig =
                (GcpGoogleFunctionInfraConfig) googleFunctionTrafficShiftRequest.getGoogleFunctionInfraConfig();
        try {
            LogCallback executionLogCallback = new NGDelegateLogCallback(iLogStreamingTaskClient,
                    GoogleFunctionsCommandUnitConstants.trafficShift.toString(),
                    true, commandUnitsProgress);
            if (!googleFunctionTrafficShiftRequest.isFirstDeployment()) {
                executionLogCallback.saveExecutionLog(format("Starting traffic shift..%n%n"), LogLevel.INFO);
                Function.Builder functionBuilder = Function.newBuilder();
                googleFunctionCommandTaskHelper.parseStringContentAsClassBuilder(
                        googleFunctionTrafficShiftRequest.getGoogleFunctionAsString(),
                        functionBuilder, "cloudFunction");
                Service.Builder serviceBuilder = Service.newBuilder();
                googleFunctionCommandTaskHelper.parseStringContentAsClassBuilder(
                        googleFunctionTrafficShiftRequest.getGoogleCloudRunServiceAsString(),
                        serviceBuilder, "cloudRunService");
                String existingRevision = googleFunctionCommandTaskHelper.getCurrentRevision(serviceBuilder.build());

                googleFunctionCommandTaskHelper.updateTraffic(serviceBuilder.getName(), googleFunctionTrafficShiftRequest.getTargetTrafficPercent(),
                        googleFunctionTrafficShiftRequest.getTargetRevision(), existingRevision,
                        googleFunctionInfraConfig.getGcpConnectorDTO(),
                        googleFunctionInfraConfig.getProject(), googleFunctionInfraConfig.getRegion(), executionLogCallback);
                Function function = googleFunctionCommandTaskHelper.getFunction(functionBuilder.getName(),
                        googleFunctionInfraConfig.getGcpConnectorDTO(),
                        googleFunctionInfraConfig.getProject(), googleFunctionInfraConfig.getRegion()).get();
                GoogleFunction googleFunction = googleFunctionCommandTaskHelper.getGoogleFunction(function,
                        googleFunctionInfraConfig, executionLogCallback);
                executionLogCallback.saveExecutionLog(color("Done",Green), LogLevel.INFO, CommandExecutionStatus.SUCCESS);
                return GoogleFunctionTrafficShiftResponse.builder()
                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                        .function(googleFunction)
                        .build();
            }
            return GoogleFunctionTrafficShiftResponse.builder()
                    .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                    .errorMessage("traffic shift not allowed with first deployment")
                    .build();
        }
         catch (Exception exception) {
            throw new GoogleFunctionException(exception);
        }
    }
}
