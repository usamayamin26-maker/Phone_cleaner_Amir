package ai.jhw.duplicates.domain.repository

import ai.jhw.duplicates.model.DuplicateScanEvent
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import kotlinx.coroutines.flow.Flow

/**
 * Delete Duplicates. Files are compared by size first, then by a hash of their first and last
 * 64 KB, and only then by a full SHA-256, so a gallery of thousands is read once at most.
 * Works with media access for photos, videos and audio; documents need All files access.
 */
interface DuplicatesRepository {

    fun scan(kinds: Set<FileKind> = DEFAULT_KINDS): Flow<DuplicateScanEvent>

    /** Goes through the storage delete path, so it may need the user's consent on Android 11+. */
    suspend fun delete(files: List<FileItem>): DeleteOutcome

    companion object {
        val DEFAULT_KINDS = setOf(FileKind.IMAGE, FileKind.VIDEO, FileKind.AUDIO, FileKind.DOCUMENT)
    }
}
