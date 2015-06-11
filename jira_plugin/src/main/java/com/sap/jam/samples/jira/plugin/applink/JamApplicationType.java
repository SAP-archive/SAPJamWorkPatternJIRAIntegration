package com.sap.jam.samples.jira.plugin.applink;

import java.net.URI;

import com.atlassian.applinks.spi.application.NonAppLinksApplicationType;
import com.atlassian.applinks.spi.application.TypeId;

public class JamApplicationType implements NonAppLinksApplicationType {
    
    static final TypeId TYPE_ID = new TypeId("sapjam");

    public TypeId getId() {
        return TYPE_ID;
    }

    @Override
    public String getI18nKey() {
        return "jam-plugin.apptype";
    }
    
    @Override
    public URI getIconUrl() {
        return URI.create("https://developer.sapjam.com/images/cubetree_global/body/sap/sap-logo.png");
    }

}
