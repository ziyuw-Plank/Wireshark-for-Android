#!/bin/bash
set -euo pipefail
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/env.sh"
cd $ROOT/src
TCF="-DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-$API"
mkdir -p "$ROOT/hosttools"
# Lemon runs on the host, so build it before CMake checks LEMON_EXECUTABLE.
[ -x "$ROOT/hosttools/lemon" ] || gcc -O2 -w -o "$ROOT/hosttools/lemon" "$ROOT/src/wireshark-4.6.9/tools/lemon/lemon.c"
if [ ! -f build-ws/build.ninja ]; then
cmake -S wireshark-4.6.9 -B build-ws -G Ninja $TCF -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_INSTALL_PREFIX=/ws -DCMAKE_FIND_ROOT_PATH=$PREFIX -DCMAKE_PREFIX_PATH=$PREFIX \
  -DCMAKE_C_FLAGS="-fstack-protector-strong -D_FORTIFY_SOURCE=2 -I$PREFIX/include" \
  -DCMAKE_CXX_FLAGS="-fstack-protector-strong -D_FORTIFY_SOURCE=2 -I$PREFIX/include" \
  -DCMAKE_EXE_LINKER_FLAGS="-L$PREFIX/lib -Wl,-z,relro,-z,now -Wl,-z,max-page-size=16384" \
  -DLEMON_EXECUTABLE=$ROOT/hosttools/lemon -DLEMON_TEMPLATE=$ROOT/src/wireshark-4.6.9/tools/lemon/lempar.c \
  -DBUILD_SHARED_LIBS=OFF -DUSE_STATIC=ON -DENABLE_PLUGINS=OFF -DENABLE_LUA=OFF \
  -DBUILD_wireshark=OFF -DBUILD_stratoshark=OFF -DBUILD_tshark=ON -DBUILD_dumpcap=ON -DBUILD_capinfos=ON -DBUILD_editcap=ON -DBUILD_mergecap=ON \
  -DBUILD_rawshark=OFF -DBUILD_text2pcap=OFF -DBUILD_reordercap=OFF -DBUILD_captype=OFF -DBUILD_randpkt=OFF -DBUILD_dftest=OFF -DBUILD_dcerpcidl2wrs=OFF \
  -DBUILD_androiddump=OFF -DBUILD_sshdump=OFF -DBUILD_ciscodump=OFF -DBUILD_dpauxmon=OFF -DBUILD_randpktdump=OFF -DBUILD_wifidump=OFF -DBUILD_udpdump=OFF \
  -DBUILD_sdjournal=OFF -DBUILD_falcodump=OFF -DBUILD_sshdig=OFF -DBUILD_sharkd=OFF -DBUILD_mmdbresolve=OFF -DBUILD_strato=OFF \
  -DENABLE_PCAP=ON -DENABLE_ZLIB=ON -DENABLE_ZLIBNG=OFF -DENABLE_XXHASH=OFF -DENABLE_MINIZIP=OFF -DENABLE_MINIZIPNG=OFF -DENABLE_LZ4=OFF -DENABLE_BROTLI=OFF \
  -DENABLE_SNAPPY=OFF -DENABLE_ZSTD=OFF -DENABLE_NGHTTP2=OFF -DENABLE_NGHTTP3=OFF -DENABLE_SMI=OFF -DENABLE_GNUTLS=OFF -DENABLE_CAP=OFF -DENABLE_NETLINK=OFF \
  -DENABLE_KERBEROS=OFF -DENABLE_SBC=OFF -DENABLE_SPANDSP=OFF -DENABLE_BCG729=OFF -DENABLE_AMRNB=OFF -DENABLE_ILBC=OFF -DENABLE_OPUS=OFF -DENABLE_SINSP=OFF \
  -DENABLE_LIBSSH=OFF -DENABLE_WERROR=OFF -DUSE_qt6=OFF \
  -DPCAP_INCLUDE_DIR=$PREFIX/include -DPCAP_LIBRARY=$PREFIX/lib/libpcap.a
fi
ninja -C build-ws -j$JOBS tshark dumpcap capinfos editcap mergecap
