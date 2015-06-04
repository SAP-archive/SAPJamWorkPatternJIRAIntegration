package com.sap.jam.samples.jira.plugin.odata.client.models;

public class GroupTemplate {
  private String Id;
  private String GroupTemplateType;
  private String Title;
  private String Description;

  public String getId() {
    return Id;
  }

  public String getGroupTemplateType() {
    return GroupTemplateType;
  }

  public String getTitle() {
    return Title;
  }

  public String getDescription() {
    return Description;
  }
  
  public String getTemplateId() {
    return String.format("Id='%s',GroupTemplateType='%s'", Id, GroupTemplateType);
  }
}
