package com.sap.jam.samples.jira.plugin.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.sap.jam.samples.jira.plugin.auth.JamConsumerProviderStore;
import com.sap.jam.samples.jira.plugin.auth.ServiceProvider;

// This class is used to configure the Jam ApplicationLink for Jam ==> JIRA communication
public class JamInboundConfigurationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String TEMPLATE  = "templates/jam-inbound-auth-config.vm";
    
    private final TemplateRenderer         templateRenderer;
    private final I18nResolver             i18nResolver;
    private final ApplicationLinkService   applicationLinkService;
    private final AuthenticationConfigurationManager authenticationConfigurationManager;

    public static final String AUTH_KEY_PARAM     = "inbound_auth_key";
    public static final String AUTH_SECRET_PARAM  = "inbound_auth_secret";
    public static final String AUTH_ENABLED_PARAM = "inbound_auth_enabled";
    public static final String PROVIDER_ID_PARAM  = "inbound_provider_id";
    public static final String X509_CERT_PARAM    = "inbound_x509_cert";
    
    public JamInboundConfigurationServlet(TemplateRenderer templateRenderer,
                                          I18nResolver i18nResolver,
                                          ApplicationLinkService applicationLinkService,
                                          AuthenticationConfigurationManager authenticationConfigurationManager)
    {
        this.templateRenderer = templateRenderer;
        this.i18nResolver = i18nResolver;
        this.applicationLinkService = applicationLinkService;
        this.authenticationConfigurationManager = authenticationConfigurationManager;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("enabled", false);
        
        ApplicationLink applicationLink = getApplicationLink(request);
        JamConsumerProviderStore jamConsumerProviderStore = new JamConsumerProviderStore(authenticationConfigurationManager);
        ServiceProvider serviceProvider = jamConsumerProviderStore.getServiceProvider(applicationLink);
        if ( serviceProvider != null )
        {
            context.put(AUTH_KEY_PARAM,    serviceProvider.getClientId());
            context.put(AUTH_SECRET_PARAM, serviceProvider.getClientSecret());
            context.put(PROVIDER_ID_PARAM, serviceProvider.getProviderId());
            context.put(X509_CERT_PARAM,   serviceProvider.getX509Cert());
            context.put("enabled", true);
        }

        response.setContentType("text/html;charset=utf-8");
        context.put("i18n", i18nResolver);
        
        templateRenderer.render(TEMPLATE, context, response.getWriter());           
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        ApplicationLink applicationLink = getApplicationLink(request);
        JamConsumerProviderStore jamConsumerProviderStore = new JamConsumerProviderStore(authenticationConfigurationManager);
        
        // Get the parameters
        boolean enabled = Boolean.parseBoolean(request.getParameter(AUTH_ENABLED_PARAM));
        
        if ( enabled )
        {
            String clientID     = RandomStringUtils.randomAlphanumeric(32);
            String clientSecret = RandomStringUtils.randomAlphanumeric(32);

            String providerID = request.getParameter(PROVIDER_ID_PARAM);
            String x509Cert   = request.getParameter(X509_CERT_PARAM);

            ServiceProvider serviceProvider = ServiceProvider.create(clientID, clientSecret, providerID, x509Cert);
            if ( serviceProvider != null ) {
                jamConsumerProviderStore.registerServiceProvider(applicationLink, serviceProvider);
            }
        }
        else
        {
            jamConsumerProviderStore.unregisterServiceProvider(applicationLink);
        }

        String message;
        if (enabled) {
            message = i18nResolver.getText("auth.oauth.config.consumer.serviceprovider.success");
        } else {
            message = i18nResolver.getText("auth.oauth.config.consumer.serviceprovider.deleted");
        }
        response.sendRedirect("./" + applicationLink.getId() + "?message=" + URLEncoder.encode(message, "UTF-8"));
    }

    protected ApplicationLink getApplicationLink(HttpServletRequest request)
    {
        String pathInfo = URI.create(request.getPathInfo()).normalize().toString();
        String[] elements = StringUtils.split(pathInfo, '/');
        if (elements.length > 0)
        {
            ApplicationId id = new ApplicationId(elements[0]);
            try {
                ApplicationLink applicationLink = applicationLinkService.getApplicationLink(id);
                if ( applicationLink != null ) {
                    return applicationLink;
                }
            } catch (TypeNotInstalledException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
