package com.sap.jam.samples.jira.plugin.odata.server.models;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.odata2.api.annotation.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntityType;
import org.apache.olingo.odata2.api.annotation.edm.EdmKey;
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.project.version.Version;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import org.apache.commons.lang.StringUtils;

@EdmEntitySet(name = "Issues")
@EdmEntityType(name = "Issue")
public class ODataIssue implements ODataObject {

  // ====== Edm Annotation Provider Properties =======
  @EdmKey
  @EdmProperty
  public String key;

  @EdmProperty
  public int id;

  @EdmProperty
  public Date created;

  @EdmProperty
  public Date updated;

  @EdmProperty
  public String summary;

  @EdmProperty
  public String description;

  @EdmProperty
  public String UILink;

  @EdmProperty
  public String priority;

  @EdmProperty
  public String status;

  @EdmProperty
  public String issueType;

  @EdmProperty
  public String project;
  
  @EdmProperty
  public String reporter;

  @EdmProperty
  public String assignee;

  @EdmProperty
  public String fixVersion;
  
  @EdmProperty
  public String affectedVersions;
  
  @EdmProperty
  public String resolution;
  
  @EdmProperty
  public Date resolved;
  
  @EdmProperty
  public String components;

  @EdmProperty
  public String labels;

  // ====== Class Implementation =======
  private Issue issue;
  private URI serviceRoot;

  public ODataIssue(Issue issue) {
    this(issue, null);
  }

  public ODataIssue(Issue issue, URI serviceRoot) {
    this.issue = issue;
    this.serviceRoot = serviceRoot;
  }

  public Map<String, Object> getOData() {
    HashMap<String, Object> data = new HashMap<String, Object>();

    data.put("Id", issue.getId());
    data.put("Created", issue.getCreated());
    data.put("Key", issue.getKey());
    data.put("Summary", issue.getSummary());
    data.put("Description", issue.getDescription());
    data.put("Project", issue.getProjectObject().getName());
    data.put("Priority", issue.getPriorityObject().getName());
    data.put("Status", issue.getStatusObject().getName());
    data.put("IssueType", issue.getIssueTypeObject().getName());
    data.put("Reporter", issue.getReporter().getDisplayName());
    data.put("Updated", issue.getUpdated());
    
    if (issue.getAssignee() != null) {
      data.put("Assignee", issue.getAssignee().getDisplayName());
    }

    if (issue.getLabels() != null) {
      ArrayList<String> labelList = new ArrayList<String>();
      for (Label label : issue.getLabels()) {
        labelList.add(label.getLabel());
      }
      data.put("Labels", StringUtils.join(labelList, ", "));
    }

    if (issue.getFixVersions() != null) {
      ArrayList<String> versions = new ArrayList<String>();
      for (Version version : issue.getFixVersions()) {
        versions.add(version.getName());
      }
      data.put("FixVersion", StringUtils.join(versions, ", "));
    }

    if (issue.getAffectedVersions() != null) {
      ArrayList<String> versions = new ArrayList<String>();
      for (Version version : issue.getAffectedVersions()) {
        versions.add(version.getName());
      }
      data.put("AffectedVersions", StringUtils.join(versions, ", "));
    }

    if (issue.getResolutionObject() != null) {
      data.put("Resolution", issue.getResolutionObject().getName());      
      data.put("Resolved", issue.getResolutionDate());      
    }
    
    if (issue.getComponentObjects() != null) {
      ArrayList<String> componentList = new ArrayList<String>();
      for (ProjectComponent component : issue.getComponentObjects()) {
        componentList.add(component.getName());
      }
      data.put("Components", StringUtils.join(componentList, ", "));
    }
    
    if (serviceRoot != null) {
      String url = "../../../../../browse/" + issue.getKey();
      // relative to the URL pattern declared in atlassian-plugin.xml and the JIRA plugin servlet root
      URI browseURL = serviceRoot.resolve(url);
      data.put("UILink", browseURL.toString());
    }

    return data;
  }
}
