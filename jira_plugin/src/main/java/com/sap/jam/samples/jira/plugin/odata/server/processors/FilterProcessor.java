package com.sap.jam.samples.jira.plugin.odata.server.processors;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchRequestEntity;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.util.Visitor;
import com.atlassian.jira.web.bean.PagerFilter;
import com.sap.jam.samples.jira.plugin.odata.server.filters.FilterFilter;
import com.sap.jam.samples.jira.plugin.odata.server.filters.IssueFilter;
import com.sap.jam.samples.jira.plugin.odata.server.models.ODataFilter;
import com.sap.jam.samples.jira.plugin.odata.server.models.ODataIssue;
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

public class FilterProcessor extends BaseProcessor {

  FilterProcessor(ODataContext ctx) {
    super(ctx);
  }

  @Override
  public ODataResponse readEntity(GetEntityUriInfo uriInfo, String contentType) throws ODataException {
    String filterID = uriInfo.getKeyPredicates().get(0).getLiteral();
    SearchRequest filter = filterManager.getSearchRequestById(Long.parseLong(filterID));

    switch (uriInfo.getNavigationSegments().size()) {
      case 0:
        ODataFilter oDataFilter = new ODataFilter(filter, serviceRoot);
        return EntityProvider.writeEntry(contentType, uriInfo.getStartEntitySet(), oDataFilter.getOData(), propertiesBuilder.build());
    }

    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse readEntitySet(GetEntitySetUriInfo uriInfo, String contentType) throws ODataException {
    int skip = getSkip(uriInfo);
    int top = getTop(uriInfo);
    FilterExpression filterExpression = uriInfo.getFilter();
    OrderByExpression orderByExpression = uriInfo.getOrderBy();

    switch (uriInfo.getNavigationSegments().size()) {
      case 0:
        final FilterFilter filterFilter = new FilterFilter();
        if (filterExpression != null) {
          filterExpression.accept(filterFilter);
        }
        
        final List<ODataFilter> oDataFilters = new ArrayList<ODataFilter>();

        filterManager.visitAll(new Visitor<SearchRequestEntity>() {
          @Override
          public void visit(SearchRequestEntity t) {
            if (filterFilter.postFilter(t.getName().toLowerCase())) {
              ODataFilter filter = new ODataFilter(filterManager.getSearchRequestById(t.getId()), serviceRoot);
              oDataFilters.add(filter);
            }
          }
        });

        return EntityProvider.writeFeed(contentType, uriInfo.getStartEntitySet(), getODataList(oDataFilters), propertiesBuilder.build());
      case 1:
        String filterID = uriInfo.getKeyPredicates().get(0).getLiteral();
        SearchRequest filter = filterManager.getSearchRequestById(Long.parseLong(filterID));
        StringBuilder queryString = new StringBuilder();

        String target = uriInfo.getNavigationSegments().get(0).getNavigationProperty().getName();
        
        IssueFilter issueFilter = new IssueFilter();

        if (target.equals("Issues")) {
          String filterString = filter.getQuery().getQueryString();
          int orderIndex = filterString.toLowerCase().indexOf("order ");
          if (orderIndex < 0) {
            queryString.append(filterString);
          } else {
            queryString.append(filterString.substring(0, orderIndex));
          }

          if (filterExpression != null) {
            queryString.append(" and (");
            queryString.append(filterExpression.accept(issueFilter));
            queryString.append(")");
          }

          if (orderByExpression != null) {
            queryString.append(orderByExpression.accept(issueFilter));
          }
        } 
        
        // Parse and sanitize the query
        SearchService.ParseResult parseResult = searchService.parseQuery(currentUser, queryString.toString());
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
}
