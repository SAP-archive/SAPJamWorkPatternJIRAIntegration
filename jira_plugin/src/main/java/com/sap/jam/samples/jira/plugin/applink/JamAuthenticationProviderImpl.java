package com.sap.jam.samples.jira.plugin.applink;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.sal.api.net.RequestFactory;
import com.sap.jam.samples.jira.plugin.auth.JamConsumer;
import com.sap.jam.samples.jira.plugin.auth.JamConsumerProviderStore;

public class JamAuthenticationProviderImpl implements JamAuthenticationProvider {

    private final RequestFactory<?> requestFactory;
    private final JamConsumer       consumer;

    public JamAuthenticationProviderImpl(RequestFactory<?> requestFactory,
                                         JamConsumerProviderStore jamConsumerProviderStore,
                                         ApplicationLink applicationLink)
    {
        this.requestFactory = requestFactory;
        this.consumer = jamConsumerProviderStore.getConsumer(applicationLink);
    }
    
    @Override
    public ApplicationLinkRequestFactory getRequestFactory(String username) {
        return new JamRequestFactory(requestFactory, consumer, username);
    }

}
