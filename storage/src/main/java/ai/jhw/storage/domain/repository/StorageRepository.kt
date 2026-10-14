package ai.jhw.storage.domain.repository

import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import ai.jhw.storage.model.StorageAccess
import android.content.Intent
import java.io.File

/**
 * The shared file layer every cleaner builds on: what the app may access, MediaStore queries and
 * the only delete path in the app. Nothing here deletes on its own - every delete is a call the
 * app makes after the user confirmed it.
 */
interface StorageRepository {

    fun access(): StorageAccess

    /** Runtime permissions to request for media access on this API level. */
    fun mediaPermissions(): Array<String>

    /** Android 11+: the system screen that grants All files access; null below Android 11. */
    fun allFilesAccessIntent(): Intent?

    /** The root of shared storage, e.g. /storage/emulated/0. */
    fun sharedStorageRoot(): File

    /** All files of one kind that MediaStore knows, newest first. */
    suspend fun queryMedia(kind: FileKind): List<FileItem>

    /** Files of at least [minBytes], largest first, at most [limit] of them. */
    suspend fun getLargeFiles(minBytes: Long = DEFAULT_LARGE_FILE_BYTES, limit: Int = 200): List<FileItem>

    /**
     * Deletes [items]. With [StorageAccess.FULL] the files go at once; otherwise MediaStore items
     * come back as [DeleteOutcome.NeedsUserConsent] on Android 11+.
     */
    suspend fun delete(items: List<FileItem>): DeleteOutcome

    /** After the user approved a consent request: which of [items] are really gone. */
    suspend fun confirmDeleted(items: List<FileItem>): DeleteResult

    /**
     * Deletes plain files and empty directories by path - junk, WhatsApp media. Needs
     * [StorageAccess.FULL]; without it everything comes back in [DeleteResult.failedPaths].
     * A directory is deleted only if it is still empty at that moment.
     */
    suspend fun deletePaths(paths: List<String>): DeleteResult

    companion object {
        const val DEFAULT_LARGE_FILE_BYTES = 50L * 1024 * 1024
    }
}
