package com.sap.jam.samples.jira.plugin.components;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.Comment;
import com.sap.jam.samples.jira.plugin.odata.client.JamClient;

import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

// A listener which posts Issue change events as external object feed posts to Jam
public class JamEventListener implements InitializingBean, DisposableBean {

    // JIRA Services
    private final EventPublisher eventPublisher;
    private final ApplicationLinkService applicationLinkService;
    private final HostApplication hostApplication;
    private final IssueManager issueManager;
    
    public JamEventListener(ApplicationLinkService applicationLinkService,
                            HostApplication hostApplication,
                            EventPublisher eventPublisher,
                            IssueManager issueManager) {
      this.eventPublisher = eventPublisher;
      this.applicationLinkService = applicationLinkService;
      this.hostApplication = hostApplication;
      this.issueManager = issueManager;
    }

    // Called when the plugin has been enabled
    @Override
    public void afterPropertiesSet() throws Exception
    {
        eventPublisher.register(this);
    }

    // Called when the plugin is being disabled or removed.
    @Override
    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent)
    {
        Issue issue = issueEvent.getIssue();
        Comment comment = issueEvent.getComment();
        JamClient jamClient = new JamClient(applicationLinkService, hostApplication, issueManager);

        Long eventTypeId = issueEvent.getEventTypeId();
        String eventBlurb = null;
        if (eventTypeId.equals(EventType.ISSUE_COMMENTED_ID)) {
          // Ignore comments on their own. Non-empty comments are appended to the action
          // at the end of this if block.
        } else if (eventTypeId.equals(EventType.ISSUE_UPDATED_ID) ||
                   eventTypeId.equals(EventType.ISSUE_RESOLVED_ID) ||
                   eventTypeId.equals(EventType.ISSUE_REOPENED_ID) ||
                   eventTypeId.equals(EventType.ISSUE_ASSIGNED_ID) ||
                   eventTypeId.equals(EventType.ISSUE_CLOSED_ID) ||
                   eventTypeId.equals(EventType.ISSUE_WORKSTOPPED_ID) ||
                   eventTypeId.equals(EventType.ISSUE_WORKSTARTED_ID)) {
          List<GenericValue> changeItems = null;
          try {
              GenericValue changeLog = issueEvent.getChangeLog();
              changeItems = changeLog.internalDelegator.findByAnd("ChangeItem", EasyMap.build("group", changeLog.get("id")));
          } catch (GenericEntityException e){
            e.printStackTrace();
          } 

          if (changeItems != null) {
            eventBlurb = "Summary of updated values:\n";
            for (GenericValue genericValue : changeItems) {
              String field = genericValue.getString("field");
              String oldString = genericValue.getString("oldstring");
              String newString = genericValue.getString("newstring");
              String oldId = genericValue.getString("oldvalue");
              String newId = genericValue.getString("newvalue");
              eventBlurb +=
                  field + ": <b>" +
                  (oldString == null ? " " : oldString) +
                  (oldId == null ? "" : "[" + oldId + "]") +
                  "</b> to <b>" +
                  (newString == null ? " " : newString) +
                  (newId == null ? "" : "[" + newId + "]") +
                  "</b>\n";
            }
          }
        }

        if (comment != null) {
          if (eventBlurb == null) {
            eventBlurb = comment.getBody();
          } else {
            eventBlurb += "\n" + comment.getBody();
          }
        }

        if (eventBlurb != null) {
          jamClient.postIssueActivity(issue, eventBlurb);
        }
    }
}