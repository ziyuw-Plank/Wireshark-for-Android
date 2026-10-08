#!/bin/bash
set -euo pipefail
source /workspace/android-wireshark/scripts/env.sh
cd $ROOT/src
rm -rf build-glib
unset CC CXX CFLAGS CXXFLAGS LDFLAGS CPPFLAGS AR RANLIB STRIP NM LD
meson setup build-glib glib-2.88.3 --cross-file $ROOT/scripts/android-cross.meson --prefix=$PREFIX --libdir=lib \
  --buildtype=release --default-library=static --wrap-mode=nofallback \
  -Dselinux=disabled -Dxattr=false -Dlibmount=disabled -Dman-pages=disabled -Ddtrace=disabled -Dsystemtap=disabled -Dsysprof=disabled \
  -Ddocumentation=false -Dtests=false -Dinstalled_tests=false -Dnls=disabled -Dintrospection=disabled -Dlibelf=disabled -Dglib_debug=disabled
ninja -C build-glib install >/dev/null && echo "glib OK"
