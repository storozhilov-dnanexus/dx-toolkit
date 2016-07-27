#!/usr/bin/env bash

set -ex

g++ -o orchestrate orchestrate.cc -lboost_filesystem -lboost_system
