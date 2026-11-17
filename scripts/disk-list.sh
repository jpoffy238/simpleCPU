#!/usr/bin/env bash
# Lists every file on a disk image.
#
# Usage: disk-list.sh <image>
#   e.g. disk-list.sh disk0.img
set -euo pipefail

if [ "$#" -ne 1 ]; then
	echo "Usage: $(basename "$0") <image>" >&2
	exit 1
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

mvn -q -f "$repo_root/pom.xml" -pl modules/Disk exec:java \
	-Dexec.mainClass=com.mj.Disk.CpmDiskTool \
	-Dexec.args="list $1"
