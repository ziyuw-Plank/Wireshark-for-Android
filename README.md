> **English summary:** SharkDroid is a port of Wireshark 4.6.9 (`dumpcap` + `tshark` with all dissectors) to rooted arm64 Android (API 29+), plus a small phone UI: live capture on Wi‑Fi / cellular (`rmnet_data*`, raw IP) / VPN / `any`, BPF capture filters, Wireshark display filters, packet detail tree + hex view, statistics, follow stream, and pcapng export/share. Only `dumpcap` runs as root (via a tiny validating helper, no shell); `tshark` runs in an isolated sandbox process; the app has no INTERNET permission. Everything is built from verified official upstream sources (see `SOURCES.txt`). **Status: pre-release.** The CLI tools are verified on an Android emulator; the app has not yet been tested on a real device. Binaries are on the [Releases](../../releases) page. License: GPL-2.0-or-later.

# SharkDroid：Android 版 Wireshark（小米 14 Pro / arm64，需要 root）

这是 **Wireshark 4.6.9**（2026-09 发布的最新稳定版，含官方截至该版本的全部安全修复）里抓包和解析的核心（`dumpcap` + `tshark`，全部协议解析器），用 Android NDK r27c 交叉编译到 arm64-v8a（API 29+），外面套了一个手机界面 App。另外附带可以通过 adb / su 手动使用的命令行版。

> Wireshark 官方没有 Android 版。这里没有移植桌面 Qt 图形界面，手机上的包列表、详情树和十六进制视图是本 App 自己做的，解析结果全部来自真正的 tshark。

## 下载（见 Releases 页面）

仓库里只有源码，编译好的 APK 和命令行包在 GitHub Releases 里。

| 文件 | 说明 |
|---|---|
| `SharkDroid-1.0.0-wireshark-4.6.9-arm64-release.apk` | App，自签名 release 版（推荐安装这个） |
| `SharkDroid-1.0.0-wireshark-4.6.9-arm64-debug.apk` | 同样的代码，debug 签名（调试用，和 release 不能互相覆盖安装） |
| `sharkdroid-cli-wireshark-4.6.9-android-arm64.tar.gz` | 命令行版：`tshark`、`dumpcap`、`capinfos`、`editcap`、`mergecap` 和数据文件 |
| `SOURCES.txt` | 所有上游源码的下载地址、签名或校验结果、SHA256 和本地补丁列表 |
| `SHA256SUMS` | 本目录所有交付文件的校验值 |
| `sharkdroid-source.tar.gz` | 本项目全部源码的快照（和本仓库内容相同） |

## 安装 App

1. 手机：设置 → 更多设置 → 开发者选项 → 打开 USB 调试（只是为了用 adb 安装；直接把 APK 传到手机上点开安装也可以）。
2. 电脑：`adb install SharkDroid-1.0.0-wireshark-4.6.9-arm64-release.apk`
   - HyperOS 可能弹出“安全检查 / 纯净模式”提示，选择继续安装即可（App 没有联网权限）。
3. 打开 SharkDroid。第一次打开时 App 会请求超级用户权限：
   - **Magisk**：弹出授权窗口时点“允许”（建议选“永久”）。
   - **KernelSU / APatch**：在管理器 App 的超级用户列表里给 SharkDroid 打开 root 开关，然后回到 SharkDroid 点 ↻ 重试。
   - 想关掉每次授权的提示：在 Magisk 设置里把 SharkDroid 的通知 / 日志关掉。
4. Android 13 及以上开始抓包时，会请求“通知”权限，用来显示“正在抓包 / 停止”通知。拒绝也能抓包。

不需要 `setenforce 0`，也不需要改 SELinux 策略：dumpcap 在 Magisk/KernelSU 自带的 su 域里运行，这个域本来就允许抓包。

## 使用

- **接口**（顶部下拉框，● = 已启用且有 IP，○ = 未启用）：
  - `wlan0`：Wi‑Fi（以太网帧）
  - `rmnet_data0…N`：蜂窝数据（高通平台，原始 IP，**没有以太网头**，按 DLT_RAW 解析）。手机开着流量时，活跃的通常是有 IP 的那个 `rmnet_dataX`
  - `any`：所有接口一起抓（Linux cooked SLL 格式）
  - `lo`：本机回环；`tun0`：开着 VPN 时的隧道（看到的是 VPN 里面的明文 IP 包）；`p2p0`、`rndis0`、`ncm0`、`bt-pan`：Wi‑Fi 直连 / USB 共享 / 蓝牙共享
  - `rmnet_ipa0`、`r_rmnet_data*` 是底层带 QMAP 头的接口，一般不要选
  - App 默认选：Wi‑Fi 已连接就选 `wlan0`，没连就选正在使用的 `rmnet_data*`，都没有就选 `any`
- **捕获过滤器**（BPF，开始前设置），例：`tcp port 443`、`host 1.2.3.4`、`udp port 53`、`not port 5555`
- **显示过滤器**（Wireshark 语法，随时可以改，点“应用”后从头重新解析），例：`dns`、`tls.handshake.extensions_server_name contains "qq"`、`http.request`、`ip.addr == 1.2.3.4`、`tcp.analysis.flags`
- 点一个包：协议详情树 + 十六进制视图（点树里的字段会高亮对应字节）。长按字段：设为显示过滤器 / 复制。右上角菜单：追踪 TCP/UDP 流。
- 主界面菜单：已保存的抓包（打开、导出、分享、删除）、导出当前文件、分享、**统计**（协议分级、IPv4/IPv6/TCP/UDP 会话、端点、专家信息、DNS 查询列表、TLS SNI 列表、HTTP 请求、IO 统计）、推送到电脑、诊断（显示 root 状态、沙箱状态、`dumpcap -D`、最近一次 dumpcap 输出）。
- “打开”：通过系统文件选择器打开已有的 pcap / pcapng（也支持 .gz）。打开文件不需要 root。
- 抓包时可以切到别的 App 去产生流量。前台服务会让抓包继续，通知栏里有“停止”按钮。
- 抓包文件保存在 **App 私有目录**（`/data/data/org.sharkdroid/files/captures/`），只有你点“导出”（用系统“另存为”，可选“下载”目录）或“分享”时才会离开 App。单个文件到 2 GiB 自动停止。

## 推送到电脑上的 Wireshark 实时查看

用 USB 连电脑并打开 USB 调试，在电脑上运行（App 菜单“推送到电脑 Wireshark”会给出带本机实际路径的命令，可以一键复制）：

```bash
adb exec-out "su -c '<App 原生库目录>/libdumpcap.so -i wlan0 -F pcapng -q -w -'" | wireshark -k -i -
```

不装 App、只用命令行版：

```bash
adb push sharkdroid-cli-wireshark-4.6.9-android-arm64.tar.gz /data/local/tmp/
adb shell "cd /data/local/tmp && tar xzf sharkdroid-cli-wireshark-4.6.9-android-arm64.tar.gz"
adb exec-out "su -c '/data/local/tmp/sharkdroid-cli-wireshark-4.6.9-android-arm64/bin/dumpcap -i wlan0 -F pcapng -q -w -'" | wireshark -k -i -
```

Windows 下 PowerShell 的管道会破坏二进制数据，请在 **cmd.exe** 里运行，并把 `wireshark` 换成 `"C:\Program Files\Wireshark\Wireshark.exe"`。数据只走已授权的 adb 通道，App 不会开放任何网络端口。

## 命令行版用法（adb shell → su）

```sh
su
cd /data/local/tmp/sharkdroid-cli-wireshark-4.6.9-android-arm64
sh ws.sh dumpcap -D                                        # 列出接口
sh ws.sh dumpcap -i wlan0 -a duration:30 -w /data/local/tmp/a.pcapng
sh ws.sh tshark -n -r /data/local/tmp/a.pcapng -Y dns
sh ws.sh tshark -n -i rmnet_data0 -f "udp port 53"         # tshark 直接抓（会调用同目录的 dumpcap）
```

二进制只依赖系统 `libc.so`（其余依赖都已静态链接）；`ws.sh` 负责设置 `WIRESHARK_DATA_DIR`。用完可以 `rm -rf /data/local/tmp/sharkdroid-cli-*`。

## 已知限制

- **监听模式（monitor mode）**：不支持。小米 14 Pro 用的是高通 FastConnect 7800 的原厂驱动，不提供标准的 nl80211 监听模式。我们编的 libpcap 没有带 libnl，App 也不会去改驱动参数。网上有 `echo 4 > /sys/module/wlan/parameters/con_mode` 之类的高通私有方法，但会断开 Wi‑Fi，可能要重启才能恢复，我们没有测试，也不推荐。所以 `wlan0` 抓到的是本机自己收发的以太网帧，不是空口 802.11 帧。
- **蜂窝接口**：`rmnet_data*` 是原始 IP 接口（ARPHRD_RAWIP）。我们给 libpcap 加了补丁，让它按 DLT_RAW 打开，这样 BPF 过滤器和解析都正确。这条代码在电脑上做过审查，但模拟器里没有 rmnet 设备，**还没在真机蜂窝网络上跑过**。如果你遇到问题，可以选 `any` 接口作为备用方案（cooked 格式，同样能解析，还带方向信息）。
- **TLS/QUIC 加密流量**只能看到握手信息（SNI、证书等），看不到内容：手机上拿不到浏览器或 App 的密钥日志。没有编进 GnuTLS / Kerberos / Lua / nghttp2 / zstd / lz4 / brotli / snappy 等可选依赖，相应的解密、解压和 Lua 插件功能不可用。
- 没有 Qt 图形界面，也没有 IO 图表这类图形化统计，统计结果都以 tshark 文本形式显示。
- 包详情是单遍解析（只读到该包为止），所以“[Response in: N]”这类指向后面包的链接可能不显示。显示过滤器在实时抓包时会从头重新解析整个文件。
- 列表最多显示 100 万行，超过的部分请导出到电脑分析。
- 第一次打开详情或统计时要启动一次 tshark（加载约 3000 个解析器），手机上大约需要 1 秒。
- 只支持 arm64-v8a（小米 14 Pro、近几年的高通/联发科手机都是）。

## 安全设计

- **源码来源**：只从官方上游下载：wireshark.org、tcpdump.org、download.gnome.org、gnupg.org、GitHub 官方 release（c-ares、PCRE2、libffi）、Google 官方 NDK。版本全部固定。有 GPG 签名的（Wireshark、libpcap、libgcrypt、libgpg-error、c-ares、PCRE2）都验证了签名；GNOME 的包对照了官方 sha256sum；NDK 对照了 Google 仓库清单里的 SHA1；libffi 上游不发布签名，只固定了 SHA256。没有使用任何第三方预编译二进制（Termux 的包也没用），全部依赖都是从源码编译的。详见 `SOURCES.txt`。
- **最少权限**：没有 INTERNET 权限，App 完全不联网，没有统计、遥测或广告。tshark 一律带 `-n` 运行，不做 DNS 反查。只申请了 `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_SPECIAL_USE`（抓包时保持运行）和可选的 `POST_NOTIFICATIONS`。
- **root 只给 dumpcap**：交给 `su -c` 的只有一个固定字符串，就是包管理器分配的原生库路径（经过白名单字符校验并加单引号）。接口名、过滤器等用户输入通过 stdin 以 NUL 分隔、带参数个数的格式传给 root 辅助程序 `libwsexec.so`（约 300 行 C，源码在 `helper/wsexec.c`）。它会：
  - 校验接口名：`[A-Za-z0-9_.-]`，不超过 15 个字符，不能以 `-` 开头，并且必须真实存在于 `/sys/class/net`，所以拒绝 `rpcap://` 这类 URL；
  - 校验捕获过滤器：只允许可打印 ASCII，最长 2048；snaplen 只能是数字且在范围内；
  - 清空环境变量、设置 `umask 077`；
  - 只执行和自己在同一目录、属主是 root 或 system、且不可被组和其他用户写的 `libdumpcap.so`；
  - 用 `fork + execv` 和固定的参数数组启动 dumpcap，全程不经过 shell，也不拼接命令字符串。
- **不可信数据在沙箱里解析**：Wireshark 解析器每年都会修一批漏洞，所以 tshark 不以 root 运行，也不以 App 自己的 UID 运行，而是放在 `android:isolatedProcess` 的隔离进程里：随机 UID、没有任何权限、读不到 App 数据，因此调用不了 su。数据通过管道或只读文件描述符交给它。即使有人用恶意 pcap 打穿了某个解析器，也拿不到 root。如果某个 ROM 的 SELinux 策略不允许隔离进程执行程序，App 会自动回退到用 App UID 运行（仍然不是 root），并在“诊断”里明确显示“沙箱不可用”。
- **文件**：抓包写在 App 私有目录，权限 0600，不是全局可读写；只有用户主动导出或分享时才离开 App（分享用的是不导出的只读 ContentProvider 加一次性 URI 授权）。关闭了备份（`allowBackup=false`，云备份和设备迁移都排除）。
- **组件**：只有启动器 Activity 是导出的（launcher 必须导出），其余 Activity、Service、Provider 都是 `exported=false`。App 不接收外部 Intent 传进来的文件，避免别的 App 塞恶意 pcap 进来。
- **编译加固**：所有原生程序都是 PIE，启用了 Full RELRO（BIND_NOW）、`-fstack-protector-strong`、`_FORTIFY_SOURCE=2`、`-fstack-clash-protection`、ARM 分支保护编译选项（`-mbranch-protection=standard`，函数返回地址 PAC 签名），并按 16 KB 页对齐（兼容 Android 15）。只动态链接系统的 `libc.so`，所以能跟着系统更新拿到 bionic 的安全修复。
- **仍然要注意**：打开来源不明的 pcap 本身就有风险（桌面版 Wireshark 也一样）；有 root 的手机安全边界取决于你的 root 管理器，建议在 Magisk/KernelSU 里只给需要的 App 授权；release APK 是自签名的，签名证书 SHA-256 见交付报告。以后升级请用同一个签名的 APK 覆盖安装。

## 自己重新编译

仓库里有完整脚本：`scripts/env.sh`、`build-deps-1.sh`、`build-glib.sh`、`build-deps-2.sh`、`build-wireshark.sh`、`helper/build.sh`、`package-cli.sh`，App 在 `app-project/` 下用 `gradle assembleRelease` 编译（需要 JDK 17、Gradle 9.6.0、Android SDK 36、NDK r27c；仓库不含 gradle-wrapper.jar，可用 `gradle wrapper --gradle-version 9.6.0` 自行生成）。编译 App 之前先运行 `scripts/copy-jnilibs.sh`，把编好的 tshark/dumpcap 等放进 `jniLibs`（tshark 超过 100 MB，所以不放进 git）。release 签名配置从环境变量 `SHARKDROID_KEYSTORE_PROPS` 指向的、不纳入版本控制的 properties 文件读取；没有这个文件时 release 包不签名。补丁在 `patches/` 下，一共只有 3 个很小的补丁：lemon 用宿主机模板、bionic 缺少 `<net/if.h>`、libpcap 把 ARPHRD_RAWIP 映射到 DLT_RAW。

## 许可证

Wireshark、dumpcap、tshark：GPL-2.0-or-later。libpcap：BSD。GLib、libgcrypt、libgpg-error：LGPL-2.1-or-later。c-ares、libffi：MIT。PCRE2：BSD。libxml2：MIT。许可证全文在 CLI 包的 `licenses/` 目录里。本项目自己的代码（App、`wsexec`、`sdnative`）以 GPL-2.0-or-later 发布，对应源码就是本仓库。上游 GPL/LGPL 组件的原始源码包作为附件放在 Release 里，下载地址和 SHA256 见 `SOURCES.txt`。
