package com.sap.jam.samples.jira.plugin.applink;

import java.util.List;
import java.util.Map;

import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFilePart;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import com.atlassian.sal.api.net.ReturningResponseHandler;
import com.atlassian.sal.api.net.auth.Authenticator;
import com.sap.jam.samples.jira.plugin.ao.OAuth2Token;

// This mostly wraps the default Request object, and adds OAuth-SAML support
// We really only need to support the methods used by com.sap.jam.samples.jira.plugin.odata.client.JamClient
// We will not support other clients
//
// See http://blogs.atlassian.com/2011/06/unified_applinks_integration_without_the_hassle___part_3 for more details.


public class JamRequest implements ApplicationLinkRequest {
    
    private final Request<?, ?> request;
    private final OAuth2Token   token;
    
    public JamRequest(OAuth2Token token, Request<?, ?> request)
    {
        this.request = request;
        this.token = token;
    }

    @Override
    public ApplicationLinkRequest addHeader(String headerName, String headerValue)
    {
    	request.addHeader(headerName, headerValue);
        return this;
    }

    @Override
    public ApplicationLinkRequest setRequestBody(String requestBody) {
        request.setRequestBody(requestBody);
        return this;
    }

    @Override
    public ApplicationLinkRequest setRequestContentType(String contentType) {
        request.setRequestContentType(contentType);
        return this;
    }

    @Override
    public String execute() throws ResponseException
    {
    	String access_token = token.getToken();
    	request.setHeader("Authorization", "Bearer " + access_token);
    	return request.execute();
    }

    // ======================================================
    // Everything below this line is not used by JamClient, and therefore stubbed out
    // ======================================================
    @Override
    public ApplicationLinkRequest setConnectionTimeout(int connectionTimeout) {
        return this;
    }

    @Override
    public ApplicationLinkRequest setSoTimeout(int soTimeout) {
        return this;
    }

    @Override
    public ApplicationLinkRequest setUrl(String url) {
        return this;
    }

    @Override
    public ApplicationLinkRequest setEntity(Object entity) {
        return this;
    }

    @Override
    public ApplicationLinkRequest setFiles(List<RequestFilePart> files) {
        return this;
    }

    @Override
    public ApplicationLinkRequest addRequestParameters(String... params) {
        return this;
    }

    @Override
    public ApplicationLinkRequest addAuthentication(Authenticator authenticator) {
        return this;
    }

    @Override
    public ApplicationLinkRequest addTrustedTokenAuthentication() {
        return this;
    }

    @Override
    public ApplicationLinkRequest addTrustedTokenAuthentication(String username) {
        return this;
    }

    @Override
    public ApplicationLinkRequest addBasicAuthentication(String username, String password) {
        return this;
    }

    @Override
    public ApplicationLinkRequest addSeraphAuthentication(String username, String password) {
        return this;
    }

    @Override
    public ApplicationLinkRequest setHeader(String headerName, String headerValue) {
        return this;
    }

    @Override
    public ApplicationLinkRequest setFollowRedirects(boolean follow) {
        return this;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return null;
    }

    @Override
    public void execute(ResponseHandler<Response> responseHandler)
            throws ResponseException {
    }

    @Override
    public <RET> RET executeAndReturn(
            ReturningResponseHandler<Response, RET> responseHandler)
            throws ResponseException {
        return null;
    }

    @Override
    public <R> R execute(ApplicationLinkResponseHandler<R> responseHandler)
            throws ResponseException {
        return null;
    }

}
