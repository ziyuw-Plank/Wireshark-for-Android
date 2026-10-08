#!/bin/bash
set -euo pipefail
source /workspace/android-wireshark/scripts/env.sh
cd /workspace/android-wireshark/helper
H="-O2 -Wall -Wextra -fstack-protector-strong -D_FORTIFY_SOURCE=2 -Wl,-z,relro,-z,now -Wl,-z,max-page-size=16384"
$CC $H -fPIE -pie -o wsexec wsexec.c && $STRIP wsexec
$CC -O2 -Wall -static -o wsexec-static wsexec.c
$CC $H -shared -fPIC -Wl,-soname,libsdnative.so -o libsdnative.so sdnative.c && $STRIP libsdnative.so
