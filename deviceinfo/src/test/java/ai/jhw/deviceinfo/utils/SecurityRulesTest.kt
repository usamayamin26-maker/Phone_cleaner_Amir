package ai.jhw.deviceinfo.utils

import ai.jhw.deviceinfo.utils.Reading.Absent
import ai.jhw.deviceinfo.utils.Reading.Unreadable
import ai.jhw.deviceinfo.utils.Reading.Value
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The debugging rows must never say "off" for a setting that is on. The cases are the reads
 * measured on a Pixel running Android 17 and on emulators, plus the ways those reads can disagree.
 */
class SecurityRulesTest {

    private data class Rows(val developer: Boolean?, val usb: Boolean?, val wireless: Boolean?)

    private fun rows(
        developerSetting: Reading,
        usbSetting: Reading,
        wirelessSetting: Reading,
        usbConfig: Reading,
        adbdRunsOnUserBuild: Boolean,
    ): Rows {
        val wireless = SecurityRules.wirelessDebugging(wirelessSetting)
        val usb = SecurityRules.usbDebugging(usbSetting, usbConfig, adbdRunsOnUserBuild, wireless)
        val developer = SecurityRules.developerOptions(developerSetting, usb, wireless, adbdRunsOnUserBuild)
        return Rows(developer, usb, wireless)
    }

    @Test
    fun `Pixel on Android 17 with USB debugging off and Wireless debugging on`() {
        val read = rows(
            developerSetting = Value("0"),
            usbSetting = Value("0"),
            wirelessSetting = Value("1"),
            usbConfig = Value(""),
            adbdRunsOnUserBuild = true,
        )
        assertEquals(Rows(developer = true, usb = false, wireless = true), read)
    }

    @Test
    fun `Pixel on Android 17 with USB and Wireless debugging on`() {
        val read = rows(
            developerSetting = Value("0"),
            usbSetting = Value("0"),
            wirelessSetting = Value("1"),
            usbConfig = Value("adb"),
            adbdRunsOnUserBuild = true,
        )
        assertEquals(Rows(developer = true, usb = true, wireless = true), read)
    }

    @Test
    fun `adb among several USB functions is on`() {
        val usb = SecurityRules.usbDebugging(Value("0"), Value("mtp,adb"), true, false)
        assertEquals(true, usb)
    }

    @Test
    fun `a property saying off is not believed while something else runs adbd`() {
        // adbd running on a user build with Wireless debugging off or unknown: the property is wrong.
        assertEquals(null, SecurityRules.usbDebugging(Value("0"), Value(""), true, false))
        assertEquals(null, SecurityRules.usbDebugging(Value("0"), Value(""), true, null))
    }

    @Test
    fun `hidden settings with nothing else readable stay unknown`() {
        val read = rows(
            developerSetting = Value("0"),
            usbSetting = Value("0"),
            wirelessSetting = Value("0"),
            usbConfig = Unreadable,
            adbdRunsOnUserBuild = false,
        )
        assertEquals(Rows(developer = null, usb = null, wireless = false), read)
    }

    @Test
    fun `a setting read as 1 is on even without the property`() {
        // Emulators read the settings truthfully and hide the property.
        val read = rows(
            developerSetting = Value("1"),
            usbSetting = Value("1"),
            wirelessSetting = Value("0"),
            usbConfig = Unreadable,
            adbdRunsOnUserBuild = false,
        )
        assertEquals(Rows(developer = true, usb = true, wireless = false), read)
    }

    @Test
    fun `a phone whose Developer options were never opened is off everywhere`() {
        val read = rows(
            developerSetting = Absent,
            usbSetting = Absent,
            wirelessSetting = Absent,
            usbConfig = Value(""),
            adbdRunsOnUserBuild = false,
        )
        assertEquals(Rows(developer = false, usb = false, wireless = false), read)
    }

    @Test
    fun `adbd running on a user build proves Developer options`() {
        assertEquals(true, SecurityRules.developerOptions(Value("0"), null, null, adbdRunsOnUserBuild = true))
    }

    @Test
    fun `wireless debugging that cannot be read is unknown`() {
        assertEquals(null, SecurityRules.wirelessDebugging(Unreadable))
        assertEquals(null, SecurityRules.wirelessDebugging(Value("2")))
    }
}
