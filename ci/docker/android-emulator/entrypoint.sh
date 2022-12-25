#!/usr/bin/env bash

set -ex

./adb_redirect.sh $1 $2
./run_emulator.sh
