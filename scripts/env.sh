# Cross-compile environment for Android arm64-v8a, API 29
export ROOT=/workspace/android-wireshark
export NDK=/opt/android-ndk-r27c
export API=29
export TARGET=aarch64-linux-android
export TC=$NDK/toolchains/llvm/prebuilt/linux-x86_64
export SYSROOT=$TC/sysroot
export PREFIX=$ROOT/prefix
export CC="$TC/bin/${TARGET}${API}-clang"
export CXX="$TC/bin/${TARGET}${API}-clang++"
export AR=$TC/bin/llvm-ar RANLIB=$TC/bin/llvm-ranlib STRIP=$TC/bin/llvm-strip NM=$TC/bin/llvm-nm LD=$TC/bin/ld.lld OBJCOPY=$TC/bin/llvm-objcopy READELF=$TC/bin/llvm-readelf
# Hardening flags
export CFLAGS="-O2 -fPIC -fstack-protector-strong -D_FORTIFY_SOURCE=2 -ffunction-sections -fdata-sections -I$PREFIX/include"
export CXXFLAGS="$CFLAGS"
export CPPFLAGS="-I$PREFIX/include"
export LDFLAGS="-L$PREFIX/lib -Wl,-z,relro,-z,now -Wl,--gc-sections -Wl,-z,max-page-size=16384"
export PKG_CONFIG_LIBDIR=$PREFIX/lib/pkgconfig
export PKG_CONFIG_PATH=
export JOBS=8
