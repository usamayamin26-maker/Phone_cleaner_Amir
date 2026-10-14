package ai.jhw.aicleaner.domain.repository

import ai.jhw.aicleaner.model.AiScanEvent
import ai.jhw.aicleaner.model.KnownVerdict
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.FileItem
import kotlinx.coroutines.flow.Flow

/**
 * AI (Artificial Intelligence) Junk Cleaner: a LiteRT (TensorFlow Lite) model, run on the phone,
 * picks out memes and forwarded junk images (and video frames) from the gallery. Nothing leaves
 * the device. Needs media access. Cancelling the collector stops the scan between two files.
 */
interface AiCleanerRepository {

    /** [known] maps a file path to the verdict the app stored for it earlier. */
    fun scan(known: Map<String, KnownVerdict>, includeVideos: Boolean = true): Flow<AiScanEvent>

    suspend fun delete(files: List<FileItem>): DeleteOutcome

    /** Frees the model (about 13 MB) - call when the AI cleaner screen is left for good. */
    fun release()
}
