package com.tip.hood.itest.testutil;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author max
 */
public final class TFileUtil {

   public static final String FIXED_FILENAME = "randomfile";

   private TFileUtil() {
   }

   /**
    * Generates n temp files with random content. Please note that the root of
    * the file name (its initial part of the name) is always the same
    * ({@link #FIXED_FILENAME}).
    *
    * @param n number of text files to create along with there MD5. All file
    * names will start with the same name with an increasing number appended to
    * them. If you give 3, it will create files with a name given by the string:
    * <code>String name = FIXED_FILENAME + i + ".txt";</code> with i from 0 to
    * 2.
    * @return an object array with two elements: the first element is an array
    * of Files, the second is an array of MD5 strings of the files. Both array
    * have size n. The files are saved in the target directory of the maven
    * project and have random content and random names.
    * @throws IOException
    * @throws NoSuchAlgorithmException
    */
   public static Object[] createFilesAndDigests(int n) throws IOException, NoSuchAlgorithmException {
      Object[] filesAndDigests = new Object[2];
      File[] files = new File[n];
      String[] md5s = new String[n];
      for (int i = 0; i < files.length; i++) {
         String poem = RandomContent.createPoem(100);
         byte[] digest = MessageDigest.getInstance("MD5").digest(poem.getBytes());
         md5s[i] = Base64.encodeBase64String(digest);
         files[i] = TUtility.createTFileWithContent(FIXED_FILENAME + i + ".txt", poem);
      }
      filesAndDigests[0] = files;
      filesAndDigests[1] = md5s;
      return filesAndDigests;
   }

}
