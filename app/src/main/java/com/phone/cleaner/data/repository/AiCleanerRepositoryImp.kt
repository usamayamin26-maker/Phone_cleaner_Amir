package com.phone.cleaner.data.repository

import ai.jhw.aicleaner.model.AiScanEvent
import ai.jhw.aicleaner.model.KnownVerdict
import ai.jhw.storage.domain.repository.StorageRepository
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import android.util.Log
import com.phone.cleaner.data.local.dao.AiScanResultDao
import com.phone.cleaner.data.local.entities.AiScanResultEntity
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.repository.AiCleanerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class AiCleanerRepositoryImp(
    private val aiCleaner: ai.jhw.aicleaner.domain.repository.AiCleanerRepository,
    private val storage: StorageRepository,
    private val aiScanResultDao: AiScanResultDao,
) : AiCleanerRepository {

    override fun scan(): Flow<AiScanEvent> = flow {
        val known = aiScanResultDao.getAll()
            .associate { it.path to KnownVerdict(it.dateModified, it.junkProbability) }
        val pending = ArrayList<AiScanResultEntity>()
        var classified = 0
        val startedAt = System.currentTimeMillis()
        try {
            aiCleaner.scan(known).collect { event ->
                when (event) {
                    is AiScanEvent.Classified -> {
                        classified++
                        pending += AiScanResultEntity(event.item.path, event.item.dateModified, event.junkProbability)
                        // Stored in batches, so a long scan that is stopped keeps what it learnt.
                        if (pending.size >= BATCH) flush(pending)
                    }
                    is AiScanEvent.Completed -> {
                        flush(pending)
                        if (event.missingPaths.isNotEmpty()) aiScanResultDao.deleteByPaths(event.missingPaths)
                        Log.d(
                            TAG,
                            "AI scan: ${known.size} cached, $classified classified, ${event.junk.size} junk, " +
                                "${event.missingPaths.size} dropped, ${System.currentTimeMillis() - startedAt} ms",
                        )
                    }
                    is AiScanEvent.Progress -> Unit
                }
                emit(event)
            }
        } finally {
            // A stopped scan still keeps the verdicts it reached; storing them deletes nothing.
            withContext(NonCancellable) { flush(pending) }
        }
    }

    override suspend fun getScreenshots(): ApiResult<List<FileItem>> = call("screenshots") {
        aiCleaner.getScreenshots()
    }

    override suspend fun getLargeFiles(): ApiResult<List<FileItem>> = call("large files") {
        storage.getLargeFiles()
    }

    override suspend fun delete(files: List<FileItem>): ApiResult<DeleteOutcome> = call("delete") {
        aiCleaner.delete(files).also { outcome ->
            if (outcome is DeleteOutcome.Completed) forgetDeleted(outcome.result)
        }
    }

    override suspend fun confirmDeleted(files: List<FileItem>): ApiResult<DeleteResult> = call("confirm delete") {
        storage.confirmDeleted(files).also { forgetDeleted(it) }
    }

    override fun release() = aiCleaner.release()

    private suspend fun flush(pending: MutableList<AiScanResultEntity>) {
        if (pending.isEmpty()) return
        aiScanResultDao.upsertAll(pending.toList())
        pending.clear()
    }

    /** Files that are gone must not come back from the verdict cache. */
    private suspend fun forgetDeleted(result: DeleteResult) {
        if (result.deletedPaths.isNotEmpty()) aiScanResultDao.deleteByPaths(result.deletedPaths)
        Log.d(TAG, "Deleted ${result.deletedPaths.size}, failed ${result.failedPaths.size}, freed ${result.freedBytes} B")
    }

    private suspend fun <T> call(what: String, block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "AI cleaner $what failed", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }

    private companion object {
        const val TAG = "AiCleanerRepositoryImp"
        const val BATCH = 50
    }
}
