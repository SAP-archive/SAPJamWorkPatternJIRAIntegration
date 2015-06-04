package com.sap.jam.samples.jira.plugin.ao;

import net.java.ao.Entity;
import net.java.ao.Preload;

// Maps a JIRA issue to a Jam external object
// It should be noted that table names cannot exceed 30 characters - hence "ExObj" instead of "ExternalObject"...
@Preload
public interface ExObjMapping extends Entity
{
    public String getExternalObjectID();
    public void setExternalObjectID(String externalObjectID);
    
    public Long getIssueID();
    public void setIssueID(Long issueID);
}
