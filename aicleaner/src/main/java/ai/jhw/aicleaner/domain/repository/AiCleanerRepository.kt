package ai.jhw.aicleaner.domain.repository

import ai.jhw.aicleaner.model.AiScanEvent
import ai.jhw.aicleaner.model.KnownVerdict
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.FileItem
import kotlinx.coroutines.flow.Flow

/**
 * AI (Artificial Intelligence) Junk Cleaner: a LiteRT (TensorFlow Lite) model, run on the phone,
 * picks out memes and forwarded junk images (and video frames). Nothing leaves the device. Needs
 * media access. Cancelling the collector stops the scan between two files.
 *
 * The model only looks where forwarded media lands - WhatsApp's and WhatsApp Business's media and
 * Download (`AiJunkRules`) - and a file counts as junk only at or above the module's own confidence
 * threshold. What is cached is the model's probability, not the verdict, so that threshold can
 * change without re-scanning.
 */
interface AiCleanerRepository {

    /**
     * [known] maps a file path to the verdict the app stored for it earlier. A verdict whose
     * probability is unknown is classified again.
     */
    fun scan(known: Map<String, KnownVerdict>, includeVideos: Boolean = true): Flow<AiScanEvent>

    /** The photos and videos in the Screenshots folders, newest first. No model is involved. */
    suspend fun getScreenshots(): List<FileItem>

    suspend fun delete(files: List<FileItem>): DeleteOutcome

    /** Frees the model (about 13 MB) - call when the AI cleaner screen is left for good. */
    fun release()
}
