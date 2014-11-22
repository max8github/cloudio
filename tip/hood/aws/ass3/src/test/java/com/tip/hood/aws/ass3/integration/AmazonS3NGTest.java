/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tip.hood.aws.ass3.integration;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.tip.hood.itest.testutil.TUtility;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author mcalderoni
 */
public class AmazonS3NGTest {

   private AmazonS3 s3Client;

   public AmazonS3NGTest() {
   }

   @BeforeClass
   public void setUpClass() throws Exception {
      final Properties p = new Properties();
      try (InputStream in = Thread.currentThread().getContextClassLoader().
              getResourceAsStream("conf/aws.properties")) {
         p.load(in);
      }

      AWSCredentials credentials = new BasicAWSCredentials(
              p.getProperty("awsAccessKeyId"),
              p.getProperty("awsSecretKey"));

      ClientConfiguration clientConfiguration = new ClientConfiguration();
      clientConfiguration.setProxyHost(p.getProperty("proxyURL"));
      clientConfiguration.setProxyPort(3128);
      clientConfiguration.setProtocol(Protocol.HTTPS);
      s3Client = new AmazonS3Client(credentials, clientConfiguration);
      S3ClientOptions s3ClientOptions = new S3ClientOptions();
      s3ClientOptions.setPathStyleAccess(true);
      s3Client.setS3ClientOptions(s3ClientOptions);
      String endpoint = "https://s3.amazonaws.com";//"s3-us-west-2.amazonaws.com";
      s3Client.setEndpoint(endpoint);
   }

   @Test
   public void testCreateBucket() throws Exception {
      //Create bucket:
      String bucketname = "mybucket02mcalderoni";
      System.out.println("Attempt to create " + bucketname);
      Bucket bucket = s3Client.createBucket(bucketname);
      Assert.assertNotNull(bucket);
      System.out.println(bucket + " created!");
   }

   /**
    * Test of get method.
    */
   @Test
   public void testGetObject() throws IOException {
      System.out.println("testGetObject");
      GetObjectRequest g = new GetObjectRequest("calderoni", "aaa/image.jpg");
      S3Object object = s3Client.getObject(g);
      System.out.println("object = " + object.getKey());
      S3ObjectInputStream stream = object.getObjectContent();
      String targetPath = TUtility.assertAndReturnCanonicalPath("target");
      File file = new File(targetPath, "fromAmz.jpg");
      boolean created = file.createNewFile();
      System.out.println("file " + file.getName() + " was" + (created ? "" : " not") + " created");
      try (BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream(file))) {
         try (BufferedInputStream bufIn = new BufferedInputStream(stream)) {
            int bytes;
            while ((bytes = bufIn.read()) != -1) {
               bufOut.write(bytes);
            }
         }
      }

      String path = "images/image.jpg";
      byte[] expectedImageBytes = TUtility.loadFileIntoBytes(path);
      byte[] actualBytes = TUtility.loadFileIntoBytes(file);
      Assert.assertEquals(actualBytes.length, expectedImageBytes.length);
      Assert.assertEquals(actualBytes, expectedImageBytes);

      // Send sample request (list objects in a given bucket).
      ObjectListing objectListing = s3Client.listObjects(
              new ListObjectsRequest().withBucketName("calderoni"));
      List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
      System.out.println("objectListing = " + objectListing.getBucketName());
      System.out.println("objectSummaries = " + objectSummaries);
   }

   @Test
   public void testPutObject() throws IOException {
      System.out.println("AWS Client doing a put");
      String bucket = "mybucket01mcalderoni";
      String targetPath = TUtility.assertAndReturnCanonicalPath("target/test-classes/images/image.jpg");
      PutObjectRequest g = new PutObjectRequest(bucket, "aaa/image04.jpg", new File(targetPath));
      PutObjectResult object = s3Client.putObject(g);
      System.out.println("put object = " + object.getServerSideEncryption());
   }

   @Test
   public void testDeleteObject() throws IOException {
      System.out.println("AWS Client doing a delete");
      String bucket = "mybucket01mcalderoni";
      DeleteObjectRequest g = new DeleteObjectRequest(bucket, "aaa/image03.jpg");
      s3Client.deleteObject(g);
   }

   @Test
   public void testListVersions() throws IOException {
      System.out.println("\ntestListVersions");
      String bucketname = "calderoni";
      ListVersionsRequest l = new ListVersionsRequest(bucketname, null, null, null, null, 100);
      VersionListing versionListing = s3Client.listVersions(l);
      List<S3VersionSummary> versionSummaries = versionListing.getVersionSummaries();
      for (S3VersionSummary s : versionSummaries) {
         System.out.println("s3VersionSummary = " + s.getBucketName()
                 + ", " + s.getKey() + ", " + s.getVersionId());
      }
   }

   @Test
   public void testGetBucketVersioningConfiguration() throws IOException {
      System.out.println("\ntestGetBucketVersioningConfiguration");
      String bucketname = "calderoni";
      BucketVersioningConfiguration bc = s3Client.
              getBucketVersioningConfiguration(bucketname);
      System.out.println("bc = " + bc.getStatus() + ", " + bc.isMfaDeleteEnabled());
   }

   @Test
   public void testDeleteAllObjects() throws IOException {
      System.out.println("\ntestDeleteAllObjects");
      String bucket = "mybucket01mcalderoni";
      DeleteObjectsRequest g = new DeleteObjectsRequest(bucket);
      s3Client.deleteObjects(g);
   }

   @Test
   public void testDeleteBucket() throws IOException {
      System.out.println("\ntestDeleteBucket");
      String bucket = "mybucket01mcalderoni";
      s3Client.deleteBucket(bucket);
   }

   @Test
   public void testListObjects() throws IOException {
      // Send sample request (list objects in a given bucket).
      ObjectListing objectListing = s3Client.listObjects(
              new ListObjectsRequest().withBucketName("calderoni"));
      List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
      System.out.println("objectListing = " + objectListing.getBucketName());
      System.out.println("objectSummaries = " + objectSummaries);
   }

   @Test
   public void testListBuckets() throws Exception {
      System.out.println("Listing buckets");
      List<Bucket> listBuckets = s3Client.listBuckets();
      for (Bucket bucket : listBuckets) {
         System.out.println("bucket = " + bucket.getName());
      }
   }

   @Test
   public void testDoesBucketExist() throws IOException {
      System.out.println("\ntestDoesBucketExist");
      boolean doesBucketExist = s3Client.doesBucketExist("calderoni");
      System.out.println("doesBucketExist = " + doesBucketExist);
   }
}
