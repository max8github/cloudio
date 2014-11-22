package com.tip.hood.aws.ass3.integration;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import static com.tip.hood.aws.ass3.integration.MachineGunNGTest.FIXED_BUCKET_NAME;
import com.tip.hood.itest.testutil.TUtility;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author max
 */
public class MachineGunGetter {

   private final AmazonS3 s3Client;
   private final int numReqs;
   private final String onameRoot;
   private final int sleepInBetween = 10;

   public MachineGunGetter(AmazonS3 s3Client, int numReqs, String onameRoot) {
      this.s3Client = s3Client;
      this.numReqs = numReqs;
      this.onameRoot = onameRoot;
   }

   /**
    * Sends {@link #numReqs} get requests. Between one request and another, sleeps {@link #sleepInBetween} ms.
    *
    * @throws InterruptedException
    * @throws AmazonClientException
    * @throws IOException
    */
   public void machineGunGet() throws
           InterruptedException, AmazonClientException, IOException {
      String oname;
      for (int i = 0; i < numReqs; i++) {
         oname = onameRoot + "-image" + i + ".jpg";
         GetObjectRequest g = new GetObjectRequest(FIXED_BUCKET_NAME, oname);
         S3Object object = s3Client.getObject(g);
         InputStream stream = object.getObjectContent();
         TUtility.dumpStream(stream, oname);
         Thread.sleep(sleepInBetween);
      }
   }
}
