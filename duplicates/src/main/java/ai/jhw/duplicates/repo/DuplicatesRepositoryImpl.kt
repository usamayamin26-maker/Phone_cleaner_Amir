package ai.jhw.duplicates.repo

import ai.jhw.duplicates.domain.repository.DuplicatesRepository
import ai.jhw.duplicates.model.DuplicateGroup
import ai.jhw.duplicates.model.DuplicateScanEvent
import ai.jhw.duplicates.model.DuplicateScanResult
import ai.jhw.duplicates.utils.BestCopy
import ai.jhw.duplicates.utils.ContentHasher
import ai.jhw.storage.domain.repository.StorageRepository
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DuplicatesRepositoryImpl(
    context: Context,
    private val storage: StorageRepository,
) : DuplicatesRepository {

    private val hasher = ContentHasher(context.contentResolver)

    override fun scan(kinds: Set<FileKind>): Flow<DuplicateScanEvent> = flow {
        // Only files that share an exact size can be duplicates; everything else is never read.
        val candidates: Map<FileKind, List<List<FileItem>>> = kinds.associateWith { kind ->
            storage.queryMedia(kind)
                .filter { it.sizeBytes > 0 }
                .groupBy { it.sizeBytes }
                .values
                .filter { it.size > 1 }
        }
        val total = candidates.values.sumOf { groups -> groups.sumOf { it.size } }.coerceAtLeast(1)
        var done = 0
        val result = ArrayList<DuplicateGroup>()

        for ((kind, sizeGroups) in candidates) {
            for (sameSize in sizeGroups) {
                currentCoroutineContext().ensureActive()
                emit(DuplicateScanEvent.Progress(done.toFloat() / total, kind))
                done += sameSize.size

                val byQuick = sameSize.groupBy { hasher.quickHash(it) }
                for ((quick, sameQuick) in byQuick) {
                    if (quick == null || sameQuick.size < 2) continue
                    val byFull = sameQuick.groupBy { hasher.fullHash(it) }
                    for ((full, identical) in byFull) {
                        if (full == null || identical.size < 2) continue
                        result += DuplicateGroup(full, kind, identical, BestCopy.indexOf(identical))
                    }
                }
            }
        }
        emit(DuplicateScanEvent.Progress(1f, null))
        emit(DuplicateScanEvent.Completed(DuplicateScanResult(result.sortedByDescending { it.wastedBytes })))
    }.flowOn(Dispatchers.IO)

    override suspend fun delete(files: List<FileItem>): DeleteOutcome = storage.delete(files)
}
