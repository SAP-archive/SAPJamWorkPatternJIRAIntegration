package com.sap.jam.samples.jira.plugin.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.xml.util.Base64;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import com.sap.jam.samples.jira.plugin.applink.JamApplicationType;
import com.sap.jam.samples.jira.plugin.auth.JamConsumerProviderStore;
import com.sap.jam.samples.jira.plugin.auth.ServiceProvider;

// SAP Jam passes the OAuth 2.0 client ID and client secret into the SAML token POST endpoint via the "Basic" Authorization header
// This get intercepted by JIRA's basic authentication handler, resulting in a 401 before the POST get processed.
// client_id is also a parameter in the SAML post, and client_secret is not used for anything but a second-level of validation.
// Perform that validation here, and clear the Authorization header.

public class SAMLOAuthTokenFilter implements Filter {

    private final JamConsumerProviderStore jamConsumerProviderStore;
    private final ApplicationLinkService applicationLinkService;
    
    public SAMLOAuthTokenFilter(AuthenticationConfigurationManager authenticationConfigurationManager,
                                ApplicationLinkService applicationLinkService)
    {
        this.jamConsumerProviderStore = new JamConsumerProviderStore(authenticationConfigurationManager);
        this.applicationLinkService = applicationLinkService;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String header = request.getHeader("Authorization");
        boolean authorized = false;

        if (header != null && header.startsWith("Basic "))
        {
            String base64Token = header.substring(6);
            String token = new String(Base64.decode(base64Token));

            int delim = token.indexOf(":");
            if (delim != -1)
            {
                String clientID = token.substring(0, delim);
                String clientSecret = token.substring(delim + 1);
                
                ServiceProvider serviceProvider = jamConsumerProviderStore.getServiceProvider(applicationLinkService.getPrimaryApplicationLink(JamApplicationType.class), clientID);
                if ( serviceProvider != null && clientSecret.equals(serviceProvider.getClientSecret()) )
                {
                    // Clear out the Authorization header so that JIRA's login handler doesn't grab it
                    request = wrapRequest(request);
                    authorized = true;
                }
            }
        }

        if ( !authorized ) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
    
    private static HttpServletRequest wrapRequest(HttpServletRequest request)
    {
        return new HttpServletRequestWrapper(request) {
            public String getHeader(String name) {
                if ( name.equalsIgnoreCase("Authorization") ) {
                    return null;
                }
                return super.getHeader(name);
            }
            
            @SuppressWarnings("unchecked")
            public Enumeration<String> getHeaders(String name) {
                if ( name.equalsIgnoreCase("Authorization") ) {
                    return Collections.enumeration(Collections.EMPTY_SET);
                }
                return super.getHeaders(name);
            }
        };
    }

}
