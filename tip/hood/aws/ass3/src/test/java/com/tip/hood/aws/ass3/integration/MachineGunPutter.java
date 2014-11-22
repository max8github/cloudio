package com.tip.hood.aws.ass3.integration;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import static com.tip.hood.aws.ass3.integration.MachineGunNGTest.FIXED_BUCKET_NAME;
import java.io.File;

/**
 *
 * @author max
 */
public class MachineGunPutter {

   /**
    * S3 client to use for sending requests.
    */
   private final AmazonS3 s3Client;
   /**
    * Number of requests to issue.
    */
   private final int numReqs;
   /**
    * The object root name. Names will have a suffix increment off of this name.
    */
   private final String onameRoot;
   /**
    * The file to use in this multiple put requests (always puts the same file).
    */
   private final File f;
   /**
    * The milliseconds to sleep between one request and another.
    */
   private final int sleepInBetween;

   public MachineGunPutter(AmazonS3 s3Client, int numReqs, String onameRoot, File f, int sleepInBetween) {
      this.s3Client = s3Client;
      this.numReqs = numReqs;
      this.onameRoot = onameRoot;
      this.f = f;
      this.sleepInBetween = sleepInBetween;
   }


   /**
    * Sends {@link #numReqs} put requests.
    *
    * @throws InterruptedException
    * @throws AmazonClientException
    */
   public void machineGunPut() throws InterruptedException, AmazonClientException {
      String oname;
      for (int i = 0; i < numReqs; i++) {
         oname = onameRoot + "-image" + i + ".jpg";
         PutObjectRequest g = new PutObjectRequest(FIXED_BUCKET_NAME, oname, f);
         s3Client.putObject(g);
         Thread.sleep(sleepInBetween);
      }
   }
}
