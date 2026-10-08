package org.sharkdroid

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Minimal read-only provider for user-initiated sharing. Not exported; access only via
 * per-URI temporary grants. Serves files directly inside files/captures by name only.
 */
class CaptureShareProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    private fun resolve(uri: Uri): File {
        val ctx = context ?: throw FileNotFoundException()
        val segs = uri.pathSegments
        if (segs.size != 1) throw FileNotFoundException()
        val f = File(Tools.capturesDir(ctx), segs[0])
        if (!Tools.isCaptureFile(ctx, f)) throw FileNotFoundException()
        return f
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("read-only")
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val f = resolve(uri)
        val cols = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(cols, 1).apply { addRow(arrayOf<Any>(f.name, f.length())) }
    }

    override fun getType(uri: Uri): String = "application/vnd.tcpdump.pcap"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
}
