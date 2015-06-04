package com.sap.jam.samples.jira.plugin.auth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.java.ao.DBParam;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.saml2.core.Audience;
import org.opensaml.saml2.core.AudienceRestriction;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml2.core.impl.AssertionMarshaller;
import org.opensaml.saml2.core.impl.AttributeBuilder;
import org.opensaml.saml2.core.impl.AttributeStatementBuilder;
import org.opensaml.saml2.core.impl.AudienceBuilder;
import org.opensaml.saml2.core.impl.AudienceRestrictionBuilder;
import org.opensaml.saml2.core.impl.ConditionsBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml2.core.impl.SubjectBuilder;
import org.opensaml.saml2.core.impl.SubjectConfirmationBuilder;
import org.opensaml.saml2.core.impl.SubjectConfirmationDataBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.impl.SignatureBuilder;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.UserUtils;
import com.sap.jam.samples.jira.plugin.ao.OAuth2Token;
import com.sap.jam.samples.jira.plugin.ao.OAuth2Token.Direction;
import com.sap.jam.samples.jira.plugin.components.AOWrapper;

import sun.security.provider.X509Factory;

// Implements JIRA acting as an OAuth2.0 consumer of Jam's services
public class JamConsumer {
    
    private static final String ACCESS_TOKEN_URL = "/api/v1/auth/token";
    private static final String AUDIENCE_RESTRICTION = "cubetree.com";
    private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:saml2-bearer";

    // Consumer credentials
    private final PrivateKey      privateKey;
    private final X509Certificate certificate;
    private final String          clientID;
    private final String          providerID;
    private final String          certificateString;
    private final String          privateKeyString;
    
    // Plugin storage
    public static final String CLIENT_ID_PARAM = "outbound_client_id";
    public static final String PROVIDER_ID_PARAM = "outbound_provider_id";
    public static final String X509_PARAM = "outbound_x509_cert";
    public static final String PRIVATE_KEY_PARAM = "outbound_private_key";

    // JIRA application link to Jam
    private final ApplicationLink   applicationLink;
    private final String            tokenURL;

    private final ActiveObjects activeObjects;
    
    private JamConsumer(final ApplicationLink applicationLink, final String clientID, final String providerID, final X509Certificate certificate, final PrivateKey privateKey, final String certificateString, final String privateKeyString)
    {
        this.applicationLink = applicationLink;
        this.tokenURL        = applicationLink.getRpcUrl().toString() + ACCESS_TOKEN_URL;
        
        this.clientID    = clientID;
        this.providerID  = providerID;
        this.certificate = certificate;
        this.privateKey  = privateKey;
        this.certificateString = certificateString;
        this.privateKeyString  = privateKeyString;

        AOWrapper aoWrapper = ComponentAccessor.getOSGiComponentInstanceOfType(AOWrapper.class);
        activeObjects = aoWrapper.getActiveObjects();
    }
    
    // Factory method will return null on error
    public static JamConsumer create(final ApplicationLink applicationLink, final String clientID, final String providerID, final String certificateString, final String privateKeyString)
    {
        X509Certificate certificate = parseCertificate(certificateString);
        PrivateKey privateKey = parsePrivateKey(privateKeyString);

        if ( certificate != null && privateKey != null ) {
            return new JamConsumer(applicationLink, clientID, providerID, certificate, privateKey, certificateString, privateKeyString);
        }
        return null;
    }
    
    public static JamConsumer create(final ApplicationLink applicationLink, Map<String, String> configuration) {
        if ( configuration.containsKey(CLIENT_ID_PARAM) ) {
            return create(applicationLink, configuration.get(CLIENT_ID_PARAM), configuration.get(PROVIDER_ID_PARAM), configuration.get(X509_PARAM), configuration.get(PRIVATE_KEY_PARAM));
        }
        return null;
    }

    // Basic property accessors
    public String getClientId() {
        return clientID;
    }
    
    public String getProviderId() {
        return providerID;
    }
    
    public String getX509Cert() {
        return certificateString;
    }
    
    public String getTokenURL() {
        return tokenURL;
    }
    
    // Plugin configuration store
    public static Set<String> getConfigurationKeys()
    {
        Set<String> configurationKeys =  new HashSet<String>();
        configurationKeys.add(CLIENT_ID_PARAM);
        configurationKeys.add(PROVIDER_ID_PARAM);
        configurationKeys.add(X509_PARAM);
        configurationKeys.add(PRIVATE_KEY_PARAM);
        return configurationKeys;
    }
    
    public Map<String, String> getConfiguration()
    {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put(CLIENT_ID_PARAM, clientID);
        configuration.put(PROVIDER_ID_PARAM, providerID);
        configuration.put(X509_PARAM, certificateString);
        configuration.put(PRIVATE_KEY_PARAM, privateKeyString);
        return configuration;
    }

    // Gets a OAuth 2.0 bearer token for the given user.  Returns null if the user does not currently have a token.
    public OAuth2Token getToken(final String username)
    {
        OAuth2Token[] dbTokens = activeObjects.find(OAuth2Token.class, "USERNAME = ? AND DIRECTION = ?", username, Direction.OUTBOUND);
        if ( dbTokens.length > 0) {
            return dbTokens[0];
        }
        return null;
    }
    
    public OAuth2Token storeToken(final String username, final String token)
    {
        return activeObjects.create(OAuth2Token.class,
                                    new DBParam("DIRECTION", Direction.OUTBOUND),
                                    new DBParam("USERNAME", username),
                                    new DBParam("TOKEN", token));
    }
    
    // Get a signed SAML assertion request for the given email address
    public String getTokenRequest(final String username)
    {
        User user = UserUtils.getUser(username); 
        String emailAddress = user.getEmailAddress();
        
        Assertion assertion = buildSAMLAssertion(emailAddress);
        String signedAssertion = signAssertion(assertion);
        
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("client_id", clientID));
        postParams.add(new BasicNameValuePair("grant_type", GRANT_TYPE));
        postParams.add(new BasicNameValuePair("assertion", signedAssertion));
        
        return URLEncodedUtils.format(postParams, "UTF-8");
    }

    private static X509Certificate parseCertificate(String x509CertString)
    {
        try {
            byte[] stringBytes = Base64.decode(x509CertString.replaceAll(X509Factory.BEGIN_CERT, "").replaceAll(X509Factory.END_CERT, ""));
            return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(stringBytes));
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static PrivateKey parsePrivateKey(String privateKeyString)
    {
        try {
            String replaceString = privateKeyString.replaceAll("(-+BEGIN RSA PRIVATE KEY-+\\r?\\n|-+END RSA PRIVATE KEY-+\\r?\\n?)", "").replaceAll("Proc-Type: 4,ENCRYPTED", "").replaceAll("DEK-Info: DES-EDE3-CBC,8B14CD0345019AAA", "").replaceAll("\\r\\n", "");
            byte[] stringBytes = Base64.decode(replaceString);
            
            ASN1Sequence primitive = (ASN1Sequence)ASN1Sequence.fromByteArray(stringBytes);
            Enumeration<?> enumeration = primitive.getObjects();
            
            // We only need modulus and private exponent. Skip the rest.
            enumeration.nextElement();
            BigInteger modulus         = ((DERInteger)enumeration.nextElement()).getValue();
            enumeration.nextElement();
            BigInteger privateExponent = ((DERInteger)enumeration.nextElement()).getValue();
            
            RSAPrivateKeySpec spec = new RSAPrivateKeySpec(modulus, privateExponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(spec);
            return privateKey;
            
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Assertion buildSAMLAssertion(final String emailAddress)
    {
        // Bootstrap the OpenSAML library
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
        }

        DateTime issueInstant = new DateTime();
        DateTime notOnOrAfter = issueInstant.plusMinutes(15);
        DateTime notBefore    = issueInstant.minusMinutes(5);
        
        NameID nameID = (new NameIDBuilder().buildObject());
        nameID.setFormat(NameIDType.EMAIL);
        nameID.setValue(emailAddress);
        
        SubjectConfirmationData subjectConfirmationData = (new SubjectConfirmationDataBuilder().buildObject());
        subjectConfirmationData.setRecipient(applicationLink.getRpcUrl().toString() + ACCESS_TOKEN_URL);
        subjectConfirmationData.setNotOnOrAfter(notOnOrAfter);
        
        SubjectConfirmation subjectConfirmation = (new SubjectConfirmationBuilder().buildObject());
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);

        Subject subject = (new SubjectBuilder().buildObject());
        subject.setNameID(nameID);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        
        Issuer issuer = (new IssuerBuilder().buildObject());
        issuer.setValue(providerID);
        
        Audience audience = (new AudienceBuilder().buildObject());
        audience.setAudienceURI(AUDIENCE_RESTRICTION);
        
        AudienceRestriction audienceRestriction = (new AudienceRestrictionBuilder().buildObject());
        audienceRestriction.getAudiences().add(audience);
        
        Conditions conditions = (new ConditionsBuilder().buildObject());
        conditions.setNotBefore(notBefore);
        conditions.setNotOnOrAfter(notOnOrAfter);
        conditions.getAudienceRestrictions().add(audienceRestriction);
        
        XSString attributeValue = (XSString)Configuration.getBuilderFactory().getBuilder(XSString.TYPE_NAME).buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attributeValue.setValue(clientID);

        Attribute attribute = (new AttributeBuilder().buildObject());
        attribute.setName("client_id");
        attribute.getAttributeValues().add(attributeValue);

        AttributeStatement attributeStatement = (new AttributeStatementBuilder().buildObject());
        attributeStatement.getAttributes().add(attribute);

        Assertion assertion = (new AssertionBuilder().buildObject());
        assertion.setID(UUID.randomUUID().toString());
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setIssueInstant(issueInstant);
        assertion.setIssuer(issuer);
        assertion.setSubject(subject);
        assertion.setConditions(conditions);
        assertion.getAttributeStatements().add(attributeStatement);

        return assertion;
    }
    
    // Signs the assertion and returns the Base64-encoded string representation of the signed assertion
    private String signAssertion(Assertion assertion)
    {
        // Build the signing credentials
        BasicX509Credential signingCredential = new BasicX509Credential();
        
        signingCredential.setEntityCertificate(certificate);
        signingCredential.setPrivateKey(privateKey);
        
        // Build up the signature
        SignatureBuilder signatureBuilder = new SignatureBuilder();
        Signature signature = signatureBuilder.buildObject();
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        signature.setSigningCredential(signingCredential);
        
        assertion.setSignature(signature);
        
        String base64Assertion = null;
        try {
            // Marshal the assertion
            AssertionMarshaller marshaller = new AssertionMarshaller();
            Element element = marshaller.marshall(assertion);
            
            // Finally, sign the assertion - this must be done after marshalling
            Signer.signObject(signature);
            
            // Dump the assertion to a Base64-encoded string and return it
            String assertionString = XMLHelper.nodeToString(element);
            base64Assertion = new String(Base64.encodeBytes(assertionString.getBytes(), Base64.DONT_BREAK_LINES));
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (MarshallingException e) {
            e.printStackTrace();
        }
        
        return base64Assertion;
    }
}
