/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tip.hood.goog.storg.integration;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Lists;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.ObjectAccessControl;
import com.google.api.services.storage.model.StorageObject;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.tip.hood.itest.testutil.TUtility;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author max
 */
final class Utils {

    private Utils() {
    }

    static void insert(Storage client, boolean useCustomMetadata, SampleSettings settings, InputStreamContent mediaContent) throws IOException {
        StorageObject objectMetadata = null;
        if (useCustomMetadata) {
            List<ObjectAccessControl> acl = Lists.newArrayList();
            if (settings.getEmail() != null && !settings.getEmail().isEmpty()) {
                acl.add(new ObjectAccessControl().setEntity("user-" + settings.getEmail()).setRole("OWNER"));
            }
            if (settings.getDomain() != null && !settings.getDomain().isEmpty()) {
                acl.add(new ObjectAccessControl().setEntity("domain-" + settings.getDomain()).setRole("READER"));
            }
            objectMetadata = new StorageObject().setName(settings.getPrefix() + "myobject").
                    setMetadata(ImmutableMap.of("key1", "value1", "key2", "value2")).
                    setAcl(acl).setContentDisposition("attachment");
        }
        Storage.Objects.Insert insertObject = client.objects().insert(settings.getBucket(), objectMetadata, mediaContent);
        if (!useCustomMetadata) {
            insertObject.setName(settings.getPrefix() + "01");
        }
        insertObject.getMediaHttpUploader().setProgressListener(new Utils.CustomUploadProgressListener()).setDisableGZipContent(true);
        insertObject.execute();
    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
    static Credential authorize(HttpTransport httpTransport, JsonFactory JSON_FACTORY,
            FileDataStoreFactory dataStoreFactory, String clientSecretsJsonPath) throws Exception {
        // Load client secrets.
        GoogleClientSecrets clientSecrets = null;
        try {
            InputStream resourceAsStream = new FileInputStream(clientSecretsJsonPath);
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(resourceAsStream));
            if (clientSecrets.getDetails().getClientId() == null
                    || clientSecrets.getDetails().getClientSecret() == null) {
                throw new Exception("client_secrets not well formed.");
            }
        } catch (Exception e) {
            System.out.println("Problem loading client_secrets.json file. Make sure it exists, you are "
                    + "loading it with the right path, and a client ID and client secret are "
                    + "defined in it.\n" + e.getMessage());
            throw new IOException(e);
        }

        // Set up authorization code flow.
        // Ask for only the permissions you need. Asking for more permissions will
        // reduce the number of users who finish the process for giving you access
        // to their accounts. It will also increase the amount of effort you will
        // have to spend explaining to users what you are doing with their data.
        // Here we are listing all of the available scopes. You should remove scopes
        // that you are not actually using.
        Set<String> scopes = new HashSet<>();
        scopes.add(StorageScopes.DEVSTORAGE_FULL_CONTROL);
        scopes.add(StorageScopes.DEVSTORAGE_READ_ONLY);
        scopes.add(StorageScopes.DEVSTORAGE_READ_WRITE);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, scopes)
                .setDataStoreFactory(dataStoreFactory)
                .build();
        // Authorize.
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static class ViewBucket extends TUtility.View<Bucket> {

        @Override
        public void show(Bucket bucket) {
            System.out.println("name: " + bucket.getName());
            System.out.println("location: " + bucket.getLocation());
            System.out.println("timeCreated: " + bucket.getTimeCreated());
            System.out.println("owner: " + bucket.getOwner());
            System.out.println("acl: " + bucket.getAcl());
        }
    }

    static class CustomUploadProgressListener implements MediaHttpUploaderProgressListener {

        private final Stopwatch stopwatch = new Stopwatch();

        public CustomUploadProgressListener() {
        }

        @Override
        public void progressChanged(MediaHttpUploader uploader) {
            switch (uploader.getUploadState()) {
                case INITIATION_STARTED:
                    stopwatch.start();
                    System.out.println("Initiation has started!");
                    break;
                case INITIATION_COMPLETE:
                    System.out.println("Initiation is complete!");
                    break;
                case MEDIA_IN_PROGRESS:
                    // TODO(nherring): Progress works iff you have a content length specified.
                    // System.out.println(uploader.getProgress());
                    System.out.println(uploader.getNumBytesUploaded());
                    break;
                case MEDIA_COMPLETE:
                    stopwatch.stop();
                    System.out.println(String.format("Upload is complete! (%s)", stopwatch));
                    break;
                case NOT_STARTED:
                    break;
            }
        }
    }

    static class CustomDownloadProgressListener implements MediaHttpDownloaderProgressListener {

        private final Stopwatch stopwatch;

        public CustomDownloadProgressListener(final Stopwatch stopwatch) {
            this.stopwatch = stopwatch;
        }

        @Override
        public void progressChanged(MediaHttpDownloader downloader) {
            switch (downloader.getDownloadState()) {
                case MEDIA_IN_PROGRESS:
                    System.out.println(downloader.getProgress());
                    break;
                case MEDIA_COMPLETE:
                    stopwatch.stop();
                    System.out.println(String.format("Download is complete! (%s)", stopwatch));
                    break;
                case NOT_STARTED:
                    break;
            }
        }
    }
}
