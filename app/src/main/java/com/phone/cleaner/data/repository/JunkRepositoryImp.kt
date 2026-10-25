package com.phone.cleaner.data.repository

import ai.jhw.junkcleaner.model.JunkItem
import ai.jhw.junkcleaner.model.JunkScanEvent
import ai.jhw.storage.model.DeleteResult
import android.util.Log
import com.phone.cleaner.data.local.dao.AiScanResultDao
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.repository.JunkRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class JunkRepositoryImp(
    private val junk: ai.jhw.junkcleaner.domain.repository.JunkRepository,
    private val aiScanResultDao: AiScanResultDao,
) : JunkRepository {

    override fun scan(): Flow<JunkScanEvent> = junk.scan()

    override suspend fun delete(items: List<JunkItem>): ApiResult<DeleteResult> = try {
        val result = junk.delete(items)
        if (result.deletedPaths.isNotEmpty()) aiScanResultDao.deleteByPaths(result.deletedPaths)
        Log.d(TAG, "Deleted ${result.deletedPaths.size}, failed ${result.failedPaths.size}, freed ${result.freedBytes} B")
        ApiResult.Success(result)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Junk delete failed", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }

    private companion object {
        const val TAG = "JunkRepositoryImp"
    }
}
