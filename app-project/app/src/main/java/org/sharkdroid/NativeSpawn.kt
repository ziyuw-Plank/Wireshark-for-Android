package org.sharkdroid

/** JNI helpers (libsdnative.so): spawn a program with explicit fds, no shell involved. */
object NativeSpawn {
    init {
        System.loadLibrary("sdnative")
    }

    /** @return pid (>0) or -errno */
    @JvmStatic external fun spawn(
        path: String, argv: Array<String>, envp: Array<String>, fdIn: Int, fdOut: Int, fdErr: Int
    ): Int

    /** Blocks until [pid] exits. @return exit code, 128+signal, or negative on error */
    @JvmStatic external fun waitPid(pid: Int): Int

    @JvmStatic external fun kill(pid: Int, sig: Int): Int
}
