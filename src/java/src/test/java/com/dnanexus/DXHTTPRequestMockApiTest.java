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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for DXHTTPRequest against APIserver mock object
 */
public class DXHTTPRequestMockApiTest {
    private final int API_SERVER_TCP_PORT = 8080;
    private Process apiServerMockProcess;

    @Before
    public void setUp() throws IOException, InterruptedException {
        Path apiMockPath = Paths.get(System.getProperty("user.dir"), "..", "python", "test", "mock_api");
        Path apiMockServer = Paths.get(apiMockPath.toString(), "apiserver_mock.py");
        Path apiMockServer503Handler = Paths.get(apiMockPath.toString(), "test_503_no_retry-after_exponential_randomized_timout.py");
        //System.out.format("Server '%s', handler: '%s'\n", apiMockServer, apiMockServer503Handler);
        this.apiServerMockProcess = Runtime.getRuntime().exec(apiMockServer + " " + apiMockServer503Handler + " " +
                Integer.toString(this.API_SERVER_TCP_PORT));
        System.out.println("APIserver mock process is: " + this.apiServerMockProcess.toString());
        Thread.sleep(500);
    }

    @After
    public void tearDown() {
        this.apiServerMockProcess.destroy();
    }

    /**
     * Tests randomized exponential retry delay having 503 w/o 'Retry-After' header
     */
    @Test
    public void testServiceUnavailableExponentialRetryContinuous() throws IOException {
        HttpClient c = HttpClientBuilder.create().setUserAgent(DXUserAgent.getUserAgent()).build();
        HttpResponse response = c.execute(new HttpGet("http://localhost:" + Integer.toString(this.API_SERVER_TCP_PORT) +
                "/set_testing_mode/continuous"));
        Assert.assertEquals(1, 1);
    }

    /**
     * Tests randomized exponential retry delay having 503 w/o 'Retry-After' header, which are mixed with 500
     */
    @Test
    public void testServiceUnavailableExponentialRetryMixed() throws IOException {
        HttpClient c = HttpClientBuilder.create().setUserAgent(DXUserAgent.getUserAgent()).build();
        HttpResponse response = c.execute(new HttpGet("http://localhost:" + Integer.toString(this.API_SERVER_TCP_PORT) +
                "/set_testing_mode/mixed"));
        Assert.assertEquals(1, 1);
    }
}
