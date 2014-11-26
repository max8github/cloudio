package com.tip.hood.goog.storg.integration;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.StorageObject;
import com.tip.hood.itest.testutil.RandomContent;
import com.tip.hood.itest.testutil.TUtility;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import java.util.List;
import java.util.Properties;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Sample code from Google Cloud Storage - Java Example. See
 * https://cloud.google.com/storage/docs/json_api/v1/json-api-java-samples . Can also do: git clone
 * https://github.com/GoogleCloudPlatform/cloud-storage-docs-json-api-examples.git except that that example needs to be
 * recompiled to make it work. Javadoc at:
 * https://developers.google.com/resources/api-libraries/documentation/storage/v1/java/latest/ Adapted part of this code
 * also from: com.google.api.services.samples.storage.cmdline.StorageSample.
 */
public class StorageSampleNGTest {

    private static String BUCKET_NAME;
    private static JsonFactory jsonFactory;
    private static Storage client;
    private static final Utils.ViewBucket view = new Utils.ViewBucket();

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
        jsonFactory = JacksonFactory.getDefaultInstance();

        //...then data store factory. Best practice is to make it a single globally shared
        //instance across your application.
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(dataStoreDir);

        //..then authorization...
        Credential credential = Utils.authorize(httpTransport, jsonFactory, dataStoreFactory, clientSecretsJsonPath);
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
    public void testBucket() throws Exception {
        System.out.println("testBucket");

        // Get metadata about the specified bucket.
        Storage.Buckets.Get getBucket = client.buckets().get(BUCKET_NAME);
        getBucket.setProjection("full");
        Bucket bucket = getBucket.execute();
        view.lineBreak();
        view.show(bucket);
    }

    /**
     * List the contents of the bucket.
     *
     * @throws Exception
     */
    @Test
    public void testListObjects() throws Exception {
        System.out.println("testListObjects");
        Storage.Objects.List listObjects = client.objects().list(BUCKET_NAME);
        com.google.api.services.storage.model.Objects objects;
        view.lineBreak("objects in bucket, start");
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
        view.lineBreak();
    }

    @Test
    public void testCreateBucket() throws Exception {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("conf/sample_settings.json");
        SampleSettings settings = SampleSettings.load(jsonFactory, in);
        view.lineBreak("Trying to create bucket '" + settings.getBucket() + "'");
        Storage.Buckets.Insert insertBucket = client.buckets()
                .insert(settings.getProject(), new Bucket().setName(settings.getBucket())
                //                .setDefaultObjectAcl(ImmutableList.of(
                //                new ObjectAccessControl().setEntity("allAuthenticatedUsers").setRole("READER")))
                );
        try {
            @SuppressWarnings("unused")
            Bucket createdBucket = insertBucket.execute();
            System.out.println("Created bucket '" + createdBucket + "'");
        } catch (GoogleJsonResponseException e) {
            GoogleJsonError error = e.getDetails();
            if (error.getCode() == HTTP_CONFLICT
                    && error.getMessage().contains("You already own this bucket.")) {
                System.out.println("already exists");
            } else {
                throw e;
            }
        }
    }

    @Test
    public void testPutObj() throws Exception {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("conf/sample_settings.json");
        SampleSettings settings = SampleSettings.load(jsonFactory, in);
        boolean useCustomMetadata = false;
        view.lineBreak("Uploading object.");
        File file = TUtility.createTFileWithContent("poem01.txt", RandomContent.createPoem(5));
        InputStreamContent mediaContent = new InputStreamContent("text/plain", new FileInputStream(file));
        // Not strictly necessary, but allows optimization in the cloud.
        mediaContent.setLength(file.length());

        Utils.insert(client, useCustomMetadata, settings, mediaContent);
    }

}
