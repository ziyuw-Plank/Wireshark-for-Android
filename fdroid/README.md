# F-Droid submission

- Merge request: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/51734 (RFP: https://gitlab.com/fdroid/rfp/-/issues/4525)
- Recipe: [`metadata/org.sharkdroid.yml`](metadata/org.sharkdroid.yml), the same file as `metadata/org.sharkdroid.yml` in fdroiddata.

## Reproducible build

F-Droid builds the app from the release tag and compares the result with the APK that is attached
to the GitHub release (`Binaries:`). It publishes the upstream-signed APK only if the two match
byte for byte once the signature is copied over (`AllowedAPKSigningKeys`: certificate SHA-256
`72590f3238296885b7d1a2b9b600594350e5ea8477277e45fe4222985196f157`).

The native build does not depend on the checkout or NDK path:

- `-ffile-prefix-map` maps `$ROOT` and `$NDK` to fixed paths;
- glib is configured with the fixed prefix `/sharkdroid/prefix` and installed with `DESTDIR` (glib compiles its locale and module directories into the binary);
- tshark and dumpcap are linked with `--build-id=none` (the build ID hashes debug info that is stripped afterwards).

Starting with v1.1.1, every release APK is built exactly like the recipe does it. The build runs on
Debian trixie with NDK r27c, Gradle 9.6.0 and JDK 17. Then the unsigned APK is signed without
changing its contents:

```bash
python3 scripts/prepare-sources.py --discard-archives
NDK=/path/to/android-ndk-r27c bash scripts/build-native.sh
(cd app-project && gradle :app:assembleRelease)    # with no keystore configured -> app-release-unsigned.apk
apksigner sign --alignment-preserved --ks <release keystore> \
  --out SharkDroid-<version>.apk app-project/app/build/outputs/apk/release/app-release-unsigned.apk
```

Building v1.1.1 in two different directories gave bit-identical unsigned APKs. After its
signature was copied over, fdroidserver's `verify_apks` reported a match.

## Updates

`UpdateCheckMode: Tags` together with `AutoUpdateMode: Version` follows tags of the form `vX.Y.Z`.
Each tagged release attaches `SharkDroid-X.Y.Z.apk`, which is the `Binaries:` URL pattern.
Fastlane metadata is in `fastlane/metadata/android/`.
