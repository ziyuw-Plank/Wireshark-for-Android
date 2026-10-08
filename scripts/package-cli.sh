#!/bin/bash
# Package standalone CLI binaries (Android arm64, API 29+) for manual use via adb/su.
set -euo pipefail
source /workspace/android-wireshark/scripts/env.sh
V=4.6.9
OUTN=sharkdroid-cli-wireshark-$V-android-arm64
STAGE=$ROOT/out/stage/$OUTN
rm -rf $ROOT/out/stage && mkdir -p $STAGE/bin $STAGE/share $STAGE/licenses $STAGE/patches
for b in tshark dumpcap capinfos editcap mergecap; do $STRIP -o $STAGE/bin/$b $ROOT/src/build-ws/run/$b; done
cp -r /tmp/wsinst/ws/share/wireshark $STAGE/share/
cp $ROOT/helper/wsexec $STAGE/bin/wsexec
cp $ROOT/src/wireshark-4.6.9/COPYING $STAGE/licenses/wireshark-COPYING
cp $ROOT/src/libpcap-1.11.0/LICENSE $STAGE/licenses/libpcap-LICENSE
cp $ROOT/src/glib-2.88.3/COPYING $STAGE/licenses/glib-COPYING 2>/dev/null || cp $ROOT/src/glib-2.88.3/LICENSES/LGPL-2.1-or-later.txt $STAGE/licenses/glib-LGPL-2.1
cp $ROOT/src/libgcrypt-1.12.4/COPYING.LIB $STAGE/licenses/libgcrypt-COPYING.LIB
cp $ROOT/src/libgpg-error-1.61/COPYING.LIB $STAGE/licenses/libgpg-error-COPYING.LIB
cp $ROOT/src/c-ares-1.34.8/LICENSE.md $STAGE/licenses/c-ares-LICENSE.md
cp $ROOT/src/pcre2-10.49/LICENCE.md $STAGE/licenses/pcre2-LICENCE.md 2>/dev/null || cp $ROOT/src/pcre2-10.49/LICENCE $STAGE/licenses/pcre2-LICENCE
cp $ROOT/src/libxml2-2.15.4/Copyright $STAGE/licenses/libxml2-Copyright
cp $ROOT/src/libffi-3.8.0/LICENSE $STAGE/licenses/libffi-LICENSE
cp $ROOT/patches/*.patch $STAGE/patches/
cp $ROOT/out/SOURCES.txt $STAGE/ 2>/dev/null || true
cat > $STAGE/ws.sh <<'SH'
#!/system/bin/sh
# Usage (as root):  sh ws.sh dumpcap -D   |   sh ws.sh tshark -r file.pcapng
D=$(cd "$(dirname "$0")" && pwd)
export WIRESHARK_DATA_DIR="$D/share/wireshark"
export HOME="${HOME:-/data/local/tmp}"
tool="$1"; shift
case "$tool" in tshark|dumpcap|capinfos|editcap|mergecap) exec "$D/bin/$tool" "$@";; *) echo "unknown tool: $tool" >&2; exit 2;; esac
SH
chmod 755 $STAGE/bin/* $STAGE/ws.sh
find $STAGE -type d -exec chmod 755 {} +; find $STAGE -type f ! -path "*/bin/*" ! -name ws.sh -exec chmod 644 {} +
(cd $ROOT/out/stage && tar --owner=0 --group=0 --numeric-owner -czf $ROOT/out/$OUTN.tar.gz $OUTN)
ls -la $ROOT/out/$OUTN.tar.gz
