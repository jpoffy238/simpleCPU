#!/usr/bin/env bash
# Redeploys a freshly-built binary onto a disk image under a CP/M name -
# the "erase whatever's there, then put the new build" dance this project
# does after almost every firmware rebuild (CCP.COM, EDIT.COM, ...). Plain
# disk-put.sh refuses if the name is already taken (CpmDiskTool.put() ->
# CpmFileSystem.createFile() errors on an existing name), so this exists to
# skip the separate disk-erase.sh step every time.
#
# Usage: disk-redeploy.sh <image> <host-file> <cpm-name>
#   e.g. disk-redeploy.sh disk0.img modules/cpu/6502/src/main/c/edit.bin EDIT.COM
set -euo pipefail

if [ "$#" -ne 3 ]; then
	echo "Usage: $(basename "$0") <image> <host-file> <cpm-name>" >&2
	echo "  e.g. $(basename "$0") disk0.img modules/cpu/6502/src/main/c/edit.bin EDIT.COM" >&2
	exit 1
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
image="$1"
host_file="$2"
cpm_name="$3"

# Erase first if it's already there - ignore failure, since CpmDiskTool's
# own "File not found" (nothing to erase yet, e.g. the very first deploy
# of this name) isn't a real error here.
mvn -q -f "$repo_root/pom.xml" -pl modules/Disk exec:java \
	-Dexec.mainClass=com.mj.Disk.CpmDiskTool \
	-Dexec.args="erase $image $cpm_name" >/dev/null 2>&1 || true

mvn -q -f "$repo_root/pom.xml" -pl modules/Disk exec:java \
	-Dexec.mainClass=com.mj.Disk.CpmDiskTool \
	-Dexec.args="put $image $host_file $cpm_name"

echo "Redeployed $host_file -> $cpm_name on $image"
