#!/usr/bin/env bash
# Deletes one named file from a disk image.
#
# Usage: disk-erase.sh <image> <cpm-name>
#   e.g. disk-erase.sh disk0.img EDIT2.COM
set -euo pipefail

if [ "$#" -ne 2 ]; then
	echo "Usage: $(basename "$0") <image> <cpm-name>" >&2
	exit 1
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

mvn -q -f "$repo_root/pom.xml" -pl modules/Disk exec:java \
	-Dexec.mainClass=com.mj.Disk.CpmDiskTool \
	-Dexec.args="erase $1 $2"
