package ai.jhw.aicleaner.repo

import ai.jhw.aicleaner.domain.repository.AiCleanerRepository
import ai.jhw.aicleaner.model.AiScanEvent
import ai.jhw.aicleaner.model.KnownVerdict
import ai.jhw.aicleaner.utils.AiJunkRules
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
import kotlinx.coroutines.withContext

class AiCleanerRepositoryImpl(
    private val context: Context,
    private val storage: StorageRepository,
) : AiCleanerRepository {

    private val loader = ThumbnailLoader(context)

    /** One model at a time: the interpreter is not thread-safe. */
    private val lock = Mutex()
    private var classifier: JunkClassifier? = null

    override fun scan(known: Map<String, KnownVerdict>, includeVideos: Boolean): Flow<AiScanEvent> = flow {
        val media = buildList {
            addAll(storage.queryMedia(FileKind.IMAGE))
            if (includeVideos) addAll(storage.queryMedia(FileKind.VIDEO))
        }
        // Only where forwarded media lands; the user's own folders are not even classified.
        val items = media.filter { AiJunkRules.isCandidate(it.path) }
        val candidates = items.mapTo(HashSet()) { it.path }
        val junk = ArrayList<FileItem>()
        var unreadable = 0
        Log.d(TAG, "Scanning ${items.size} candidates of ${media.size} media files, ${known.size} verdicts known")

        items.forEachIndexed { index, item ->
            currentCoroutineContext().ensureActive()
            emit(AiScanEvent.Progress(index.toFloat() / items.size.coerceAtLeast(1), index, items.size))

            val cached = known[item.path]
                ?.takeIf { it.dateModified == item.dateModified }
                ?.junkProbability
            val probability = cached ?: classify(item)?.also { emit(AiScanEvent.Classified(item, it)) }
            if (probability == null) unreadable++ else if (probability >= AiJunkRules.JUNK_THRESHOLD) junk += item
        }

        if (unreadable > 0) Log.w(TAG, "$unreadable files could not be read and were skipped")
        Log.d(TAG, "${junk.size} of ${items.size} candidates are junk at ${AiJunkRules.JUNK_THRESHOLD}")
        emit(AiScanEvent.Progress(1f, items.size, items.size))
        emit(
            AiScanEvent.Completed(
                junk = junk.sortedByDescending { it.dateModified },
                // Gone from the phone, or no longer a candidate: either way the verdict is of no
                // use, and the cache is rebuilt by scanning again.
                missingPaths = known.keys.filter { it !in candidates },
            )
        )
    }.flowOn(Dispatchers.Default)

    override suspend fun getScreenshots(): List<FileItem> = withContext(Dispatchers.Default) {
        (storage.queryMedia(FileKind.IMAGE) + storage.queryMedia(FileKind.VIDEO))
            .filter { AiJunkRules.isScreenshot(it.path) }
            .sortedByDescending { it.dateModified }
    }

    override suspend fun delete(files: List<FileItem>): DeleteOutcome = storage.delete(files)

    override fun release() {
        classifier?.close()
        classifier = null
    }

    /** The model's junk probability for [item], or null when its thumbnail cannot be read. */
    private suspend fun classify(item: FileItem): Float? {
        val bitmap = loader.load(item) ?: return null
        return lock.withLock {
            try {
                val model = classifier ?: JunkClassifier(context).also { classifier = it }
                model.junkProbability(bitmap)
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
