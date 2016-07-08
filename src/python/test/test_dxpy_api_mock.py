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
import requests
import json

#from __future__ import print_function, unicode_literals, division, absolute_import

#import os, unittest, tempfile, filecmp, time, json, sys
#import shutil
#import string
#import subprocess
#import platform

#import requests
#from requests.packages.urllib3.exceptions import SSLError

#import dxpy
#import dxpy_testutil as testutil
#from dxpy.exceptions import (DXAPIError, DXFileError, DXError, DXJobFailureError, ResourceNotFound)
#from dxpy.utils import pretty_print, warn
#from dxpy.utils.resolver import resolve_path, resolve_existing_path, ResolutionError, is_project_explicit

class TestDxpyApiMock(unittest.TestCase):
    apiServerMockSubprocess = None

    def setUp(self):
        # Starting APIserver mock-object
        apiServerMockFilename = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                "apiserver_mock", "apiserver_mock.py")
        apiServerMockHandlerFilename = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                "apiserver_mock", "test_503_no-retry-after_exponential_randomized_timout.py")
        self.apiServerMockSubprocess = subprocess.Popen([apiServerMockFilename, apiServerMockHandlerFilename])
        time.sleep(0.2)

    def tearDown(self):
        # Stopping APIserver mock-object
        self.apiServerMockSubprocess.kill()

    def test_exponential_retry(self):
        requests.post("http://127.0.0.1:8080/file/new", data = {'key':'value'})
        apiServerStats = json.loads(requests.get("http://127.0.0.1:8080/stats").content)
        print("APIserver stats is '{}'".format(apiServerStats))

if __name__ == '__main__':
    unittest.main()
