package com.sap.jam.samples.jira.plugin.odata.server.processors;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;
import org.apache.olingo.odata2.api.uri.info.GetEntitySetUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntityUriInfo;
import org.apache.olingo.odata2.api.uri.info.PostUriInfo;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import com.atlassian.crowd.embedded.api.User;
import com.sap.jam.samples.jira.plugin.ao.OAuth2Token;
import com.sap.jam.samples.jira.plugin.applink.JamApplicationType;
import com.sap.jam.samples.jira.plugin.auth.JamConsumerProviderStore;
import com.sap.jam.samples.jira.plugin.auth.ServiceProvider;


public class JiraODataProcessor extends ODataSingleProcessor {

	private final IssueProcessor issueProcessor;
	private final FilterProcessor filterProcessor;
	
    public JiraODataProcessor(ODataContext ctx) throws ODataException
	{
    	// Authenticate this user
	    authenticate((HttpServletRequest) ctx.getParameter(ODataContext.HTTP_SERVLET_REQUEST_OBJECT)); 

	    issueProcessor   = new IssueProcessor(ctx);
            filterProcessor = new FilterProcessor(ctx);
	}

	@Override
	public ODataResponse readEntity(GetEntityUriInfo uriInfo, String contentType) throws ODataException
	{
		String entitySet = uriInfo.getStartEntitySet().getName();
		if ( entitySet.equals("Issues") ) {
                  return issueProcessor.readEntity(uriInfo, contentType);
		} else if ( entitySet.equals("Filters") ) {
                  return filterProcessor.readEntity(uriInfo, contentType);
		}   
		throw new ODataNotImplementedException();
	}

	@Override
	public ODataResponse readEntitySet(GetEntitySetUriInfo uriInfo, String contentType) throws ODataException
	{
		String entitySet = uriInfo.getStartEntitySet().getName();
		if ( entitySet.equals("Issues") ) {
                  return issueProcessor.readEntitySet(uriInfo, contentType);
		} else if ( entitySet.equals("Filters") ) {
                  return filterProcessor.readEntitySet(uriInfo, contentType);
		}
		throw new ODataNotImplementedException();
	}
	
	@Override
	public ODataResponse createEntity(PostUriInfo uriInfo, InputStream content, String requestContentType, String contentType) throws ODataException
	{
		throw new ODataNotImplementedException();
	}
	
	private void authenticate(HttpServletRequest request) throws ODataException
	{
        final String authorization = request.getHeader("Authorization");
        
        if (authorization != null && authorization.startsWith("Bearer "))
        {
            String token = authorization.substring("Bearer".length()).trim();

            // Initialize JIRA services
            AuthenticationConfigurationManager authenticationConfigurationManager = ComponentAccessor.getOSGiComponentInstanceOfType(AuthenticationConfigurationManager.class);
            ApplicationLinkService applicationLinkService = ComponentAccessor.getOSGiComponentInstanceOfType(ApplicationLinkService.class);
            
            JamConsumerProviderStore jamConsumerProviderStore = new JamConsumerProviderStore(authenticationConfigurationManager);
            ServiceProvider jamServiceProvider = jamConsumerProviderStore.getServiceProvider(applicationLinkService.getPrimaryApplicationLink(JamApplicationType.class));
            
            if ( jamServiceProvider != null )
            {
                OAuth2Token jamToken = jamServiceProvider.getTokenFromBearerToken(token);
                if ( jamToken != null ) {
                    User user = ComponentAccessor.getUserUtil().getUserObject(jamToken.getUsername());
                    if ( user != null ) {
                        // Set the current user context to this user!
                        ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
                        return;
                    }
                }
            }
        }
    	throw new ODataUnauthorizedException();
    }
}
