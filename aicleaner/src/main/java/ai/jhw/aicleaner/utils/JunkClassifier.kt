package ai.jhw.aicleaner.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * The meme / junk-image model from the old source (softmax_fit_gene.tflite in src/main/assets):
 * a 224x224 RGB (Red Green Blue) input scaled to -1..1, two output scores.
 *
 * The two scores are already a probability pair: measured on a device over the whole of a gallery
 * (2026-10-27), every pair summed to 1.0. So [junkProbability] is simply the junk one, and the
 * caller decides from it; see [AiJunkRules.JUNK_THRESHOLD].
 *
 * Which output means "junk": score 0 ([JUNK_INDEX]), "Meme" in labels.txt. Verified on an
 * emulator (2026-10-25): 10 of 10 test memes (captioned, two-panel, a forwarded quote) came out
 * junk, and none of the 18 other files - photographs (some as WhatsApp copies), videos and
 * stickers - did.
 *
 * Not thread-safe: the repository serialises every call.
 */
internal class JunkClassifier(context: Context) {

    private val interpreter = Interpreter(loadModel(context))
    private val input: ByteBuffer = ByteBuffer
        .allocateDirect(4 * SIZE * SIZE * 3)
        .order(ByteOrder.nativeOrder())
    private val output = Array(1) { FloatArray(2) }
    private val pixels = IntArray(SIZE * SIZE)

    /**
     * How sure the model is that [bitmap] is junk, 0..1. [bitmap] must already be [SIZE] x [SIZE].
     *
     * The score is taken as it comes. Putting the pair through a softmax here - which looks right,
     * since two scores and a decision usually mean logits - squeezes every verdict into 0.2689 to
     * 0.7311, because softmax of a pair that already sums to 1 can never leave that range. Measured
     * that way, nothing in a 4,007-file gallery reached 0.75 and no threshold above 0.73 could ever
     * match anything.
     */
    fun junkProbability(bitmap: Bitmap): Float {
        input.rewind()
        bitmap.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        for (pixel in pixels) {
            input.putFloat(((pixel shr 16 and 0xFF) - MEAN) / STD)
            input.putFloat(((pixel shr 8 and 0xFF) - MEAN) / STD)
            input.putFloat(((pixel and 0xFF) - MEAN) / STD)
        }
        interpreter.run(input, output)
        return output[0][JUNK_INDEX]
    }

    fun close() = interpreter.close()

    private fun loadModel(context: Context): MappedByteBuffer {
        // .tflite assets are stored uncompressed by AGP, so openFd can map them directly.
        context.assets.openFd(MODEL_FILE).use { fd ->
            FileInputStream(fd.fileDescriptor).use { stream ->
                Log.d(TAG, "Loading $MODEL_FILE (${fd.declaredLength} bytes)")
                return stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
    }

    companion object {
        const val SIZE = 224
        private const val TAG = "JunkClassifier"
        private const val MODEL_FILE = "softmax_fit_gene.tflite"
        private const val MEAN = 128f
        private const val STD = 128f
        private const val JUNK_INDEX = 0
    }
}
