package ai.jhw.deviceinfo.repo

import ai.jhw.deviceinfo.domain.repository.DeviceInfoRepository
import ai.jhw.deviceinfo.model.CameraDetails
import ai.jhw.deviceinfo.model.DeviceDetails
import ai.jhw.deviceinfo.model.DisplayDetails
import ai.jhw.deviceinfo.model.ProcessorDetails
import ai.jhw.deviceinfo.model.RamDetails
import ai.jhw.deviceinfo.model.StorageDetails
import ai.jhw.deviceinfo.model.WifiDetails
import ai.jhw.deviceinfo.utils.CameraReader
import ai.jhw.deviceinfo.utils.DisplayReader
import ai.jhw.deviceinfo.utils.MemoryReader
import ai.jhw.deviceinfo.utils.ProcessorReader
import ai.jhw.deviceinfo.utils.RootChecker
import ai.jhw.deviceinfo.utils.WifiReader
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceInfoRepositoryImpl(private val context: Context) : DeviceInfoRepository {

    @SuppressLint("HardwareIds") // Shown to the user on the Device Information screen, nothing else.
    override suspend fun getDevice(): DeviceDetails = withContext(Dispatchers.IO) {
        DeviceDetails(
            brand = Build.BRAND,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() },
            buildId = Build.DISPLAY,
            fingerprint = Build.FINGERPRINT,
            kernelVersion = System.getProperty("os.version"),
            androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID),
            uptimeMillis = SystemClock.elapsedRealtime(),
            isRooted = RootChecker.isRooted(),
        )
    }

    override suspend fun getProcessor(): ProcessorDetails =
        withContext(Dispatchers.IO) { ProcessorReader.read() }

    override suspend fun getWifi(): WifiDetails =
        withContext(Dispatchers.IO) { WifiReader.read(context) }

    override suspend fun getDisplay(): DisplayDetails =
        withContext(Dispatchers.IO) { DisplayReader.read(context) }

    override suspend fun getRam(): RamDetails =
        withContext(Dispatchers.IO) { MemoryReader.ram(context) }

    override suspend fun getStorage(): StorageDetails =
        withContext(Dispatchers.IO) { MemoryReader.storage(context) }

    override suspend fun getCameras(): List<CameraDetails> =
        withContext(Dispatchers.IO) { CameraReader.read(context) }
}
