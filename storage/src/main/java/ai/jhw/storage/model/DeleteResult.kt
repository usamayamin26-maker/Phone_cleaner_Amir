package ai.jhw.storage.model

import android.content.IntentSender

data class DeleteResult(
    val deletedPaths: List<String>,
    val failedPaths: List<String>,
    val freedBytes: Long,
)

/**
 * What a delete led to. Without [StorageAccess.FULL], Android 11+ asks the user to confirm the
 * deletion of media the app does not own: the app launches [NeedsUserConsent.intentSender] with
 * `ActivityResultContracts.StartIntentSenderForResult`, and on RESULT_OK calls
 * [ai.jhw.storage.domain.repository.StorageRepository.confirmDeleted] with the same items.
 */
sealed interface DeleteOutcome {
    data class Completed(val result: DeleteResult) : DeleteOutcome
    data class NeedsUserConsent(val intentSender: IntentSender, val items: List<FileItem>) :
        DeleteOutcome
}
