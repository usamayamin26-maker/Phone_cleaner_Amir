package com.phone.cleaner.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * What the AI (Artificial Intelligence) Junk Cleaner's model said about one gallery image.
 *
 * Classifying a whole gallery on every scan is slow, so each verdict is kept here and reused while
 * the file is unchanged. [dateModified] is the file's own timestamp at classification time: when
 * the file on disk reports a different one, the cached verdict is stale and the image is
 * classified again.
 */
@Entity(tableName = "ai_scan_results")
data class AiScanResultEntity(
    @PrimaryKey val path: String,
    val dateModified: Long,
    val isJunk: Boolean,
)
