#!/bin/bash
set -euo pipefail
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/env.sh"
cd $ROOT/src
# libffi (autotools)
(cd libffi-3.8.0 && ./configure --host=$TARGET --prefix=$PREFIX --disable-shared --enable-static --disable-docs --disable-multi-os-directory >/dev/null && make -j$JOBS >/dev/null && make install >/dev/null)
echo "libffi OK"
# pcre2 (cmake)
cmake -S pcre2-10.49 -B build-pcre2 -G Ninja -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-$API \
  -DCMAKE_INSTALL_PREFIX=$PREFIX -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF -DBUILD_STATIC_LIBS=ON -DPCRE2_BUILD_PCRE2GREP=OFF -DPCRE2_BUILD_TESTS=OFF \
  -DPCRE2_SUPPORT_JIT=ON -DPCRE2_BUILD_PCRE2_8=ON -DPCRE2_BUILD_PCRE2_16=OFF -DPCRE2_BUILD_PCRE2_32=OFF -DCMAKE_POSITION_INDEPENDENT_CODE=ON >/dev/null
ninja -C build-pcre2 install >/dev/null
echo "pcre2 OK"
# c-ares (cmake)
cmake -S c-ares-1.34.8 -B build-cares -G Ninja -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-$API \
  -DCMAKE_INSTALL_PREFIX=$PREFIX -DCMAKE_BUILD_TYPE=Release -DCARES_SHARED=OFF -DCARES_STATIC=ON -DCARES_STATIC_PIC=ON -DCARES_BUILD_TOOLS=OFF -DCARES_BUILD_TESTS=OFF >/dev/null
ninja -C build-cares install >/dev/null
echo "c-ares OK"
# libgpg-error
(cd libgpg-error-1.61 && cp -f src/syscfg/lock-obj-pub.aarch64-unknown-linux-androideabi.h src/syscfg/lock-obj-pub.linux-android.h && ./configure --host=$TARGET --prefix=$PREFIX --disable-shared --enable-static --disable-doc --disable-tests --disable-nls --disable-languages --enable-install-gpg-error-config >/dev/null && make -j$JOBS >/dev/null && make install >/dev/null)
echo "gpg-error OK"
# libgcrypt
(cd libgcrypt-1.12.4 && ./configure --host=$TARGET --prefix=$PREFIX --disable-shared --enable-static --disable-doc --with-libgpg-error-prefix=$PREFIX --with-gpg-error-prefix=$PREFIX --disable-jent-support >/dev/null && make -j$JOBS >/dev/null && make install >/dev/null)
echo "gcrypt OK"
