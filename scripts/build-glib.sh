#!/bin/bash
set -euo pipefail
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/env.sh"
cd "$ROOT/src"
# Resolve paths for this checkout and the NDK selected by F-Droid/the caller.
python3 - "$ROOT" "$NDK" <<'PY'
import pathlib, sys
root, ndk = sys.argv[1:]
template = (pathlib.Path(root) / "scripts/android-cross.meson").read_text()
# Meson string literals need backslashes and quotes escaped.
escape = lambda value: value.replace("\\", "\\\\").replace("'", "\\'")
(pathlib.Path(root) / "src/android-cross.meson").write_text(
    template.replace("@ROOT@", escape(root)).replace("@NDK@", escape(ndk)))
PY
rm -rf build-glib
unset CC CXX CFLAGS CXXFLAGS LDFLAGS CPPFLAGS AR RANLIB STRIP NM LD
meson setup build-glib glib-2.88.3 --cross-file $ROOT/src/android-cross.meson --prefix=$PREFIX --libdir=lib \
  --buildtype=release --default-library=static --wrap-mode=nofallback \
  -Dselinux=disabled -Dxattr=false -Dlibmount=disabled -Dman-pages=disabled -Ddtrace=disabled -Dsystemtap=disabled -Dsysprof=disabled \
  -Ddocumentation=false -Dtests=false -Dinstalled_tests=false -Dnls=disabled -Dintrospection=disabled -Dlibelf=disabled -Dglib_debug=disabled
ninja -C build-glib install >/dev/null
echo "glib OK"
