/*
 * 
 */
package com.tip.hood.goog.storg.integration;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.StorageObject;
import com.tip.hood.itest.testutil.TUtility;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Sample code from Google Cloud Storage - Java Example. See
 * https://cloud.google.com/storage/docs/json_api/v1/json-api-java-samples . Can also do: git clone
 * https://github.com/GoogleCloudPlatform/cloud-storage-docs-json-api-examples.git except that that example needs to be
 * recompiled to make it work.
 */
public class StorageSampleNGTest {

    private static String BUCKET_NAME;

    private static Storage client;

    public StorageSampleNGTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Configuration properties
        final Properties p = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().
                getResourceAsStream("conf/props.properties")) {
            p.load(in);
        }
        File dataStoreDir = new File(p.getProperty("dataStoreDir"));//directory containing file StoredCredential
        BUCKET_NAME = p.getProperty("bucketName");
        String applicationName = p.getProperty("applicationName");
        String clientSecretsJsonPath = p.getProperty("clientSecretsJsonPath");
        TUtility.assertFileExists(clientSecretsJsonPath);

        // Initialize transport...
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        //then json factory...
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        
        //...then data store factory. Best practice is to make it a single globally shared
        //instance across your application.
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(dataStoreDir);

        //..then authorization...
        Credential credential = authorize(httpTransport, jsonFactory, dataStoreFactory, clientSecretsJsonPath);
        //...then a Storage instance for the client
        client = new Storage.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(applicationName).build();
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
    public void testStorageSample() throws Exception {
        System.out.println("storageSample");

        // Get metadata about the specified bucket.
        Storage.Buckets.Get getBucket = client.buckets().get(BUCKET_NAME);
        getBucket.setProjection("full");
        Bucket bucket = getBucket.execute();
        System.out.println("name: " + BUCKET_NAME);
        System.out.println("location: " + bucket.getLocation());
        System.out.println("timeCreated: " + bucket.getTimeCreated());
        System.out.println("owner: " + bucket.getOwner());

        // List the contents of the bucket.
        Storage.Objects.List listObjects = client.objects().list(BUCKET_NAME);
        com.google.api.services.storage.model.Objects objects;
        do {
            objects = listObjects.execute();
            List<StorageObject> items = objects.getItems();
            if (null == items) {
                System.out.println("There were no objects in the given bucket; try adding some and re-running.");
                break;
            }
            for (StorageObject object : items) {
                System.out.println(object.getName() + " (" + object.getSize() + " bytes)");
            }
            listObjects.setPageToken(objects.getNextPageToken());
        } while (null != objects.getNextPageToken());

    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private static Credential authorize(HttpTransport httpTransport, JsonFactory JSON_FACTORY,
            FileDataStoreFactory dataStoreFactory, String clientSecretsJsonPath) throws Exception {
        // Load client secrets.
        GoogleClientSecrets clientSecrets = null;
        try {
            InputStream resourceAsStream = new FileInputStream(clientSecretsJsonPath);
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(resourceAsStream));
            if (clientSecrets.getDetails().getClientId() == null
                    || clientSecrets.getDetails().getClientSecret() == null) {
                throw new Exception("client_secrets not well formed.");
            }
        } catch (Exception e) {
            System.out.println("Problem loading client_secrets.json file. Make sure it exists, you are "
                    + "loading it with the right path, and a client ID and client secret are "
                    + "defined in it.\n" + e.getMessage());
            throw new IOException(e);
        }

        // Set up authorization code flow.
        // Ask for only the permissions you need. Asking for more permissions will
        // reduce the number of users who finish the process for giving you access
        // to their accounts. It will also increase the amount of effort you will
        // have to spend explaining to users what you are doing with their data.
        // Here we are listing all of the available scopes. You should remove scopes
        // that you are not actually using.
        Set<String> scopes = new HashSet<>();
        scopes.add(StorageScopes.DEVSTORAGE_FULL_CONTROL);
        scopes.add(StorageScopes.DEVSTORAGE_READ_ONLY);
        scopes.add(StorageScopes.DEVSTORAGE_READ_WRITE);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, scopes)
                .setDataStoreFactory(dataStoreFactory)
                .build();
        // Authorize.
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

}
