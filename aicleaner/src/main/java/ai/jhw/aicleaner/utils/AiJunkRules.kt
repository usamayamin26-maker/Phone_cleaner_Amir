package ai.jhw.aicleaner.utils

import java.util.Locale

/**
 * What the AI (Artificial Intelligence) Junk Cleaner counts as a junk candidate, and how sure the
 * model has to be before it says so.
 *
 * The model is a meme classifier, not a "would you want to keep this photo" classifier. Measured
 * over a 4,841-file gallery (2026-10-27), it flagged 86-98% of everything outside WhatsApp at any
 * threshold - close to "anything that is not a camera photograph". So it only looks where
 * forwarded media lands: WhatsApp's and WhatsApp Business's media and the Download folder. The
 * user's own folders (DCIM, Pictures and the rest) are never candidates, and none of its verdicts
 * is ever selected for the user.
 */
internal object AiJunkRules {

    /**
     * A file is junk only at or above this junk probability ([JunkClassifier.junkProbability]).
     *
     * Chosen on a Pixel 6 Pro and an emulator (2026-10-27) as the highest of the candidates
     * 0.5 / 0.7 / 0.8 / 0.9 / 0.95 that still catches at least 9 of the 10 test memes: all ten
     * score 1.0, so every candidate catches all ten and the highest wins.
     *
     * What is cached is the probability, not the verdict, so this can be changed without
     * re-scanning anything.
     */
    const val JUNK_THRESHOLD = 0.95f

    /**
     * The folders forwarded media lands in, relative to a storage volume's root: each WhatsApp
     * variant's Media folder at both of its locations (under Android/media since Android 11, and
     * the old top-level one), and Download. Anything below them counts.
     */
    private val CANDIDATE_FOLDERS = listOf(
        "android/media/com.whatsapp/whatsapp/media/",
        "whatsapp/media/",
        "android/media/com.whatsapp.w4b/whatsapp business/media/",
        "whatsapp business/media/",
        "download/",
    )

    /** Where Android and the phone makers save screenshots, relative to a volume's root. */
    private val SCREENSHOT_FOLDERS = listOf("dcim/screenshots/", "pictures/screenshots/")

    /** `/storage/emulated/0/` or a removable volume's `/storage/1A2B-3C4D/`. */
    private val VOLUME_ROOT = Regex("^/storage/(emulated/\\d+|[^/]+)/")

    /** Whether the model looks at [path] at all. */
    fun isCandidate(path: String): Boolean = inAny(path, CANDIDATE_FOLDERS)

    /** A screenshot: shown on its own, with no verdict. */
    fun isScreenshot(path: String): Boolean = inAny(path, SCREENSHOT_FOLDERS)

    private fun inAny(path: String, folders: List<String>): Boolean {
        val relative = path.lowercase(Locale.ROOT).replaceFirst(VOLUME_ROOT, "")
        return folders.any { relative.startsWith(it) }
    }
}
