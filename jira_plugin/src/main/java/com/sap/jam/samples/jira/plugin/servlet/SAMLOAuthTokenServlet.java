package com.sap.jam.samples.jira.plugin.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.user.UserUtils;
import com.sap.jam.samples.jira.plugin.ao.OAuth2Token;
import com.sap.jam.samples.jira.plugin.applink.JamApplicationType;
import com.sap.jam.samples.jira.plugin.auth.JamConsumerProviderStore;
import com.sap.jam.samples.jira.plugin.auth.ServiceProvider;


// Implements the SAML Bearer Assertion Flow for the inbound SAP Jam Application Link
// See https://tools.ietf.org/html/draft-ietf-oauth-saml2-bearer-23
// The SAMLOAuthTokenFilter will have already validated the client secret.

public class SAMLOAuthTokenServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:saml2-bearer";
    
    private final JamConsumerProviderStore jamConsumerProviderStore;
    private final ApplicationLinkService applicationLinkService;
    
    public SAMLOAuthTokenServlet(AuthenticationConfigurationManager authenticationConfigurationManager,
                                 ApplicationLinkService applicationLinkService)
    {
        this.jamConsumerProviderStore = new JamConsumerProviderStore(authenticationConfigurationManager);
        this.applicationLinkService = applicationLinkService;
        
        // Bootstrap the OpenSAML library
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // Should be the right form type
        if ( !FORM_CONTENT_TYPE.equals(request.getContentType()) ) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "content_type must be " + FORM_CONTENT_TYPE);
            return;
        }
        
        // Should be the right grant type
        if ( !GRANT_TYPE.equals(request.getParameter("grant_type")) ) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "grant_type must be " + GRANT_TYPE);
            return;
        }

        // The clientID in the request parameters must match the one in the authorization header
        String clientID = request.getParameter("client_id");
        if ( clientID == null ) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing client_id parameter");
            return;
        }
        
        // Check to be sure the client has actually sent us an assertion
        String assertion = request.getParameter("assertion");
        if ( assertion == null ) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing assertion parameter");
            return;
        }
        
        // OK - let's get the service provider corresponding to the given client_id and see if the assertion matches a user
        ServiceProvider serviceProvider = jamConsumerProviderStore.getServiceProvider(applicationLinkService.getPrimaryApplicationLink(JamApplicationType.class), clientID);
        if ( serviceProvider == null ) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "could not find configuration for the given client_id");
            return;
        }
        
        // Get the token for the user
        OAuth2Token token = serviceProvider.getTokenFromSAMLAssertion(assertion);
        if ( token == null ) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "assertion failed validation");
            return;
        }
        
        // Double-check that token corresponds to a valid user
        String username = token.getUsername();
        User user = UserUtils.getUser(username);
        if ( user == null ) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "user not found: " + username);
            return;
        }
        
        // OK, all good - return the token
        response.setContentType("application/json");
        PrintWriter responseWriter = response.getWriter();
        responseWriter.print("{\"access_token\":\"" + token.getToken() + "\"}");
    }
}
