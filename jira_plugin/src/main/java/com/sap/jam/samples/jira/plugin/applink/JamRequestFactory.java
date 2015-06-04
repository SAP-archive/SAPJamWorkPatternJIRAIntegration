package com.sap.jam.samples.jira.plugin.applink;

import java.net.URI;

import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;

import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.RequestFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sap.jam.samples.jira.plugin.ao.OAuth2Token;
import com.sap.jam.samples.jira.plugin.auth.JamConsumer;

// Request factory for creating signed Jam OAuth requests
public class JamRequestFactory implements ApplicationLinkRequestFactory {
    
    private final RequestFactory<?> requestFactory;
    private final JamConsumer       consumer;
    private final String            username;
    
    // The POST to Jam will return a token response
    private class TokenResponse {
        public String access_token;
    }
    private Gson gson = new GsonBuilder().create();
    
    
    public JamRequestFactory(RequestFactory<?> requestFactory, JamConsumer consumer, String username)
    {
        this.requestFactory = requestFactory;
        this.consumer       = consumer;
        this.username       = username;

    
        // Bootstrap the OpenSAML library
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
        }
    }

    @Override
    public ApplicationLinkRequest createRequest(MethodType methodType, String url) throws CredentialsRequiredException
    {
        Request<?, ?> request = requestFactory.createRequest(methodType, url);
        
        // First, check the DB
        OAuth2Token token = consumer.getToken(username);
        
        // If it's not in the DB, then get it via a POSTed SAML assertion
        if ( token == null ) {
            token = getTokenViaSAMLAssertion(username);
        }

        // Create and return the new Jam request object
        return new JamRequest(token, request);
    }

    // The two authorizationURI methods return null, because this is an SSO provider.  Failure to get credentials means no access.
    @Override
    public URI getAuthorisationURI(URI callback) {
        return null;
    }

    @Override
    public URI getAuthorisationURI() {
        return null;
    }
    
    private OAuth2Token getTokenViaSAMLAssertion(final String username)
    {
        // Token not found in DB cache.  Let's go ask Jam for it.
        Request<?, ?> request = requestFactory.createRequest(MethodType.POST, consumer.getTokenURL());
        request.setRequestContentType("application/x-www-form-urlencoded");
        String requestBody = consumer.getTokenRequest(username);
        request.setRequestBody(requestBody);
        
        OAuth2Token token = null;
        try {
            String response = request.execute();
            TokenResponse tokenResponse = gson.fromJson(response, TokenResponse.class);
            String access_token = tokenResponse.access_token;
            
            // We have a consumer token. Store it in the database
            token = consumer.storeToken(username, access_token);
        } catch (ResponseException e) {
            e.printStackTrace();
        }
        return token;
    }

}
