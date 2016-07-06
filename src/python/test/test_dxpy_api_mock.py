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

import os, unittest

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

class TestDxpy(unittest.TestCase):
    def setUp(self):
        # TODO: Start APIserver mock-object
        return

    def tearDown(self):
        # TODO: Stop APIserver mock-object
        return

    def test_exponential_retry(self):
        self.assertTrue(True)

if __name__ == '__main__':
    unittest.main()
