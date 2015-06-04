package com.sap.jam.samples.jira.plugin.odata.client;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.java.ao.DBParam;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseStatusException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.sap.jam.samples.jira.plugin.ao.ExObjMapping;
import com.sap.jam.samples.jira.plugin.ao.JamPluginTimestamp;
import com.sap.jam.samples.jira.plugin.ao.MemberMapping;
import com.sap.jam.samples.jira.plugin.applink.JamApplicationType;
import com.sap.jam.samples.jira.plugin.components.AOWrapper;
import com.sap.jam.samples.jira.plugin.odata.client.models.Activity;
import com.sap.jam.samples.jira.plugin.odata.client.models.ExternalObject;
import com.sap.jam.samples.jira.plugin.odata.client.models.Group;
import com.sap.jam.samples.jira.plugin.odata.client.models.GroupTemplate;
import com.sap.jam.samples.jira.plugin.odata.client.models.Member;
import com.sap.jam.samples.jira.plugin.odata.client.models.ObjectReference;
import com.sap.jam.samples.jira.plugin.odata.client.models.ObjectReference.URI;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class JamClient {
    
    private static final String baseURL = "/api/v1/OData/";
    private static final String feedQuery = "/FeedEntries?$expand=Creator,ThumbnailImage,TargetObjectReference&$select=Id,Text,Liked,ActionOnly,LikesCount,RepliesCount,CreatedAt,Creator/Id,Creator/Email,Creator/FullName,ThumbnailImage,TargetObjectReference&internal_api_subject_to_change=true";
    private static final String annotationsURL = "/download/resources/com.sap.jam.samples.jira.plugin.jam-add-timeline-to-view-issue-screen/annotations.xml";

    private final ApplicationLinkService applicationLinkService;
    private final ApplicationLink        jamLink;
    private final HostApplication        hostApplication;
    private final IssueManager           issueManager;
    private final ActiveObjects          activeObjects;
    private static final Logger log = LogManager.getLogger(JamClient.class);

    private final String jiraODataURL;
    
    private Gson gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter(Date.class, new ODataDateDeserializer()).create();
    private final I18nBean i18n;
    
    public JamClient(ApplicationLinkService applicationLinkService,
                     HostApplication hostApplication,
                     IssueManager issueManager) {
       this.applicationLinkService = applicationLinkService;
       this.hostApplication = hostApplication;
       this.issueManager = issueManager;

       jamLink = this.applicationLinkService.getPrimaryApplicationLink(JamApplicationType.class);
       jiraODataURL = this.hostApplication.getBaseUrl().toString() + "/plugins/servlet/sapjam/api/OData/";
       
       AOWrapper aoWrapper = ComponentAccessor.getOSGiComponentInstanceOfType(AOWrapper.class);
       activeObjects = aoWrapper.getActiveObjects();

       i18n = new I18nBean();
    }

    public String getJamUrl() {
      return jamLink.getRpcUrl().toString();
    }
    
    public String getSingleUseToken() {
      String requestURL = "/v1/single_use_tokens";

      String response = null;
      try {
        response = doPost(requestURL, "");        
      } catch (ResponseStatusException e) {
          int statusCode = e.getResponse().getStatusCode();
          switch (statusCode) {
            case 400:
              log.error("getSingleUseToken response:400 - Bad Request");
              break;
            default:
              e.printStackTrace();
              break;
          }
      }
      
      String token = null;
      
      if (response == null) {
        return token;
      }
      
      Pattern p = Pattern.compile("<single_use_token\\s+id=\"(.+?)\">");
      Matcher m = p.matcher(response);
      if (m.find()) {
        token = m.group(1);
      }
      
      return token;
    }

    public List<GroupTemplate> getGroupTemplates(Issue issue) {
      String objectId = getExternalObjectID(issue);
      
      String requestURL = baseURL + "ExternalObjects('" + objectId + "')/Templates";
      
      String response = null;
      try {
        response = doGet(requestURL);
      } catch (ResponseStatusException e) {
          int statusCode = e.getResponse().getStatusCode();
          switch (statusCode) {
            case 400:
              log.error("getGroupTemplates response:400 - Bad Request");
              break;
            default:
              e.printStackTrace();
              break;
          }
      }

      if (response == null) {
        return new ArrayList<GroupTemplate>();
      }

      ODataListResponse<GroupTemplate> oResponse = gson.fromJson(
          response, new TypeToken<ODataListResponse<GroupTemplate>>(){}.getType());

      return oResponse.d.results;
    }

    // Returns list of groups the current user has access to.
    public List<Group> getUserGroups() {
        String requestURL = baseURL + "Groups?$top=100";
        String responseStr = null;

        try {
          responseStr = doGet(requestURL);
        } catch (ResponseStatusException e) {
          int statusCode = e.getResponse().getStatusCode();
          switch (statusCode) {
            case 400:
              log.error("getUserGroups response:400 - Bad Request");
              break;
            default:
              e.printStackTrace();
              break;
          }
        }

        if (responseStr == null) {
          return new ArrayList<Group>();
        }

        ODataListResponse<Group> response = gson.fromJson(
            responseStr, new TypeToken<ODataListResponse<Group>>(){}.getType());

        return response.d.results;
    }

    // Returns list of groups the issue is a primary or featured in.
    public List<Group> getIssueGroups(Issue issue) {
        // Get the external ID for the issue
        String externalID = getExternalObjectID(issue);
        String requestURL = baseURL + "ExternalObjects('" + externalID + "')/Groups?$top=100";
        String responseStr = null;
        try {
          responseStr = doGet(requestURL);
        } catch (ResponseStatusException e) {
          int statusCode = e.getResponse().getStatusCode();
          switch (statusCode) {
            case 400:
              log.error("getIssueGroups response:400 - Bad Request");
              break;
            default:
              e.printStackTrace();
              break;
          }
        }

        if (responseStr == null) {
          return new ArrayList<Group>();        
        }

        ODataListResponse<Group> response = gson.fromJson(
            responseStr, new TypeToken<ODataListResponse<Group>>(){}.getType());

        return response.d.results;
    }

    // Returns the timestamp which the Jam plugin was applied to the project.
    // If the timestamp doesn't exist, if creates one for the project.
    public static Long getProjectJamTimestamp(Project project) {
      AOWrapper aoWrapper = ComponentAccessor.getOSGiComponentInstanceOfType(AOWrapper.class);
      ActiveObjects activeObjects = aoWrapper.getActiveObjects();

      JamPluginTimestamp[] mappings = activeObjects.find(JamPluginTimestamp.class, "PROJECT_ID = ?", project.getId());
      if ( mappings.length > 0) {
        return mappings[0].getJamTimestamp();
      }

      Long now = System.currentTimeMillis();
      activeObjects.create(
          JamPluginTimestamp.class,
          new DBParam("JAM_TIMESTAMP", now),
          new DBParam("PROJECT_ID", project.getId())
      );

      return now;
    }
    
    // An ExternalObject activity is some type of entity change, possibly with a comment
    public void postIssueActivity(Issue issue, String comment) {
        String externalID = getExternalObjectID(issue);
        
        // Get the list of watchers for the issue and their Jam member IDs
        List<User> watchers = issueManager.getWatchers(issue);
        List<String> distributionMembers = new ArrayList<String>();
        for ( User watcher : watchers ) {
            String memberID = getMemberID(watcher);
            if ( memberID != null ) {
                distributionMembers.add(memberID);
            }
        }

        // Construct activity message
        Activity activity = new Activity();
        activity.Content = comment;
        activity.Object = new ObjectReference();
        activity.Object.__metadata = activity.Object.new URI();
        activity.Object.__metadata.uri = jamLink.getRpcUrl().toString() + baseURL + "ExternalObjects('" + externalID + "')";
        
        activity.Distribution = new ArrayList<ObjectReference>();
        for ( String memberID : distributionMembers ) {
            ObjectReference distribution = new ObjectReference();
            distribution.__metadata = distribution.new URI();
            distribution.__metadata.uri = jamLink.getRpcUrl().toString() + baseURL + "Members('" + memberID + "')";
            activity.Distribution.add(distribution);
        }
        
        String requestURL = baseURL + "Activities";
        String postBody = gson.toJson(activity);
        try {
          doPost(requestURL, postBody);
        } catch (ResponseStatusException e) {
          e.printStackTrace();
        }
    }

    // Will try to feature issue in group. Returns a string specifying the error
    // of the operation, or null on success.
    public String featureIssueInGroup(Issue issue, Group group) {
        String externalID = getExternalObjectID(issue);

        ExternalObject exObj = getExternalObject(issue);

        JsonObject postJson = (JsonObject)gson.toJsonTree(exObj);
        postJson.addProperty("uri", jamLink.getRpcUrl().toString() + baseURL + "ExternalObjects('" + externalID + "')");

        String requestURL = baseURL + "Groups('" + group.Id + "')/$links/FeaturedExternalObjects";
        String postBody = gson.toJson(postJson);
        String responseStr = null;
        try {
          responseStr = doPost(requestURL, postBody);
        } catch (ResponseStatusException e) {
          int statusCode = e.getResponse().getStatusCode();
          switch (statusCode) {
            case 409:
              return i18n.getText("sapjam.issue.errors.already_featued", issue.getKey(), group.Name);
            case 400:
              return i18n.getText("sapjam.issue.errors.bad_request");
          }

          return i18n.getText("sapjam.issue.errors.unknown_error", statusCode);
        }

        return null;
    }
    
    public void setCommentLiked(String commentID, boolean liked) {
        
    }
    
    public void setFeedLiked(String feedID, boolean liked) {
        
    }
    
    // Will try create specified group. Returns an error string, or null on success.
    public String createGroup(Group group) {
      String requestURL = baseURL + "Groups";
      String postBody = gson.toJson(group);

      try {
        doPost(requestURL, postBody);
      } catch (ResponseStatusException e) {
        int statusCode = e.getResponse().getStatusCode();
        switch (statusCode) {
          case 409:
            return i18n.getText("sapjam.issue.errors.group_exists", group.Name);
          case 400:
            return i18n.getText("sapjam.issue.errors.bad_request");
        }

        return i18n.getText("sapjam.issue.errors.unknown_error", statusCode);
      }

      return null;
    }

    private class ODataDateDeserializer implements JsonDeserializer<Date> {
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
           String s = json.getAsJsonPrimitive().getAsString();
           long l = Long.parseLong(s.substring(6, s.length() - 2));
           Date d = new Date(l);
           return d; 
        } 
    }

    // OData Queries used by this class, for GSON deserialization

    // Here's some class templates which help define the GSON deserialization of OData response payloads
    private class ODataListResult<EntityType> {
        public List<EntityType> results;
        public String __next;
    }

    private class ODataEntityResult<EntityType> {
        public EntityType results;
    }

    private class ODataEntityResponse<EntityType> {
        public ODataEntityResult<EntityType> d;
    }
    
    private class ODataListResponse<EntityType> {
        public ODataListResult<EntityType> d;
    }
    
    // Here's a more complex class required for the GSON serialization of an OData object reference
    public class ODataObjectReferenceURICreator implements InstanceCreator<ObjectReference.URI> {
        private final ObjectReference objectReference;
        
        public ODataObjectReferenceURICreator(ObjectReference objectReference) {
            this.objectReference = objectReference;
        }

        @Override
        public URI createInstance(Type type) {
            return objectReference.new URI();
        }
    }
    
    public ExternalObject getExternalObject(Issue issue) {
      ExternalObject exObj = new ExternalObject();
      exObj.Exid = jiraODataURL + "Issues('" + issue.getKey() + "')";
      exObj.Name = issue.getKey();
      exObj.Summary = issue.getSummary();
      exObj.ODataLink = exObj.Exid;
      exObj.ODataMetadata = jiraODataURL + "$metadata#Issues";
      exObj.ObjectType = exObj.ODataMetadata;
      exObj.Permalink = hostApplication.getBaseUrl().toString() + "/browse/" + issue.getKey();
      exObj.ODataAnnotations = hostApplication.getBaseUrl().toString() + "/download/resources/com.sap.jam.samples.jira.plugin.jam-plugin/annotations.xml";
      
      return exObj;
    }

    // Gets the Jam external object ID for a given JIRA Issue.
    // To do this we POST an ExternalObject to Jam, and extract the ID from the response.
    public String getExternalObjectID(Issue issue) {
        // First, check our database
        ExObjMapping[] mappings = activeObjects.find(ExObjMapping.class, "ISSUE_ID = ?", issue.getId());
        if ( mappings.length > 0) {
            return mappings[0].getExternalObjectID();
        }
        
        // Not in our DB -- call Jam for it. The POST here will return the ExternalObject if it already exists
        ExternalObject exObj = getExternalObject(issue);
        
        String requestURL = baseURL + "ExternalObjects";
        String postBody = gson.toJson(exObj);
        ODataEntityResponse<ExternalObject> response = null;
        try {
          response = gson.fromJson(
              doPost(requestURL, postBody),
              new TypeToken<ODataEntityResponse<ExternalObject>>(){}.getType()
          );
          
          String externalObjectID = response.d.results.Id;

          // Store this in the DB
          activeObjects.create(ExObjMapping.class,
                               new DBParam("EXTERNAL_OBJECT_ID", externalObjectID),
                               new DBParam("ISSUE_ID", issue.getId()));

          return externalObjectID;
        } catch (ResponseStatusException e) {
          int statusCode = e.getResponse().getStatusCode();
          switch (statusCode) {
            case 400:
              log.error("getExternalObjectID response:400 - Bad Request");
              break;
            default:
              e.printStackTrace();
              break;
          }
        }
        
        return "";
    }
    
    // Gets the Jam member ID corresponding to the given JIRA user.  Can return null if the user is not found.
    private String getMemberID(User user)
    {
        // First, check our database
        MemberMapping[] mappings = activeObjects.find(MemberMapping.class, "USERNAME = ?", user.getName());
        if ( mappings.length > 0) {
            return mappings[0].getMemberID();
        }
        
        // Not in our DB -- invoke Jam OData service operation to query for Jam member
        String requestURL = baseURL + "Members_FindByEmail?Email='" + user.getEmailAddress() + "'";

        String responseStr = null;
        try {
          responseStr = doGet(requestURL);
        } catch (ResponseStatusException e) {
          int statusCode = e.getResponse().getStatusCode();
          switch (statusCode) {
            case 400:
              // The API call isn't supported.
              log.error("getMemberID response:400 - Bad Request");
              break;
            default:
              e.printStackTrace();
              break;
          }
        }

        if (responseStr == null) {
          return null;
        }

        ODataEntityResponse<Member> response = gson.fromJson(
            responseStr, new TypeToken<ODataEntityResponse<Member>>(){}.getType());
        
        String memberID = response.d.results.Id;
        
        // Store this in the DB
        activeObjects.create(MemberMapping.class,
                             new DBParam("MEMBER_ID", memberID),
                             new DBParam("USERNAME", user.getName()));
        
        return memberID;
    }

    private String doGet(String requestURL) throws ResponseStatusException {
        String responseBody = null;
        if ( jamLink == null ) {
            return null;
        }

        try {
          // We will have a current user context by using an impersonating API call.
          ApplicationLinkRequestFactory requestFactory = jamLink.createImpersonatingAuthenticatedRequestFactory();
          if ( requestFactory == null ) {
              return null;
          }
          ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, requestURL);
          request.addHeader("Accept", "application/json");
          responseBody = request.execute();
        } catch (CredentialsRequiredException e) {
    			e.printStackTrace();
        } catch (ResponseStatusException e) {
          throw e;
        } catch (ResponseException e) {
    			e.printStackTrace();
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return responseBody;
    }
    
    private String doPost(String requestURL, String requestBody) throws ResponseStatusException
    {
        if ( jamLink == null ) {
            return null;
        }

        String responseBody = null;
        try {
            // We will have a current user context by using an impersonating API call.
            ApplicationLinkRequestFactory requestFactory = jamLink.createImpersonatingAuthenticatedRequestFactory();
            if ( requestFactory == null ) {
                return null;
            }
            ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.POST, requestURL);
            request.addHeader("Accept", "application/json");
            request.setRequestContentType("application/json");
            request.setRequestBody(requestBody);
            responseBody = request.execute();
        } catch (CredentialsRequiredException e) {
            e.printStackTrace();
        } catch (ResponseStatusException e) {
          throw e;
        } catch (ResponseException e) {
            e.printStackTrace();
        }
        return responseBody;
    }
}
