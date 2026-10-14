package ai.jhw.compressor.domain.repository

import ai.jhw.compressor.model.CompressOptions
import ai.jhw.compressor.model.CompressedImage
import ai.jhw.storage.model.FileItem

/**
 * Photo Compressor. [compress] never touches the original: it writes to the app's cache. The user
 * then keeps both ([saveCopy]) or swaps the original for the smaller file ([replaceOriginal]).
 */
interface CompressorRepository {

    suspend fun compress(item: FileItem, options: CompressOptions): CompressedImage

    /**
     * Saves the compressed file into Pictures/[albumName] and returns it, or null on failure.
     * The album name is user-visible, so the app passes it from its strings.
     */
    suspend fun saveCopy(image: CompressedImage, albumName: String): FileItem?

    /**
     * Overwrites the original with the compressed file. Needs All files access, and the format
     * must match the original's extension (a .jpg stays a .jpg); returns false otherwise.
     */
    suspend fun replaceOriginal(image: CompressedImage): Boolean

    /** Deletes the cached output of compressions the user did not keep. */
    suspend fun clearCache()
}
