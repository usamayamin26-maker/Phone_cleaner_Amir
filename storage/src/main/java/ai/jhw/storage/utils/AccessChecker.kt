package ai.jhw.storage.utils

import ai.jhw.storage.model.FileKind
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

    /**
     * The runtime permissions for [kinds]. From Android 13 photos, videos and audio are separate
     * permissions, so a feature asks only for what it reads; before that one permission covers all.
     */
    fun mediaPermissions(kinds: Set<FileKind>): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> buildList {
            if (FileKind.IMAGE in kinds) add(Manifest.permission.READ_MEDIA_IMAGES)
            if (FileKind.VIDEO in kinds) add(Manifest.permission.READ_MEDIA_VIDEO)
            if (FileKind.AUDIO in kinds) add(Manifest.permission.READ_MEDIA_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                (FileKind.IMAGE in kinds || FileKind.VIDEO in kinds)
            ) {
                add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        }.toTypedArray()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }
}
