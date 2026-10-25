package com.phone.cleaner.domain.repository

import ai.jhw.storage.model.StorageAccess
import android.content.Intent

/**
 * What the app may read and delete in shared storage, and how to ask for more. Every cleaner
 * checks this before it scans. These are immediate system reads that cannot fail, so they return
 * their values directly rather than an ApiResult.
 */
interface StorageAccessRepository {

    fun access(): StorageAccess

    /** The runtime permissions that grant media access on this Android version. */
    fun mediaPermissions(): Array<String>

    /** Android 11+: the system page that grants All files access; null below Android 11. */
    fun allFilesAccessIntent(): Intent?
}
