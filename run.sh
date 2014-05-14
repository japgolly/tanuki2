#!/bin/bash
cd $(dirname $(readlink -e "$0")) || exit 1
cd target/tanuki-2.0-SNAPSHOT-linux-x86_64/tanuki-2.0-SNAPSHOT && ./tanuki2.sh
