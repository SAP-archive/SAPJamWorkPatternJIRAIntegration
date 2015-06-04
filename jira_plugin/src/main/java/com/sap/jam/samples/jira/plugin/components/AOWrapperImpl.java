package com.sap.jam.samples.jira.plugin.components;

import com.atlassian.activeobjects.external.ActiveObjects;

// This logic is required because ActiveObjects can only be retrieved via constructor injection in JIRA,
// and constructor injection is not available to us in the OData processor.  So we can add a little wrapper...
public class AOWrapperImpl implements AOWrapper {

    private final ActiveObjects activeObjects;
    
    public AOWrapperImpl(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }
    
    @Override
    public ActiveObjects getActiveObjects() {
        return activeObjects;
    }

}
