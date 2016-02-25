// Copyright (C) 2013-2016 DNAnexus, Inc.
//
// This file is part of dx-toolkit (DNAnexus platform client libraries).
//
//   Licensed under the Apache License, Version 2.0 (the "License"); you may
//   not use this file except in compliance with the License. You may obtain a
//   copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
//   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
//   License for the specific language governing permissions and limitations
//   under the License.

package com.dnanexus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.dnanexus.DXHTTPRequest.RetryStrategy;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * A file (an opaque sequence of bytes).
 */
public class DXFile extends DXDataObject {

    /**
     * Builder class for creating a new {@code DXFile} object. To obtain an instance, call
     * {@link DXFile#newFile()}.
     */
    public static class Builder extends DXDataObject.Builder<Builder, DXFile> {
        private String media;
        private InputStream uploadData;

        private Builder() {
            super();
        }

        private Builder(DXEnvironment env) {
            super(env);
        }

        /**
         * Creates the file.
         *
         * @return a {@code DXFile} object corresponding to the newly created object
         */
        @Override
        public DXFile build() {
            DXFile file = new DXFile(DXAPI.fileNew(this.buildRequestHash(), ObjectNewResponse.class, this.env).getId(),
                    this.project, this.env, null);

            if (uploadData != null) {
                file.upload(uploadData);
            }

            return file;
        }

        /**
         * Use this method to test the JSON hash created by a particular builder call without
         * actually executing the request.
         *
         * @return a JsonNode
         */
        @VisibleForTesting
        JsonNode buildRequestHash() {
            checkAndFixParameters();
            return MAPPER.valueToTree(new FileNewRequest(this));
        }

        /*
         * (non-Javadoc)
         *
         * @see com.dnanexus.DXDataObject.Builder#getThisInstance()
         */
        @Override
        protected Builder getThisInstance() {
            return this;
        }

        /**
         * Sets the Internet Media Type of the file to be created.
         *
         * @param mediaType Internet Media Type
         *
         * @return the same {@code Builder} object
         */
        public Builder setMediaType(String mediaType) {
            Preconditions.checkState(this.media == null, "Cannot call setMediaType more than once");
            this.media = Preconditions.checkNotNull(mediaType, "mediaType may not be null");
            return getThisInstance();
        }

        /**
         * Uploads the data in the specified byte array to the file to be created.
         *
         * @param data data to be uploaded
         *
         * @return the same {@code Builder} object
         */
        public Builder upload(byte[] data) {
            Preconditions.checkNotNull(data, "data may not be null");
            InputStream dataStream = new ByteArrayInputStream(data);
            return this.upload(dataStream);
        }

        /**
         * Uploads the data in the specified stream to the file to be created.
         *
         * @param data stream containing data to be uploaded
         *
         * @return the same {@code Builder} object
         */
        public Builder upload(InputStream data) {
            Preconditions.checkNotNull(this.uploadData == null, "Cannot call upload more than once");
            this.uploadData = Preconditions.checkNotNull(data, "data may not be null");
            return getThisInstance();
        }
    }

    /**
     * Contains metadata for a file.
     */
    public static class Describe extends DXDataObject.Describe {
        @JsonProperty
        private String media;
        @JsonProperty
        private long size;

        private Describe() {
            super();
        }

        /**
         * Returns the Internet Media Type of the file.
         *
         * @return Internet Media Type
         */
        public String getMediaType() {
            Preconditions.checkState(this.media != null,
                    "media type is not accessible because it was not retrieved with the describe call");
            return media;
        }

        /**
         * Returns the size of the file.
         *
         * @return size of file
         */
        public long getFileSize(){
            return size;
        }
    }

    /**
     * Request to /file-xxxx/download.
     */
    @JsonInclude(Include.NON_NULL)
    private static class FileDownloadRequest {
        @JsonProperty("preauthenticated")
        private boolean preauth;

        private FileDownloadRequest(boolean preauth) {
            this.preauth = preauth;
        }
    }

    /**
     * Deserialized output from the /file-xxxx/download route.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FileDownloadResponse {
        @JsonProperty
        private Map<String, String> headers;
        @JsonProperty
        private String url;
    }

    @JsonInclude(Include.NON_NULL)
    private static class FileNewRequest extends DataObjectNewRequest {
        @JsonProperty
        private final String media;

        public FileNewRequest(Builder builder) {
            super(builder);
            this.media = builder.media;
        }
    }

    /**
     * Request to /file-xxxx/upload.
     */
    @JsonInclude(Include.NON_NULL)
    private static class FileUploadRequest {
        @JsonProperty
        private String md5;
        @JsonProperty
        private int size;
        @JsonProperty
        private int index = 1;

        private FileUploadRequest(int size, String md5) {
            this.size = size;
            this.md5 = md5;
        }

        private FileUploadRequest(int size, String md5, int index) {
            this.size = size;
            this.md5 = md5;
            this.index = index;
        }
    }

    /**
     * Response from /file-xxxx/upload
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FileUploadResponse {
        @JsonProperty
        private Map<String, String> headers;
        @JsonProperty
        private String url;
    }

    private static final String USER_AGENT = DXUserAgent.getUserAgent();

    /**
     * Deserializes a DXFile from JSON containing a DNAnexus link.
     *
     * @param value JSON object map
     *
     * @return data object
     */
    @JsonCreator
    private static DXFile create(Map<String, Object> value) {
        checkDXLinkFormat(value);
        // TODO: how to set the environment?
        return DXFile.getInstance((String) value.get("$dnanexus_link"));
    }

    /**
     * Returns a {@code DXFile} associated with an existing file.
     *
     * @throws NullPointerException If {@code fileId} is null
     */
    public static DXFile getInstance(String fileId) {
        return new DXFile(fileId, null);
    }

    /**
     * Returns a {@code DXFile} associated with an existing file in a particular project or
     * container.
     *
     * @throws NullPointerException If {@code fileId} or {@code container} is null
     */
    public static DXFile getInstance(String fileId, DXContainer project) {
        return new DXFile(fileId, project, null, null);
    }

    /**
     * Returns a {@code DXFile} associated with an existing file in a particular project using the
     * specified environment, with the specified cached describe output.
     *
     * <p>
     * This method is for use exclusively by bindings to the "find" routes when describe hashes are
     * returned with the find output.
     * </p>
     *
     * @throws NullPointerException If any argument is null
     */
    static DXFile getInstanceWithCachedDescribe(String fileId, DXContainer project,
            DXEnvironment env, JsonNode describe) {
        return new DXFile(fileId, project, Preconditions.checkNotNull(env, "env may not be null"),
                Preconditions.checkNotNull(describe, "describe may not be null"));
    }

    /**
     * Returns a {@code DXFile} associated with an existing file in a particular project using the
     * specified environment.
     *
     * @throws NullPointerException If {@code fileId} or {@code container} is null
     */
    public static DXFile getInstanceWithEnvironment(String fileId, DXContainer project,
            DXEnvironment env) {
        return new DXFile(fileId, project, Preconditions.checkNotNull(env, "env may not be null"),
                null);
    }

    /**
     * Returns a {@code DXFile} associated with an existing file using the specified environment.
     *
     * @throws NullPointerException If {@code fileId} is null
     */
    public static DXFile getInstanceWithEnvironment(String fileId, DXEnvironment env) {
        return new DXFile(fileId, Preconditions.checkNotNull(env, "env may not be null"));
    }

    /**
     * Returns a Builder object for creating a new {@code DXFile}.
     *
     * @return a newly initialized builder object
     */
    public static Builder newFile() {
        return new Builder();
    }

    /**
     * Returns a Builder object for creating a new {@code DXFile} using the specified environment.
     *
     * @param env environment to use to make API calls
     *
     * @return a newly initialized builder object
     */
    public static Builder newFileWithEnvironment(DXEnvironment env) {
        return new Builder(env);
    }

    // Size of the part to be uploaded
    public int uploadChunkSize = 16 * 1024 * 1024;

    // Minimum size of the part to be downloaded
    public final int minDownloadChunkSize = 64 * 1024;

    // Maximum size of the part to be downloaded
    public final int maxDownloadChunkSize = 16 * 1024 * 1024;

    // Number of bytes to return to outputStream for each call
    public int numBytesToProcess = 5 * 1024 * 1024;

    // Ramp up factor for downloading by parts
    public final int ramp = 2;
    public final int numRequestsBetweenRamp = 4;

    private DXFile(String fileId, DXContainer project, DXEnvironment env, JsonNode describe) {
        super(fileId, "file", project, env, describe);
    }

    private DXFile(String fileId, DXEnvironment env) {
        super(fileId, "file", env, null);
    }

    @Override
    public DXFile close() {
        super.close();
        return this;
    }

    @Override
    public DXFile closeAndWait() {
        super.closeAndWait();
        return this;
    }

    @Override
    public Describe describe() {
        return DXJSON.safeTreeToValue(apiCallOnObject("describe", RetryStrategy.SAFE_TO_RETRY),
                Describe.class);
    }

    @Override
    public Describe describe(DescribeOptions options) {
        return DXJSON.safeTreeToValue(
                apiCallOnObject("describe", MAPPER.valueToTree(options),
                        RetryStrategy.SAFE_TO_RETRY), Describe.class);
    }

    /**
     * Downloads the file and returns a byte array of its contents.
     *
     * @return byte array containing file contents
     */
    // TODO: set project ID containing the file to be downloaded
    public byte[] downloadBytes() {
        // TODO: fix this
//        ByteArrayOutputStream data = (ByteArrayOutputStream) downloadByParts(numBytesToProcess);
//        return data.toByteArray();
        return null;
    }

    /**
     * Downloads the file and returns a stream of its contents.
     *
     * @return stream containing file contents
     */
    public InputStream downloadStream() {
        return downloadByParts(numBytesToProcess);
    }

    @Override
    public Describe getCachedDescribe() {
        this.checkCachedDescribeAvailable();
        return DXJSON.safeTreeToValue(this.cachedDescribe, Describe.class);
    }

    /**
     * HTTP GET request to download part of the file.
     *
     * @param url URL to which an HTTP GET request is made to download the file
     * @param chunkStart beginning of the part (in the byte array containing the file contents) to
     *        be downloaded
     * @param chunkEnd end of the part (in the byte array containing the file contents) to be
     *        downloaded
     *
     * @return byte array containing the part of the file contents that is downloaded
     */
    private static byte[] partDownloadRequest(String url, int chunkStart, int chunkEnd) {
        HttpClient httpclient = HttpClientBuilder.create().setUserAgent(USER_AGENT).build();
        InputStream content = null;
        byte[] data;

        // HTTP GET request with bytes/_ge range header
        HttpGet request = new HttpGet(url);
        request.addHeader("Range", "bytes=" + chunkStart + "-" + chunkEnd);
        HttpResponse response;
        try {
            response = httpclient.execute(request);
            content = response.getEntity().getContent();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                data = IOUtils.toByteArray(content);
                content.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return data;
    }

    private class FileApiInputStream extends InputStream {

        private final long readStart;
        private final long readEnd;

        private long nextByteFromApi;
        private InputStream unreadBytes;

        private FileApiInputStream(long readStart, long readEnd) {
            this.readStart = readStart;
            this.readEnd = readEnd;
            this.nextByteFromApi = readStart;

            OutputStream fileContentsBuffer = new ByteArrayOutputStream();
            FileDownloadResponse apiResponse;
            try {
                apiResponse = MAPPER.treeToValue(output, FileDownloadResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int read() throws IOException {
            // TODO: implement this in terms of read(byte[])
        }

        @Override
        public int read(byte[] b) throws IOException {
            if (unreadBytes == null || unreadBytes.available() == 0) {
                long startRange = nextByteFromApi;
                long endRange = Math.min(startRange + maxDownloadChunkSize - 1, readEnd - 1); // Inclusive

                byte[] downloadPart = partDownloadRequest(apiResponse.url, chunkStart, chunkEnd);
            }
            // TODO: determine if we're at EOF
            assert(unreadBytes != null && unreadBytes.available() > 0);
            return unreadBytes.read(b);
        }

    }

    private InputStream downloadByParts(int numBytes) {
        return new FileApiInputStream(0, describe().getFileSize());
    }

    /**
     * Downloads contents of file in chunks and sends them to an output stream.
     *
     * @param numBytes number of bytes to send to the output stream at each iteration
     *
     * @return output stream containing file contents
     */
    private OutputStream downloadByParts2(int numBytes) {
        int fileSize = describe().getFileSize();

        // API call returns URL and headers for HTTP GET requests
        JsonNode output = apiCallOnObject("download", MAPPER.valueToTree(new FileDownloadRequest(true)),
                RetryStrategy.SAFE_TO_RETRY);

        int chunkStart = 0;
        int chunkSize = minDownloadChunkSize;
        int chunkEnd = Math.min(chunkSize, fileSize);
        InputStream downloadBuffer = new ByteArrayInputStream(new byte[0]);

        int request = 1;
        while (chunkStart < fileSize) {
            // chunk size ramp up
            if (chunkSize < maxDownloadChunkSize) {
                if (request > numRequestsBetweenRamp) {
                    request = 1;
                    chunkSize = chunkSize * ramp;
                }
                request++;
            }

            try {
                ByteArrayOutputStream chunkAsOutputStream = new ByteArrayOutputStream();
                IOUtils.copy(downloadBuffer, chunkAsOutputStream);

                // API requests to download chunks
                while (chunkStart < fileSize && chunkAsOutputStream.size() < numBytes) {
                    chunkEnd = Math.min(chunkStart + chunkSize - 1, fileSize);
                    byte[] downloadPart = partDownloadRequest(apiResponse.url, chunkStart, chunkEnd);
                    chunkAsOutputStream.write(downloadPart);
                    chunkStart = chunkEnd + 1;
                }

                downloadBuffer = new ByteArrayInputStream(chunkAsOutputStream.toByteArray());

                // Write remaining bytes to output stream
                if (chunkEnd == fileSize) {
                    byte[] bytesRead = new byte[downloadBuffer.available()];
                    downloadBuffer.read(bytesRead);
                    fileContentsBuffer.write(bytesRead);
                }

                // Parts to propagate to output stream based on requested part size
                while (downloadBuffer.available() >= numBytes) {
                    byte[] bytesRead = new byte[numBytes];
                    downloadBuffer.read(bytesRead, 0, numBytes);
                    fileContentsBuffer.write(bytesRead);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            downloadBuffer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileContentsBuffer;
    }

    /**
     * HTTP PUT request to upload the data part to the server.
     *
     * @param dataChunk data part that is uploaded
     * @param index position for which the data lies in the file
     */
    private void partUploadRequest(byte[] dataChunk, int index) {
        // MD5 digest as 32 character hex string
        String dataMD5 = DigestUtils.md5Hex(dataChunk);

        // API call returns URL and headers
        JsonNode output =
                apiCallOnObject("upload", MAPPER.valueToTree(new FileUploadRequest(dataChunk.length, dataMD5, index)),
                        RetryStrategy.SAFE_TO_RETRY);

        FileUploadResponse apiResponse;
        try {
            apiResponse = MAPPER.treeToValue(output, FileUploadResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Check that the content-length received by the apiserver is the same
        // as the length of the data
        if (apiResponse.headers.containsKey("content-length")) {
            int apiserverContentLength = Integer.parseInt(apiResponse.headers.get("content-length"));
            if (apiserverContentLength != dataChunk.length) {
                throw new AssertionError(
                        "Content-length received by the apiserver did not match that of the input data");
            }
        }

        // HTTP PUT request to upload URL and headers
        HttpPut request = new HttpPut(apiResponse.url);

        // Set headers
        for (Map.Entry<String, String> header : apiResponse.headers.entrySet()) {
            String key = header.getKey();

            // The request implicitly supplies the content length in the headers
            // when executed
            if (key.equals("content-length")) {
                continue;
            }

            request.setHeader(key, header.getValue());
        }

        // Set entity
        request.setEntity(new ByteArrayEntity(dataChunk));
        HttpClient httpclient = HttpClientBuilder.create().setUserAgent(USER_AGENT).build();
        try {
            httpclient.execute(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uploads data from the specified byte array to the file.
     *
     * <p>
     * The file must be in the "open" state. This method assumes exclusive access to the file: the
     * file must have no parts uploaded before this call is made, and no other clients may upload
     * data to the same file concurrently.
     * </p>
     *
     * @param data part of data in bytes to be uploaded
     */
    public void upload(InputStream data) {
        Preconditions.checkNotNull(data, "data may not be null");

        InputStream uploadBuffer = new ByteArrayInputStream(new byte[0]);
        int numBytes = numBytesToProcess;

        int index = 1;
        try {
            while (data.available() > 0) {
                // Bytes of uploadChunkSize to be buffered in output stream
                ByteArrayOutputStream chunk = new ByteArrayOutputStream();

                // Bytes from buffer left over are copied over to the output stream
                IOUtils.copy(uploadBuffer, chunk);

                // Buffer bytes in chunks. Number of bytes in buffer should be at least numBytes.
                while (chunk.size() < numBytes && data.available() > 0) {
                    byte[] temp = new byte[Math.min(uploadChunkSize, data.available())];
                    data.read(temp, 0, Math.min(uploadChunkSize, data.available()));
                    chunk.write(temp);
                }

                // Need to convert buffer to input stream so it can be read
                uploadBuffer = new ByteArrayInputStream(chunk.toByteArray());

                // Upload bytes from buffer
                while (uploadBuffer.available() >= numBytes) {
                    byte[] uploadPart = new byte[numBytes];
                    uploadBuffer.read(uploadPart, 0, numBytes);
                    partUploadRequest(uploadPart, index);
                    index++;
                }

                // Uploads last few bytes from buffer when there is no more data to be read
                if (uploadBuffer.available() < numBytes && data.available() == 0) {
                    byte[] uploadPart = IOUtils.toByteArray(uploadBuffer);
                    partUploadRequest(uploadPart, index);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /**
     * Uploads data from the specified byte array to the file.
     *
     * <p>
     * The file must be in the "open" state. This method assumes exclusive access to the file: the
     * file must have no parts uploaded before this call is made, and no other clients may upload
     * data to the same file concurrently.
     * </p>
     *
     * @param data data in bytes to be uploaded
     */
    public void upload(byte[] data) {
        Preconditions.checkNotNull(data, "data may not be null");

        upload(new ByteArrayInputStream(data));
    }
}
