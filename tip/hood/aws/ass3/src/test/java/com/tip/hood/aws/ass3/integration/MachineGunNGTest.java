package com.tip.hood.aws.ass3.integration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.tip.hood.itest.testutil.TUtility;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Heavy-duty puts and gets stress test client code. This class has only client code: you need to start a server first
 * in order to run it.
 *
 * @author max
 */
public class MachineGunNGTest {

   private AmazonS3 s3Client;
   private final Random random = new Random(System.currentTimeMillis());
   static final String FIXED_BUCKET_NAME = System.getProperty("user.name") + "20140212";
   private String objectname;
   private static final String dataserverHost = "127.0.0.1";
   private int dataserverPort;

   @BeforeClass
   public void setupClass() throws Exception {
      AWSCredentials credentials;
      ClientConfiguration clientConfiguration = new ClientConfiguration();
      try (InputStream in = Thread.currentThread().getContextClassLoader().
              getResourceAsStream("conf/config.properties")) {
         Properties props = new Properties();
         props.load(in);
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

         String awsAccessKeyId = props.getProperty("awsAccessKeyId");
         String awsSecretKey = props.getProperty("awsSecretKey");

         credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKey);
      }

      s3Client = new AmazonS3Client(credentials, clientConfiguration);
      S3ClientOptions s3ClientOptions = new S3ClientOptions();
      s3ClientOptions.setPathStyleAccess(true);
      s3Client.setS3ClientOptions(s3ClientOptions);
      String endpoint = "http://" + dataserverHost + ":" + dataserverPort;
      s3Client.setEndpoint(endpoint);
      System.out.println("endpoint = " + endpoint);

      //For overriding regions file
      String regions = TUtility.assertAndReturnCanonicalPath("target/test-classes/etc/regions.xml");
      System.setProperty("com.amazonaws.regions.RegionUtils.fileOverride", regions);
   }

   @AfterClass
   public void tearDownClass() throws Exception {
   }

   @BeforeMethod
   public void setUpMethod() throws Exception {
   }

   @AfterMethod
   public void tearDownMethod() throws Exception {
   }

   @Test
   public void testCreateBucket() throws Exception {
      Bucket b = s3Client.createBucket(FIXED_BUCKET_NAME);
      Assert.assertEquals(FIXED_BUCKET_NAME, b.getName());
      System.out.println("Bucket " + b.getName() + " created!");
   }

   @Test
   public void testPutObject() throws IOException {
      System.out.println("\nClient doing a put");
      objectname = random.nextLong() + "/image.jpg";
      System.out.println("objectname = " + objectname);
      File f = new File(TUtility.assertAndReturnCanonicalPath(
              "target/test-classes/images/image.jpg"));
      PutObjectRequest g = new PutObjectRequest(FIXED_BUCKET_NAME, objectname, f);
      PutObjectResult object = s3Client.putObject(g);
      String contentMd5 = object.getContentMd5();
      System.out.println("contentMd5 = " + contentMd5);
   }

   @Test
   public void testGetObject() throws Exception {
      System.out.println("\ntestGetObject");
      String bucket = FIXED_BUCKET_NAME;
      String oKey = "-101147348544572787/image.jpg";
      String expected = "images/image.jpg";
      String actual = "fromAmz.jpg";

      GetObjectRequest g = new GetObjectRequest(bucket, oKey);
      S3Object object = s3Client.getObject(g);
      Assert.assertNotNull(object);
      System.out.println("object = " + object.getKey());

      InputStream stream = object.getObjectContent();
      TUtility.assertStream(stream, actual, expected);
   }

   @Test
   public void testListObjects() throws Exception {
      System.out.println("\ntestListObjects");
      // Send sample request (list objects in a given bucket).
      ObjectListing objectListing = s3Client.listObjects(
              new ListObjectsRequest().withBucketName(FIXED_BUCKET_NAME));
      List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
      StringBuilder bu = new StringBuilder("\nobject listing of bucket ").
              append(objectListing.getBucketName()).append("\n");
      for (S3ObjectSummary osum : objectSummaries) {
         bu.append("object = ").append(osum.getBucketName()).append(" / ").
                 append(osum.getKey()).append("\n");
      }
      bu.append("\n");
      System.out.println(bu);
      TUtility.outputToFile(bu.toString().getBytes(), "latestObjList.txt");
   }

   @Test
   public void testMachineGunPut() throws Exception {
      System.out.println("\n testPutMachineGun");
      File f = new File(TUtility.assertAndReturnCanonicalPath(
              "target/test-classes/images/image.jpg"));
      String onameRoot = "mgun" + random.nextLong();
      int numReqs = 100;
      MachineGunPutter mput = new MachineGunPutter(s3Client, numReqs, onameRoot, f, 3);
      mput.machineGunPut();
   }

   @Test
   public void testMachineGunPutGet() throws Exception {
      System.out.println("\n testPutMachineGun");
      File f = new File(TUtility.assertAndReturnCanonicalPath(
              "target/test-classes/images/image.jpg"));
      String onameRoot = "mgun" + random.nextLong();
      int numReqs = 100;
      MachineGunPutter mput = new MachineGunPutter(s3Client, numReqs, onameRoot, f, 10);
      MachineGunGetter mget = new MachineGunGetter(s3Client, numReqs, onameRoot);
      for (int i = 0; i < 100; i++) {
         mput.machineGunPut();
         Thread.sleep(1000);
         mget.machineGunGet();
         Thread.sleep(1000);
      }
   }
}
