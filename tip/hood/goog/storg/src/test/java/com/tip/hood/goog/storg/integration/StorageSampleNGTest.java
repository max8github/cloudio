package com.tip.hood.goog.storg.integration;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Lists;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.ObjectAccessControl;
import com.google.api.services.storage.model.StorageObject;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.tip.hood.itest.testutil.RandomContent;
import com.tip.hood.itest.testutil.TUtility;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
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
 * recompiled to make it work. Javadoc at:
 * https://developers.google.com/resources/api-libraries/documentation/storage/v1/java/latest/ Adapted part of this code
 * also from: com.google.api.services.samples.storage.cmdline.StorageSample.
 */
public class StorageSampleNGTest {

    private static String BUCKET_NAME;
    private static JsonFactory jsonFactory;
    private static Storage client;
    private static final ViewBucket view = new ViewBucket();

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

        StorageObject objectMetadata = null;

        if (useCustomMetadata) {
            // If you have custom settings for metadata on the object you want to set
            // then you can allocate a StorageObject and set the values here. You can
            // leave out setBucket(), since the bucket is in the insert command's
            // parameters.
            List<ObjectAccessControl> acl = Lists.newArrayList();
            if (settings.getEmail() != null && !settings.getEmail().isEmpty()) {
                acl.add(
                        new ObjectAccessControl().setEntity("user-" + settings.getEmail()).setRole("OWNER"));
            }
            if (settings.getDomain() != null && !settings.getDomain().isEmpty()) {
                acl.add(new ObjectAccessControl().setEntity("domain-" + settings.getDomain())
                        .setRole("READER"));
            }
            objectMetadata = new StorageObject().setName(settings.getPrefix() + "myobject")
                    .setMetadata(ImmutableMap.of("key1", "value1", "key2", "value2")).setAcl(acl)
                    .setContentDisposition("attachment");
        }

        Storage.Objects.Insert insertObject
                = client.objects().insert(settings.getBucket(), objectMetadata, mediaContent);

        if (!useCustomMetadata) {
            // If you don't provide metadata, you will have specify the object
            // name by parameter. You will probably also want to ensure that your
            // default object ACLs (a bucket property) are set appropriately:
            // https://developers.google.com/storage/docs/json_api/v1/buckets#defaultObjectAcl
            insertObject.setName(settings.getPrefix() + "01");
        }

        insertObject.getMediaHttpUploader()
                .setProgressListener(new CustomUploadProgressListener()).setDisableGZipContent(true);
        //For small files where content length is not specified, you may wish to call setDirectUploadEnabled(true), to
        //reduce the number of HTTP requests made to the server.
//        if (mediaContent.getLength() > 0 && mediaContent.getLength() <= 2 * 1000 * 1000 /* 2MB */) {
//            insertObject.getMediaHttpUploader().setDirectUploadEnabled(true);
//        }
        insertObject.execute();
    }

    private static class CustomUploadProgressListener implements MediaHttpUploaderProgressListener {

        private final Stopwatch stopwatch = new Stopwatch();

        public CustomUploadProgressListener() {
        }

        @Override
        public void progressChanged(MediaHttpUploader uploader) {
            switch (uploader.getUploadState()) {
                case INITIATION_STARTED:
                    stopwatch.start();
                    System.out.println("Initiation has started!");
                    break;
                case INITIATION_COMPLETE:
                    System.out.println("Initiation is complete!");
                    break;
                case MEDIA_IN_PROGRESS:
                    // TODO(nherring): Progress works iff you have a content length specified.
                    // System.out.println(uploader.getProgress());
                    System.out.println(uploader.getNumBytesUploaded());
                    break;
                case MEDIA_COMPLETE:
                    stopwatch.stop();
                    System.out.println(String.format("Upload is complete! (%s)", stopwatch));
                    break;
                case NOT_STARTED:
                    break;
            }
        }
    }

    private static class CustomDownloadProgressListener implements MediaHttpDownloaderProgressListener {

        private final Stopwatch stopwatch;

        public CustomDownloadProgressListener(final Stopwatch stopwatch) {
            this.stopwatch = stopwatch;
        }

        @Override
        public void progressChanged(MediaHttpDownloader downloader) {
            switch (downloader.getDownloadState()) {
                case MEDIA_IN_PROGRESS:
                    System.out.println(downloader.getProgress());
                    break;
                case MEDIA_COMPLETE:
                    stopwatch.stop();
                    System.out.println(String.format("Download is complete! (%s)", stopwatch));
                    break;
                case NOT_STARTED:
                    break;
            }
        }
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

    public static class ViewBucket extends TUtility.View<Bucket> {

        @Override
        public void show(Bucket bucket) {
            System.out.println("name: " + bucket.getName());
            System.out.println("location: " + bucket.getLocation());
            System.out.println("timeCreated: " + bucket.getTimeCreated());
            System.out.println("owner: " + bucket.getOwner());
            System.out.println("acl: " + bucket.getAcl());
        }
    }
}
