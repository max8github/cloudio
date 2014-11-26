package com.tip.hood.aws.ass3.integration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.tip.hood.itest.testutil.TUtility;

public class LargeFileTest {
   private static final long TOTAL_SIZE = 10L * 1024 * 1024 * 1024;

   private File signature;
   MessageDigest digest;
   private final int bufSize = 64*1024*1024;

   private AmazonS3 s3Client;
   private Random random;
   private String FIXED_BUCKET_NAME = System.getProperty("user.name");
   private String objectname;
   private String dataserverHost;
   private int dataserverPort;
   private HttpClient client;

   public LargeFileTest() {
   }


   class MyInputStream extends InputStream {
      byte[] buf;
      long _size;
      File sig;
      BufferedOutputStream sigOut;
      long count;
      int current;
      Random random;

      public MyInputStream(long size, File signature) throws FileNotFoundException, NoSuchAlgorithmException {
         buf = new byte[bufSize];
         this._size = size;
         this.sig = signature;
         sigOut = new BufferedOutputStream(new FileOutputStream(sig));

         count = 0;

         random = new Random(System.currentTimeMillis());
         random.nextBytes(buf);
         current = 0;
      }

      @Override
      public int read() throws IOException {
         if (count == _size) {
            return -1;
         }
         int b = buf[current++] & 0xff;
         if (current == bufSize) {
            digest.reset();
            sigOut.write(digest.digest(buf));
            random.nextBytes(buf);
            current = 0;
            TUtility.View.decoratedPrint("Uploading in progress: " + (float)count * 100 / _size + "%. " + count + " " + _size);
         }

         count++;
         if (count == _size) {
            sigOut.flush();
         }

         return b;
      }

   }

   @BeforeClass
   public void setUpClass() throws Exception {
      //grab endpoint details to create data server client
      AWSCredentials credentials;
      try (InputStream in = Thread.currentThread().getContextClassLoader().
              getResourceAsStream("conf/config.properties")) {
         Properties props = new Properties();
         props.load(in);
         dataserverHost = props.getProperty("dataserverHost");
         dataserverPort = Integer.parseInt(props.getProperty("dataserverPort"));

         credentials = new BasicAWSCredentials(
                 props.getProperty("awsAccessKeyId"),
                 props.getProperty("awsSecretKey"));
      }

      /*vblob = new VblobServerStub(EnumSet.of(VblobServerStub.Servers.START_DS,
              VblobServerStub.Servers.START_MDSERVER));
      vblob.start();*/

      s3Client = new AmazonS3Client(credentials);
      String endpoint = "http://" + dataserverHost + ":" + dataserverPort;
      s3Client.setEndpoint(endpoint);

      //For overriding regions file
      String regions = TUtility.assertAndReturnCanonicalPath("src/test/resources/etc/regions.xml");
      System.setProperty("com.amazonaws.regions.RegionUtils.fileOverride", regions);

      //for flush use
      //Create http client
      client = new HttpClient();
      client.setFollowRedirects(false);
      // max number of connections per-destination
      client.setMaxConnectionsPerDestination(32768);
      // idle timeout
      //client.setIdleTimeout(TimeUnit.SECONDS.toMillis(30));
      client.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
      client.setStopTimeout(TimeUnit.MINUTES.toMillis(10));
      // max threads = 16
      QueuedThreadPool executor = new QueuedThreadPool(16);
      executor.setName("integration-test");
      client.setExecutor(executor);
      client.start();
      client.getContentDecoderFactories().clear();

      setUpCreateBucket();

      signature = new File("/tmp/LargeFileTest-" + FIXED_BUCKET_NAME);
      digest = MessageDigest.getInstance("md5");
   }

   private void setUpCreateBucket() throws Exception {
      //Create bucket:
      random = new Random(System.currentTimeMillis());
      FIXED_BUCKET_NAME += Math.abs(random.nextLong());
      TUtility.View.decoratedPrint("\nvBlob to create bucket" + FIXED_BUCKET_NAME);
      Bucket bucket = s3Client.createBucket(FIXED_BUCKET_NAME);
      Assert.assertNotNull(bucket);
      TUtility.View.decoratedPrint(bucket + " created!");
   }

   @Test
   public void testPutObject() throws Exception {
      TUtility.View.decoratedPrint("vBlob Client doing a put");
      objectname = random.nextLong() + ".bin";
      TUtility.View.decoratedPrint("objectname = " + objectname);
      ObjectMetadata objMd = new ObjectMetadata();
      objMd.setContentLength(TOTAL_SIZE);

      InputStream in = new MyInputStream(TOTAL_SIZE, signature);
      PutObjectRequest g = new PutObjectRequest(FIXED_BUCKET_NAME, objectname, in, objMd);
      PutObjectResult object = s3Client.putObject(g);
      if (object == null) {
         TUtility.View.decoratedPrint("object is null!");
      }
      else {
         TUtility.View.decoratedPrint(object.toString());
      }
      Assert.assertNotNull(object);
   }

   /**
    * Test of get method.
    */
   @Test(dependsOnMethods = {"testPutObject"})
   public void testGetObject() throws IOException {
      TUtility.View.decoratedPrint("\ntestGetObject");
      GetObjectRequest g = new GetObjectRequest(FIXED_BUCKET_NAME, objectname);
      S3Object object = s3Client.getObject(g);
      Assert.assertNotNull(object);
      TUtility.View.decoratedPrint("object = " + object.getKey());
      S3ObjectInputStream stream = object.getObjectContent();

      try (BufferedInputStream sigIn = new BufferedInputStream(new FileInputStream(signature))) {
         try (BufferedInputStream bufIn = new BufferedInputStream(stream)) {
            byte buf[] = new byte[bufSize];
            int offset = 0, len = bufSize;
            byte sig[] = new byte[16];
            int ret;
            int count = 0;

            while ((ret = bufIn.read(buf, offset, len)) != -1) {
               if (ret < len) {
                  offset += ret;
                  len -= ret;
                  continue;
               }

               ret = sigIn.read(sig);
               Assert.assertEquals(ret, 16);
               digest.reset();
               Assert.assertEquals(digest.digest(buf), sig);
               TUtility.View.decoratedPrint("Downloading in progress: " + (float) (++count) * 100 * bufSize / TOTAL_SIZE + "%.");

               offset = 0;
               len = bufSize;
            }
            ret = sigIn.read(sig);
            Assert.assertEquals(ret, -1);
         }
      }
   }

   @Test(dependsOnMethods = {"testGetObject"})
   public void deleteObject() throws IOException {
      s3Client.deleteObject(FIXED_BUCKET_NAME, objectname);
   }

   @AfterClass
   public void shutdownClass() throws Exception {
      //vblob.stop();
   }
}
