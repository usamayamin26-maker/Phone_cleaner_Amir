package ai.jhw.aicleaner.model

import ai.jhw.storage.model.FileItem

/**
 * A verdict the app already has (it keeps them in Room). Reused while the file's [dateModified]
 * still matches; a changed file is classified again.
 *
 * [junkProbability] is the model's own 0..1 score, not a yes or no, so the threshold the module
 * judges by can change without re-scanning. It is null for a row written before the app stored
 * probabilities (database version 1), which counts as unknown: the file is classified again.
 */
data class KnownVerdict(val dateModified: Long, val junkProbability: Float?)

sealed interface AiScanEvent {
    data class Progress(val fraction: Float, val processed: Int, val total: Int) : AiScanEvent

    /** A file the model looked at in this scan - the app stores [junkProbability] for it. */
    data class Classified(val item: FileItem, val junkProbability: Float) : AiScanEvent

    /**
     * [junk] is everything judged junk, from the cache or from this scan, newest first.
     * [missingPaths] were in the known verdicts but are no longer junk candidates - they are gone
     * from the phone, or they sit in a folder the scan now leaves alone.
     */
    data class Completed(val junk: List<FileItem>, val missingPaths: List<String>) : AiScanEvent
}
