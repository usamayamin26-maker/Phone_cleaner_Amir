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
 *
 * [junkProbability] is the model's 0..1 score, not a yes or no, so the confidence threshold the
 * AI cleaner judges by can be changed without re-scanning the gallery. It is null in the rows
 * carried over from database version 1, which stored only the verdict: those count as unknown and
 * are classified again.
 */
@Entity(tableName = "ai_scan_results")
data class AiScanResultEntity(
    @PrimaryKey val path: String,
    val dateModified: Long,
    val junkProbability: Float?,
)
