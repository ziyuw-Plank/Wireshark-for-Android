package org.sharkdroid

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import java.io.File

/**
 * Runs in an isolated process (android:isolatedProcess="true"): random UID, no permissions,
 * no access to our app data, and therefore no access to su. Its only job is to spawn tshark
 * (the large, untrusted-input-parsing component) with file descriptors handed over by the app.
 */
class DissectorService : Service() {

    companion object {
        const val DESCRIPTOR = "org.sharkdroid.IDissector"
        const val TX_SPAWN = IBinder.FIRST_CALL_TRANSACTION
        const val TX_WAIT = IBinder.FIRST_CALL_TRANSACTION + 1
        const val TX_KILL = IBinder.FIRST_CALL_TRANSACTION + 2
        const val TX_PING = IBinder.FIRST_CALL_TRANSACTION + 3
        private const val TAG = "SharkDissector"
    }

    private val binder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) return super.onTransact(code, data, reply, flags)
            // Only our own app (the non-isolated part) may drive this service.
            if (Binder.getCallingUid() != applicationInfo.uid) {
                Log.w(TAG, "rejecting caller uid ${Binder.getCallingUid()}")
                return false
            }
            data.enforceInterface(DESCRIPTOR)
            when (code) {
                TX_PING -> reply?.writeInt(android.os.Process.myUid())
                TX_SPAWN -> {
                    val args = data.createStringArray() ?: emptyArray()
                    val fin = data.readFileDescriptor()
                    val fout = data.readFileDescriptor()
                    val ferr = data.readFileDescriptor()
                    var pid = -22
                    try {
                        if (fin != null && fout != null && ferr != null) {
                            val tshark = File(applicationInfo.nativeLibraryDir, "libtshark.so").path
                            val argv = arrayOf(tshark) + args
                            pid = NativeSpawn.spawn(tshark, argv, Tools.tsharkEnv("/nonexistent"),
                                fin.fd, fout.fd, ferr.fd)
                        }
                    } finally {
                        fin?.close(); fout?.close(); ferr?.close()
                    }
                    reply?.writeInt(pid)
                }
                TX_WAIT -> reply?.writeInt(NativeSpawn.waitPid(data.readInt()))
                TX_KILL -> reply?.writeInt(NativeSpawn.kill(data.readInt(), 9))
                else -> return super.onTransact(code, data, reply, flags)
            }
            return true
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
