package ai.jhw.aicleaner.model

import ai.jhw.storage.model.FileItem

/**
 * A verdict the app already has (it keeps them in Room). Reused while the file's [dateModified]
 * still matches; a changed file is classified again.
 */
data class KnownVerdict(val dateModified: Long, val isJunk: Boolean)

sealed interface AiScanEvent {
    data class Progress(val fraction: Float, val processed: Int, val total: Int) : AiScanEvent

    /** A file the model looked at in this scan - the app stores the verdict. */
    data class Classified(val item: FileItem, val isJunk: Boolean) : AiScanEvent

    /**
     * [junk] is everything judged junk, from the cache or from this scan, newest first.
     * [missingPaths] were in the known verdicts but are no longer on the phone - drop them.
     */
    data class Completed(val junk: List<FileItem>, val missingPaths: List<String>) : AiScanEvent
}
