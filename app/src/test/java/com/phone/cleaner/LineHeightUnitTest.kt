package com.phone.cleaner

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every line height in the app is `ssp`, set through `app:lineHeight` (or a style's `lineHeight`
 * item). The text is `ssp` and follows the user's font size; an `sdp` or `dp` line box does not, so
 * at font size 2.0 the lines of a paragraph overlap. `android:lineHeight` and
 * `lineSpacingMultiplier` are not used at all. See "Typography" in CLAUDE.md ("Project guidelines").
 */
class LineHeightUnitTest {

    // Unit tests run with the module directory as the working directory.
    private val resDir = File("src/main/res")

    private val xmlFiles: List<File>
        get() = resDir.walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .filter { it.parentFile?.name?.let { dir -> dir.startsWith("layout") || dir.startsWith("values") } == true }
            .filterNot { adsBundle.matches(it.name) }
            .toList()

    // The ads bundle is UniversalRemoteControl's files verbatim, raw dp and sp and comments included
    // (CLAUDE.md, "Ads"), so it is not held to this app's line height rules.
    private val adsBundle = Regex(
        "(native_ad_.*|layout_native_ad_.*|layout_banner_ad_.*|layout_ad_slot_.*|view_ad_slot|" +
            "dialog_ad_loading|dialog_full_screen_native_ad|(styles|attrs|ids|strings)_ads)\\.xml"
    )

    @Test
    fun resourcesAreFound() {
        assertTrue("No layouts found under ${resDir.absolutePath}", xmlFiles.any { it.parentFile?.name == "layout" })
    }

    @Test
    fun everyLineHeightIsSsp() {
        val attribute = Regex("""app:lineHeight="([^"]*)"""")
        val styleItem = Regex("""<item name="lineHeight">([^<]*)</item>""")
        val ssp = Regex("""@dimen/_\d+ssp""")

        val offenders = xmlFiles.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val value = (attribute.find(line) ?: styleItem.find(line))?.groupValues?.get(1)
                if (value != null && !ssp.matches(value)) "${file.path}:${index + 1} $value" else null
            }
        }
        assertTrue("Line heights that are not ssp:\n${offenders.joinToString("\n")}", offenders.isEmpty())
    }

    @Test
    fun noPlatformLineHeightOrMultiplier() {
        val forbidden = listOf("android:lineHeight", "lineSpacingMultiplier", "lineSpacingExtra")
        val offenders = xmlFiles.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                forbidden.firstOrNull { line.contains(it) }?.let { "${file.path}:${index + 1} $it" }
            }
        }
        assertTrue("Forbidden line spacing:\n${offenders.joinToString("\n")}", offenders.isEmpty())
    }
}
