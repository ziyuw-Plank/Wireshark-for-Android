#!/bin/bash
# Build only from sources already prepared by prepare-sources.py.
set -euo pipefail
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/env.sh"
for tool in gcc make cmake ninja meson pkg-config python3; do
  command -v "$tool" >/dev/null || { echo "Missing build tool: $tool" >&2; exit 1; }
done
test -x "$CC" || { echo "NDK compiler not found: $CC" >&2; exit 1; }
mkdir -p "$PREFIX/include" "$PREFIX/lib" "$ROOT/src"
# Original no-op gettext implementation, cross-compiled with the same NDK.
"$CC" $CFLAGS -c "$ROOT/stubs/intl/intl.c" -o "$ROOT/src/intl.o"
"$AR" rcs "$PREFIX/lib/libintl.a" "$ROOT/src/intl.o"
cp "$ROOT/stubs/intl/libintl.h" "$PREFIX/include/"
bash "$ROOT/scripts/build-deps-1.sh"
bash "$ROOT/scripts/build-glib.sh"
bash "$ROOT/scripts/build-deps-2.sh"
bash "$ROOT/scripts/build-wireshark.sh"
bash "$ROOT/helper/build.sh"
bash "$ROOT/scripts/copy-jnilibs.sh"
