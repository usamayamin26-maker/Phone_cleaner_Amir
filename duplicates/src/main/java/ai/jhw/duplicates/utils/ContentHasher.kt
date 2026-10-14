package ai.jhw.duplicates.utils

import ai.jhw.storage.model.FileItem
import android.content.ContentResolver
import android.util.Log
import java.io.InputStream
import java.security.MessageDigest

/** Hashes through the ContentResolver, which works with media access as well as full access. */
internal class ContentHasher(private val resolver: ContentResolver) {

    /** A cheap fingerprint: size plus the first and last [SAMPLE] bytes. */
    fun quickHash(item: FileItem): String? = digest(item) { input, md ->
        val head = ByteArray(SAMPLE)
        val headRead = input.readFully(head)
        md.update(head, 0, headRead)
        val skip = item.sizeBytes - SAMPLE.toLong() * 2
        if (skip > 0) {
            input.skipFully(skip)
            val tail = ByteArray(SAMPLE)
            md.update(tail, 0, input.readFully(tail))
        }
        md.update(item.sizeBytes.toString().toByteArray())
    }

    fun fullHash(item: FileItem): String? = digest(item) { input, md ->
        val buffer = ByteArray(BUFFER)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            md.update(buffer, 0, read)
        }
    }

    private fun digest(item: FileItem, block: (InputStream, MessageDigest) -> Unit): String? = try {
        resolver.openInputStream(item.uri)?.use { input ->
            val md = MessageDigest.getInstance("SHA-256")
            block(input, md)
            md.digest().joinToString("") { "%02x".format(it) }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Cannot read ${item.path}: ${e.javaClass.simpleName}")
        null
    }

    private fun InputStream.readFully(buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val read = read(buffer, total, buffer.size - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    private fun InputStream.skipFully(count: Long) {
        var left = count
        while (left > 0) {
            val skipped = skip(left)
            if (skipped <= 0) break
            left -= skipped
        }
    }

    private companion object {
        const val TAG = "ContentHasher"
        const val SAMPLE = 64 * 1024
        const val BUFFER = 64 * 1024
    }
}
