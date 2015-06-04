package com.sap.jam.samples.jira.plugin.ao;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface JamPluginTimestamp extends Entity {
  // Timestamp is reserved so use JamTimestamp instead.
  public Long getJamTimestamp();
  public void setJamTimestamp(Long timestamp);

  public Long getProjectID();
  public void setProjectID(Long projectId);
}
