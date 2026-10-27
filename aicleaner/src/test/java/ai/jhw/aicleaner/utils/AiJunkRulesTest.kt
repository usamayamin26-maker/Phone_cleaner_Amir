package ai.jhw.aicleaner.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The candidate folders are what keeps the AI (Artificial Intelligence) cleaner away from the
 * owner's own photos, so the rules that recognise them are checked against the paths real phones
 * use.
 */
class AiJunkRulesTest {

    @Test
    fun `forwarded media folders are candidates`() {
        val candidatePaths = listOf(
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/IMG-1.jpg",
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/VID-1.mp4",
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent/IMG-2.jpg",
            // The old top-level location, from before WhatsApp moved for Android 11.
            "/storage/emulated/0/WhatsApp/Media/WhatsApp Images/IMG-3.jpg",
            "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Images/IMG-4.jpg",
            "/storage/emulated/0/WhatsApp Business/Media/WhatsApp Business Images/IMG-5.jpg",
            "/storage/emulated/0/Download/forwarded.jpg",
            "/storage/emulated/0/Download/Telegram/meme.png",
            // A second user and a removable card have their own volume roots.
            "/storage/emulated/10/Download/forwarded.jpg",
            "/storage/1A2B-3C4D/WhatsApp/Media/WhatsApp Images/IMG-6.jpg",
            // The folder names are matched whatever their case.
            "/storage/emulated/0/download/x.jpg",
            "/storage/emulated/0/DOWNLOAD/x.jpg",
        )
        candidatePaths.forEach { assertTrue(it, AiJunkRules.isCandidate(it)) }
    }

    @Test
    fun `the user's own folders are not candidates`() {
        val otherPaths = listOf(
            "/storage/emulated/0/DCIM/Camera/IMG_20260101_120000.jpg",
            "/storage/emulated/0/DCIM/IMG_0004.jpg",
            "/storage/emulated/0/DCIM/Screenshots/Screenshot_20260101.png",
            "/storage/emulated/0/Pictures/meme.jpg",
            "/storage/emulated/0/Pictures/Screenshots/Screenshot_20260101.png",
            "/storage/emulated/0/Movies/clip.mp4",
            "/storage/emulated/0/Documents/scan.jpg",
            // A WhatsApp folder that is not its Media folder.
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Backups/x.jpg",
            // Only the volume's own Download folder, not one nested somewhere else.
            "/storage/emulated/0/Pictures/Download/x.jpg",
            "/storage/emulated/0/Android/media/com.example/Download/x.jpg",
            // A name that merely starts like a candidate folder.
            "/storage/emulated/0/Downloads/x.jpg",
            "/storage/emulated/0/WhatsAppMedia/x.jpg",
        )
        otherPaths.forEach { assertFalse(it, AiJunkRules.isCandidate(it)) }
    }

    @Test
    fun `screenshots are recognised in both screenshot folders`() {
        listOf(
            "/storage/emulated/0/DCIM/Screenshots/Screenshot_20260101.png",
            "/storage/emulated/0/Pictures/Screenshots/Screenshot_20260101.png",
            "/storage/emulated/0/pictures/screenshots/s.jpg",
        ).forEach { assertTrue(it, AiJunkRules.isScreenshot(it)) }
        listOf(
            "/storage/emulated/0/DCIM/Camera/IMG_1.jpg",
            "/storage/emulated/0/Download/Screenshot_20260101.png",
            "/storage/emulated/0/Pictures/screenshot.png",
            "/storage/emulated/0/Download/Screenshots/s.png",
        ).forEach { assertFalse(it, AiJunkRules.isScreenshot(it)) }
    }

    @Test
    fun `a screenshot is never a candidate`() {
        val screenshot = "/storage/emulated/0/DCIM/Screenshots/Screenshot_20260101.png"
        assertTrue(AiJunkRules.isScreenshot(screenshot))
        assertFalse(AiJunkRules.isCandidate(screenshot))
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
