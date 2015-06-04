package com.sap.jam.samples.jira.plugin.odata.server.models;

import java.util.Map;

// Returns the OData representation of a given object
public interface ODataObject {
	public Map<String, Object> getOData();
}
