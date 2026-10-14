package com.phone.cleaner.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.phone.cleaner.data.local.entities.AiScanResultEntity

@Dao
interface AiScanResultDao {

    @Query("SELECT * FROM ai_scan_results")
    suspend fun getAll(): List<AiScanResultEntity>

    @Upsert
    suspend fun upsertAll(results: List<AiScanResultEntity>)

    /** Called after files are deleted, so a later scan does not report them again. */
    @Query("DELETE FROM ai_scan_results WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("DELETE FROM ai_scan_results")
    suspend fun clear()
}
