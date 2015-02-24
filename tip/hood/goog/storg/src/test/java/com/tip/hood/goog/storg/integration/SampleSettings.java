package com.tip.hood.goog.storg.integration;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Key;
import java.io.IOException;
import java.io.InputStream;

/**
 * Samples settings JSON Model.
 * Got this class as is from sample google code tutorial.
 */
public final class SampleSettings extends GenericJson {
    @Key(value = "project")
    private String project;
    @Key(value = "bucket")
    private String bucket;
    @Key(value = "prefix")
    private String prefix;
    @Key(value = "email")
    private String email;
    @Key(value = "domain")
    private String domain;

    public String getProject() {
        return project;
    }

    public String getBucket() {
        return bucket;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getEmail() {
        return email;
    }

    public String getDomain() {
        return domain;
    }

    public static SampleSettings load(JsonFactory jsonFactory, InputStream inputStream) {
        try {
            return jsonFactory.fromInputStream(inputStream, SampleSettings.class);
        } catch (IOException e) {
            return new SampleSettings();
        }
    }
    
}
