**English** | [简体中文](README.zh-CN.md)

# SharkDroid: Wireshark for rooted Android (arm64)

SharkDroid brings the capture and dissection core of **Wireshark 4.6.9** (the latest stable release, September 2026, with every upstream security fix up to that version) to Android: `dumpcap` and `tshark` with all protocol dissectors, cross-compiled with Android NDK r27c for arm64-v8a (Android 10 / API 29 and later). It comes with a phone-friendly Material 3 app, plus a command-line bundle you can use over adb / su.

> Wireshark has no official Android version. The desktop Qt GUI is not ported. The packet list, protocol tree and hex view are SharkDroid's own UI, but every dissection result comes from the real tshark.

**Status: pre-release.** The command-line tools have been verified on an Android emulator. The app's UI has been verified with JVM screenshot tests, but it has not yet been run on a real phone. Feedback is welcome.

<p>
<img src="docs/screenshots/01_main_light.png" width="240" alt="Packet list (light)">
<img src="docs/screenshots/02_main_dark.png" width="240" alt="Packet list (dark)">
<img src="docs/screenshots/08_packet_detail.png" width="240" alt="Packet detail with protocol tree and bytes">
</p>
<p>
<img src="docs/screenshots/04_interface_sheet.png" width="240" alt="Interface picker">
<img src="docs/screenshots/06_display_filter_valid.png" width="240" alt="Display filter with validation, history and suggestions">
<img src="docs/screenshots/11_statistics.png" width="240" alt="Statistics">
</p>

## Download

The repository only contains source code. The APK and the command-line bundle are on the [Releases](../../releases) page.

| File | What it is |
|---|---|
| `SharkDroid-<version>-wireshark-4.6.9-arm64-release.apk` | The app, self-signed release build (install this one) |
| `sharkdroid-cli-wireshark-4.6.9-android-arm64.tar.gz` | Command-line bundle: `tshark`, `dumpcap`, `capinfos`, `editcap`, `mergecap` and data files |
| `SOURCES.txt` | Download URL, signature / checksum verification, SHA256 and local patches for every upstream source |
| `SHA256SUMS` | Checksums of the release files |
| `sharkdroid-source.tar.gz` | Snapshot of this repository at the release tag |

The release APK is signed with a self-signed certificate, SHA-256 `72590f3238296885b7d1a2b9b600594350e5ea8477277e45fe4222985196f157`. You can check it with `apksigner verify --print-certs`. Every release is signed with the same certificate, so newer versions install over older ones.

## Installing the app

1. On the phone, enable USB debugging in Developer options. This is only needed for adb; you can also copy the APK to the phone and open it there.
2. On the computer: `adb install SharkDroid-<version>-wireshark-4.6.9-arm64-release.apk`
   - Some ROMs show a "security check" screen before installing. Choose to continue (the app has no internet permission).
3. Open SharkDroid. The first time, it asks for superuser access:
   - **Magisk**: tap "Allow" in the prompt ("Forever" is recommended).
   - **KernelSU / APatch**: enable root for SharkDroid in the manager app, then go back to SharkDroid and tap **Retry** on the banner.
4. On Android 13 and later, the app asks for the notification permission when you start a capture. It is used for the "capturing" notification with its **Stop** button. Capturing works without it.

You don't need `setenforce 0` or any SELinux policy change: dumpcap runs in the su domain of Magisk / KernelSU, which already allows packet capture.

## Using it

- **Interface**: tap the interface card at the top. The picker shows each interface's type (Wi-Fi, Cellular, VPN, Loopback, Any, …) and state (up, no address, down):
  - `wlan0`: Wi-Fi (Ethernet frames)
  - `rmnet_data0…N`: mobile data on Qualcomm devices (raw IP, **no Ethernet header**, dissected as DLT_RAW). With mobile data on, the active one is usually the `rmnet_dataX` that has an IP address. MediaTek devices use `ccmni*`.
  - `any`: all interfaces at once (Linux cooked SLL)
  - `lo`: loopback. `tun0`: the VPN tunnel when a VPN is active (you see the plain IP packets inside the VPN). `p2p0`, `rndis0`, `ncm0`, `bt-pan`: Wi-Fi Direct, USB tethering, Bluetooth tethering.
  - `rmnet_ipa0` and `r_rmnet_data*` are lower-level interfaces with QMAP headers; you normally don't want them.
  - The default choice is `wlan0` if Wi-Fi is connected, otherwise the active `rmnet_data*`, otherwise `any`.
- **Start capture**: the large button at the bottom right opens the "New capture" sheet. There you can set an optional **capture filter** (BPF, e.g. `tcp port 443`, `host 1.2.3.4`, `udp port 53`, `not port 5555`) or pick one of the suggestions. While capturing, the button turns red and shows **Stop** with the elapsed time. The bar above the list shows packets, data and duration.
- **Display filter**: the search bar uses Wireshark syntax, e.g. `dns`, `tls.handshake.extensions_server_name contains "example"`, `http.request`, `ip.addr == 1.2.3.4`, `tcp.analysis.flags`. As you type, tshark itself checks the filter and the bar turns green (valid) or red (invalid, with tshark's error message), just like in Wireshark. Recent filters and common suggestions are listed below the bar. Press search on the keyboard to apply; the file is then re-dissected from the start.
- **Packet list**: protocol colour coding inspired by Wireshark's colouring rules, with monospace numbers. The list follows new packets automatically. Scroll up to pause; tap the arrow button or the auto-scroll toggle to resume. Long-press a packet to filter on its protocol or addresses, or to copy the row.
- **Packet detail**: tap a packet to see the expandable protocol tree, with the packet bytes in a bottom sheet (drag it up for more). Tapping a field highlights its bytes. Long-press a field for **Apply as filter**, **Copy as filter**, **Copy value** and **Copy line**. The top bar has **Follow TCP/UDP stream**.
- **Overflow menu (⋮)**:
  - **Open file**: open a pcap / pcapng file through the system picker (no root needed).
  - **Saved captures**: open, share, export or delete. Swipe left to delete; **Undo** is available in the snackbar.
  - **Statistics**: summary cards, protocol distribution, and tshark reports (protocol hierarchy, IPv4/IPv6/TCP/UDP conversations, endpoints, expert info, DNS queries, TLS SNI, HTTP requests, DNS statistics, I/O per second).
  - **Export**, **Share**, **Clear packet list**
  - **Stream to desktop Wireshark**
  - **Diagnostics**: root status, sandbox status, `dumpcap -D`, the interface table and the last dumpcap output.
  - **Settings**: theme (system / light / dark), Material You dynamic color, snapshot length, auto-scroll, plus the open-source licenses.
- You can switch to other apps while capturing. A foreground service keeps the capture running and the notification has a **Stop** button. Pressing back never stops a running capture.
- Captures are stored in **app-private storage** (`/data/data/org.sharkdroid/files/captures/`). They only leave the app when you **export** them (system "Save as") or **share** them. A capture stops automatically at 2 GiB.
- The UI is available in English and Simplified Chinese and follows the system language (Android 13+ also lets you set a per-app language).

## Live view in Wireshark on your computer

Connect the phone by USB with USB debugging enabled and run this on the computer. **Stream to desktop Wireshark** in the app gives you the exact command with the real path, ready to copy:

```bash
adb exec-out "su -c '<app native library dir>/libdumpcap.so -i wlan0 -F pcapng -q -w -'" | wireshark -k -i -
```

With only the command-line bundle (no app):

```bash
adb push sharkdroid-cli-wireshark-4.6.9-android-arm64.tar.gz /data/local/tmp/
adb shell "cd /data/local/tmp && tar xzf sharkdroid-cli-wireshark-4.6.9-android-arm64.tar.gz"
adb exec-out "su -c '/data/local/tmp/sharkdroid-cli-wireshark-4.6.9-android-arm64/bin/dumpcap -i wlan0 -F pcapng -q -w -'" | wireshark -k -i -
```

On Windows, PowerShell pipes corrupt binary data. Use **cmd.exe** and replace `wireshark` with `"C:\Program Files\Wireshark\Wireshark.exe"`. Data only travels over the authorized adb connection; the app never opens a network port.

## Command-line bundle (adb shell → su)

```sh
su
cd /data/local/tmp/sharkdroid-cli-wireshark-4.6.9-android-arm64
sh ws.sh dumpcap -D                                        # list interfaces
sh ws.sh dumpcap -i wlan0 -a duration:30 -w /data/local/tmp/a.pcapng
sh ws.sh tshark -n -r /data/local/tmp/a.pcapng -Y dns
sh ws.sh tshark -n -i rmnet_data0 -f "udp port 53"         # tshark captures directly (runs dumpcap from the same dir)
```

The binaries only depend on the system `libc.so`; everything else is statically linked. `ws.sh` sets `WIRESHARK_DATA_DIR`. Clean up with `rm -rf /data/local/tmp/sharkdroid-cli-*`.

## Known limitations

- **Monitor mode** is not supported. Phone Wi-Fi chips use vendor drivers that don't offer standard nl80211 monitor mode. Our libpcap is built without libnl and the app never touches driver parameters. Vendor-specific tricks such as `echo 4 > /sys/module/wlan/parameters/con_mode` disconnect Wi-Fi and may need a reboot to recover; they are untested and not recommended. `wlan0` therefore shows the Ethernet frames the phone itself sends and receives, not over-the-air 802.11 frames.
- **Cellular interfaces**: `rmnet_data*` are raw-IP interfaces (ARPHRD_RAWIP). A libpcap patch makes them open as DLT_RAW so BPF filters and dissection are correct. The code has been reviewed, but the emulator has no rmnet device, so it **has not been run on a real cellular connection yet**. If you hit a problem, use the `any` interface instead (cooked format, dissects fine and includes the direction).
- **Encrypted TLS/QUIC** traffic: you see the handshake (SNI, certificates, …) but not the content, because the key logs of browsers and apps are not available on the phone. GnuTLS, Kerberos, Lua, nghttp2, zstd, lz4, brotli and snappy are not built in, so the matching decryption, decompression and Lua plugin features are unavailable.
- There is no Qt GUI and no graphical statistics such as I/O graphs. Statistics reports are shown as tshark text.
- Packet details come from a single pass that reads up to that packet, so links to later packets ("[Response in: N]") may be missing. Changing the display filter during a live capture re-dissects the whole file.
- The list shows up to 1,000,000 rows; export larger captures to a computer.
- The first detail view or statistics report starts tshark once (about 3,000 dissectors), which takes roughly a second on a phone. Display filter validation also uses tshark, so the green/red result appears after a short delay.
- arm64-v8a only.

## Security design

- **Sources**: everything comes from official upstreams only: wireshark.org, tcpdump.org, download.gnome.org, gnupg.org, official GitHub releases (c-ares, PCRE2, libffi) and Google's official NDK. All versions are pinned.
  - GPG signatures are verified where published (Wireshark, libpcap, libgcrypt, libgpg-error, c-ares, PCRE2).
  - GNOME tarballs are checked against the official sha256sum files.
  - The NDK is checked against the SHA1 in Google's repository manifest.
  - libffi publishes no signatures, so only its SHA256 is pinned.
  - No third-party prebuilt binaries (not even Termux packages) are used; every dependency is built from source. See `SOURCES.txt`.
- **Minimal permissions**: no INTERNET permission. The app never goes online and has no analytics, telemetry or ads. tshark always runs with `-n` (no reverse DNS). Requested permissions:
  - `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`, to keep a capture running
  - optional `POST_NOTIFICATIONS`

  The EmojiCompat downloadable-font initializer and the exported profile-installer receiver that AndroidX would add are removed from the manifest.
- **Root only for dumpcap**: the only string ever passed to `su -c` is a fixed path: the native library path assigned by the package manager, checked against a character whitelist and single-quoted. User input (interface name, filter) goes over stdin to the root helper `libwsexec.so` (about 300 lines of C, source in `helper/wsexec.c`), NUL-separated with an argument count. The helper:
  - validates the interface name (`[A-Za-z0-9_.-]`, at most 15 characters, no leading `-`, must exist in `/sys/class/net`), which rejects URLs such as `rpcap://`;
  - validates the capture filter (printable ASCII only, at most 2048 characters) and the snaplen (numeric, within range);
  - clears the environment and sets `umask 077`;
  - only runs a `libdumpcap.so` that sits in its own directory, is owned by root or system, and is not group- or world-writable;
  - starts dumpcap with `fork + execv` and a fixed argument array. No shell and no command-string concatenation are involved at any point.
- **Untrusted data is dissected in a sandbox**: Wireshark fixes a batch of dissector vulnerabilities every year. tshark therefore runs neither as root nor as the app's own UID, but in an `android:isolatedProcess` service. That process has a random UID, no permissions and no access to app data, so it can't call su. Packet data reaches it through a pipe or a read-only file descriptor, so even a malicious pcap that exploits a dissector can't get root. Display filter validation runs in the same sandbox. If a ROM's SELinux policy forbids exec from isolated processes, the app falls back to its own UID (still not root) and **Diagnostics** clearly shows "sandbox unavailable".
- **Files**: captures are written to app-private storage with mode 0600, never world-readable or writable. They only leave the app when you export or share them; sharing uses a non-exported, read-only ContentProvider with a one-time URI grant. Backup is disabled (`allowBackup=false`, and both cloud backup and device transfer are excluded).
- **Components**: only the launcher activity is exported (a launcher must be). All services and providers are `exported=false`. The app doesn't accept files from other apps' intents, so nobody can push a malicious pcap into it.
- **Hardened native builds**: every native binary is built with:
  - PIE and Full RELRO (BIND_NOW)
  - `-fstack-protector-strong`, `_FORTIFY_SOURCE=2` and `-fstack-clash-protection`
  - `-mbranch-protection=standard` (PAC-signed return addresses)
  - 16 KB page alignment (Android 15 compatible)

  The binaries only link the system `libc.so` dynamically, so they pick up bionic security fixes with system updates.
- **Things to keep in mind**: opening a pcap of unknown origin is inherently risky (on desktop Wireshark too). On a rooted phone, the security boundary is your root manager, so only grant root to apps that need it. The release APK is self-signed (certificate SHA-256 above); always upgrade with an APK signed by the same certificate.

## Building from source

The repository has all the scripts: `scripts/env.sh`, `build-deps-1.sh`, `build-glib.sh`, `build-deps-2.sh`, `build-wireshark.sh`, `helper/build.sh` and `package-cli.sh`.

The app lives in `app-project/` and is built with `gradle assembleRelease`. It is Kotlin with Jetpack Compose and Material 3, and needs:

- JDK 17
- Gradle 9.6.0. The repository has no `gradle-wrapper.jar`; generate one with `gradle wrapper --gradle-version 9.6.0`.
- Android SDK platform 37
- NDK r27c

Before building the app, run `scripts/copy-jnilibs.sh` to put the compiled tshark / dumpcap into `jniLibs`. tshark is larger than 100 MB, so it is not in git.

Release signing is read from an untracked properties file that the `SHARKDROID_KEYSTORE_PROPS` environment variable points to. Without that file, the release APK is left unsigned.

`patches/` holds just three small patches:

- lemon uses the host template
- bionic is missing `<net/if.h>`
- libpcap maps ARPHRD_RAWIP to DLT_RAW

To regenerate the UI screenshots (Robolectric + Roborazzi, fake data, no device needed):

```bash
cd app-project && gradle :app:testDebugUnitTest --tests 'org.sharkdroid.ScreenshotTest' -PscreenshotDir=$PWD/../docs/screenshots
```

"Wireshark" and the Wireshark fin logo are trademarks of the Wireshark Foundation. SharkDroid is an independent port, not affiliated with or endorsed by the Wireshark Foundation, and its icon is original artwork.
