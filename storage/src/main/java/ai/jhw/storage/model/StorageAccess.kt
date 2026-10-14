package ai.jhw.storage.model

/** How much of shared storage the app can read and delete right now. */
enum class StorageAccess {
    /**
     * All files access (Android 11+) or the legacy storage permissions (Android 10 and older):
     * every cleaner works and deletes need no further consent.
     */
    FULL,

    /** All photos, videos and audio, but not other files. Deleting asks the user per batch. */
    MEDIA_ALL,

    /** Android 14+: only the photos and videos the user picked. */
    MEDIA_PARTIAL,

    NONE,
}
