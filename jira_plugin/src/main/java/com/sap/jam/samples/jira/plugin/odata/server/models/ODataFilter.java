package com.sap.jam.samples.jira.plugin.odata.server.models;

import com.atlassian.jira.issue.search.SearchRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.annotation.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntityType;
import org.apache.olingo.odata2.api.annotation.edm.EdmKey;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;

@EdmEntitySet(name = "Filters")
@EdmEntityType(name = "Filter")
public class ODataFilter implements ODataObject {

	@EdmKey
	@EdmProperty
	public int id;
	
	@EdmProperty
	public String name;

	@EdmProperty
	public String description;
	
	@EdmProperty
	public String UILink;
	
	@EdmProperty
	public String searchString;
	
	@EdmProperty
	public boolean favourite;
	
	@EdmNavigationProperty
	public List<ODataIssue> issues;
        
	// ====== Class Implementation =======
	private SearchRequest filter;
        private URI serviceRoot;
	
	public ODataFilter(SearchRequest filter) {
          this(filter, null);
	}
	
	public ODataFilter(SearchRequest filter, URI serviceRoot) {
          this.filter = filter;
          this.serviceRoot = serviceRoot;
	}
	
	public Map<String, Object> getOData() {
          HashMap<String, Object> data = new HashMap<String, Object>();

          data.put("Id", filter.getId());
          data.put("Name", filter.getName());
          data.put("Description", filter.getDescription());
          data.put("SearchString", filter.getQuery().getQueryString());
          
          if (serviceRoot != null) {
            String url = "../../../../../issues/?filter=" + filter.getId();
            // relative to the URL pattern declared in atlassian-plugin.xml and the JIRA plugin servlet root
            URI browseURL = serviceRoot.resolve(url); 
            data.put("UILink", browseURL.toString());
          }

          return data;
	}
}
