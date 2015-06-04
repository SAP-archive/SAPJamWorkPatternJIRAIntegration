package com.sap.jam.samples.jira.plugin.auth;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import com.sap.jam.samples.jira.plugin.applink.JamAuthenticationProvider;

public class JamConsumerProviderStore
{
    private final AuthenticationConfigurationManager authenticationConfigurationManager;

    public JamConsumerProviderStore(final AuthenticationConfigurationManager authenticationConfigurationManager)
    {
        this.authenticationConfigurationManager = authenticationConfigurationManager;
    }
    
    public ServiceProvider getServiceProvider(ApplicationLink applicationLink, String clientID)
    {
        Map<String, String> configuration = getConfiguration(applicationLink);
        if ( !configuration.isEmpty() && clientID.equals(configuration.get(ServiceProvider.CLIENT_ID_PARAM))) {
            return ServiceProvider.create(configuration);
        }
        return null;
    }

    public ServiceProvider getServiceProvider(ApplicationLink applicationLink)
    {
        Map<String, String> configuration = getConfiguration(applicationLink);
        if ( !configuration.isEmpty() ) {
            return ServiceProvider.create(configuration);
        }
        return null;
    }

    public JamConsumer getConsumer(ApplicationLink applicationLink)
    {
        Map<String, String> configuration = getConfiguration(applicationLink);
        if ( !configuration.isEmpty() ) {
            return JamConsumer.create(applicationLink, configuration);
        }
        return null;
    }

    public void registerServiceProvider(ApplicationLink applicationLink, ServiceProvider serviceProvider)
    {
        Map<String, String> configuration = getConfiguration(applicationLink);
        configuration.putAll(serviceProvider.getConfiguration());
        updateConfiguration(applicationLink, configuration);
    }

    public void registerConsumer(ApplicationLink applicationLink, JamConsumer consumer)
    {
        Map<String, String> configuration = getConfiguration(applicationLink);
        configuration.putAll(consumer.getConfiguration());
        updateConfiguration(applicationLink, configuration);
    }

    public boolean unregisterServiceProvider(ApplicationLink applicationLink)
    {
        Map<String, String> configuration = getConfiguration(applicationLink);
        if ( configuration.isEmpty() ) {
            return false;  // Could not find consumer
        }
        
        for (String configKey : ServiceProvider.getConfigurationKeys()) {
            configuration.remove(configKey);
        }
        updateConfiguration(applicationLink, configuration);
        return true;
    }

    public boolean unregisterConsumer(ApplicationLink applicationLink)
    {
        Map<String, String> configuration = getConfiguration(applicationLink);
        if ( configuration.isEmpty() ) {
            return false;  // Could not find consumer
        }
        
        for (String configKey : JamConsumer.getConfigurationKeys()) {
            configuration.remove(configKey);
        }
        updateConfiguration(applicationLink, configuration);
        return true;
    }
    
    private void updateConfiguration(ApplicationLink applicationLink, Map<String, String> configuration)
    {
        ApplicationId applicationID = applicationLink.getId();
        if ( authenticationConfigurationManager.isConfigured(applicationID, JamAuthenticationProvider.class) ) {
            authenticationConfigurationManager.unregisterProvider(applicationID, JamAuthenticationProvider.class);
        }
        
        if ( !configuration.isEmpty() ) {
            authenticationConfigurationManager.registerProvider(applicationID, JamAuthenticationProvider.class, configuration);
        }
    }
    
    private Map<String, String> getConfiguration(ApplicationLink applicationLink)
    {
        ApplicationId applicationID = applicationLink.getId();
        Map<String, String> configuration = new HashMap<String, String>();
        if ( authenticationConfigurationManager.isConfigured(applicationID, JamAuthenticationProvider.class) ) {
            configuration.putAll(authenticationConfigurationManager.getConfiguration(applicationID, JamAuthenticationProvider.class));
        }
        return configuration;
    }
}
