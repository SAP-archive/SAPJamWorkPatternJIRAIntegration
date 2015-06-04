package com.sap.jam.samples.jira.plugin.issue;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.sap.jam.samples.jira.plugin.odata.client.JamClient;
import java.util.Collection;
import java.util.Map;

public class CanAccessJamPluginCondition extends AbstractIssueCondition {
  public static final String JamUser = "JamUser";

  private boolean displayIfNewerThanJam;

  public CanAccessJamPluginCondition() {
    displayIfNewerThanJam = false;
  }

  public void init(Map params) {
    if (params.containsKey("displayIfNewerThanJam")) {
      displayIfNewerThanJam = (params.get("displayIfNewerThanJam").equals("true"));
    }
  }

  public boolean shouldDisplay(User user, Issue issue, JiraHelper jiraHelper) {
    ProjectRoleManager roleManager = (ProjectRoleManager)ComponentAccessor.getComponentOfType(ProjectRoleManager.class); 
    Collection<ProjectRole> roles = roleManager.getProjectRoles(user, jiraHelper.getProjectObject());

    // Check if the user has the required role.
    boolean hasJamRole = false;
    for (ProjectRole role : roles) {
      if (role.getName().equals(JamUser)) {
        hasJamRole = true;
        break;
      }
    }

    if (!hasJamRole) {
      return false;
    }

    // Check if we are filtering based on creation time or not.
    if (displayIfNewerThanJam) {
      Long jamTs = JamClient.getProjectJamTimestamp(issue.getProjectObject());
      Long issueTs = issue.getCreated().getTime();
      return (jamTs <= issueTs);
    }

    return true;
  }
}
