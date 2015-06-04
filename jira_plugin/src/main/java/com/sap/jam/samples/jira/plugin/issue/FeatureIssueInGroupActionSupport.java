package com.sap.jam.samples.jira.plugin.issue;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sap.jam.samples.jira.plugin.odata.client.JamClient;
import com.sap.jam.samples.jira.plugin.odata.client.models.Group;
import java.util.List;
import java.util.Hashtable;

public class FeatureIssueInGroupActionSupport extends JiraWebActionSupport {
  private final IssueManager issueManager;
  private final JiraAuthenticationContext authenticationContext;
  private final JamClient jamClient;

  private Long id;
  private List<Group> groupsList;
  private String groupId;
  private String errorMessage;
  private Hashtable<String, Group> groupIdToNameHt;
  private Gson gson;

  public FeatureIssueInGroupActionSupport(
      JiraAuthenticationContext authenticationContext,
      ApplicationLinkService applicationLinkService,
      HostApplication hostApplication,
      IssueManager issueManager) {
    this.authenticationContext = authenticationContext;
    this.issueManager = issueManager;
    jamClient = new JamClient(applicationLinkService, hostApplication, issueManager);
    groupIdToNameHt = new Hashtable<String, Group>();
    gson = new Gson();
  }

  protected void doValidation() {
  }

  protected String doExecute() throws Exception {
    Issue issue = getIssueObject();
    Group group = getGroup(groupId);
    errorMessage = jamClient.featureIssueInGroup(issue, group);

    if (errorMessage != null) {
      return ERROR;
    }

    return returnComplete("/browse/" + issue.getKey());
  }

  public String doDefault() throws Exception {
    groupsList = jamClient.getUserGroups();

    // Populate hash table of groups by id.
    for (Group group : groupsList) {
      groupIdToNameHt.put(group.Id, group);
    }

    return INPUT;
  }

  public Issue getIssueObject() {
    return issueManager.getIssueObject(id);
  }

  public Group getGroup(String id) {
    return groupIdToNameHt.get(id);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public List<Group> getGroupsList() {
    return groupsList;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getGroupIdToNameHt() {
    return gson.toJson(groupIdToNameHt);
  }

  public void setGroupIdToNameHt(String groupIdToNameStr) {
    groupIdToNameHt = gson.fromJson(groupIdToNameStr, new TypeToken<Hashtable<String, Group>>(){}.getType());
  }
}
