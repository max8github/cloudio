package com.tip.hood.net;

import com.tip.hood.itest.testutil.TUtility;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.Test;

/**
 *
 * @author max
 */
public class URLTest {
    
    public URLTest() {
    }

    @Test
    public void testURL() throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("my.xml")) {
            //do something
        }
        //
        String tkfXml = TUtility.assertAndReturnCanonicalPath("src/test/resources/my.xml");
        try (InputStream in = new FileInputStream(tkfXml)) {
        }
        //
        String home = System.getProperty("user.home");
        URL[] myurl = new URL[]{new URL("file:" + home + "/.m2/repository/com/tip/hood/itest/testutil/0.1/testutil-0.1-tests.jar")};
        URLClassLoader cl = new URLClassLoader(myurl);
        try (InputStream in = cl.getResourceAsStream("my.xml")) {
        }
        //
        URL url = new URL("jar:file:" + home + "/.m2/repository/com/tip/hood/itest/testutil/0.1/testutil-0.1-tests.jar!/com/tip/hood/itest/testutil/FileGen.class");
        try (InputStream in = url.openStream()) {
        }
    }
    
}
