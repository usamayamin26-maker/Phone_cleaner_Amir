package com.phone.cleaner.data.repository

import ai.jhw.duplicates.model.DuplicateScanEvent
import ai.jhw.storage.domain.repository.StorageRepository
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import android.util.Log
import com.phone.cleaner.data.local.dao.AiScanResultDao
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.repository.DuplicatesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class DuplicatesRepositoryImp(
    private val duplicates: ai.jhw.duplicates.domain.repository.DuplicatesRepository,
    private val storage: StorageRepository,
    private val aiScanResultDao: AiScanResultDao,
) : DuplicatesRepository {

    /** The screen has sections for photos and videos only. */
    override fun scan(): Flow<DuplicateScanEvent> = duplicates.scan(setOf(FileKind.IMAGE, FileKind.VIDEO))

    override suspend fun getLargeFiles(): ApiResult<List<FileItem>> = call("large files") {
        storage.getLargeFiles()
    }

    override suspend fun delete(files: List<FileItem>): ApiResult<DeleteOutcome> = call("delete") {
        duplicates.delete(files).also { outcome ->
            if (outcome is DeleteOutcome.Completed) forgetDeleted(outcome.result)
        }
    }

    override suspend fun confirmDeleted(files: List<FileItem>): ApiResult<DeleteResult> = call("confirm delete") {
        storage.confirmDeleted(files).also { forgetDeleted(it) }
    }

    /** Files that are gone must not come back from the AI (Artificial Intelligence) scan cache. */
    private suspend fun forgetDeleted(result: DeleteResult) {
        if (result.deletedPaths.isNotEmpty()) aiScanResultDao.deleteByPaths(result.deletedPaths)
        Log.d(TAG, "Deleted ${result.deletedPaths.size}, failed ${result.failedPaths.size}, freed ${result.freedBytes} B")
    }

    private suspend fun <T> call(what: String, block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Duplicates $what failed", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }

    private companion object {
        const val TAG = "DuplicatesRepositoryImp"
    }
}
