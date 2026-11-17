#!/usr/bin/env bash
# Copies a CP/M file off a disk image onto the host filesystem (length
# rounded up to the nearest 128-byte record - see CpmFileSystem.readFile()).
#
# Usage: disk-get.sh <image> <cpm-name> <host-file>
#   e.g. disk-get.sh disk0.img CCP.COM /tmp/ccp.bin
set -euo pipefail

if [ "$#" -ne 3 ]; then
	echo "Usage: $(basename "$0") <image> <cpm-name> <host-file>" >&2
	echo "  e.g. $(basename "$0") disk0.img CCP.COM /tmp/ccp.bin" >&2
	exit 1
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

mvn -q -f "$repo_root/pom.xml" -pl modules/Disk exec:java \
	-Dexec.mainClass=com.mj.Disk.CpmDiskTool \
	-Dexec.args="get $1 $2 $3"
