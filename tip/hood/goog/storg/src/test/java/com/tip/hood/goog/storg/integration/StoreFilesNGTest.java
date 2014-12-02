package com.tip.hood.goog.storg.integration;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.storage.Storage;
import com.tip.hood.itest.testutil.RandomContent;
import com.tip.hood.itest.testutil.TUtility;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 */
public class StoreFilesNGTest {

    private static String BUCKET_NAME;
    private static JsonFactory jsonFactory;
    private static Storage client;
    private static final Utils.ViewBucket view = new Utils.ViewBucket();

    public StoreFilesNGTest() {
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

    /**
     *
     * @param baseObjectSize size in MB of initial image
     * @param blockSize buffer block size to read
     * @param n number of snaps
     * @return the array of base plus snaps (n+1)
     * @throws IOException
     */
    RandomContent.DataBlockInputStream[] createImagesOnDisk(long baseObjectSize, int blockSize, int n)
            throws IOException {
        Random r = new Random();
        //initial img
        RandomContent.DataBlockInputStream b = new RandomContent.DataBlockInputStream(baseObjectSize, blockSize);
        //snaps
        RandomContent.DataBlockInputStream[] s = new RandomContent.DataBlockInputStream[n + 1];
        s[0] = b;
        for (int i = 1; i < n + 1; i++) {
            baseObjectSize = (long) (r.nextInt(4000) * Math.pow(2., 10.));
            s[i] = new RandomContent.DataBlockInputStream(baseObjectSize, blockSize);
        }
        return s;
    }

    /**
     *
     * @param baseObjectSize size in MB of initial image
     * @param blockSize buffer block size to read
     * @param n number of snaps
     * @return the array of base plus snaps (n+1)
     * @throws IOException
     */
    File[] createBinFilesOnDisk(long baseObjectSize, int blockSize, int n)
            throws IOException {
        RandomContent.DataBlockInputStream[] s = createImagesOnDisk(baseObjectSize, blockSize, n);
        File[] dumpStream = new File[s.length];
        for (int i = 0; i < dumpStream.length; i++) {
            dumpStream[i] = TUtility.dumpStream(s[i], "img" + i + ".bin");
        }
        return dumpStream;
    }

    @Test
    public void testStore() throws Exception {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("conf/sample_settings.json");
        SampleSettings settings = SampleSettings.load(jsonFactory, in);
        boolean useCustomMetadata = false;
        view.lineBreak("Uploading objects");

        //create a 10MB file
        long baseObjectSize = (long) (10 * Math.pow(2., 20.));//10 MB
        File[] s = createBinFilesOnDisk(baseObjectSize, 1024, 10);
        int i = 0;
        for (File file : s) {
            InputStreamContent mediaContent = new InputStreamContent("application/octet-stream", new FileInputStream(file));
            mediaContent.setLength(file.length());
            Utils.insert(client, useCustomMetadata, settings, mediaContent, "0"+(i++));
        }
    }
}
