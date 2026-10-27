package com.phone.cleaner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aiScanResultDao(): AiScanResultDao

    companion object {
        /**
         * Version 1 stored the AI cleaner's verdict as a boolean, so the confidence behind it could
         * not be recovered and the threshold could not be changed without re-scanning. Version 2
         * stores the model's probability instead.
         *
         * The old boolean cannot be turned into a probability, so each row keeps its path and
         * timestamp with no probability at all: the scan treats that as unknown and classifies the
         * file again. SQLite before 3.35 cannot drop a column, so the table is rebuilt.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ai_scan_results_new` (`path` TEXT NOT NULL, " +
                        "`dateModified` INTEGER NOT NULL, `junkProbability` REAL, PRIMARY KEY(`path`))"
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO `ai_scan_results_new` (`path`, `dateModified`, `junkProbability`) " +
                        "SELECT `path`, `dateModified`, NULL FROM `ai_scan_results`"
                )
                db.execSQL("DROP TABLE `ai_scan_results`")
                db.execSQL("ALTER TABLE `ai_scan_results_new` RENAME TO `ai_scan_results`")
            }
        }
    }
}
