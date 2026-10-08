/*
 * wsexec - tiny privileged launcher for SharkDroid.
 *
 * Runs as root (started by the app via `su -c '<nativeLibraryDir>/libwsexec.so'`).
 * The command string given to su is a constant path controlled by the package
 * manager; ALL user-controlled data (interface name, capture filter) is passed
 * on stdin as NUL-separated tokens and is never interpreted by a shell.
 * After validation the request is executed with execv() with a fixed argv.
 *
 * Request format on stdin:  "WSX1\0<cmd>\0<argc>\0<arg1>\0...<argN>\0"
 *   id                          -> prints uid/selinux context
 *   list                        -> prints interface table (one line per iface)
 *   pcaplist                    -> exec dumpcap -D
 *   capture <iface> <filter> <snaplen>
 *                               -> fork/exec dumpcap -i IF [-f FILTER] -s N -w - -n -q
 *                                  pcapng goes to stdout; the capture is stopped with
 *                                  SIGTERM when stdin reaches EOF or a 'q' byte arrives.
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
#define _GNU_SOURCE
#include <arpa/inet.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <ifaddrs.h>
#include <limits.h>
#include <net/if.h>
#include <netinet/in.h>
#include <poll.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/prctl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#define MAX_REQ    8192
#define MAX_TOKENS 16
#define MAX_FILTER 2048

static char self_dir[PATH_MAX];
static char dumpcap_path[PATH_MAX];

static void die(const char *msg) {
    fprintf(stderr, "wsexec: %s\n", msg);
    exit(2);
}

static int valid_ifname(const char *s) {
    size_t n = strlen(s);
    if (n == 0 || n >= IFNAMSIZ) return 0;
    if (s[0] == '-' || s[0] == '.') return 0;
    for (size_t i = 0; i < n; i++) {
        char c = s[i];
        if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
              c == '_' || c == '-' || c == '.'))
            return 0;
    }
    return 1;
}

static int iface_exists(const char *s) {
    char p[PATH_MAX];
    struct stat st;
    if (strcmp(s, "any") == 0) return 1;
    snprintf(p, sizeof p, "/sys/class/net/%s", s);
    return stat(p, &st) == 0;
}

static int valid_filter(const char *s) {
    size_t n = strlen(s);
    if (n > MAX_FILTER) return 0;
    for (size_t i = 0; i < n; i++) {
        unsigned char c = (unsigned char)s[i];
        if (c < 0x20 || c > 0x7e) return 0; /* BPF syntax is printable ASCII */
    }
    return 1;
}

static int valid_snaplen(const char *s, long *out) {
    char *end;
    if (!*s || strlen(s) > 6) return 0;
    for (const char *p = s; *p; p++) if (*p < '0' || *p > '9') return 0;
    long v = strtol(s, &end, 10);
    if (*end || v < 64 || v > 262144) return 0;
    *out = v;
    return 1;
}

/* Only execute binaries that live next to us, are regular files, not writable by
 * group/other and owned by root or system (i.e. installed by the package manager). */
static void check_binary(const char *path) {
    struct stat st;
    if (lstat(path, &st) != 0) die("dumpcap not found next to helper");
    if (!S_ISREG(st.st_mode)) die("dumpcap is not a regular file");
    if (st.st_mode & (S_IWGRP | S_IWOTH)) die("dumpcap is group/world writable; refusing");
    if (st.st_uid != 0 && st.st_uid != 1000) die("dumpcap has unexpected owner; refusing");
}

static void read_sys(const char *ifn, const char *attr, char *buf, size_t len) {
    char p[PATH_MAX];
    snprintf(p, sizeof p, "/sys/class/net/%s/%s", ifn, attr);
    buf[0] = 0;
    int fd = open(p, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return;
    ssize_t r = read(fd, buf, len - 1);
    close(fd);
    if (r <= 0) { buf[0] = 0; return; }
    buf[r] = 0;
    for (ssize_t i = 0; i < r; i++) if (buf[i] == '\n' || buf[i] == '\t') { buf[i] = 0; break; }
}

static int cmd_list(void) {
    DIR *d = opendir("/sys/class/net");
    if (!d) { perror("opendir /sys/class/net"); return 1; }
    struct ifaddrs *ifa = NULL;
    getifaddrs(&ifa);
    struct dirent *de;
    while ((de = readdir(d)) != NULL) {
        if (de->d_name[0] == '.' || !valid_ifname(de->d_name)) continue;
        char type[32], oper[32], flags[32], carrier[8], p[PATH_MAX];
        struct stat st;
        read_sys(de->d_name, "type", type, sizeof type);
        read_sys(de->d_name, "operstate", oper, sizeof oper);
        read_sys(de->d_name, "flags", flags, sizeof flags);
        read_sys(de->d_name, "carrier", carrier, sizeof carrier);
        snprintf(p, sizeof p, "/sys/class/net/%s/wireless", de->d_name);
        int wl = stat(p, &st) == 0;
        snprintf(p, sizeof p, "/sys/class/net/%s/phy80211", de->d_name);
        wl |= stat(p, &st) == 0;
        /* IF <tab> name type operstate flags carrier wireless addrs(comma-separated) */
        printf("IF\t%s\t%s\t%s\t%s\t%s\t%d\t", de->d_name, type, oper, flags, carrier, wl);
        int first = 1;
        for (struct ifaddrs *a = ifa; a; a = a->ifa_next) {
            if (!a->ifa_addr || !a->ifa_name || strcmp(a->ifa_name, de->d_name) != 0) continue;
            char buf[INET6_ADDRSTRLEN] = {0};
            if (a->ifa_addr->sa_family == AF_INET)
                inet_ntop(AF_INET, &((struct sockaddr_in *)a->ifa_addr)->sin_addr, buf, sizeof buf);
            else if (a->ifa_addr->sa_family == AF_INET6)
                inet_ntop(AF_INET6, &((struct sockaddr_in6 *)a->ifa_addr)->sin6_addr, buf, sizeof buf);
            else continue;
            printf("%s%s", first ? "" : ",", buf);
            first = 0;
        }
        printf("\n");
    }
    closedir(d);
    if (ifa) freeifaddrs(ifa);
    return 0;
}

static int cmd_capture(char **tok, int ntok) {
    if (ntok != 5) die("capture: bad argument count");
    const char *ifn = tok[2], *filter = tok[3];
    long snap;
    if (!valid_ifname(ifn) || !iface_exists(ifn)) die("capture: invalid interface name");
    if (!valid_filter(filter)) die("capture: invalid capture filter (printable ASCII only, max 2048)");
    if (!valid_snaplen(tok[4], &snap)) die("capture: invalid snaplen");
    check_binary(dumpcap_path);

    char snapbuf[16];
    snprintf(snapbuf, sizeof snapbuf, "%ld", snap);
    const char *argv[16];
    int i = 0;
    argv[i++] = dumpcap_path; /* full path as argv[0] */
    argv[i++] = "-i"; argv[i++] = ifn;
    if (filter[0]) { argv[i++] = "-f"; argv[i++] = filter; }
    argv[i++] = "-s"; argv[i++] = snapbuf;
    argv[i++] = "-F"; argv[i++] = "pcapng";
    argv[i++] = "-q";
    argv[i++] = "-w"; argv[i++] = "-";
    argv[i] = NULL;

    pid_t pid = fork();
    if (pid < 0) die("fork failed");
    if (pid == 0) {
        prctl(PR_SET_PDEATHSIG, SIGTERM);
        int nul = open("/dev/null", O_RDONLY | O_CLOEXEC);
        if (nul >= 0) { dup2(nul, 0); }
        execv(dumpcap_path, (char *const *)argv);
        perror("wsexec: execv dumpcap");
        _exit(127);
    }
    /* Parent: don't keep stdout busy; wait for stop request or child exit. */
    signal(SIGPIPE, SIG_IGN);
    int status = 0, stopping = 0;
    time_t stop_at = 0;
    for (;;) {
        pid_t w = waitpid(pid, &status, WNOHANG);
        if (w == pid) break;
        if (stopping && time(NULL) - stop_at > 4) { kill(pid, SIGKILL); }
        struct pollfd pfd = { .fd = 0, .events = POLLIN };
        int pr = poll(&pfd, 1, stopping ? 200 : 500);
        if (pr > 0 && !stopping) {
            char c[64];
            ssize_t r = read(0, c, sizeof c);
            int q = (r <= 0);
            for (ssize_t k = 0; k < r; k++) if (c[k] == 'q') q = 1;
            if (q) { kill(pid, SIGTERM); stopping = 1; stop_at = time(NULL); }
        }
    }
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    return 128 + (WIFSIGNALED(status) ? WTERMSIG(status) : 0);
}

int main(void) {
    umask(077);
    /* Minimal, known environment for anything we exec as root. */
    clearenv();
    setenv("PATH", "/system/bin", 1);
    setenv("HOME", "/data/local/tmp", 1);

    ssize_t n = readlink("/proc/self/exe", self_dir, sizeof self_dir - 1);
    if (n <= 0) die("readlink /proc/self/exe failed");
    self_dir[n] = 0;
    char *slash = strrchr(self_dir, '/');
    if (!slash) die("bad self path");
    *slash = 0;
    snprintf(dumpcap_path, sizeof dumpcap_path, "%s/libdumpcap.so", self_dir);

    /* Read request: "WSX1\0<cmd>\0<argc>\0<arg>\0..." (exactly argc args, which may be
     * empty strings). Read byte-wise so that we never consume the stop byte that may
     * follow the request. */
    static char req[MAX_REQ];
    size_t len = 0;
    char *tok[MAX_TOKENS];
    int ntok = 0, want = 3;
    size_t tok_start = 0;
    while (ntok < want) {
        if (len >= sizeof req) die("request too large");
        ssize_t r = read(0, &req[len], 1);
        if (r <= 0) die("unexpected EOF in request");
        len++;
        if (req[len - 1] != 0) continue;
        if (ntok >= MAX_TOKENS) die("too many tokens");
        tok[ntok++] = &req[tok_start];
        tok_start = len;
        if (ntok == 1 && strcmp(tok[0], "WSX1") != 0) die("bad request magic");
        if (ntok == 3) {
            const char *a = tok[2];
            if (strlen(a) != 1 || a[0] < '0' || a[0] > '8') die("bad argc");
            want = 3 + (a[0] - '0');
        }
    }
    /* Re-pack as: tok[0]=magic, tok[1]=cmd, tok[2..]=args (drop the argc token). */
    for (int k = 2; k + 1 < ntok; k++) tok[k] = tok[k + 1];
    ntok--;
    if (ntok < 2 || strcmp(tok[0], "WSX1") != 0) die("bad request magic");
    const char *cmd = tok[1];

    if (strcmp(cmd, "id") == 0) {
        char ctx[256] = "?";
        int fd = open("/proc/self/attr/current", O_RDONLY | O_CLOEXEC);
        if (fd >= 0) { ssize_t r = read(fd, ctx, sizeof ctx - 1); if (r > 0) ctx[r] = 0; else strcpy(ctx, "?"); close(fd); }
        for (char *p = ctx; *p; p++) if (*p == '\n' || *p == '\0') *p = 0;
        printf("uid=%d euid=%d ctx=%s\n", getuid(), geteuid(), ctx);
        return 0;
    }
    if (strcmp(cmd, "list") == 0) return cmd_list();
    if (strcmp(cmd, "pcaplist") == 0) {
        check_binary(dumpcap_path);
        execl(dumpcap_path, dumpcap_path, "-D", (char *)NULL);
        perror("wsexec: execl dumpcap");
        return 127;
    }
    if (strcmp(cmd, "capture") == 0) return cmd_capture(tok, ntok);
    die("unknown command");
    return 2;
}
