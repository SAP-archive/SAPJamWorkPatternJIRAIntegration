package com.sap.jam.samples.jira.plugin.odata.server.processors;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.api.uri.info.GetEntitySetUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntityUriInfo;
import org.apache.olingo.odata2.api.uri.info.PostUriInfo;

import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.bean.PagerFilter;
import com.sap.jam.samples.jira.plugin.odata.server.filters.IssueFilter;
import com.sap.jam.samples.jira.plugin.odata.server.models.ODataIssue;

public class IssueProcessor extends BaseProcessor {
	
	IssueProcessor(ODataContext ctx) {
		super(ctx);
	}
	
	@Override
	public ODataResponse readEntity(GetEntityUriInfo uriInfo, String contentType) throws ODataException
	{
		String issueKey = uriInfo.getKeyPredicates().get(0).getLiteral();
		Issue issue = issueManager.getIssueObject(issueKey);

		switch (uriInfo.getNavigationSegments().size()) {
		case 0:
			ODataIssue odataIssue = new ODataIssue(issue, serviceRoot);
			return EntityProvider.writeEntry(contentType, uriInfo.getStartEntitySet(), odataIssue.getOData(), propertiesBuilder.build());
		}
		throw new ODataNotImplementedException();
	}
	
	@Override
	public ODataResponse readEntitySet(GetEntitySetUriInfo uriInfo, String contentType) throws ODataException
	{
          int skip = getSkip(uriInfo);
          int top = getTop(uriInfo);

          if (uriInfo.getNavigationSegments().isEmpty()) {
            String queryString = "";
            IssueFilter issueFilter = new IssueFilter();

            FilterExpression filterExpression = uriInfo.getFilter();
            if (filterExpression != null) {
              queryString = (String) filterExpression.accept(issueFilter);
            }

            OrderByExpression orderByExpression = uriInfo.getOrderBy();
            if (orderByExpression != null) {
              queryString += (String) orderByExpression.accept(issueFilter);
            }

            // Parse and sanitize the query
            ParseResult parseResult = searchService.parseQuery(currentUser, queryString);
            if (parseResult.isValid()) {
              try {
                PagerFilter<Issue> pagerFilter = null;
                if (top > 0 && skip == 0) {
                  pagerFilter = new PagerFilter<Issue>(top);
                } else if (top > 0 && skip > 0) {
                  pagerFilter = new PagerFilter<Issue>(skip, top);
                } else {
                  pagerFilter = PagerFilter.getUnlimitedFilter();
                }

                SearchResults searchResults = searchService.search(currentUser, parseResult.getQuery(), pagerFilter);
                List<Issue> issues = searchResults.getIssues();

                List<ODataIssue> odataIssues = new ArrayList<ODataIssue>();
                for (Issue issue : issues) {
                  if (issueFilter.postFilter(issue.getKey().toLowerCase())) {                    
                    odataIssues.add(new ODataIssue(issue, serviceRoot));
                  }
                }

                return EntityProvider.writeFeed(contentType, uriInfo.getTargetEntitySet(), getODataList(odataIssues), propertiesBuilder.build());

              } catch (SearchException e) {
                throw new ODataException(e.getMessage());
              }
            }
          }
	    
          throw new ODataNotImplementedException();
	}
	
	@Override
	public ODataResponse createEntity(PostUriInfo uriInfo, InputStream content, String requestContentType, String contentType) throws ODataException
	{
		throw new ODataNotImplementedException();
	}

}
