# F-Droid submission

This directory contains a **draft**, not a verified F-Droid build or an already-submitted merge request.

## Files

- `metadata/org.sharkdroid.yml`: copy this file into the same path in a fork of `fdroid/fdroiddata`.
- `merge-request.md`: draft MR description, linked to the existing RFP issue #4525.

The recipe pins the full upstream commit `2e99277c6114ff3deedd2ed9293a692f7df81a41`, which contains portable build scripts and Fastlane listing metadata. It builds every native component from the SHA256-pinned source archives in `SOURCES.txt`; no release APK or prebuilt native tool is used.

Preparation runs in `init` and deletes downloaded source archives before the source scan. Native compilation runs in `build`, after scanning. F-Droid's selected NDK is passed explicitly; Gradle then produces the release APK without upstream signing credentials.

## Validation still required

Local checks passed for Bash/Python syntax, all nine source manifest entries, deriving the checkout/NDK paths, missing-NDK errors, offline extraction, repeat preparation, and rejection of a modified source archive. A complete Android/native build has **not** been run in this environment: Android SDK/NDK, Gradle and host native build tools are absent. Tool versions, source patches, source scanning and the resulting APK still require verification in F-Droid's build environment. Do not tick the MR's successful-build checkbox until that test passes.

In a configured F-Droid environment:

```bash
cp /path/to/SharkDroid/fdroid/metadata/org.sharkdroid.yml metadata/
fdroid readmeta
fdroid rewritemeta org.sharkdroid
fdroid lint org.sharkdroid
fdroid build -v -l org.sharkdroid:2
```

Use the official buildserver/container instructions for host dependencies and the Android toolchain. Submit initially as a **Draft** MR if requesting help with build verification.

## Release and updates

The existing `v1.1.0` tag predates these build/metadata changes and is deliberately left intact. The draft recipe pins the new source commit while retaining Android versionCode 2; it does not claim to reproduce the signed APK attached to that old tag. Before finalizing the submission, make a tested release tag containing these changes, update the recipe's commit/version fields to that release, and enable automatic updates only once the recipe is validated.

Fastlane metadata is in `fastlane/metadata/android/` at the repository root. Text descriptions must not be duplicated in this YAML. Screenshots use demo data, disclosed in the descriptions.

F-Droid will sign its build. Installing it over an upstream APK with a different signing key will generally require uninstalling that APK first; export any captures you wish to keep before changing distributions.

Keep [RFP #4525](https://gitlab.com/fdroid/rfp/-/issues/4525); do not create a duplicate request.

References:
- https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- https://f-droid.org/docs/Build_Metadata_Reference/
