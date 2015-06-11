package com.sap.jam.samples.jira.plugin.applink;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import org.osgi.framework.Version;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.auth.AuthenticationProvider;
import com.atlassian.applinks.spi.Manifest;
import com.atlassian.applinks.spi.application.ApplicationIdUtil;
import com.atlassian.applinks.spi.application.TypeId;
import com.atlassian.applinks.spi.manifest.ApplicationStatus;
import com.atlassian.applinks.spi.manifest.ManifestNotFoundException;
import com.atlassian.applinks.spi.manifest.ManifestProducer;

public class JamManifestProducer implements ManifestProducer {

    @Override
    public ApplicationStatus getStatus(final URI url) {
        return ApplicationStatus.AVAILABLE;
    }
    
    @Override
    public Manifest getManifest(final URI url) throws ManifestNotFoundException {
        return new Manifest()
        {
            public Long getBuildNumber() {
                return 0L;
            }

            public String getName() {
                return "SAP Jam";
            }

            public URI getIconUrl() {
                return URI.create("https://developer.sapjam.com/images/cubetree_global/body/sap/sap-logo.png");
            }

            public ApplicationId getId() {
                return ApplicationIdUtil.generate(url);
            }

            public Set<Class<? extends AuthenticationProvider>> getInboundAuthenticationTypes() {
                return Collections.<Class<? extends AuthenticationProvider>>singleton(JamAuthenticationProvider.class);
            }

            public Set<Class<? extends AuthenticationProvider>> getOutboundAuthenticationTypes() {
            	return Collections.<Class<? extends AuthenticationProvider>>singleton(JamAuthenticationProvider.class);
            }

            public TypeId getTypeId() {
                return JamApplicationType.TYPE_ID;
            }

            public URI getUrl() {
                return url;
            }

            public String getVersion() {
                return "1.0";
            }

            public Boolean hasPublicSignup() {
                return false;
            }

            public Version getAppLinksVersion() {
                return null;
            }
        };
    }
}
