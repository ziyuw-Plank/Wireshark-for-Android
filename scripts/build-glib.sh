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
rm -rf build-glib glib-stage
# glib compiles its prefix (locale/module dirs) into the binary, so configure a fixed
# prefix and stage the install; this keeps the output independent of the checkout path.
GP=/sharkdroid/prefix
unset CC CXX CFLAGS CXXFLAGS LDFLAGS CPPFLAGS AR RANLIB STRIP NM LD
meson setup build-glib glib-2.88.3 --cross-file $ROOT/src/android-cross.meson --prefix=$GP --libdir=lib \
  --buildtype=release --default-library=static --wrap-mode=nofallback \
  -Dselinux=disabled -Dxattr=false -Dlibmount=disabled -Dman-pages=disabled -Ddtrace=disabled -Dsystemtap=disabled -Dsysprof=disabled \
  -Ddocumentation=false -Dtests=false -Dinstalled_tests=false -Dnls=disabled -Dintrospection=disabled -Dlibelf=disabled -Dglib_debug=disabled
ninja -C build-glib >/dev/null
DESTDIR="$ROOT/src/glib-stage" meson install -C build-glib --no-rebuild >/dev/null
cp -a "$ROOT/src/glib-stage$GP/." "$PREFIX/"
sed -i "s|^prefix=$GP\$|prefix=$PREFIX|" "$PREFIX"/lib/pkgconfig/*.pc
echo "glib OK"
