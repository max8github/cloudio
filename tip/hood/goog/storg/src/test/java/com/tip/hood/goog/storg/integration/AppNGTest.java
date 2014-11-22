package com.tip.hood.goog.storg.integration;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.tip.hood.itest.testutil.TUtility;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Properties;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author max
 */
public class AppNGTest {

    /**
     * Global configuration of Google Cloud Storage, OAuth 2.0.
     */
    private static final String STORAGE_SCOPE
            = "https://www.googleapis.com/auth/devstorage.read_write";
    private HttpTransport httpTransport;
    private GoogleCredential credential;
    private String bucketName;
    private JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public AppNGTest() {
    }

    @BeforeClass
    public void setUpClass() throws Exception {
        final Properties p = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().
                getResourceAsStream("conf/props.properties")) {
            p.load(in);
        }

        String serviceAccountEmail = p.getProperty("serviceAccountEmail");
        bucketName = p.getProperty("bucketName");
        String keysFilePath = p.getProperty("keysFilePath");

        // Check for valid setup.
        Assert.assertNotEquals(serviceAccountEmail, "", "Please provide your "
                + "service account e-mail from the Google APIs "
                + "in your ~/.m2/settings.xml file (read about settings.xml "
                + "properties in Maven)");
        Assert.assertNotEquals(bucketName, "", "Please provide your "
                + "your desired Google Cloud Storage bucket name "
                + "in your ~/.m2/settings.xml file (read about settings.xml "
                + "properties in Maven)");
        TUtility.assertFileExists(keysFilePath);
        
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JSON_FACTORY = JacksonFactory.getDefaultInstance();

        // Build a service account credential.
        credential = new GoogleCredential.Builder().setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(serviceAccountEmail)
                .setServiceAccountScopes(Collections.singleton(STORAGE_SCOPE))
                .setServiceAccountPrivateKeyFromP12File(new File(keysFilePath))
                .build();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    @Test
    public void testMain() throws Exception {
        System.out.println("main");

        // Set up and execute a Google Cloud Storage request.
        String URI = "https://storage.googleapis.com/" + bucketName;
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
        GenericUrl url = new GenericUrl(URI);
        HttpRequest request = requestFactory.buildGetRequest(url);
        HttpResponse response = request.execute();
        String content = response.parseAsString();

        // Instantiate transformer input.
        Source xmlInput = new StreamSource(new StringReader(content));
        StreamResult xmlOutput = new StreamResult(new StringWriter());

        // Configure transformer.
        Transformer transformer = TransformerFactory.newInstance().newTransformer(); // An identity
        // transformer
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "testing.dtd");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(xmlInput, xmlOutput);

        // Pretty print the output XML.
        System.out.println("\nBucket listing for " + bucketName + ":\n");
        System.out.println(xmlOutput.getWriter().toString());
    }

}
