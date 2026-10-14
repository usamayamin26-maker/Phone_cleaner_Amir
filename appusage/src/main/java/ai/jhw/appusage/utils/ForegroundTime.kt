package ai.jhw.appusage.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager

/**
 * Foreground time per package, counted from activity resume/pause events.
 *
 * UsageStatsManager's aggregated stats come in whole-day buckets, so "today" from them includes
 * part of yesterday. The event log has exact timestamps, which is what both reports need.
 */
internal object ForegroundTime {

    data class Result(val timeMillis: Map<String, Long>, val lastUsedAt: Map<String, Long>)

    fun compute(usm: UsageStatsManager, start: Long, end: Long): Result {
        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()
        // Keyed by package + activity, so two activities of one app do not cancel each other.
        val resumedAt = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        val lastUsed = HashMap<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val key = pkg + "/" + event.className
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    resumedAt[key] = event.timeStamp
                    lastUsed[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    resumedAt.remove(key)?.let { begin ->
                        total[pkg] = (total[pkg] ?: 0L) + (event.timeStamp - begin).coerceAtLeast(0)
                        lastUsed[pkg] = event.timeStamp
                    }
                }
            }
        }
        // Still in the foreground now.
        resumedAt.forEach { (key, begin) ->
            val pkg = key.substringBefore('/')
            total[pkg] = (total[pkg] ?: 0L) + (end - begin).coerceAtLeast(0)
            lastUsed[pkg] = end
        }
        return Result(total, lastUsed)
    }
}
