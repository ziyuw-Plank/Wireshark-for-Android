#!/bin/bash
# Copy freshly built native executables into the app as lib*.so (extracted to nativeLibraryDir at install).
set -euo pipefail
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/env.sh"
J=$ROOT/app-project/app/src/main/jniLibs/arm64-v8a
mkdir -p $J
$STRIP -o $J/libtshark.so  $ROOT/src/build-ws/run/tshark
$STRIP -o $J/libdumpcap.so $ROOT/src/build-ws/run/dumpcap
cp $ROOT/helper/wsexec $J/libwsexec.so
cp $ROOT/helper/libsdnative.so $J/libsdnative.so
chmod 755 $J/*.so
