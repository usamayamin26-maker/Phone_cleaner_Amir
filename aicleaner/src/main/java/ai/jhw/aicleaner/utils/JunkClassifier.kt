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
 * Which output means "junk": the source treated the image as a meme when score 0 beat score 1,
 * even though labels.txt lists "Not Meme" first. The source's behaviour is kept ([JUNK_INDEX] = 0)
 * because that is what shipped; confirm it on a device with a few known memes and photos, and
 * flip [JUNK_INDEX] if the verdicts come out reversed.
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

    /** [bitmap] must already be [SIZE] x [SIZE]. */
    fun isJunk(bitmap: Bitmap): Boolean {
        input.rewind()
        bitmap.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        for (pixel in pixels) {
            input.putFloat(((pixel shr 16 and 0xFF) - MEAN) / STD)
            input.putFloat(((pixel shr 8 and 0xFF) - MEAN) / STD)
            input.putFloat(((pixel and 0xFF) - MEAN) / STD)
        }
        interpreter.run(input, output)
        val scores = output[0]
        return scores[JUNK_INDEX] > scores[1 - JUNK_INDEX]
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
