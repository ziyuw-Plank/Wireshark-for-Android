#!/bin/bash
set -euo pipefail
cd /workspace/android-wireshark
tar --owner=0 --group=0 --numeric-owner -czf out/sharkdroid-source.tar.gz \
  --exclude='app-project/app/build' --exclude='app-project/.gradle' --exclude='app-project/build' \
  --exclude='app-project/app/src/main/jniLibs/arm64-v8a/*.so' --exclude='app-project/local.properties' \
  --exclude='helper/wsexec' --exclude='helper/wsexec-static' --exclude='helper/libsdnative.so' \
  app-project helper scripts patches stubs out/README.md out/SOURCES.txt
ls -la out/sharkdroid-source.tar.gz
