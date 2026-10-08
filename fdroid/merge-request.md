# Draft: New App: org.sharkdroid

SharkDroid provides an Android interface for Wireshark's dumpcap and tshark tools. Live packet capture requires root; opening existing pcap/pcapng files does not. The app supports arm64-v8a devices running Android 10 or later.

Related RFP: https://gitlab.com/fdroid/rfp/-/issues/4525
Upstream: https://github.com/ziyuw-Plank/Wireshark-for-Android

The build recipe compiles the native dependencies and tools from official source archives with SHA256 pins from upstream SOURCES.txt. Source preparation and patches happen before scanning; native compilation happens in the build stage. There are no downloaded APKs or prebuilt native libraries. The upstream app includes English and Simplified Chinese Fastlane metadata; screenshots are rendered from demo data and labeled accordingly.

The author reports successful testing of the existing app on a real Android phone. This does not constitute verification of this new F-Droid build recipe.

Validation completed: shell/Python syntax, source-manifest validation, portable environment setup, offline extraction, repeat preparation and checksum mismatch rejection.
Validation pending: a complete `fdroid build`, native source patches, source scanning and APK verification in the official build environment. Please keep this MR in Draft until these checks pass.

The recipe pins upstream commit 2e99277c6114ff3deedd2ed9293a692f7df81a41. The existing release tag predates the packaging changes; a tested release containing them is needed before finalizing inclusion. Automatic updates are disabled pending validation.

- [x] Existing RFP referenced
- [x] Upstream store metadata present
- [ ] Complete F-Droid build verified
- [ ] Tested release tag containing the packaging changes
