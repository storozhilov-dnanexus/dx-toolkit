#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# Copyright (C) 2013-2016 DNAnexus, Inc.
#
# This file is part of dx-toolkit (DNAnexus platform client libraries).
#
#   Licensed under the Apache License, Version 2.0 (the "License"); you may not
#   use this file except in compliance with the License. You may obtain a copy
#   of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#   License for the specific language governing permissions and limitations
#   under the License.

import os
import unittest
import tempfile
import subprocess
import time
import json
import dxpy
import requests
import dateutil.parser

class TestDxpyApiMock(unittest.TestCase):
    apiServerMockSubprocess = None

    def setUp(self):
        # Setting up DXPY to use APIserver mock object
        apiServerTcpPort = 8080
        dxpy.set_api_server_info(host="127.0.0.1", port=apiServerTcpPort, protocol="http")

        # Starting APIserver mock-object
        apiServerMockFilename = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                "mock_api", "apiserver_mock.py")
        apiServerMockHandlerFilename = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                "mock_api", "test_503_no_retry-after_exponential_randomized_timout.py")
        self.apiServerMockSubprocess = subprocess.Popen([apiServerMockFilename, apiServerMockHandlerFilename, str(apiServerTcpPort)])
        time.sleep(0.2)

    def tearDown(self):
        # Stopping APIserver mock-object
        self.apiServerMockSubprocess.kill()
        # TODO: Restoring DXPY env settings

    def test_503_exponential_retry(self):
        res = dxpy.DXHTTPRequest("/system/whoami", {}, want_full_response=True, always_retry=True)
        apiServerStats = json.loads(requests.get("http://127.0.0.1:8080/stats").content)
        for i in range(4, 7):
            tried = dateutil.parser.parse(apiServerStats['postRequests'][i]['timestamp'])
            retried = dateutil.parser.parse(apiServerStats['postRequests'][i + 1]['timestamp'])
            interval = (retried - tried).total_seconds()
            self.assertTrue(((i - 4) ** 2) <= interval)

if __name__ == '__main__':
    unittest.main()
