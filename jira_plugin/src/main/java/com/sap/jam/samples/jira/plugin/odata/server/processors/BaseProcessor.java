package com.sap.jam.samples.jira.plugin.odata.server.processors;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties.ODataEntityProviderPropertiesBuilder;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;
import org.apache.olingo.odata2.api.uri.info.GetEntitySetUriInfo;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.util.UserManager;
import com.sap.jam.samples.jira.plugin.odata.server.models.ODataObject;
import java.net.URISyntaxException;

public class BaseProcessor extends ODataSingleProcessor {

  protected URI serviceRoot;
  protected ODataEntityProviderPropertiesBuilder propertiesBuilder;

  // Here are the JIRA services we will will need
  protected final IssueManager issueManager;
  protected final SearchRequestManager filterManager;
  protected final UserManager userManager;
  protected final ProjectManager projectManager;
  protected final SearchService searchService;

  protected final User currentUser;

  BaseProcessor(ODataContext ctx) {
    // Initialize JIRA services
    this.issueManager = ComponentAccessor.getIssueManager();
    this.userManager = ComponentAccessor.getUserManager();
    this.projectManager = ComponentAccessor.getProjectManager();
    this.searchService = ComponentAccessor.getComponent(SearchService.class);
    this.filterManager = ComponentAccessor.getComponent(SearchRequestManager.class);

    // Get the current user
    this.currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

    // Initialize OLingo services
    try {
      this.serviceRoot = ctx.getPathInfo().getServiceRoot();
      if (("http".equals(this.serviceRoot.getScheme()) && this.serviceRoot.getPort() == 80) || 
          ("https".equals(this.serviceRoot.getScheme()) && this.serviceRoot.getPort() == 443)) {
        this.serviceRoot = new URI(
                this.serviceRoot.getScheme(),
                this.serviceRoot.getHost(),
                this.serviceRoot.getPath(),
                this.serviceRoot.getQuery(),
                this.serviceRoot.getFragment());
      }
      this.propertiesBuilder = EntityProviderWriteProperties.serviceRoot(serviceRoot);
    } catch (ODataException e) {
      e.printStackTrace();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  protected int getSkip(GetEntitySetUriInfo uriInfo) {
    if (uriInfo.getSkip() != null) {
      return uriInfo.getSkip();
    } else {
      return 0;
    }
  }

  protected int getTop(GetEntitySetUriInfo uriInfo) {
    if (uriInfo.getTop() != null) {
      return uriInfo.getTop();
    } else {
      return 0;
    }
  }

  protected List<Map<String, Object>> getODataList(List<? extends ODataObject> list) {
    List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
    for (ODataObject object : list) {
      data.add(object.getOData());
    }
    return data;
  }
}
