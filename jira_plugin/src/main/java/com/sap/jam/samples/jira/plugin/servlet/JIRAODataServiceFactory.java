package com.sap.jam.samples.jira.plugin.servlet;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.odata2.annotation.processor.core.edm.AnnotationEdmProvider;
import org.apache.olingo.odata2.api.ODataService;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.rt.RuntimeDelegate;

import com.sap.jam.samples.jira.plugin.odata.server.processors.JiraODataProcessor;

public class JIRAODataServiceFactory extends ODataServiceFactory {

	@Override
	public ODataService createService(ODataContext ctx) throws ODataException {
		List<Class<?>> edmClasses = new ArrayList<Class<?>>();
		edmClasses.add(com.sap.jam.samples.jira.plugin.odata.server.models.ODataIssue.class);
		edmClasses.add(com.sap.jam.samples.jira.plugin.odata.server.models.ODataFilter.class);
		
		return RuntimeDelegate.createODataSingleProcessorService(
				new AnnotationEdmProvider(edmClasses),
        		new JiraODataProcessor(ctx));
	}
}

