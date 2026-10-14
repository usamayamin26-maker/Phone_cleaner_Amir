package com.phone.cleaner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.phone.cleaner.data.local.dao.AiScanResultDao
import com.phone.cleaner.data.local.entities.AiScanResultEntity

/**
 * The app's local cache. Every table here can be rebuilt by scanning the device again, so nothing
 * in it is the only copy of anything.
 *
 * Bump [version] with every schema change and add a migration; the exported schemas live in
 * app/schemas.
 */
@Database(
    entities = [AiScanResultEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aiScanResultDao(): AiScanResultDao
}
