/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.pojos;

import java.io.Serializable;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class PipelineExecutionSummaryCd implements Serializable {
  private static final long serialVersionUID = 1L;

  private String id;
  private String accountid;
  private String orgidentifier;
  private String projectidentifier;
  private String pipelineidentifier;
  private String name;
  private String status;
  private String moduleinfoType;
  private Long startts;
  private Long endts;
  private String planexecutionid;
  private String triggerType;
  private String authorName;
  private String moduleinfoAuthorId;
  private String authorAvatar;
  private String moduleinfoRepository;
  private String moduleinfoBranchName;
  private String sourceBranch;
  private String moduleinfoEvent;
  private String moduleinfoBranchCommitId;
  private String moduleinfoBranchCommitMessage;
  private String originalExecutionId;
  private Long meanTimeToRestore;
  private Boolean isRevertExecution;

  public PipelineExecutionSummaryCd() {}

  public PipelineExecutionSummaryCd(PipelineExecutionSummaryCd value) {
    this.id = value.id;
    this.accountid = value.accountid;
    this.orgidentifier = value.orgidentifier;
    this.projectidentifier = value.projectidentifier;
    this.pipelineidentifier = value.pipelineidentifier;
    this.name = value.name;
    this.status = value.status;
    this.moduleinfoType = value.moduleinfoType;
    this.startts = value.startts;
    this.endts = value.endts;
    this.planexecutionid = value.planexecutionid;
    this.triggerType = value.triggerType;
    this.authorName = value.authorName;
    this.moduleinfoAuthorId = value.moduleinfoAuthorId;
    this.authorAvatar = value.authorAvatar;
    this.moduleinfoRepository = value.moduleinfoRepository;
    this.moduleinfoBranchName = value.moduleinfoBranchName;
    this.sourceBranch = value.sourceBranch;
    this.moduleinfoEvent = value.moduleinfoEvent;
    this.moduleinfoBranchCommitId = value.moduleinfoBranchCommitId;
    this.moduleinfoBranchCommitMessage = value.moduleinfoBranchCommitMessage;
    this.originalExecutionId = value.originalExecutionId;
    this.meanTimeToRestore = value.meanTimeToRestore;
    this.isRevertExecution = value.isRevertExecution;
  }

  public PipelineExecutionSummaryCd(String id, String accountid, String orgidentifier, String projectidentifier,
      String pipelineidentifier, String name, String status, String moduleinfoType, Long startts, Long endts,
      String planexecutionid, String triggerType, String authorName, String moduleinfoAuthorId, String authorAvatar,
      String moduleinfoRepository, String moduleinfoBranchName, String sourceBranch, String moduleinfoEvent,
      String moduleinfoBranchCommitId, String moduleinfoBranchCommitMessage, String originalExecutionId,
      Long meanTimeToRestore, Boolean isRevertExecution) {
    this.id = id;
    this.accountid = accountid;
    this.orgidentifier = orgidentifier;
    this.projectidentifier = projectidentifier;
    this.pipelineidentifier = pipelineidentifier;
    this.name = name;
    this.status = status;
    this.moduleinfoType = moduleinfoType;
    this.startts = startts;
    this.endts = endts;
    this.planexecutionid = planexecutionid;
    this.triggerType = triggerType;
    this.authorName = authorName;
    this.moduleinfoAuthorId = moduleinfoAuthorId;
    this.authorAvatar = authorAvatar;
    this.moduleinfoRepository = moduleinfoRepository;
    this.moduleinfoBranchName = moduleinfoBranchName;
    this.sourceBranch = sourceBranch;
    this.moduleinfoEvent = moduleinfoEvent;
    this.moduleinfoBranchCommitId = moduleinfoBranchCommitId;
    this.moduleinfoBranchCommitMessage = moduleinfoBranchCommitMessage;
    this.originalExecutionId = originalExecutionId;
    this.meanTimeToRestore = meanTimeToRestore;
    this.isRevertExecution = isRevertExecution;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.id</code>.
   */
  public String getId() {
    return this.id;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.id</code>.
   */
  public PipelineExecutionSummaryCd setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.accountid</code>.
   */
  public String getAccountid() {
    return this.accountid;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.accountid</code>.
   */
  public PipelineExecutionSummaryCd setAccountid(String accountid) {
    this.accountid = accountid;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.orgidentifier</code>.
   */
  public String getOrgidentifier() {
    return this.orgidentifier;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.orgidentifier</code>.
   */
  public PipelineExecutionSummaryCd setOrgidentifier(String orgidentifier) {
    this.orgidentifier = orgidentifier;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.projectidentifier</code>.
   */
  public String getProjectidentifier() {
    return this.projectidentifier;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.projectidentifier</code>.
   */
  public PipelineExecutionSummaryCd setProjectidentifier(String projectidentifier) {
    this.projectidentifier = projectidentifier;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.pipelineidentifier</code>.
   */
  public String getPipelineidentifier() {
    return this.pipelineidentifier;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.pipelineidentifier</code>.
   */
  public PipelineExecutionSummaryCd setPipelineidentifier(String pipelineidentifier) {
    this.pipelineidentifier = pipelineidentifier;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.name</code>.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.name</code>.
   */
  public PipelineExecutionSummaryCd setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.status</code>.
   */
  public String getStatus() {
    return this.status;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.status</code>.
   */
  public PipelineExecutionSummaryCd setStatus(String status) {
    this.status = status;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.moduleinfo_type</code>.
   */
  public String getModuleinfoType() {
    return this.moduleinfoType;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.moduleinfo_type</code>.
   */
  public PipelineExecutionSummaryCd setModuleinfoType(String moduleinfoType) {
    this.moduleinfoType = moduleinfoType;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.startts</code>.
   */
  public Long getStartts() {
    return this.startts;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.startts</code>.
   */
  public PipelineExecutionSummaryCd setStartts(Long startts) {
    this.startts = startts;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.endts</code>.
   */
  public Long getEndts() {
    return this.endts;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.endts</code>.
   */
  public PipelineExecutionSummaryCd setEndts(Long endts) {
    this.endts = endts;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.planexecutionid</code>.
   */
  public String getPlanexecutionid() {
    return this.planexecutionid;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.planexecutionid</code>.
   */
  public PipelineExecutionSummaryCd setPlanexecutionid(String planexecutionid) {
    this.planexecutionid = planexecutionid;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.trigger_type</code>.
   */
  public String getTriggerType() {
    return this.triggerType;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.trigger_type</code>.
   */
  public PipelineExecutionSummaryCd setTriggerType(String triggerType) {
    this.triggerType = triggerType;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.author_name</code>.
   */
  public String getAuthorName() {
    return this.authorName;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.author_name</code>.
   */
  public PipelineExecutionSummaryCd setAuthorName(String authorName) {
    this.authorName = authorName;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.moduleinfo_author_id</code>.
   */
  public String getModuleinfoAuthorId() {
    return this.moduleinfoAuthorId;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.moduleinfo_author_id</code>.
   */
  public PipelineExecutionSummaryCd setModuleinfoAuthorId(String moduleinfoAuthorId) {
    this.moduleinfoAuthorId = moduleinfoAuthorId;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.author_avatar</code>.
   */
  public String getAuthorAvatar() {
    return this.authorAvatar;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.author_avatar</code>.
   */
  public PipelineExecutionSummaryCd setAuthorAvatar(String authorAvatar) {
    this.authorAvatar = authorAvatar;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.moduleinfo_repository</code>.
   */
  public String getModuleinfoRepository() {
    return this.moduleinfoRepository;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.moduleinfo_repository</code>.
   */
  public PipelineExecutionSummaryCd setModuleinfoRepository(String moduleinfoRepository) {
    this.moduleinfoRepository = moduleinfoRepository;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.moduleinfo_branch_name</code>.
   */
  public String getModuleinfoBranchName() {
    return this.moduleinfoBranchName;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.moduleinfo_branch_name</code>.
   */
  public PipelineExecutionSummaryCd setModuleinfoBranchName(String moduleinfoBranchName) {
    this.moduleinfoBranchName = moduleinfoBranchName;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.source_branch</code>.
   */
  public String getSourceBranch() {
    return this.sourceBranch;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.source_branch</code>.
   */
  public PipelineExecutionSummaryCd setSourceBranch(String sourceBranch) {
    this.sourceBranch = sourceBranch;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.moduleinfo_event</code>.
   */
  public String getModuleinfoEvent() {
    return this.moduleinfoEvent;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.moduleinfo_event</code>.
   */
  public PipelineExecutionSummaryCd setModuleinfoEvent(String moduleinfoEvent) {
    this.moduleinfoEvent = moduleinfoEvent;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.moduleinfo_branch_commit_id</code>.
   */
  public String getModuleinfoBranchCommitId() {
    return this.moduleinfoBranchCommitId;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.moduleinfo_branch_commit_id</code>.
   */
  public PipelineExecutionSummaryCd setModuleinfoBranchCommitId(String moduleinfoBranchCommitId) {
    this.moduleinfoBranchCommitId = moduleinfoBranchCommitId;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.moduleinfo_branch_commit_message</code>.
   */
  public String getModuleinfoBranchCommitMessage() {
    return this.moduleinfoBranchCommitMessage;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.moduleinfo_branch_commit_message</code>.
   */
  public PipelineExecutionSummaryCd setModuleinfoBranchCommitMessage(String moduleinfoBranchCommitMessage) {
    this.moduleinfoBranchCommitMessage = moduleinfoBranchCommitMessage;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.original_execution_id</code>.
   */
  public String getOriginalExecutionId() {
    return this.originalExecutionId;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.original_execution_id</code>.
   */
  public PipelineExecutionSummaryCd setOriginalExecutionId(String originalExecutionId) {
    this.originalExecutionId = originalExecutionId;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.mean_time_to_restore</code>.
   */
  public Long getMeanTimeToRestore() {
    return this.meanTimeToRestore;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.mean_time_to_restore</code>.
   */
  public PipelineExecutionSummaryCd setMeanTimeToRestore(Long meanTimeToRestore) {
    this.meanTimeToRestore = meanTimeToRestore;
    return this;
  }

  /**
   * Getter for <code>public.pipeline_execution_summary_cd.is_revert_execution</code>.
   */
  public Boolean getIsRevertExecution() {
    return this.isRevertExecution;
  }

  /**
   * Setter for <code>public.pipeline_execution_summary_cd.is_revert_execution</code>.
   */
  public PipelineExecutionSummaryCd setIsRevertExecution(Boolean isRevertExecution) {
    this.isRevertExecution = isRevertExecution;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final PipelineExecutionSummaryCd other = (PipelineExecutionSummaryCd) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (accountid == null) {
      if (other.accountid != null)
        return false;
    } else if (!accountid.equals(other.accountid))
      return false;
    if (orgidentifier == null) {
      if (other.orgidentifier != null)
        return false;
    } else if (!orgidentifier.equals(other.orgidentifier))
      return false;
    if (projectidentifier == null) {
      if (other.projectidentifier != null)
        return false;
    } else if (!projectidentifier.equals(other.projectidentifier))
      return false;
    if (pipelineidentifier == null) {
      if (other.pipelineidentifier != null)
        return false;
    } else if (!pipelineidentifier.equals(other.pipelineidentifier))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (status == null) {
      if (other.status != null)
        return false;
    } else if (!status.equals(other.status))
      return false;
    if (moduleinfoType == null) {
      if (other.moduleinfoType != null)
        return false;
    } else if (!moduleinfoType.equals(other.moduleinfoType))
      return false;
    if (startts == null) {
      if (other.startts != null)
        return false;
    } else if (!startts.equals(other.startts))
      return false;
    if (endts == null) {
      if (other.endts != null)
        return false;
    } else if (!endts.equals(other.endts))
      return false;
    if (planexecutionid == null) {
      if (other.planexecutionid != null)
        return false;
    } else if (!planexecutionid.equals(other.planexecutionid))
      return false;
    if (triggerType == null) {
      if (other.triggerType != null)
        return false;
    } else if (!triggerType.equals(other.triggerType))
      return false;
    if (authorName == null) {
      if (other.authorName != null)
        return false;
    } else if (!authorName.equals(other.authorName))
      return false;
    if (moduleinfoAuthorId == null) {
      if (other.moduleinfoAuthorId != null)
        return false;
    } else if (!moduleinfoAuthorId.equals(other.moduleinfoAuthorId))
      return false;
    if (authorAvatar == null) {
      if (other.authorAvatar != null)
        return false;
    } else if (!authorAvatar.equals(other.authorAvatar))
      return false;
    if (moduleinfoRepository == null) {
      if (other.moduleinfoRepository != null)
        return false;
    } else if (!moduleinfoRepository.equals(other.moduleinfoRepository))
      return false;
    if (moduleinfoBranchName == null) {
      if (other.moduleinfoBranchName != null)
        return false;
    } else if (!moduleinfoBranchName.equals(other.moduleinfoBranchName))
      return false;
    if (sourceBranch == null) {
      if (other.sourceBranch != null)
        return false;
    } else if (!sourceBranch.equals(other.sourceBranch))
      return false;
    if (moduleinfoEvent == null) {
      if (other.moduleinfoEvent != null)
        return false;
    } else if (!moduleinfoEvent.equals(other.moduleinfoEvent))
      return false;
    if (moduleinfoBranchCommitId == null) {
      if (other.moduleinfoBranchCommitId != null)
        return false;
    } else if (!moduleinfoBranchCommitId.equals(other.moduleinfoBranchCommitId))
      return false;
    if (moduleinfoBranchCommitMessage == null) {
      if (other.moduleinfoBranchCommitMessage != null)
        return false;
    } else if (!moduleinfoBranchCommitMessage.equals(other.moduleinfoBranchCommitMessage))
      return false;
    if (originalExecutionId == null) {
      if (other.originalExecutionId != null)
        return false;
    } else if (!originalExecutionId.equals(other.originalExecutionId))
      return false;
    if (meanTimeToRestore == null) {
      if (other.meanTimeToRestore != null)
        return false;
    } else if (!meanTimeToRestore.equals(other.meanTimeToRestore))
      return false;
    if (isRevertExecution == null) {
      if (other.isRevertExecution != null)
        return false;
    } else if (!isRevertExecution.equals(other.isRevertExecution))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
    result = prime * result + ((this.accountid == null) ? 0 : this.accountid.hashCode());
    result = prime * result + ((this.orgidentifier == null) ? 0 : this.orgidentifier.hashCode());
    result = prime * result + ((this.projectidentifier == null) ? 0 : this.projectidentifier.hashCode());
    result = prime * result + ((this.pipelineidentifier == null) ? 0 : this.pipelineidentifier.hashCode());
    result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
    result = prime * result + ((this.status == null) ? 0 : this.status.hashCode());
    result = prime * result + ((this.moduleinfoType == null) ? 0 : this.moduleinfoType.hashCode());
    result = prime * result + ((this.startts == null) ? 0 : this.startts.hashCode());
    result = prime * result + ((this.endts == null) ? 0 : this.endts.hashCode());
    result = prime * result + ((this.planexecutionid == null) ? 0 : this.planexecutionid.hashCode());
    result = prime * result + ((this.triggerType == null) ? 0 : this.triggerType.hashCode());
    result = prime * result + ((this.authorName == null) ? 0 : this.authorName.hashCode());
    result = prime * result + ((this.moduleinfoAuthorId == null) ? 0 : this.moduleinfoAuthorId.hashCode());
    result = prime * result + ((this.authorAvatar == null) ? 0 : this.authorAvatar.hashCode());
    result = prime * result + ((this.moduleinfoRepository == null) ? 0 : this.moduleinfoRepository.hashCode());
    result = prime * result + ((this.moduleinfoBranchName == null) ? 0 : this.moduleinfoBranchName.hashCode());
    result = prime * result + ((this.sourceBranch == null) ? 0 : this.sourceBranch.hashCode());
    result = prime * result + ((this.moduleinfoEvent == null) ? 0 : this.moduleinfoEvent.hashCode());
    result = prime * result + ((this.moduleinfoBranchCommitId == null) ? 0 : this.moduleinfoBranchCommitId.hashCode());
    result = prime * result
        + ((this.moduleinfoBranchCommitMessage == null) ? 0 : this.moduleinfoBranchCommitMessage.hashCode());
    result = prime * result + ((this.originalExecutionId == null) ? 0 : this.originalExecutionId.hashCode());
    result = prime * result + ((this.meanTimeToRestore == null) ? 0 : this.meanTimeToRestore.hashCode());
    result = prime * result + ((this.isRevertExecution == null) ? 0 : this.isRevertExecution.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("PipelineExecutionSummaryCd (");

    sb.append(id);
    sb.append(", ").append(accountid);
    sb.append(", ").append(orgidentifier);
    sb.append(", ").append(projectidentifier);
    sb.append(", ").append(pipelineidentifier);
    sb.append(", ").append(name);
    sb.append(", ").append(status);
    sb.append(", ").append(moduleinfoType);
    sb.append(", ").append(startts);
    sb.append(", ").append(endts);
    sb.append(", ").append(planexecutionid);
    sb.append(", ").append(triggerType);
    sb.append(", ").append(authorName);
    sb.append(", ").append(moduleinfoAuthorId);
    sb.append(", ").append(authorAvatar);
    sb.append(", ").append(moduleinfoRepository);
    sb.append(", ").append(moduleinfoBranchName);
    sb.append(", ").append(sourceBranch);
    sb.append(", ").append(moduleinfoEvent);
    sb.append(", ").append(moduleinfoBranchCommitId);
    sb.append(", ").append(moduleinfoBranchCommitMessage);
    sb.append(", ").append(originalExecutionId);
    sb.append(", ").append(meanTimeToRestore);
    sb.append(", ").append(isRevertExecution);

    sb.append(")");
    return sb.toString();
  }
}
