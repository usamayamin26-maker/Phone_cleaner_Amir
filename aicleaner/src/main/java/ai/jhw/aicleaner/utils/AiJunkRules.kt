package ai.jhw.aicleaner.utils

import java.util.Locale

/**
 * What the AI (Artificial Intelligence) Junk Cleaner counts as a junk candidate, and how sure the
 * model has to be before it says so.
 *
 * The model is a meme classifier, not a "would you want to keep this photo" classifier, and on a
 * real gallery it says junk far more often than anyone would. Measured over a 4,841-file gallery
 * (2026-10-27): with the camera folders left out, 2,519 of 4,007 candidates were junk at a
 * probability of 0.5 and 2,401 still were at 0.95 - the model is as confident about the files it
 * is wrong about as about the ones it is right about, so the threshold alone cannot make it
 * careful. What makes the screen safe is that none of its verdicts is ever selected for the user;
 * these two rules only keep the list honest.
 */
internal object AiJunkRules {

    /**
     * A file is junk only at or above this junk probability ([JunkClassifier.junkProbability]).
     *
     * Chosen on a Pixel 6 Pro and an emulator (2026-10-27) as the highest of the candidates
     * 0.5 / 0.7 / 0.8 / 0.9 / 0.95 that still catches at least 9 of the 10 test memes: all ten
     * score 1.0, so every candidate catches all ten and the highest wins. It drops 118 of the
     * weakest verdicts on a real gallery (2,519 at 0.5 against 2,401 here) and none of the memes.
     *
     * What is cached is the probability, not the verdict, so this can be changed without
     * re-scanning anything.
     */
    const val JUNK_THRESHOLD = 0.95f

    /**
     * The camera folders - `DCIM/Camera`, and a `Camera` folder under any `DCIM` subfolder - hold
     * the photos and videos the user took themselves. They are never junk candidates: they are not
     * classified, not counted and not shown.
     */
    fun isCameraFolder(path: String): Boolean {
        val lower = path.lowercase(Locale.ROOT)
        val dcim = lower.indexOf("/dcim/")
        if (dcim < 0) return false
        // A file directly in a Camera folder or anywhere below one, under DCIM.
        return lower.indexOf("/camera/", dcim + DCIM.length) >= 0
    }

    private const val DCIM = "/dcim"
}
