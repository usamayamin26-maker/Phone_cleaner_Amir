package ai.jhw.whatsapp.repo

import ai.jhw.storage.domain.repository.StorageRepository
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.StorageAccess
import ai.jhw.storage.utils.FileKinds
import ai.jhw.whatsapp.domain.repository.WhatsAppRepository
import ai.jhw.whatsapp.model.WhatsAppCategory
import ai.jhw.whatsapp.model.WhatsAppMediaType
import ai.jhw.whatsapp.model.WhatsAppScanEvent
import ai.jhw.whatsapp.model.WhatsAppScanResult
import ai.jhw.whatsapp.utils.WhatsAppFolders
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class WhatsAppRepositoryImpl(private val storage: StorageRepository) : WhatsAppRepository {

    override fun scan(): Flow<WhatsAppScanEvent> = flow {
        if (storage.access() != StorageAccess.FULL) {
            Log.w(TAG, "WA Cleaner needs All files access")
            emit(WhatsAppScanEvent.Completed(WhatsAppScanResult(emptySet(), emptyList(), needsFullAccess = true)))
            return@flow
        }

        val roots = WhatsAppFolders.mediaRoots(storage.sharedStorageRoot())
        // Progress steps are the type folders (Images, Video, ...) of every Media root.
        val typeFolders = roots.values.flatten().flatMap { it.listFiles()?.filter { f -> f.isDirectory }.orEmpty() }
        val byType = HashMap<WhatsAppMediaType, MutableList<FileItem>>()

        typeFolders.forEachIndexed { index, folder ->
            currentCoroutineContext().ensureActive()
            emit(WhatsAppScanEvent.Progress(index.toFloat() / typeFolders.size, folder.path))
            val type = WhatsAppFolders.typeOf(folder.name)
            folder.walkTopDown()
                .filter { it.isFile && it.name != ".nomedia" && it.length() > 0 }
                .forEach { file -> byType.getOrPut(type) { ArrayList() } += FileKinds.fromFile(file) }
        }

        val categories = WhatsAppMediaType.entries.mapNotNull { type ->
            byType[type]?.takeIf { it.isNotEmpty() }?.let { files ->
                WhatsAppCategory(type, files.sortedByDescending { it.dateModified })
            }
        }
        emit(WhatsAppScanEvent.Progress(1f, null))
        emit(WhatsAppScanEvent.Completed(WhatsAppScanResult(roots.keys, categories)))
    }.flowOn(Dispatchers.IO)

    override suspend fun delete(files: List<FileItem>): DeleteResult =
        withContext(Dispatchers.IO) { storage.deletePaths(files.map { it.path }) }

    private companion object {
        const val TAG = "WhatsAppRepositoryImpl"
    }
}
