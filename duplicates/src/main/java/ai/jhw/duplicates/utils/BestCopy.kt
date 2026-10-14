package ai.jhw.duplicates.utils

import ai.jhw.storage.model.FileItem
import java.util.Locale

/** Picks the copy "Keep best one" keeps. Lower rank wins. */
internal object BestCopy {

    fun indexOf(files: List<FileItem>): Int =
        files.indices.minWith(
            compareBy<Int> { rank(files[it].path) }
                .thenBy { files[it].dateModified }
                .thenBy { files[it].path.length }
        )

    private fun rank(path: String): Int {
        val p = path.lowercase(Locale.ROOT)
        return when {
            "/dcim/camera/" in p -> 0
            "/dcim/" in p -> 1
            "/pictures/" in p || "/movies/" in p || "/music/" in p || "/documents/" in p -> 2
            "/whatsapp" in p || "/download" in p || "/telegram" in p -> 4
            else -> 3
        }
    }
}
