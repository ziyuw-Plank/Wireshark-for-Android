#!/bin/bash
set -euo pipefail
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/env.sh"
cd $ROOT/src
TCF="-DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-$API"
cmake -S libxml2-2.15.4 -B build-libxml2 -G Ninja $TCF -DCMAKE_INSTALL_PREFIX=$PREFIX -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF \
  -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DLIBXML2_WITH_PROGRAMS=OFF -DLIBXML2_WITH_TESTS=OFF -DLIBXML2_WITH_PYTHON=OFF -DLIBXML2_WITH_ICU=OFF \
  -DLIBXML2_WITH_LZMA=OFF -DLIBXML2_WITH_ZLIB=OFF -DLIBXML2_WITH_HTTP=OFF -DLIBXML2_WITH_MODULES=OFF -DLIBXML2_WITH_CATALOG=OFF -DLIBXML2_WITH_DEBUG=OFF >/dev/null
ninja -C build-libxml2 install >/dev/null
echo "libxml2 OK"
# libpcap: Linux PF_PACKET backend only; no remote/dbus/rdma/bluetooth/netmap/libnl
(cd libpcap-1.11.0 && ./configure --host=$TARGET --prefix=$PREFIX --with-pcap=linux --disable-shared --enable-usb --disable-netmap \
  --disable-bluetooth --disable-dbus --disable-rdma --disable-remote --without-libnl --without-dag --without-snf --without-septel --without-turbocap >/dev/null && \
  make -j$JOBS >/dev/null && make install >/dev/null)
echo "libpcap OK"
