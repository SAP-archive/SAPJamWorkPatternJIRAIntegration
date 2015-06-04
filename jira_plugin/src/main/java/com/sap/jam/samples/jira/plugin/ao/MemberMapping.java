package com.sap.jam.samples.jira.plugin.ao;

import net.java.ao.Entity;
import net.java.ao.Preload;

// Maps a JIRA user to a Jam member
@Preload
public interface MemberMapping extends Entity
{
    public String getMemberID();
    public void setMemberID(String memberID);
    
    public String getUsername();
    public void setUsername(String userName);
}
