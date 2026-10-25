package com.phone.cleaner.data.repository

import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.whatsapp.model.WhatsAppScanEvent
import android.util.Log
import com.phone.cleaner.data.local.dao.AiScanResultDao
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.repository.WhatsAppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class WhatsAppRepositoryImp(
    private val whatsApp: ai.jhw.whatsapp.domain.repository.WhatsAppRepository,
    private val aiScanResultDao: AiScanResultDao,
) : WhatsAppRepository {

    override fun scan(): Flow<WhatsAppScanEvent> = whatsApp.scan().onEach { event ->
        if (event is WhatsAppScanEvent.Completed) {
            val result = event.result
            Log.d(
                TAG,
                "WA scan: variants ${result.variantsFound}, " +
                    result.categories.joinToString { "${it.type} ${it.files.size}" },
            )
        }
    }

    override suspend fun delete(files: List<FileItem>): ApiResult<DeleteResult> = try {
        val result = whatsApp.delete(files)
        if (result.deletedPaths.isNotEmpty()) aiScanResultDao.deleteByPaths(result.deletedPaths)
        Log.d(TAG, "Deleted ${result.deletedPaths.size}, failed ${result.failedPaths.size}, freed ${result.freedBytes} B")
        ApiResult.Success(result)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "WhatsApp delete failed", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }

    private companion object {
        const val TAG = "WhatsAppRepositoryImp"
    }
}
