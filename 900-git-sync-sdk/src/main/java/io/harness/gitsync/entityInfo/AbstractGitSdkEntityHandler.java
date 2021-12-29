package io.harness.gitsync.entityInfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.gitsync.scm.EntityObjectIdUtils;
import io.harness.gitsync.scm.ScmGitUtils;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.manage.GlobalContextManager;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DX)
public abstract class AbstractGitSdkEntityHandler<B extends GitSyncableEntity, Y extends YamlDTO>
    implements GitSdkEntityHandlerInterface<B, Y> {
  @Override
  public Y upsert(String accountIdentifier, String yaml, String filePath) {
    final Optional<EntityGitDetails> entityGitDetailsOptional = getEntityDetailsIfExists(accountIdentifier, yaml);
    if (entityGitDetailsOptional.isPresent()) {
      final EntityGitDetails entityGitDetails = entityGitDetailsOptional.get();
      final String objectIdOfNewYaml = EntityObjectIdUtils.getObjectIdOfYaml(yaml);
      final String lastCompleteFilePath =
          ScmGitUtils.createFilePath(entityGitDetails.getRootFolder(), entityGitDetails.getFilePath());
      if (entityGitDetails.getObjectId().equals(objectIdOfNewYaml)) {
        if (lastCompleteFilePath.equals(filePath)) {
          log.info("Object already processed hence skipping database update.");
          return getYamlDTO(yaml);
        } else {
          throw new InvalidRequestException("An entity with the given name and identifier already exists");
        }
      }
      log.info("Object Id or FilePath differs for database object: [{}] and git object: [{}] hence updating.",
          entityGitDetails.getObjectId(), objectIdOfNewYaml);
      return update(accountIdentifier, yaml, ChangeType.MODIFY);
    } else {
      log.info("Object not found for yaml hence creating in database");
      return save(accountIdentifier, yaml);
    }
  }

  public abstract Optional<EntityGitDetails> getEntityDetailsIfExists(String accountIdentifier, String yaml);

  public abstract Y getYamlDTO(String yaml);

  @Override
  public Y fullSyncEntity(FullSyncChangeSet fullSyncChangeSet) {
    final EntityDetailProtoDTO entityDetail = fullSyncChangeSet.getEntityDetail();
    final String yaml = getYamlFromEntityRef(entityDetail);
    try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {
      GlobalContextManager.upsertGlobalContextRecord(createGitEntityInfo(fullSyncChangeSet));
      return update(fullSyncChangeSet.getAccountIdentifier(), yaml, ChangeType.ADD);
    }
  }

  public GitSyncBranchContext createGitEntityInfo(FullSyncChangeSet fullSyncChangeSet) {
    return GitSyncBranchContext.builder()
        .gitBranchInfo(GitEntityInfo.builder()
                           .branch(fullSyncChangeSet.getBranchName())
                           .folderPath(fullSyncChangeSet.getFolderPath())
                           .filePath(fullSyncChangeSet.getFilePath())
                           .yamlGitConfigId(fullSyncChangeSet.getYamlGitConfigIdentifier())
                           .isFullSyncFlow(true)
                           .commitMsg(fullSyncChangeSet.getCommitMessage())
                           .build())
        .build();
  }

  public abstract String getYamlFromEntityRef(EntityDetailProtoDTO entityReference);
}
