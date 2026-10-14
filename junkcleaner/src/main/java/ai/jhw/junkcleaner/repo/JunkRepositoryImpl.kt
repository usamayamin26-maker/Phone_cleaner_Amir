package ai.jhw.junkcleaner.repo

import ai.jhw.junkcleaner.domain.repository.JunkRepository
import ai.jhw.junkcleaner.model.JunkCategory
import ai.jhw.junkcleaner.model.JunkItem
import ai.jhw.junkcleaner.model.JunkScanEvent
import ai.jhw.junkcleaner.model.JunkScanResult
import ai.jhw.junkcleaner.model.JunkType
import ai.jhw.junkcleaner.model.UnavailableReason
import ai.jhw.junkcleaner.utils.JunkScanner
import ai.jhw.junkcleaner.utils.PackageChecker
import ai.jhw.storage.domain.repository.StorageRepository
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.StorageAccess
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class JunkRepositoryImpl(
    context: Context,
    private val storage: StorageRepository,
) : JunkRepository {

    private val packages = PackageChecker(context)

    override fun scan(): Flow<JunkScanEvent> = flow {
        if (storage.access() != StorageAccess.FULL) {
            Log.w(TAG, "Smart Scan needs All files access")
            val blocked = JunkType.entries.map {
                JunkCategory(it, emptyList(), UnavailableReason.NEEDS_FULL_ACCESS)
            }
            emit(JunkScanEvent.Completed(JunkScanResult(blocked)))
            return@flow
        }
        val result = JunkScanner(storage.sharedStorageRoot(), packages) { fraction, path ->
            emit(JunkScanEvent.Progress(fraction, path))
        }.scan()
        emit(JunkScanEvent.Completed(result))
    }.flowOn(Dispatchers.IO)

    override suspend fun delete(items: List<JunkItem>): DeleteResult = withContext(Dispatchers.IO) {
        val paths = items.flatMap { item ->
            when {
                item.type == JunkType.RESIDUAL_FILES &&
                    item.packageName?.let { packages.installedVersion(it) } != null -> {
                    Log.w(TAG, "${item.packageName} was installed after the scan; keeping ${item.path}")
                    emptyList()
                }
                item.isDirectory -> contentsBottomUp(File(item.path), keepRoot = item.type == JunkType.APP_CACHE)
                else -> listOf(item.path)
            }
        }
        storage.deletePaths(paths)
    }

    /** Every file, then every folder deepest first, so each folder is empty when it is reached. */
    private fun contentsBottomUp(dir: File, keepRoot: Boolean): List<String> =
        dir.walkBottomUp()
            .filter { !(keepRoot && it == dir) }
            .map { it.path }
            .toList()

    private companion object {
        const val TAG = "JunkRepositoryImpl"
    }
}
