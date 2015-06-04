package com.sap.jam.samples.jira.plugin.auth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.java.ao.DBParam;

import org.apache.commons.lang.RandomStringUtils;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.UserUtils;
import com.sap.jam.samples.jira.plugin.ao.OAuth2Token;
import com.sap.jam.samples.jira.plugin.ao.OAuth2Token.Direction;
import com.sap.jam.samples.jira.plugin.components.AOWrapper;

// Implements JIRA acting as an OAuth 2.0 Service Provider
public class ServiceProvider
{
    private final String    clientID;
    private final String    clientSecret;
    private final String    providerID;
    private final String    x509Cert;
    private final PublicKey publicKey;

    public static final String CLIENT_ID_PARAM = "inbound_client_id";
    public static final String CLIENT_SECRET_PARAM = "inbound_client_secret";
    public static final String PROVIDER_ID_PARAM = "inbound_provider_id";
    public static final String X509_PARAM = "inbound_x509_cert";
    
    private final ActiveObjects activeObjects;
    
    private ServiceProvider(final String clientID, final String clientSecret, final String providerID, final String x509Cert, final PublicKey publicKey)
    {
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.providerID = providerID;
        this.publicKey = publicKey;
        this.x509Cert = x509Cert;
        
        AOWrapper aoWrapper = ComponentAccessor.getOSGiComponentInstanceOfType(AOWrapper.class);
        activeObjects = aoWrapper.getActiveObjects();
    }
    
    // Factory method will return null on error
    public static ServiceProvider create(final String clientID, final String clientSecret, final String providerID, final String x509Cert)
    {
        // Extract the public key from the X509 certificate
        PublicKey publicKey = null;
        try {
            byte[] certificateBytes = x509Cert.getBytes();
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
            Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(certificateBytes));
            publicKey = certificate.getPublicKey();
        } catch (CertificateException e) {
            return null;
        }
        
        return new ServiceProvider(clientID, clientSecret, providerID, x509Cert, publicKey);
    }

    public static ServiceProvider create(Map<String, String> configuration)
    {
        if ( configuration.containsKey(CLIENT_ID_PARAM) ) {
            return create(configuration.get(CLIENT_ID_PARAM), configuration.get(CLIENT_SECRET_PARAM),
                          configuration.get(PROVIDER_ID_PARAM), configuration.get(X509_PARAM));
        }
        return null;
    }    
    
    public String getClientId() {
        return clientID;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public String getProviderId() {
        return providerID;
    }
    
    public String getX509Cert() {
        return x509Cert;
    }

    // Plugin configuration store
    public static Set<String> getConfigurationKeys()
    {
        Set<String> configurationKeys =  new HashSet<String>();
        configurationKeys.add(CLIENT_ID_PARAM);
        configurationKeys.add(CLIENT_SECRET_PARAM);
        configurationKeys.add(PROVIDER_ID_PARAM);
        configurationKeys.add(X509_PARAM);
        return configurationKeys;
    }
    
    public Map<String, String> getConfiguration()
    {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put(CLIENT_ID_PARAM, clientID);
        configuration.put(CLIENT_SECRET_PARAM, clientSecret);
        configuration.put(PROVIDER_ID_PARAM, providerID);
        configuration.put(X509_PARAM, x509Cert);
        return configuration;
    }

    // Gets an OAuth2.0 token from the given bearer token string.  Returns null on bad token.
    public OAuth2Token getTokenFromBearerToken(final String bearerToken)
    {
        // For a bearer token, the corresponding access token MUST be in our DB
        OAuth2Token[] dbTokens = activeObjects.find(OAuth2Token.class, "TOKEN = ?", bearerToken);
        if ( dbTokens.length > 0) {
            return dbTokens[0];
        }
        return null;
    }

    // Gets an OAuth2.0 token from the given SAML assertion.  Returns null on bad assertion.
    // Will store tokens in the JIRA DB.
    public OAuth2Token getTokenFromSAMLAssertion(final String base64Assertion)
    {
        OAuth2Token token = null;
        
        try {
            String decodedAssertion = new String(Base64.decode(base64Assertion));
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            
            Document document = documentBuilder.parse(new ByteArrayInputStream(decodedAssertion.getBytes()));
            Element element = document.getDocumentElement();
    
            Unmarshaller unmarshaller = (Configuration.getUnmarshallerFactory()).getUnmarshaller(element);
            Assertion assertion = (Assertion)unmarshaller.unmarshall(element);
            
            // Validate the signature
            BasicX509Credential publicCredential = new BasicX509Credential();
            publicCredential.setPublicKey(publicKey);
    
            Signature signature = assertion.getSignature();
            SignatureValidator signatureValidator = new SignatureValidator(publicCredential);
            signatureValidator.validate(signature);
            
            // Validate the conditions
            Conditions conditions = assertion.getConditions();
            if ( conditions.getNotOnOrAfter().isBeforeNow() ) {
                return null;
            }
            if ( conditions.getNotBefore().isAfterNow() ) {
                return null;
            }
    
            // Confirm the issuer
            Issuer issuer = assertion.getIssuer();
            if ( !providerID.equals(issuer.getValue()) ) {
                return null;
            }
            
            // Get the asserted user
            NameID nameID = assertion.getSubject().getNameID();
            
            // At the moment, we only accept email assertions
            if ( !NameIDType.EMAIL.equals(nameID.getFormat()) ) {
                return null;
            }
            
            // Look up this user
            User user = UserUtils.getUserByEmail(nameID.getValue());
            
            if ( user != null ) {
                // Return a token but first, check if we already have a token in the DB
                OAuth2Token[] dbTokens = activeObjects.find(OAuth2Token.class, "USERNAME = ? AND DIRECTION = ?", user.getName(), Direction.INBOUND);
                if ( dbTokens.length > 0) {
                    token = dbTokens[0];
                } else {
                    token = activeObjects.create(OAuth2Token.class,
                            new DBParam("DIRECTION", Direction.INBOUND),
                            new DBParam("USERNAME", user.getName()),
                            new DBParam("TOKEN", RandomStringUtils.randomAlphanumeric(32)));
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnmarshallingException e) {
            e.printStackTrace();
        } catch (ValidationException e) {
            e.printStackTrace();
        }
        return token;
    }
}
