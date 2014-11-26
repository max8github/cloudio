package com.tip.hood.itest.testutil;

import java.io.InputStream;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Utility class for generating content.
 *
 * @author max
 */
public class RandomContent {

    private static Random random = new Random(System.currentTimeMillis());

    /**
     * Returns a string worth numMB Megabytes of random ASCII content.
     *
     * @param numMB size of content in MB to be generated randomly
     * @return
     */
    public static String createText(int numMB) {
        if (numMB > 1000) {
            throw new IllegalArgumentException("You are creating too much content"
                    + " (over 1GB!)");
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < numMB * 10000; i++) {
            buf.append(RandomStringUtils.random(100, 0, 0, true, false, null,
                    random)).append("\n");
        }
        return buf.toString();
    }

    /**
     * Creates a poem with the given number of lines.
     *
     * @param numVerses number of verses (lines) to create in the random poem. One hundred verses will be about 5KB.
     * @return
     */
    private static String createPoetryVerses(int numVerses) {
        if (numVerses > 100000) {
            throw new IllegalArgumentException("You are creating too much content"
                    + " (over 1GB!)");
        }
        StringBuilder buf = new StringBuilder();
        Poem poem = new Poem();
        for (int i = 0; i < numVerses; i++) {
            buf.append(poem.makePhrase()).append("\n");
        }
        return buf.toString();
    }

    /**
     * Creates a String containing a random poem. The string will be approximately <code>numKB</code> Kilobytes large.
     *
     * @param numKB approximate size in KB of the generated poem.
     * @return
     */
    public static String createPoem(int numKB) {
        int v = 20 * numKB;
        return createPoetryVerses(v);
    }

    /**
     * Copied/adapted from Stackoverflow.
     */
    public static class Poem {

        private static final Random rd = new Random(System.currentTimeMillis());
        private static final String[] n = {"you", "I", "girl", "boy", "man",
            "swan", "woman", "Hobbit"};
        private static final String[] v = {"run", "jump", "dive", "sink", "fall",
            "collapse", "swim", "love", "fly"};
        private static final String[] ot = {"like a", "into a", "nothing like a"};
        private static final String[] r1 = {"ball", "call", "mall", "hall",
            "guy named Paul"};
        private static final String[] r2 = {"cat", "hat", "bat", "rat", "mat", "guy named Pat"};

        public String makePhrase() {
            StringBuilder b = new StringBuilder();
            boolean boo = rd.nextBoolean();
            String[] rhyme = boo ? r1 : r2;
            String phrase = b.append(n[rd.nextInt(n.length)]).append(" ").
                    append(v[rd.nextInt(v.length)]).
                    append(" ").append(ot[rd.nextInt(ot.length)]).append(" ").
                    append(rhyme[rd.nextInt(rhyme.length)]).
                    append(", ").
                    append(n[rd.nextInt(n.length)]).append(" ").
                    append(v[rd.nextInt(v.length)]).
                    append(" ").append(ot[rd.nextInt(ot.length)]).append(" ").
                    append(rhyme[rd.nextInt(rhyme.length)]).
                    toString();
            return phrase;

        }
    }

//   public static void createRamdomBinary(int nbDesiredBytes) {
//      if (nbDesiredBytes < 0) {
//         throw new IllegalArgumentException("desired number of butes must be positive");
//      }
//      int bufferSize = 1024;
//      byte[] out = new byte[nbDesiredBytes];
//      Random r = new Random();
//
//      int n = 0;
//      while (n < nbDesiredBytes) {
//         int nToWrite = Math.min(nbDesiredBytes - n, bufferSize);
//         byte[] bytes = new byte[nToWrite];
//         r.nextBytes(bytes);
//
//         n += nToWrite;
//      }
//
//   }
    /**
     * Generates a random data block and repeats it to provide the stream.
     *
     * Using a buffer instead of just filling from java.util.Random because the latter causes noticeable lag in stream
     * reading, which detracts from upload speed. This class takes all that cost in the constructor.
     *
     * Copied this code from com.google.api.services.samples.storage.cmdline.StorageSample, under Apache 2 license.
     */
    public static class RandomDataBlockInputStream extends InputStream {

        private long byteCountRemaining;
        private final byte[] buffer;

        public RandomDataBlockInputStream(long size, int blockSize) {
            byteCountRemaining = size;
            final Random random = new Random();
            buffer = new byte[blockSize];
            random.nextBytes(buffer);
        }

        /*
         * (non-Javadoc)
         *
         * @see java.io.InputStream#read()
         */
        @Override
        public int read() {
            throw new AssertionError("Not implemented; too slow.");
        }

        /*
         * (non-Javadoc)
         *
         * @see java.io.InputStream#read(byte [], int, int)
         */
        @Override
        public int read(byte b[], int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            } else if (byteCountRemaining == 0) {
                return -1;
            }
            int actualLen = len > byteCountRemaining ? (int) byteCountRemaining : len;
            for (int i = off; i < actualLen; i++) {
                b[i] = buffer[i % buffer.length];
            }
            byteCountRemaining -= actualLen;
            return actualLen;
        }
    }
}
