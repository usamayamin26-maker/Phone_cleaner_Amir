package ai.jhw.aicleaner.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The camera folders are what keeps the AI (Artificial Intelligence) cleaner away from the photos
 * the owner took, so the rule that recognises them is checked against the paths real phones use.
 */
class AiJunkRulesTest {

    @Test
    fun `camera folders are excluded`() {
        val cameraPaths = listOf(
            "/storage/emulated/0/DCIM/Camera/IMG_20260101_120000.jpg",
            "/storage/emulated/0/DCIM/Camera/VID_20260101_120000.mp4",
            // Some phones and cameras number their folders.
            "/storage/emulated/0/DCIM/100ANDRO/Camera/IMG_0001.jpg",
            "/storage/1A2B-3C4D/DCIM/Camera/IMG_0002.jpg",
            // Any folder below a camera folder is camera output too.
            "/storage/emulated/0/DCIM/Camera/raw/IMG_0003.dng",
            // The folder names are matched whatever their case.
            "/storage/emulated/0/dcim/camera/img.jpg",
            "/storage/emulated/0/Dcim/CAMERA/img.jpg",
        )
        cameraPaths.forEach { assertTrue(it, AiJunkRules.isCameraFolder(it)) }
    }

    @Test
    fun `everything else is a candidate`() {
        val otherPaths = listOf(
            "/storage/emulated/0/DCIM/Screenshots/Screenshot_20260101.png",
            "/storage/emulated/0/DCIM/IMG_0004.jpg",
            "/storage/emulated/0/Pictures/meme.jpg",
            // Not under DCIM, so not camera output, whatever the folder is called.
            "/storage/emulated/0/Pictures/Camera/edited.jpg",
            "/storage/emulated/0/Download/forwarded.jpg",
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/img.jpg",
            // A file whose own name says camera is still a candidate.
            "/storage/emulated/0/Pictures/camera.jpg",
            "/storage/emulated/0/DCIM/camera_roll.jpg",
        )
        otherPaths.forEach { assertFalse(it, AiJunkRules.isCameraFolder(it)) }
    }

    @Test
    fun `the threshold is a probability the model can reach`() {
        assertTrue(AiJunkRules.JUNK_THRESHOLD > 0.5f)
        assertTrue(AiJunkRules.JUNK_THRESHOLD < 1f)
    }

    @Test
    fun `the threshold is one of the measured candidates`() {
        // Measured on the owner's gallery and the test meme set; see docs/prompt-history.
        assertEquals(1, listOf(0.5f, 0.7f, 0.8f, 0.9f, 0.95f).count { it == AiJunkRules.JUNK_THRESHOLD })
    }
}
