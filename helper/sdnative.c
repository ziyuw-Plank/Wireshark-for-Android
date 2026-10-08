/*
 * sdnative - minimal JNI helpers for SharkDroid: spawn a program with explicit
 * stdin/stdout/stderr file descriptors (no shell), wait for it, signal it.
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
#include <errno.h>
#include <jni.h>
#include <signal.h>
#include <spawn.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

static char **to_cstr_array(JNIEnv *env, jobjectArray arr) {
    jsize n = (*env)->GetArrayLength(env, arr);
    char **out = calloc((size_t)n + 1, sizeof(char *));
    if (!out) return NULL;
    for (jsize i = 0; i < n; i++) {
        jstring s = (jstring)(*env)->GetObjectArrayElement(env, arr, i);
        const char *c = s ? (*env)->GetStringUTFChars(env, s, NULL) : NULL;
        out[i] = strdup(c ? c : "");
        if (c) (*env)->ReleaseStringUTFChars(env, s, c);
        if (s) (*env)->DeleteLocalRef(env, s);
    }
    out[n] = NULL;
    return out;
}

static void free_cstr_array(char **a) {
    if (!a) return;
    for (char **p = a; *p; p++) free(*p);
    free(a);
}

JNIEXPORT jint JNICALL
Java_org_sharkdroid_NativeSpawn_spawn(JNIEnv *env, jclass clazz, jstring jpath, jobjectArray jargv,
                                      jobjectArray jenvp, jint fd_in, jint fd_out, jint fd_err) {
    (void)clazz;
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    char **argv = to_cstr_array(env, jargv);
    char **envp = to_cstr_array(env, jenvp);
    int rc = -ENOMEM;
    pid_t pid = -1;
    posix_spawn_file_actions_t fa;
    posix_spawnattr_t attr;
    if (!path || !argv || !envp) goto out;

    posix_spawn_file_actions_init(&fa);
    posix_spawn_file_actions_adddup2(&fa, fd_in, 0);
    posix_spawn_file_actions_adddup2(&fa, fd_out, 1);
    posix_spawn_file_actions_adddup2(&fa, fd_err, 2);

    posix_spawnattr_init(&attr);
    sigset_t empty, defs;
    sigemptyset(&empty);
    sigemptyset(&defs);
    /* ART ignores SIGPIPE and blocks some signals; give the child sane defaults. */
    int sigs[] = { SIGPIPE, SIGINT, SIGTERM, SIGHUP, SIGQUIT, SIGUSR1, SIGUSR2, SIGCHLD };
    for (size_t i = 0; i < sizeof sigs / sizeof sigs[0]; i++) sigaddset(&defs, sigs[i]);
    posix_spawnattr_setsigmask(&attr, &empty);
    posix_spawnattr_setsigdefault(&attr, &defs);
    posix_spawnattr_setflags(&attr, POSIX_SPAWN_SETSIGMASK | POSIX_SPAWN_SETSIGDEF);

    rc = posix_spawn(&pid, path, &fa, &attr, argv, envp);
    posix_spawn_file_actions_destroy(&fa);
    posix_spawnattr_destroy(&attr);
    rc = (rc == 0) ? (jint)pid : -rc;
out:
    if (path) (*env)->ReleaseStringUTFChars(env, jpath, path);
    free_cstr_array(argv);
    free_cstr_array(envp);
    return rc;
}

JNIEXPORT jint JNICALL
Java_org_sharkdroid_NativeSpawn_waitPid(JNIEnv *env, jclass clazz, jint pid) {
    (void)env; (void)clazz;
    int status = 0;
    for (;;) {
        pid_t r = waitpid((pid_t)pid, &status, 0);
        if (r == pid) break;
        if (r < 0 && errno == EINTR) continue;
        return -1000 - errno;
    }
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    if (WIFSIGNALED(status)) return 128 + WTERMSIG(status);
    return -1;
}

JNIEXPORT jint JNICALL
Java_org_sharkdroid_NativeSpawn_kill(JNIEnv *env, jclass clazz, jint pid, jint sig) {
    (void)env; (void)clazz;
    if (pid <= 1) return -EINVAL;
    return kill((pid_t)pid, sig) == 0 ? 0 : -errno;
}
