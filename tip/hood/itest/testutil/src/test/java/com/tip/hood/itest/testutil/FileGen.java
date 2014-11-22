package com.tip.hood.itest.testutil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 *
 *
 */
public class FileGen {

   private static final Logger logger = LoggerFactory.getLogger(FileGen.class);
   private final List<String> fileList = new ArrayList<>();
   private File dir;
   private String path;
   private Random rand;
   private final int BLOCK_SIZE = 128;

   public FileGen() {
      this("target/testdir");
   }

   public FileGen(String targetPath) {
      //Create Directory to hold all the created files
      path = targetPath;
      dir = new File(path);
      if (dir.exists()) {
         logger.info("{} already exists, no need to create", path);
      } else if (!dir.mkdir()) {
         Assert.fail("Failed to create dir :" + path);
      }
      rand = new Random(System.currentTimeMillis());
   }

   public void cleanUp() {
      if (dir.exists()) {
         for (String file : fileList) {
            File f = new File(path, file);
            boolean deleted = f.delete();
            if (!deleted) {
               logger.warn("could not delete file {}", file);
            }
         }
         if (dir.list().length == 0) {
            dir.delete();
            logger.info("{} deleted.", path);
         } else {
            logger.info("{} not empty.", path);
         }
      } else {
         logger.info("{} already deleted, nothing to be done", path);
      }
   }

   /**
    *
    * @param filesize Size of the file
    * @param blocksize Block size to use to write the file
    * @return
    */
   public File getNewFile(int filesize, int blocksize) throws IOException {
      return getNewFile(filesize, blocksize, -1);
   }

   /**
    *
    * @param filesize Size of the file
    * @param blocksize Block size to use to write the file
    * @param byte contains either -1 or the byte to use for this file
    * @return
    */
   public File getNewFile(int filesize, int blocksize, int content) throws IOException {
      if (filesize < blocksize) {
         Assert.fail("Block size cannot be greater than file size");
      }
      String filename = "testfile-" + Math.abs(rand.nextLong());
      File f = new File(path + "/" + filename);
      if (!f.createNewFile()) {
         cleanUp();
         Assert.fail("Unable to create file in " + path);
      }
      fileList.add(filename);
      try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f))) {

         int remaining = filesize;
         while (remaining > 0) {
            byte[] b;
            if (remaining > blocksize) {
               b = new byte[blocksize];
            } else {
               b = new byte[remaining];
            }
            //Fill up the array
            if (content == -1) {
               rand.nextBytes(b);
            } else {
               Arrays.fill(b, (byte) content);
            }
            bos.write(b);
            remaining -= b.length;
         }
      }
      return f;
   }

   public File getNewEmptyFile() throws IOException {
      String filename = "testfile-" + Math.abs(rand.nextLong());
      File f = new File(path + "/" + filename);
      if (!f.createNewFile()) {
         cleanUp();
         Assert.fail("Unable to create file in " + path);
      }
      fileList.add(filename);
      return f;
   }

   public File streamToFile(InputStream in) throws IOException {
      byte[] b = new byte[BLOCK_SIZE];
      String filename = "testfile-" + Math.abs(rand.nextLong());
      File f = new File(path + "/" + filename);
      if (!f.createNewFile()) {
         cleanUp();
         Assert.fail("Unable to create file in " + path);
      }
      fileList.add(filename);
      try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f))) {
         do {
            int length = in.read(b, 0, BLOCK_SIZE);
            if (length == -1) {
               bos.flush();
               break;
            }
            bos.write(b, 0, length);
         } while (true);
      }
      return f;
   }

   public void streamToFile(File f, InputStream in) throws IOException {
      byte[] b = new byte[BLOCK_SIZE];
      try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f))) {
         do {
            int length = in.read(b, 0, BLOCK_SIZE);
            if (length == -1) {
               bos.flush();
               break;
            }
            bos.write(b, 0, length);
         } while (true);
      }
   }

   public boolean compareMultiPartFiles(File actual, File[] parts, int blockSize)
   throws IOException {
      // Check one file at the time
      long offset = 0;
      for (int partsIndex = 0; partsIndex < parts.length; partsIndex++) {
         if (compareFiles(actual, offset, parts[partsIndex], blockSize) == false) {
            return false;
         }
         offset += parts[partsIndex].length();
      }
      return true;
   }

   public boolean compareFiles(File actual, File expected, int blocksize) throws IOException {
      return compareFiles(actual, -1, expected, blocksize);
   }

   public boolean compareFiles(File actual, long skipActual,
                               File expected, int blocksize) throws IOException {
      byte[] actualBlock = new byte[blocksize];
      byte[] expectedBlock = new byte[blocksize];

      try (InputStream acBuffer = new FileInputStream(actual)) {
         if (skipActual != -1) {
            acBuffer.skip(skipActual);
         }
         try (InputStream exBuffer = new FileInputStream(expected)) {

            do {
               int acbytesread = acBuffer.read(actualBlock, 0, blocksize);
               int exbytesread = exBuffer.read(expectedBlock, 0, blocksize);

               if (acbytesread == -1 && exbytesread == -1) {
                  return true;
               }

               if (skipActual != -1 && exbytesread == -1) {
                  return true;
               }

               if (acbytesread != exbytesread) {
                  return false;
               }

               //compare data
               if (!Arrays.equals(actualBlock, expectedBlock)) {
                  return false;
               }
            } while (true);
         }
      }
   }
}