package ai.jhw.storage.utils

import ai.jhw.storage.model.StorageAccess
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

internal object AccessChecker {

    fun access(context: Context): StorageAccess {
        fun granted(permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager() ->
                StorageAccess.FULL
            // Android 10 runs in legacy storage mode (requestLegacyExternalStorage), so the old
            // write permission still covers every file there.
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                granted(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> StorageAccess.FULL
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> when {
                granted(Manifest.permission.READ_MEDIA_IMAGES) &&
                    granted(Manifest.permission.READ_MEDIA_VIDEO) -> StorageAccess.MEDIA_ALL
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ->
                    StorageAccess.MEDIA_PARTIAL
                else -> StorageAccess.NONE
            }
            granted(Manifest.permission.READ_EXTERNAL_STORAGE) -> StorageAccess.MEDIA_ALL
            else -> StorageAccess.NONE
        }
    }

    fun mediaPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }
}
