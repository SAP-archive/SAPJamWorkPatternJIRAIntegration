package com.sap.jam.samples.jira.plugin.odata.client.models;

public class Group {
  public String Name;  
  public String Description;
  public String GroupType;  
  public ObjectReference Template;
  public ObjectReference PrimaryExternalObject;
  public String Id;
  public String WebURL;

  public String getName() {
  	return Name;
  }

  public String getWebURL() {
  	return WebURL;
  }

  public String getId() {
  	return Id;
  }
}
