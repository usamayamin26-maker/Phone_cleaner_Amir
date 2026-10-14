package ai.jhw.aicleaner.repo

import ai.jhw.aicleaner.domain.repository.AiCleanerRepository
import ai.jhw.aicleaner.model.AiScanEvent
import ai.jhw.aicleaner.model.KnownVerdict
import ai.jhw.aicleaner.utils.JunkClassifier
import ai.jhw.aicleaner.utils.ThumbnailLoader
import ai.jhw.storage.domain.repository.StorageRepository
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AiCleanerRepositoryImpl(
    private val context: Context,
    private val storage: StorageRepository,
) : AiCleanerRepository {

    private val loader = ThumbnailLoader(context)

    /** One model at a time: the interpreter is not thread-safe. */
    private val lock = Mutex()
    private var classifier: JunkClassifier? = null

    override fun scan(known: Map<String, KnownVerdict>, includeVideos: Boolean): Flow<AiScanEvent> = flow {
        val items = buildList {
            addAll(storage.queryMedia(FileKind.IMAGE))
            if (includeVideos) addAll(storage.queryMedia(FileKind.VIDEO))
        }
        val onPhone = items.mapTo(HashSet()) { it.path }
        val junk = ArrayList<FileItem>()

        items.forEachIndexed { index, item ->
            currentCoroutineContext().ensureActive()
            emit(AiScanEvent.Progress(index.toFloat() / items.size.coerceAtLeast(1), index, items.size))

            val cached = known[item.path]?.takeIf { it.dateModified == item.dateModified }
            val isJunk = cached?.isJunk ?: classify(item)?.also { emit(AiScanEvent.Classified(item, it)) }
            if (isJunk == true) junk += item
        }

        emit(AiScanEvent.Progress(1f, items.size, items.size))
        emit(
            AiScanEvent.Completed(
                junk = junk.sortedByDescending { it.dateModified },
                missingPaths = known.keys.filter { it !in onPhone },
            )
        )
    }.flowOn(Dispatchers.Default)

    override suspend fun delete(files: List<FileItem>): DeleteOutcome = storage.delete(files)

    override fun release() {
        classifier?.close()
        classifier = null
    }

    private suspend fun classify(item: FileItem): Boolean? {
        val bitmap = loader.load(item) ?: return null
        return lock.withLock {
            try {
                val model = classifier ?: JunkClassifier(context).also { classifier = it }
                model.isJunk(bitmap)
            } catch (e: Exception) {
                Log.w(TAG, "Classification failed for ${item.path}", e)
                null
            } finally {
                bitmap.recycle()
            }
        }
    }

    private companion object {
        const val TAG = "AiCleanerRepositoryImpl"
    }
}
