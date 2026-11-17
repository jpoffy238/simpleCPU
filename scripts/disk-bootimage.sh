#!/usr/bin/env bash
# Writes a raw binary onto track 0 as a boot image (sector 1 = header, 2+ =
# code, capped at 25 sectors). Predates BDOS's find-by-name B/CCP.COM boot
# path - see Doc/Architecture.md - but still usable for writing something
# else to the now-otherwise-unused track 0.
#
# Usage: disk-bootimage.sh <image> <host-file>
#   e.g. disk-bootimage.sh disk0.img modules/cpu/6502/src/main/c/hello.bin
set -euo pipefail

if [ "$#" -ne 2 ]; then
	echo "Usage: $(basename "$0") <image> <host-file>" >&2
	exit 1
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

mvn -q -f "$repo_root/pom.xml" -pl modules/Disk exec:java \
	-Dexec.mainClass=com.mj.Disk.CpmDiskTool \
	-Dexec.args="bootimage $1 $2"
