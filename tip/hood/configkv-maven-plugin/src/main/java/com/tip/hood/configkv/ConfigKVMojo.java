package com.tip.hood.configkv;

import asia.redact.bracket.properties.OutputAdapter;
import asia.redact.bracket.properties.OutputFormat;
import asia.redact.bracket.properties.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Creates a configkv.properties file in the target directory. The properties file is the map of every configuration
 * property present in the source tree of the entire project. The plugin assumes that configuration properties are saved
 * in profiles in settings.xml and as defaults in the main pom.xml file (the container of the whole source tree. It also
 * assumes that the default properties in pom.xml are contained in profiles, whose id starts with the string "default_".
 * <code>
 * <plugin>
 * <groupId>com.tip.hood</groupId>
 * <artifactId>configkv</artifactId>
 * <version>0.4.2</version>
 * <executions>
 * <execution>
 * <phase>post-clean</phase>
 * <goals>
 * <goal>configkv</goal>
 * </goals>
 * </execution>
 * </executions>
 * </plugin>
 * </code>
 *
 */
@Mojo(name = "configkv", defaultPhase = LifecyclePhase.PROCESS_SOURCES, aggregator = false)
public class ConfigKVMojo extends AbstractMojo {

    static final String DEFAULT = "default_";
    /**
     * Location of the file relative to the target directory.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDirectory", required = false, readonly = true)
    private File outputDirectory;

    /**
     * The relative path to the file where the user wants to save the configuration db information. This path will be
     * relative to the target directory. Example of correct file path values: config/mydb.properties, config.db.
     */
    @Parameter(defaultValue = "configdkv.properties", property = "file", required = false)
    private String filepath;

    @Parameter(defaultValue = "${settings}", property = "_settings", required = false, readonly = true)
    private Settings _settings;

    /**
     * The absolute path to a user-provided settings.xml.
     */
    @Parameter(property = "settings", required = false)
    private File settingsXml;

    /**
     * The absolute path to a user-provided pom.xml.
     */
    @Parameter(property = "pom", required = false)
    private File pomXml;

    @Parameter(defaultValue = "${reactorProjects}", property = "reactorProjects", required = false, readonly = true)
    private List<MavenProject> projects;

    private Model model;

    @Override
    public void execute() throws MojoExecutionException {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        //If pom.xml is provided by the user, use that one, else, get it from the topmost
        //pom in the entire source tree.
        if (pomXml != null) {
            try (Reader reader = new FileReader(pomXml)) {
                MavenXpp3Reader mavenreader = new MavenXpp3Reader();
                model = mavenreader.read(reader);
                model.setPomFile(pomXml);
            } catch (IOException | XmlPullParserException ex) {
                throw new MojoExecutionException("Could not read provided settings.xml file " + settingsXml, ex);
            }
        } else {
            //Get the top level pom of the entire tree (first one in reactor list)
            model = projects.get(0).getModel();
        }

        //If settings.xml is provided by the user, use that one, else the standard one will be used.
        if (settingsXml != null) {
            SettingsXpp3Reader s = new SettingsXpp3Reader();
            try (FileInputStream sFileStream = new FileInputStream(settingsXml)) {
                _settings = s.read(sFileStream);
            } catch (IOException | XmlPullParserException ex) {
                throw new MojoExecutionException("Could not read provided settings.xml file " + settingsXml, ex);
            }
        }

        Properties defaultProps = Properties.Factory.getInstance();
        Properties nonDefaultProps = Properties.Factory.getInstance();
        
        //All properties with defaults are assumed to be in the pom.
        //It is also assumed that profiles containing defaults
        //have id that starts with "default_".
        //Not all properties defined in a "default_" profile are defaults, because they could contain a maven token
        //such as for example ${project.basedir}. In that case, it is not a default configuration item, it is an 
        //item that must be edited by the deployer before deploying.
        extractPropsFromPom(defaultProps, nonDefaultProps);

        //Extract all properties from the given settings.xml. Here is where all config properties
        //that don't have a reasonable default should be (it is assumed that there are no sensible defaults in settings.xml).
        extractPropsFromSettings(nonDefaultProps);

        //Combine the two sets of properties into one file. First write the non-default ones,
        //then the default ones
      
        
        
//        #
//#NONE of the values in this section should be taken as is: these are *NOT* defaults.
//#This section must be edited by the deployer for the system to function.
//#The values present for each key are only here to show an *example* of a value for a key, not to be used.
//#Whoever deploys the system with this configuration, must first *EDIT* this section manually.
//#This section contains settings such as IP addresses, absolute paths, password, usernames, etc, all properties
//#that need to be given as input to the system to execute.

//################ DEFAULTS Section ###############
//#
//#This section contains default values.
//#These values should be considered fine for most deployments.
//#The deployer does not necessarily need to change these values: 
//#the app will run fine with the settings below in most deployments.
//#
//########################################################
        File outFile = new File(outputDirectory, filepath);
        String edit_Section = "################ KEY-VALUE STORE OF CONFIGURATION ###############\n"
                + "#\n"
                + "This file represents the set of all configurable properties in your application, provided\n"
                + "that the maintainer of the app has declared configurable properties as specified in the configkv maven plugin.\n"
                + "In short, configurable properties should be declared in the top pom.xml of the project and in settings.xml\n"
                + "as specified in the documentation for the configkv maven plugin.\n"
                + "#\n"
                + "This file is a key/value store of configurable properties in a system.\n"
                + "It has two sections. The last section contains defaults, that is values that the deployer does not necessarily need\n"
                + "to set. The system will reasonably work with those default values.\n"
                + "The first section instead contains values that need to be overridden before deploying the system. The system\n"
                + "will not run without correctly setting this section.\n"
                + "#\n"
                + "#######################################################\n\n\n"
                + "############### MANDATORY Section ###############\n"
                + "Please note: the deployer should *EDIT* this section manually\n"
                + "before using the file for replacing tokens\n"
                + "#######################################################";
        save(nonDefaultProps, edit_Section, outFile, false);
        String defaults_Section = "###############DEFAULTS Section###############\n"
                + "This section contains values that should already be fine for deployment\n"
                + "The deployer does not necessarily need to change these values\n"
                + "#######################################################";
        save(defaultProps, defaults_Section, outFile, true);

        getLog().info("\n\n**********************\n\n Saved Configuration database in file\n"
                + outFile.getPath() + ".\n\n"
                + "**********************\n\n");
    }

    /**
     * Saves given properties, appending or not to the given file.
     *
     * @param props properties to be saved
     * @param headerString optional header string
     * @param outFile required file for properties to be saved into
     * @param append if appending to the given file or not
     * @throws MojoExecutionException
     */
    private void save(Properties props, String headerString, File outFile, boolean append)
            throws MojoExecutionException {
        Objects.requireNonNull(props, "properties argument cannot be null");
        Objects.requireNonNull(outFile, "input file argument cannot be null");
        OutputAdapter out = new OutputAdapter(props);
        OutputFormat format = new ConfigKVFormat(headerString);
        try (Writer w = new FileWriter(outFile, append)) {
            out.writeTo(w, format);
        } catch (IOException ex) {
            String msg = "Error saving file " + outFile.getPath();
            getLog().error(msg, ex);
            throw new MojoExecutionException(msg, ex);
        }
    }

    static List<Profile> extractActiveProfiles(Settings settings) {
        List<Profile> profiles = settings.getProfiles();
        List<String> activeProfIds = settings.getActiveProfiles();
        List<Profile> activeProfiles = new ArrayList<>();
        Set<String> hashSet = new HashSet<>();
        hashSet.addAll(activeProfIds);
        for (Profile profile : profiles) {
            if (hashSet.contains(profile.getId())) {
                activeProfiles.add(profile);
            }
        }
        return activeProfiles;
    }

    private void extractPropsFromPom(Properties defaultProps, Properties nonDefaultProps) {
        for (org.apache.maven.model.Profile profile : model.getProfiles()) {
            //we are interested in only profiles that contain default properties. By convention of this plugin,
            //it is assumed that all those profiles are named starting with "default_" and contain only one 
            //property each.
            if (profile.getId().startsWith(DEFAULT)) {
                java.util.Properties properties = profile.getProperties();
                Map.Entry<Object, Object> entry = validateDefaultProfile(properties);
                String value = (String) entry.getValue();
                //look at the value of property first. If the property contains a dollar symbol, then it is a property
                //that has been set among defaults, but still contains a token to be resolved by Maven.
                //That means that this one property cannot be taken as is by the deployer: the deployer must change it.
                //So this property goes in the non-defaults section.
                if (value.contains("$")) {
                    nonDefaultProps.put((String) entry.getKey(), value);
                } else {
                    defaultProps.put((String) entry.getKey(), value);
                }

            }
        }
    }

    private Map.Entry<Object, Object> validateDefaultProfile(java.util.Properties properties) throws IllegalArgumentException {
        String errMesg = "There must be only one property in a default_ profile";
        Objects.requireNonNull(properties, errMesg);
        Set<Map.Entry<Object, Object>> entrySet = properties.entrySet();
        if (entrySet.isEmpty() || entrySet.size() > 1) {
            throw new IllegalArgumentException(errMesg);
        }
        Map.Entry<Object, Object> entry = entrySet.iterator().next();
        return entry;
    }

    /**
     * Given the list of profiles in s
     *
     * @param profiles
     * @return
     */
    private void extractPropsFromSettings(Properties props) {
        List<Profile> asettingsProfiles = ConfigKVMojo.extractActiveProfiles(_settings);
        for (Profile profile : asettingsProfiles) {
            java.util.Properties properties = profile.getProperties();
            Set<Map.Entry<Object, Object>> entrySet = properties.entrySet();
            for (Map.Entry<Object, Object> entry : entrySet) {
                props.put((String) entry.getKey(), (String) entry.getValue());
            }
        }
    }
}
