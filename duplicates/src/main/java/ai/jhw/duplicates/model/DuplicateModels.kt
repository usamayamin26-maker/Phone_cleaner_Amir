package ai.jhw.duplicates.model

import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind

/**
 * Files with byte-for-byte identical content. [bestIndex] is the copy "Keep best one" keeps: the
 * one in the camera folder over a WhatsApp or Download copy, then the oldest, then the shortest
 * path. Identical bytes mean identical quality, so "best" is about where the original lives.
 */
data class DuplicateGroup(
    /** The content's SHA-256, hex. */
    val hash: String,
    val kind: FileKind,
    val files: List<FileItem>,
    val bestIndex: Int,
) {
    val best: FileItem get() = files[bestIndex]

    /** Every copy except [best] - what "Keep best one" selects for deletion. */
    val extras: List<FileItem> get() = files.filterIndexed { index, _ -> index != bestIndex }

    val wastedBytes: Long get() = extras.sumOf { it.sizeBytes }
}

data class DuplicateScanResult(val groups: List<DuplicateGroup>) {
    fun groupsOf(kind: FileKind): List<DuplicateGroup> = groups.filter { it.kind == kind }
    val wastedBytes: Long get() = groups.sumOf { it.wastedBytes }
}

sealed interface DuplicateScanEvent {
    data class Progress(val fraction: Float, val kind: FileKind?) : DuplicateScanEvent
    data class Completed(val result: DuplicateScanResult) : DuplicateScanEvent
}
