package com.sap.jam.samples.jira.plugin.applink;

import javax.servlet.http.HttpServletRequest;

import org.osgi.framework.Version;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.auth.AuthenticationProvider;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import com.atlassian.applinks.spi.auth.AuthenticationDirection;
import com.atlassian.applinks.spi.auth.AuthenticationProviderPluginModule;
import com.atlassian.applinks.spi.auth.IncomingTrustAuthenticationProviderPluginModule;
import com.atlassian.sal.api.net.RequestFactory;
import com.sap.jam.samples.jira.plugin.auth.JamConsumerProviderStore;

public class JamAuthenticationProviderPluginModule implements AuthenticationProviderPluginModule, IncomingTrustAuthenticationProviderPluginModule
{
    // Imported JIRA components
    private final HostApplication   hostApplication;
    private final RequestFactory<?> requestFactory;
    
    // Jam OAuth store
    private final JamConsumerProviderStore jamConsumerProviderStore;
    
    public JamAuthenticationProviderPluginModule(HostApplication hostApplication,
                                                 RequestFactory<?> requestFactory,
                                                 AuthenticationConfigurationManager authenticationConfigurationManager)
    {
        this.hostApplication = hostApplication;
        this.requestFactory = requestFactory;
        this.jamConsumerProviderStore = new JamConsumerProviderStore(authenticationConfigurationManager);
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider(ApplicationLink applicationLink)
    {
        if ( jamConsumerProviderStore.getConsumer(applicationLink) != null ) {
            return new JamAuthenticationProviderImpl(requestFactory, jamConsumerProviderStore, applicationLink);
        } else {
            return null;
        }
    }

    @Override
    public String getConfigUrl(ApplicationLink link, Version applicationLinksVersion, AuthenticationDirection direction, HttpServletRequest request)
    {
    	String directionURISegment = "inbound/";
        if (direction == AuthenticationDirection.OUTBOUND)
        {
        	directionURISegment = "outbound/";
        }
        return hostApplication.getBaseUrl().toString() + "/plugins/servlet/sapjam/config/" + directionURISegment + link.getId().get();
    }

    @Override
    public Class<? extends AuthenticationProvider> getAuthenticationProviderClass()
    {
        return JamAuthenticationProvider.class;
    }

	@Override
	public boolean incomingEnabled(ApplicationLink applicationLink)
	{
	    return false;
	}

}
