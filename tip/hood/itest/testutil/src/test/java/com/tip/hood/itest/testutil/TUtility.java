package com.tip.hood.itest.testutil;

import com.google.common.io.ByteStreams;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Test utility methods.
 *
 * @author Massimo Calderoni
 */
public final class TUtility {

   private static final Logger logger = LoggerFactory.getLogger(TUtility.class);
   private TUtility() {
   }

   /**
    * Given a path, asserts its correctness and returns its canonical form. The
    * canonical form is returned also as a path (a string).
    *
    * @param path the string containing the path that can be a relative one.
    * @return
    * @throws IOException
    */
   public static String assertAndReturnCanonicalPath(String path) throws IOException {
      File file = new File(path);
      Assert.assertTrue(file.exists());
      return file.getCanonicalPath();
   }

   /**
    * Asserts existence of the given path.
    *
    * @param path the string containing the path that can be a relative one.
    */
   public static void assertFileExists(String path) {
      Assert.assertTrue(new File(path).exists());
   }

   public static void assertEquals(File actual, File expected) throws IOException {
      byte[] expectedBytes = loadFileIntoBytes(expected);
      byte[] actualBytes = loadFileIntoBytes(actual);
      Assert.assertEquals(actualBytes.length, expectedBytes.length);
      Assert.assertEquals(actualBytes, expectedBytes);
   }

   public static String loadFileIntoString(String path) throws IOException {
      String jsonString;
      try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
         jsonString = IOUtils.toString(in, "UTF-8");
      }
      return jsonString;
   }

   public static byte[] loadFileIntoBytes(String path) throws IOException {
      byte[] bytes;
      try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
         bytes = IOUtils.toByteArray(in);
      }
      return bytes;
   }

   public static byte[] loadFileIntoBytes(File absolutePath) throws IOException {
      byte[] bytes;
      try (InputStream in = new FileInputStream(absolutePath)) {
         bytes = IOUtils.toByteArray(in);
      }
      return bytes;
   }

   public static Properties loadProps(String path) throws IOException {
      try (InputStream in = Thread.currentThread().getContextClassLoader().
              getResourceAsStream(path)) {
         Properties props = new Properties();
         props.load(in);
         return props;
      }
   }

   /**
    * Outputs the given byte array into a named file under the 'target'
    * directory.
    *
    * @param bytes bytes to save into a file
    * @param filename string defining the file name (no path). The file will be
    * saved with taht name under the standard project build directory 'target'.
    * @throws IOException
    */
   public static void outputToFile(byte[] bytes, String filename) throws IOException {
      String target = assertAndReturnCanonicalPath("target");
      File outFile = new File(target, filename);
      try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile))) {
         bos.write(bytes);
         bos.flush();
      }
   }

   /**
    * Creates a file in the maven target directory and puts the given content
    * into it. If file exists, content is overridden.
    *
    * @param filename the file name. This file will end up into the target
    * directory.
    * @param content The string content you want the file to contain.
    * @return the absolute File object that was created.
    * @throws IOException
    */
   public static File createTFileWithContent(String filename, String content) throws IOException {
      String targetPath = TUtility.assertAndReturnCanonicalPath("target");
      File file = new File(targetPath, filename);
      boolean created = file.createNewFile();
      logger.debug("file={} was{} created",
         file.getName(),
         created ? "" : " not");

      try (BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream(file))) {
         try (BufferedInputStream bufIn = new BufferedInputStream(new ByteArrayInputStream(content.getBytes("UTF-8")))) {
            int bytes;
            while ((bytes = bufIn.read()) != -1) {
               bufOut.write(bytes);
            }
         }
      }
      return file;
   }

   /**
    * Saves the stream's content into a temp file with the given file name.
    *
    * @param stream the stream of which content to be dumped to a file
    * @param tempFilename the file name that will contain the stream's content.
    * This file will reside under the target maven directory for this module.
    * @return the file object of the created temp file
    * @throws IOException
    */
   public static File dumpStream(InputStream stream, String tempFilename) throws IOException {
      String targetPath = TUtility.assertAndReturnCanonicalPath("target");
      File file = new File(targetPath, tempFilename);
      boolean created = file.createNewFile();
      System.out.println("file " + file.getName() + " was" + (created ? "" : " not") + " created");
      try (BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream(file))) {
         ByteStreams.copy(stream, bufOut);
      }
      return file;
   }

   /**
    * Asserts if the content given by the provided stream is the same as the
    * content in the provided expected file.
    *
    * @param stream the stream of content to test. The content will be tested
    * against the content provided in the expected file. The expected file
    * location is given by providing its path with expectedFilename. Its path is
    * a relative to the test resources directory in this maven module.
    * @param actualFilename relative location of where the stream will be dumped
    * in. The location is relative to the 'target' directory for this maven
    * module.
    * @param expectedFilename The expected file location with the correct
    * content. Its path is a relative to the test resources directory in this
    * maven module.
    * @throws Exception
    */
   public static void assertStream(InputStream stream,
           String actualFilename, String expectedFilename) throws Exception {
      File actualFile = TUtility.dumpStream(stream, actualFilename);
      byte[] expectedBytes = TUtility.loadFileIntoBytes(expectedFilename);
      byte[] actualBytes = TUtility.loadFileIntoBytes(actualFile);
      Assert.assertEquals(actualBytes.length, expectedBytes.length);
      Assert.assertEquals(actualBytes, expectedBytes);

   }

   public static void decoratedPrint(String string) {
      System.out.println("*******************************************");
      System.out.println(string);
      System.out.println("*******************************************");
   }
}
