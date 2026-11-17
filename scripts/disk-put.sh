#!/usr/bin/env bash
# Copies a host file onto a disk image under a CP/M (8.3) name.
#
# Usage: disk-put.sh <image> <host-file> <cpm-name>
#   e.g. disk-put.sh disk0.img modules/cpu/6502/src/main/c/edit.bin EDIT.COM
set -euo pipefail

if [ "$#" -ne 3 ]; then
	echo "Usage: $(basename "$0") <image> <host-file> <cpm-name>" >&2
	echo "  e.g. $(basename "$0") disk0.img modules/cpu/6502/src/main/c/edit.bin EDIT.COM" >&2
	exit 1
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

mvn -q -f "$repo_root/pom.xml" -pl modules/Disk exec:java \
	-Dexec.mainClass=com.mj.Disk.CpmDiskTool \
	-Dexec.args="put $1 $2 $3"
