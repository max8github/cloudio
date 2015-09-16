package com.tip.hood.configkv;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Set;
import junit.framework.Assert;
import org.maven.pom.Model;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author max
 */
public class ConfigXMLUtilTest {

    public ConfigXMLUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testUnmarshal() throws Exception {
        File pomXml = new File("src/it/simple-it/pom.xml");
        FileInputStream in = new FileInputStream(pomXml);
        Assert.assertNotNull(in);
        File xsd = new File("src/main/resources/pom.xsd");
        Assert.assertTrue(xsd.exists());
        Model project = ConfigXMLUtil.unmarshal(in, xsd);
        Map<String, String> nonDefaults = ConfigXMLUtil.findIllegitDefaults(project);
        Set<Map.Entry<String, String>> entrySet = nonDefaults.entrySet();
        System.out.println("\n-------\n");
        for (Map.Entry<String, String> e : entrySet) {
            System.out.println(e.getKey() + "\t->\t" + e.getValue());
        }
        System.out.println("\n-------\n");
    }

}
