package com.sap.jam.samples.jira.plugin.ao;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface OAuth2Token extends Entity
{
    public enum Direction {
        INBOUND, OUTBOUND
    }
    
    public Direction getDirection();
    public void setDirection(Direction direction);
    
    public String getUsername();
    public void setUsername(String username);
    
    public String getToken();
    public void setToken(String token);
}
