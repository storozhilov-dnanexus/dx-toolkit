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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Tests for DXHTTPRequest against APIserver mock object
 */
public class DXHTTPRequestMockApiTest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class WhoamiRequest {
        @JsonProperty
        private final boolean preauthenticated = true;
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    private final int API_SERVER_TCP_PORT = 8080;
    private Process apiServerMockProcess = null;
    private DXEnvironment env = null;

    @Before
    public void setUp() throws IOException, InterruptedException {
        Path apiMockPath = Paths.get(System.getProperty("user.dir"), "..", "python", "test", "mock_api");
        Path apiMockServer = Paths.get(apiMockPath.toString(), "apiserver_mock.py");
        Path apiMockServer503Handler = Paths.get(apiMockPath.toString(), "test_503_no_retry-after_exponential_randomized_timout.py");
        apiServerMockProcess = Runtime.getRuntime().exec(apiMockServer + " " + apiMockServer503Handler + " " +
                Integer.toString(API_SERVER_TCP_PORT));
        //System.out.println("APIserver mock process is: " + apiServerMockProcess.toString());
        Thread.sleep(500);

        env = DXEnvironment.Builder.fromEnvironment(DXEnvironment.create()).setApiserverHost("localhost")
                        .setApiserverPort(API_SERVER_TCP_PORT).setApiserverProtocol("http").build();
    }

    @After
    public void tearDown() {
        apiServerMockProcess.destroy();
    }

    private void checkExponentialRetry(String testingMode) throws IOException, java.text.ParseException {
        HttpClient c = HttpClientBuilder.create().setUserAgent(DXUserAgent.getUserAgent()).build();
        HttpResponse response = c.execute(new HttpGet("http://localhost:" + Integer.toString(this.API_SERVER_TCP_PORT) +
                "/set_testing_mode/" + testingMode));
        final DXHTTPRequest req = new DXHTTPRequest(env);
        //WhoamiResponse response = DXJSON.safeTreeToValue(req.request("/system/whoami",
        //        mapper.valueToTree(new WhoamiRequest()), DXHTTPRequest.RetryStrategy.SAFE_TO_RETRY), WhoamiResponse.class);
        req.request("/system/whoami", mapper.valueToTree(new WhoamiRequest()), DXHTTPRequest.RetryStrategy.SAFE_TO_RETRY);
        HttpResponse statResponse = c.execute(new HttpGet("http://localhost:" + Integer.toString(this.API_SERVER_TCP_PORT) +
                "/stats"));
        BufferedReader rd = new BufferedReader(new InputStreamReader(statResponse.getEntity().getContent()));
        StringBuffer statBuffer = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            statBuffer.append(line);
        }
        JsonNode statJson = DXJSON.parseJson(statBuffer.toString());
        //System.out.println("POST request stat is: " + statJson.get("postRequests").toString() + ", node type is: " + statJson.get("postRequests").getNodeType().toString());
        Iterator<JsonNode> statIterator = statJson.get("postRequests").iterator();
        double prevItemTimestamp = 0.0;
        for (int i = 0; statIterator.hasNext(); i++) {
            double itemTimestamp =  (double) DatatypeConverter.parseDateTime(statIterator.next().get("timestamp").asText()).getTimeInMillis() / 1000.0;
            if (prevItemTimestamp > 0.0) {
                double interval = itemTimestamp - prevItemTimestamp;
                //System.out.println("Limits are: " + Math.pow(2.0, (i - 1.0)) + ", " + (Math.pow(2.0, i) + 0.5));
                Assert.assertTrue(Math.pow(2.0, (i - 1.0)) <= interval);
                Assert.assertTrue(interval <= Math.pow(2.0, i) + 0.5);
            }
            prevItemTimestamp = itemTimestamp;
        }
    }

    /**
     * Tests randomized exponential retry delay having 503 w/o 'Retry-After' header
     */
    @Test
    public void testServiceUnavailableExponentialRetryContinuous() throws IOException, java.text.ParseException {
        checkExponentialRetry("continuous");
    }

    /**
     * Tests randomized exponential retry delay having 503 w/o 'Retry-After' header, which are mixed with 500
     */
    @Test
    public void testServiceUnavailableExponentialRetryMixed() throws IOException, java.text.ParseException {
        checkExponentialRetry("mixed");
    }
}
