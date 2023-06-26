package io.harness.cdng.manifest.steps.task;

import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.delegate.beans.TaskData;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.tasks.ResponseData;

import java.util.Map;
import java.util.Optional;

public interface ManifestTaskService {
  boolean isSupported(Ambiance ambiance, ManifestOutcome manifest);

  Optional<TaskData> createTaskData(Ambiance ambiance, ManifestOutcome manifest);

  void handleTaskResponses(
      Map<String, ResponseData> responseDataMap, ManifestsOutcome manifests, Map<String, String> taskIdMapping);
}
