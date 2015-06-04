package com.sap.jam.samples.jira.plugin.issue;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.sap.jam.samples.jira.plugin.odata.client.JamClient;
import com.sap.jam.samples.jira.plugin.odata.client.models.Group;
import com.sap.jam.samples.jira.plugin.odata.client.models.GroupTemplate;
import com.sap.jam.samples.jira.plugin.odata.client.models.ObjectReference;
import java.util.*;

public class CreateIssueGroupActionSupport extends JiraWebActionSupport {

  private final IssueManager issueManager;
  private final JiraAuthenticationContext authenticationContext;
  private final JamClient jamClient;
  
  private Long id;
  private List<GroupTemplate> groupTemplates;
  private String groupName;
  private String groupDescription;
  private String groupType;
  private String groupTemplate;
  private String errorMessage;

  public CreateIssueGroupActionSupport(JiraAuthenticationContext authenticationContext, ApplicationLinkService applicationLinkService,
          HostApplication hostApplication,
          IssueManager issueManager) {
    this.authenticationContext = authenticationContext;
    this.issueManager = issueManager;
    this.jamClient = new JamClient(applicationLinkService, hostApplication, issueManager);
  }

  protected void doValidation() {
    log.debug("Entering doValidation");
    for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
      String n = (String) e.nextElement();
      String[] vals = request.getParameterValues(n);
      log.debug("name " + n + ": " + vals[0]);
    }

    if (groupName == null || groupName.length() == 0) {
      addErrorMessage("The local variable didn't get set");
      return;
    }

    // invalidInput() checks for error messages, and errors too.
    if (invalidInput()) {
      for (Iterator it = getErrorMessages().iterator(); it.hasNext();) {
        String msg = (String) it.next();
        log.debug("Error message during validation: " + msg);
      }

      for (Iterator it2 = getErrors().entrySet().iterator(); it2.hasNext();) {
        Map.Entry entry = (Map.Entry) it2.next();
        log.debug("Error during validation: field=" + entry.getKey() + ", error=" + entry.getValue());
      }
    }
  }

  /**
   * This method is always called when this Action's .jspa URL is invoked if
   * there were no errors in doValidation().
   */
  protected String doExecute() throws Exception {
    Issue issue = getIssueObject();
    
    Group group = new Group();
    group.Name = groupName;
    group.Description = groupDescription;
    group.GroupType = groupType;

    if (groupTemplate != null && groupTemplate.length() > 0) {
      group.Template = new ObjectReference();
      group.Template.__metadata = group.Template.new URI();
      group.Template.__metadata.uri = "GroupTemplates(" + groupTemplate + ")";
    }
    
    if (issue != null) {
      group.PrimaryExternalObject = new ObjectReference();
      group.PrimaryExternalObject.__metadata = group.PrimaryExternalObject.new URI();
      group.PrimaryExternalObject.__metadata.uri = "ExternalObjects('" + jamClient.getExternalObjectID(issue) + "')";    
    }
    
    errorMessage = jamClient.createGroup(group);
    if (errorMessage != null) {
      return ERROR;
    }

    return returnComplete("/browse/" + issue.getKey());
  }

  public String doDefault() throws Exception {
    Issue issue = getIssueObject();

    groupTemplates = jamClient.getGroupTemplates(issue);    
    groupName = String.format("%s - %s", issue.getKey(), issue.getSummary());
    
    return INPUT;
  }

  public Issue getIssueObject() {
    Issue issue = issueManager.getIssueObject(id);
    
    return issue;
  }
  
  public Long getId() {
      return id;
  }

  public void setId(Long id) {
      this.id = id;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getGroupType() {
    return groupType;
  }

  public void setGroupType(String groupType) {
    this.groupType = groupType;
  }

  public String getGroupDescription() {
    return groupDescription;
  }

  public void setGroupDescription(String groupDescription) {
    this.groupDescription = groupDescription;
  }

  public String getGroupTemplate() {
    return groupTemplate;
  }

  public void setGroupTemplate(String groupTemplate) {
    this.groupTemplate = groupTemplate;
  }

  public List<GroupTemplate> getGroupTemplates() {
    return groupTemplates;
  }

  public void setGroupTemplates(List<GroupTemplate> groupTemplates) {
    this.groupTemplates = groupTemplates;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
