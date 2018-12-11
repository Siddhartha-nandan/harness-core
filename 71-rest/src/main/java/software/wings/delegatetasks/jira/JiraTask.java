package software.wings.delegatetasks.jira;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.JiraExecutionData.JiraApprovalActionType.CREATE_WEBHOOK;
import static software.wings.api.JiraExecutionData.JiraApprovalActionType.DELETE_WEBHOOK;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.delegate.task.protocol.TaskParameters;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Issue.FluentUpdate;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.Resource;
import net.rcarz.jiraclient.RestException;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.JiraExecutionData;
import software.wings.app.MainConfiguration;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.JiraConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.beans.jira.JiraWebhookParameters;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JiraTask extends AbstractDelegateRunnableTask {
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;
  @Inject private MainConfiguration mainConfiguration;

  private static final String WEBHOOK_CREATION_URL = "/rest/webhooks/1.0/webhook/";
  private static final String JIRA_APPROVAL_API_PATH = "api/ticketing/jira-approval/";

  private static final Logger logger = LoggerFactory.getLogger(JiraTask.class);

  public JiraTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ResponseData run(Object[] parameters) {
    return run((JiraTaskParameters) parameters[0]);
  }

  public ResponseData run(JiraTaskParameters parameters) {
    JiraAction jiraAction = parameters.getJiraAction();

    switch (jiraAction) {
      case AUTH:
        break;

      case UPDATE_TICKET:
        return updateTicket(parameters);

      case CREATE_TICKET:
        return createTicket(parameters);

      case APPROVE_TICKET:
        return createWebhook(parameters);

      case CREATE_AND_APPROVE_TICKET:
        return createTicketAndWebhook(parameters);

      case DELETE_WEBHOOK:
        return deleteWebhook(parameters);

      case GET_PROJECTS:
        return getProjects(parameters);

      case GET_FIELDS:
        return getFields(parameters);

      default:
        break;
    }

    return null;
  }

  private ResponseData getFields(JiraTaskParameters parameters) {
    JiraClient jiraClient = getJiraClient(parameters);

    URI uri = null;
    JSONArray fieldsArray = null;
    try {
      uri = jiraClient.getRestClient().buildURI(Resource.getBaseUri() + "field");
      JSON response = jiraClient.getRestClient().get(uri);
      fieldsArray = JSONArray.fromObject(response);
    } catch (URISyntaxException | IOException | RestException e) {
      String errorMessage = "Failed to fetch fields from JIRA server.";
      logger.error(errorMessage);

      return JiraExecutionData.builder().errorMessage(errorMessage).build();
    }

    return JiraExecutionData.builder().fields(fieldsArray).build();
  }

  private ResponseData getProjects(JiraTaskParameters parameters) {
    JiraClient jira = getJiraClient(parameters);

    JSONArray projectsArray = null;
    try {
      URI uri = jira.getRestClient().buildURI(Resource.getBaseUri() + "project");
      JSON response = jira.getRestClient().get(uri);
      projectsArray = JSONArray.fromObject(response);
    } catch (URISyntaxException | IOException | RestException e) {
      String errorMessage = "Failed to fetch projects from JIRA server.";
      logger.error(errorMessage);

      return JiraExecutionData.builder().errorMessage(errorMessage).build();
    }

    return JiraExecutionData.builder().projects(projectsArray).build();
  }

  private ResponseData updateTicket(JiraTaskParameters parameters) {
    JiraClient jira = getJiraClient(parameters);

    Issue issue = null;
    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.RUNNING;
    try {
      issue = jira.getIssue(parameters.getIssueId());
      boolean fieldsUpdated = false;

      FluentUpdate update = issue.update();
      if (EmptyPredicate.isNotEmpty(parameters.getSummary())) {
        update.field(Field.SUMMARY, parameters.getSummary());
        fieldsUpdated = true;
      }

      if (EmptyPredicate.isNotEmpty(parameters.getLabels())) {
        update.field(Field.LABELS, parameters.getLabels());
        fieldsUpdated = true;
      }

      if (fieldsUpdated) {
        update.execute();
      }

      if (EmptyPredicate.isNotEmpty(parameters.getComment())) {
        issue.addComment(parameters.getComment());
      }

      if (EmptyPredicate.isNotEmpty(parameters.getStatus())) {
        issue.transition().execute(parameters.getStatus());
      }

    } catch (JiraException e) {
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      String errorMessage = "Failed to update the new JIRA ticket " + parameters.getIssueId();
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(errorMessage).build();
    }

    commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    saveExecutionLog(
        parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .errorMessage("Updated JIRA ticket " + issue.getKey() + " at " + getIssueUrl(parameters.getJiraConfig(), issue))
        .build();
  }

  private ResponseData createTicket(JiraTaskParameters parameters) {
    JiraClient jira = getJiraClient(parameters);
    Issue issue = null;

    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.RUNNING;
    try {
      issue = jira.createIssue(parameters.getProject(), parameters.getIssueType())
                  .field(Field.SUMMARY, parameters.getSummary())
                  .field(Field.DESCRIPTION, parameters.getDescription())
                  .field(Field.ASSIGNEE, parameters.getAssignee())
                  .field(Field.LABELS, parameters.getLabels())
                  .execute();

      if (isNotBlank(parameters.getStatus())) {
        issue.transition().execute(parameters.getStatus());
      }
      commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    } catch (JiraException e) {
      logger.error("Unable to create a new JIRA ticket", e);
      commandExecutionStatus = CommandExecutionStatus.FAILURE;

      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);
      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage("Unable to create the new JIRA ticket " + parameters.getIssueId())
          .build();
    }

    saveExecutionLog(
        parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .jiraAction(JiraAction.CREATE_TICKET)
        .errorMessage("Created JIRA ticket " + issue.getKey() + " at " + getIssueUrl(parameters.getJiraConfig(), issue))
        .issueId(issue.getId())
        .build();
  }

  private String getIssueUrl(JiraConfig jiraConfig, Issue issue) {
    try {
      URL jiraUrl = new URL(jiraConfig.getBaseUrl());
      URL issueUrl = new URL(jiraUrl, "/browse/" + issue.getKey());

      return issueUrl.toString();
    } catch (MalformedURLException e) {
      logger.info("Incorrect url");
    }

    return null;
  }

  private void saveExecutionLog(
      JiraTaskParameters parameters, String line, CommandExecutionStatus commandExecutionStatus) {
    logService.save(parameters.getAccountId(),
        aLog()
            .withAppId(parameters.getAppId())
            .withActivityId(parameters.getActivityId())
            .withLogLevel(INFO)
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }

  private JiraClient getJiraClient(JiraTaskParameters parameters) {
    JiraConfig jiraConfig = parameters.getJiraConfig();
    encryptionService.decrypt(jiraConfig, parameters.getEncryptionDetails());
    BasicCredentials creds = new BasicCredentials(jiraConfig.getUsername(), new String(jiraConfig.getPassword()));

    return new JiraClient(jiraConfig.getBaseUrl(), creds);
  }

  private ResponseData createTicketAndWebhook(JiraTaskParameters parameters) {
    JiraExecutionData jiraExecutionData = (JiraExecutionData) createTicket(parameters);
    if (jiraExecutionData.getExecutionStatus().equals(ExecutionStatus.FAILED)) {
      return jiraExecutionData;
    }
    parameters.setIssueId(jiraExecutionData.getIssueId());
    return createWebhook(parameters);
  }

  private ResponseData deleteWebhook(JiraTaskParameters parameters) {
    JiraConfig jiraConfig = parameters.getJiraConfig();
    encryptionService.decrypt(jiraConfig, parameters.getEncryptionDetails());
    JiraClient jira = getJiraClient(parameters);

    CommandExecutionStatus commandExecutionStatus;
    try {
      jira.getRestClient().delete(new URI(parameters.getWebhookUrl()));
    } catch (IOException | URISyntaxException | RestException e) {
      logger.error("Unable to delete a new JIRA webhook", e);
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage("Unable to delete the JIRA webhook " + parameters.getIssueId())
          .jiraApprovalActionType(DELETE_WEBHOOK)
          .build();
    }
    commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    saveExecutionLog(
        parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .jiraApprovalActionType(DELETE_WEBHOOK)
        .errorMessage("Deleted Webhook After the approval: " + parameters.getWebhookUrl())
        .build();
  }

  private ResponseData createWebhook(JiraTaskParameters parameters) {
    JiraConfig jiraConfig = parameters.getJiraConfig();
    encryptionService.decrypt(jiraConfig, parameters.getEncryptionDetails());
    JiraClient jira = getJiraClient(parameters);
    CommandExecutionStatus commandExecutionStatus;
    Issue issue;
    try {
      issue = jira.getIssue(parameters.getIssueId());
    } catch (JiraException e) {
      String error = "Not creating webhook as unable to fetch Jira for id: " + parameters.getIssueId();
      logger.error(error, e);
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .activityId(parameters.getActivityId())
          .approvalId(parameters.getApprovalId())
          .jiraApprovalActionType(CREATE_WEBHOOK)
          .errorMessage(error)
          .build();
    }

    List<String> events = new ArrayList<>();
    events.add("jira:issue_updated");

    Map<String, String> filters = new HashMap<>();
    filters.put("issue-related-events-section", "issue = " + parameters.getIssueId());
    String token = parameters.getJiraToken();

    // Todo: Replace hardcoded url after checking the Url from config

    String url = getBaseUrl() + JIRA_APPROVAL_API_PATH + token;

    JiraWebhookParameters jiraWebhookParameters = JiraWebhookParameters.builder()
                                                      .name("webhook for issue = " + issue.getKey())
                                                      .events(events)
                                                      .filters(filters)
                                                      .excludeBody(false)
                                                      .jqlFilter(filters)
                                                      .excludeIssueDetails(false)
                                                      .url(url)
                                                      .build();

    JSONObject json = JSONObject.fromObject(jiraWebhookParameters);

    String webhookUrl;
    try {
      JSON resp = jira.getRestClient().post(new URI(jiraConfig.getBaseUrl() + WEBHOOK_CREATION_URL), json);
      JSONObject object = JSONObject.fromObject(resp);
      webhookUrl = object.getString("self");
    } catch (RestException | IOException | URISyntaxException e) {
      String error = "Unable to create a new JIRA webhook for " + getIssueUrl(jiraConfig, issue);
      logger.error(error, e);

      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .activityId(parameters.getActivityId())
          .approvalId(parameters.getApprovalId())
          .jiraApprovalActionType(CREATE_WEBHOOK)
          .errorMessage(error)
          .build();
    }

    commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    saveExecutionLog(
        parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .webhookUrl(webhookUrl)
        .errorMessage("Waiting for Approval on ticket: " + getIssueUrl(jiraConfig, issue))
        .approvalId(parameters.getApprovalId())
        .activityId(parameters.getActivityId())
        .jiraApprovalActionType(CREATE_WEBHOOK)
        .build();
  }

  private String getBaseUrl() {
    String baseUrl = mainConfiguration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }
}
