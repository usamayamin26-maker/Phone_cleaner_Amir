package ai.jhw.deviceinfo.utils

import java.io.File

/**
 * Reads small kernel files under /proc and /sys. SELinux (Security-Enhanced Linux) denies many of
 * them on newer devices, so every read can fail and returns null rather than throwing.
 */
internal object SysFs {

    fun readLine(path: String): String? = try {
        File(path).takeIf { it.canRead() }?.bufferedReader()?.use { it.readLine() }?.trim()
            ?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }

    fun readLong(path: String): Long? = readLine(path)?.toLongOrNull()

    fun readLines(path: String): List<String> = try {
        File(path).takeIf { it.canRead() }?.readLines().orEmpty()
    } catch (_: Exception) {
        emptyList()
    }
}
