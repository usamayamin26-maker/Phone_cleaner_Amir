package com.phone.cleaner.data.repository

import ai.jhw.storage.domain.repository.StorageRepository
import ai.jhw.storage.model.StorageAccess
import android.content.Intent
import com.phone.cleaner.domain.repository.StorageAccessRepository

class StorageAccessRepositoryImp(
    private val storage: StorageRepository,
) : StorageAccessRepository {

    override fun access(): StorageAccess = storage.access()

    override fun mediaPermissions(): Array<String> = storage.mediaPermissions()

    override fun allFilesAccessIntent(): Intent? = storage.allFilesAccessIntent()
}
