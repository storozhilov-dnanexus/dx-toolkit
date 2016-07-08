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

from BaseHTTPServer import BaseHTTPRequestHandler
import json
import datetime

class MockHandler(BaseHTTPRequestHandler):
    stats = {
        'postRequests': []}

    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(self.stats))

    def do_POST(self):
        #self.stats['postRequests'].append({
        #        'client_address': self.client_address,
        #        'command': self.command,
        #        'path': self.path,
        #        'request_version': self.request_version,
        #        'headers': self.headers})
        self.stats['postRequests'].append({
                'timestamp': datetime.datetime.now().isoformat(),
                'client_address': self.client_address,
                'command': self.command,
                'path': self.path,
                'request_version': self.request_version,})
        self.send_response(200)
        self.send_header('Content-type','text/html')
        self.end_headers()
        self.wfile.write('Hello from APIserver mock object')

