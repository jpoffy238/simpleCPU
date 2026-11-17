#!/usr/bin/env bash
# Formats a disk image blank - CpmDiskTool's own "format" (any existing
# content is wiped, even if the image already existed).
#
# Usage: disk-format.sh <image>
#   e.g. disk-format.sh disk0.img
set -euo pipefail

if [ "$#" -ne 1 ]; then
	echo "Usage: $(basename "$0") <image>" >&2
	exit 1
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

mvn -q -f "$repo_root/pom.xml" -pl modules/Disk exec:java \
	-Dexec.mainClass=com.mj.Disk.CpmDiskTool \
	-Dexec.args="format $1"
