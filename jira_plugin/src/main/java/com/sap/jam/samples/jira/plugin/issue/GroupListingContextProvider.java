package com.sap.jam.samples.jira.plugin.issue;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.sap.jam.samples.jira.plugin.odata.client.JamClient;
import com.sap.jam.samples.jira.plugin.odata.client.models.Group;
import java.util.List;
import java.util.Map;

public class GroupListingContextProvider extends AbstractJiraContextProvider {
	private final JamClient jamClient;

  public GroupListingContextProvider(
      ApplicationLinkService applicationLinkService,
      HostApplication hostApplication,
      IssueManager issueManager) {
    jamClient = new JamClient(applicationLinkService, hostApplication, issueManager);
  }

  public Map getContextMap(User user, JiraHelper jiraHelper) {
    Map contextMap = jiraHelper.getContextParams();
    Issue issue = (Issue)contextMap.get("issue");

    List<Group> groups = jamClient.getIssueGroups(issue);
    contextMap.put("groups", groups);

    return contextMap;
  }
}
