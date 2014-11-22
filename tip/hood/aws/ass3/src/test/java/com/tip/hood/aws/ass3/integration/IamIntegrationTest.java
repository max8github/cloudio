package com.tip.hood.aws.ass3.integration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import com.tip.hood.itest.testutil.TUtility;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class IamIntegrationTest {

   private AmazonS3 tenantAdmin1Client;
   private AmazonS3 user1Client;
   private HttpClient client;
   private URL dsURL;
   private final Random random = new Random(System.currentTimeMillis());
   private String bucketName;
   private String tmpBucketName;
   private String objectName;
   private String tmpObjectName;
   private File expectedFile;
   private File tmpExpectedFile;

   @BeforeClass
   public void setupClass() throws Exception {

      System.out.println("Servers started!!!");

      //s3 client
      String dataserverHost;
      int dataserverPort;
      ClientConfiguration clientConfiguration = new ClientConfiguration();
      try (InputStream in = Thread.currentThread().getContextClassLoader().
              getResourceAsStream("conf/config.properties")) {
         Properties props = new Properties();
         props.load(in);

         dataserverHost = props.getProperty("dataserverHost");
         dataserverPort = Integer.parseInt(props.getProperty("dataserverPort"));

         String proxyHost = props.getProperty("proxyHost", null);
         if (proxyHost.equals("${proxyHost}")) {
            proxyHost = null;
         }

         String proxyPortString = props.getProperty("proxyPort", "-1");
         int proxyPort = -1;
         if (!proxyPortString.equals("${proxyPort}")) {
            proxyPort = Integer.parseInt(proxyPortString);
         }

         String protString = props.getProperty("protocol", Protocol.HTTPS.name());
         Protocol protocol = protString.equals(Protocol.HTTPS.name())
                 ? Protocol.HTTPS
                 : Protocol.HTTP;

         clientConfiguration.setProxyHost(proxyHost);
         clientConfiguration.setProxyPort(proxyPort);
         clientConfiguration.setProtocol(protocol);
      }

      tenantAdmin1Client = new AmazonS3Client(new BasicAWSCredentials("4c5186cd-190a-4f90-892f-bcd45f0d2535", "a6827823b29899b929a8ba5f1ec8aad0"), clientConfiguration);
      user1Client = new AmazonS3Client(new BasicAWSCredentials("7b3cb848-2a6f-4cc9-925b-818a0ad95668", "84e74a6f5b57a4a6206a6c957541702a"), clientConfiguration);

      S3ClientOptions ClientOptions = new S3ClientOptions();
      ClientOptions.setPathStyleAccess(true);
      tenantAdmin1Client.setS3ClientOptions(ClientOptions);
      user1Client.setS3ClientOptions(ClientOptions);

      String endpoint = "http://" + dataserverHost + ":" + dataserverPort;
      tenantAdmin1Client.setEndpoint(endpoint);
      user1Client.setEndpoint(endpoint);
      System.out.println("endpoint = " + endpoint);

      //For overriding regions file
      String regions = TUtility.assertAndReturnCanonicalPath("target/test-classes/etc/regions.xml");
      System.setProperty("com.amazonaws.regions.RegionUtils.fileOverride", regions);

      dsURL = new URL("http", dataserverHost, dataserverPort, "");

      //Create http client
      client = new HttpClient();
      client.setFollowRedirects(false);
      client.setMaxConnectionsPerDestination(32768);
      client.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
      client.setStopTimeout(TimeUnit.MINUTES.toMillis(10));

      QueuedThreadPool executor = new QueuedThreadPool(16);
      executor.setName("integration-test");
      client.setExecutor(executor);
      client.start();
      client.getContentDecoderFactories().clear();

      bucketName = "bucket" + random.nextLong();
      tmpBucketName = "bucket" + random.nextLong();
   }

   @AfterClass
   public void tearDownClass() throws Exception {
   }

   @Test
   public void testCreateBucket() throws Exception {
      System.out.println("\n****** testCreateBucket ******");
      Bucket b = tenantAdmin1Client.createBucket(bucketName);
      Assert.assertEquals(bucketName, b.getName());
      System.out.println("Bucket " + b.getName() + " created!");
   }

   @Test(dependsOnMethods = {"testCreateBucket"})
   public void testPutObject() throws Exception {
      System.out.println("\n****** testPutObject ******");
      objectName = random.nextLong() + "/image.jpg";
      expectedFile = new File(TUtility.assertAndReturnCanonicalPath(
            "target/test-classes/images/image.jpg"));
      PutObjectRequest putRequest = new PutObjectRequest(bucketName, objectName, expectedFile);
      PutObjectResult result = tenantAdmin1Client.putObject(putRequest);

      System.out.println("Object " + bucketName + "/" + objectName + " saved, with Etag = " + result.getETag());
   }

   @Test(dependsOnMethods = {"testPutObject"}, expectedExceptions = {AmazonS3Exception.class},
         expectedExceptionsMessageRegExp = ".*Access denied.*")
   public void testIamUserPolicy() throws Exception {
      System.out.println("\n****** testIamUserPolicy ******");
      GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, objectName);
      S3Object s3Object = user1Client.getObject(getObjectRequest);
      Assert.assertEquals(bucketName, s3Object.getBucketName());
      System.out.println("Object " + s3Object.getObjectContent() + " got");
   }

   @Test(dependsOnMethods = {"testIamUserPolicy"})
   public void testDeleteObject() throws IOException {
      System.out.println("\n****** testDeleteObject ******");
      DeleteObjectRequest g = new DeleteObjectRequest(bucketName, objectName);
      tenantAdmin1Client.deleteObject(g);
      System.out.println("object deleted");
   }

   @Test(dependsOnMethods = {"testDeleteObject"})
   public void testDeleteBucket() throws Exception {
      System.out.println("\n****** testDeleteBucket ******");
      tenantAdmin1Client.deleteBucket(bucketName);
      System.out.println("Bucket " + bucketName + " has been deleted!");
   }

   private static void sleep(int seconds) {
      try{
         Thread.sleep(1000 * seconds);
      }  catch (Exception ex) {

      }
   }
}
