# F-Droid listing metadata

The store listing lives in `metadata/android/<locale>/`, using F-Droid's supported Fastlane layout. No Fastlane installation, Ruby setup, or Google Play credentials are needed.

- `en-US` is the fallback locale; `zh-CN` is Simplified Chinese.
- Keep titles within 50 characters, short descriptions within 80 characters, and full descriptions within 4000 characters.
- Changelog filenames must match Android `versionCode` (currently `2`), and each entry must stay within 500 characters.
- PNG screenshots belong in `images/phoneScreenshots/`. Existing images are copied from `docs/screenshots/` and use demo data, as disclosed in both descriptions. Refresh these copies when regenerating the source screenshots.
- Include metadata updates in the next release tag: F-Droid reads metadata from the release it builds, rather than automatically using the current default branch.

Documentation: https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/
