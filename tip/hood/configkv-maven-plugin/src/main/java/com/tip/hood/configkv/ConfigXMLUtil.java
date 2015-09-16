package com.tip.hood.configkv;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.maven.pom.Model;
import org.maven.pom.Profile;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Utility class for marshalling, unmarshalling, pretty printing, validating of xml
 *
 * @author mcalderoni
 */
public final class ConfigXMLUtil {

    public static final String SCHEMA_LOCATION = "http://maven.apache.org/POM/4.0.0\n"
            + "         http://maven.apache.org/xsd/maven-4.0.0.xsd";
    private static final Logger logger = LogManager.getLogger(ConfigXMLUtil.class);

    private ConfigXMLUtil() {
    }

    /**
     * Unmarshals given the input stream of the xml file to be unmarshalled and an optional schema. If schema is null,
     * it will simply not validate.
     *
     * @param inputStream
     * @param xsd schema file. Can be null.
     * @return
     * @throws JAXBException
     */
    public static Model unmarshal(InputStream inputStream, File xsd) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance("org.maven.pom");
        Unmarshaller u = jc.createUnmarshaller();
        u.setSchema(createSchema(xsd));
        JAXBElement root = (JAXBElement) u.unmarshal(inputStream);
        Model value = (Model) root.getValue();
        return value;
    }

    /**
     * Extracts from the given pom file all illegitimate defaults. By convention (established by this plugin) properties
     * present in profiles with name starting with "default_" should contain defaults values to be resolved by maven.
     * However, it is handy to be able to set in those profiles also properties such as
     * <code>${project.base.dir}</code>, which can be resolved by Maven with no intervention. When creating the database
     * of all properties in the system, which is what this plugin is all about, these resolved values cannot be taken as
     * defaults: they should instead go in the non-defaults section in the output file. For that, these are illegitimate
     * defaults, because they live in the presumably "defaults" area (pom.xml default profiles), but they are not really
     * defaults.
     *
     * This method takes teh JAXB model of the input pom.xml file and looks for profile properties, the value of which
     * start with "${": those are all illegitimate defaults. If the property contains a dollar symbol, then it is a
     * property that has been set among defaults, but still contains a token to be resolved by Maven. That means that
     * this one property cannot be taken as is by the deployer: the deployer must change it. So this property goes in
     * the non-defaults section. In order to find these properties, i have to parse the plain XML of pom.xml as text: i
     * cannot use Maven's objects, because by the time those objects are given to me, Maven has already resolved all
     * tokens (tokens are the ones starting with dollar-curly brace).
     *
     * @param project
     * @return
     * @throws DOMException
     */
    public static Map<String, String> findIllegitDefaults(Model project) throws DOMException {
        Map<String, String> map = new LinkedHashMap<>();
        Model.Profiles profiles = project.getProfiles();
        List<Profile> profile = profiles.getProfile();
        for (Profile p : profile) {
            //we are interested in only profiles that contain default properties. By convention of this plugin,
            //it is assumed that all those profiles are named starting with "default_" and contain only one
            //property each.
            if (p.getId().startsWith(ConfigKVMojo.DEFAULT)) {
                Profile.Properties properties = p.getProperties();
                List<Element> any = properties.getAny();
                for (Element a : any) {
                    Node firstChild = a.getFirstChild();
                    String nodeName = a.getNodeName();
                    String nodeValue = firstChild.getNodeValue();
                    if (nodeValue.startsWith("${")) {
                        map.put(nodeName, nodeValue);
                    }
                }
            }
        }
        return map;
    }

    private static Schema createSchema(File xsd) {
        //validate
        SchemaFactory f = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = null;
        try {
            if (xsd != null) {
                schema = f.newSchema(xsd);
            }
        } catch (SAXException ex) {
            logger.warn("Could not validate with provided schema '" + xsd + "'. Exception: " + ex);
        }
        return schema;
    }

    /**
     * Utility method for validating a generic xml file with its schema. See
     * http://java.sun.com/developer/technicalArticles/xml/validationxpath/
     *
     * One other way of validating, now deprecated in JAXB 2.0 was: javax.xml.bind.Validator validator =
     * jc.createValidator(); validator.validate(model);
     *
     * @param xmlFile xml file to be validated.
     * @param xsd schema used to validate the provided xml file.
     * @return
     */
    public static boolean validate(File xmlFile, File xsd) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            // Parse an XML document into a DOM tree.
            DocumentBuilder parser = dbf.newDocumentBuilder();
            Document document = parser.parse(xmlFile);

            // Create a SchemaFactory capable of understanding WXS schemas.
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // Load a WXS schema, represented by a Schema instance.
            Source schemaFile = new StreamSource(xsd);
            Schema schema = factory.newSchema(schemaFile);

            // Create a Validator object, which can be used to validate
            // an instance document.
            Validator validator = schema.newValidator();

            // Validate the DOM tree.
            validator.validate(new DOMSource(document));

        } catch (ParserConfigurationException e) {
            logger.error("" + e);
            return false;
        } catch (SAXException e) {
            logger.error("document not valid! " + e);
            return false;
        } catch (IOException e) {
            logger.error("" + e);
            return false;
        }
        return true;
    }

    public static void printOut(File xmlFile) throws FileNotFoundException, IOException {
        BufferedReader buf = new BufferedReader(new FileReader(xmlFile));
        String line = null;
        while ((line = buf.readLine()) != null) {
            System.out.println(line);
        }
        buf.close();
    }
}
