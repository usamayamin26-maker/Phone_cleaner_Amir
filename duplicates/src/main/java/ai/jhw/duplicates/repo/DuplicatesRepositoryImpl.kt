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
        // Every candidate is read twice at most: a quick hash, and a full hash when the quick ones
        // match. Each read is one step of the progress, and the steps a file skips are counted as
        // done when its group finishes.
        val total = candidates.values.sumOf { groups -> groups.sumOf { it.size } }.times(2).coerceAtLeast(1)
        var done = 0
        val result = ArrayList<DuplicateGroup>()

        for ((kind, sizeGroups) in candidates) {
            for (sameSize in sizeGroups) {
                val groupEnd = done + sameSize.size * 2
                val byQuick = LinkedHashMap<String?, MutableList<FileItem>>()
                for (file in sameSize) {
                    currentCoroutineContext().ensureActive()
                    byQuick.getOrPut(hasher.quickHash(file)) { ArrayList() } += file
                    done++
                    emit(DuplicateScanEvent.Progress(done.toFloat() / total, kind))
                }
                for ((quick, sameQuick) in byQuick) {
                    if (quick == null || sameQuick.size < 2) continue
                    val byFull = LinkedHashMap<String?, MutableList<FileItem>>()
                    for (file in sameQuick) {
                        currentCoroutineContext().ensureActive()
                        byFull.getOrPut(hasher.fullHash(file)) { ArrayList() } += file
                        done++
                        emit(DuplicateScanEvent.Progress(done.toFloat() / total, kind))
                    }
                    for ((full, identical) in byFull) {
                        if (full == null || identical.size < 2) continue
                        result += DuplicateGroup(full, kind, identical, BestCopy.indexOf(identical))
                    }
                }
                done = groupEnd
            }
        }
        emit(DuplicateScanEvent.Progress(1f, null))
        emit(DuplicateScanEvent.Completed(DuplicateScanResult(result.sortedByDescending { it.wastedBytes })))
    }.flowOn(Dispatchers.IO)

    override suspend fun delete(files: List<FileItem>): DeleteOutcome = storage.delete(files)
}
