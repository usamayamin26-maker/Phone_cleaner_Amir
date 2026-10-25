package com.phone.cleaner.data.mapper

import ai.jhw.junkcleaner.model.JunkItem
import ai.jhw.junkcleaner.model.JunkType
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import ai.jhw.storage.utils.FileKinds
import android.net.Uri
import java.io.File

/**
 * A junk item as a [FileItem], so the cleaner screens can list, select and size it like any other
 * file. A folder item (thumbnails, an empty or residual folder) keeps its own size and is shown as
 * a plain file icon; deleting still goes through the junk repository with the original item.
 */
fun JunkItem.toFileItem(): FileItem = FileItem(
    path = path,
    name = name,
    uri = Uri.fromFile(File(path)),
    sizeBytes = sizeBytes,
    dateModified = lastModified,
    // An empty folder is reported as a plain item, not a directory: it is deleted as one path.
    kind = if (isDirectory || type == JunkType.EMPTY_FOLDERS) FileKind.OTHER else FileKinds.of(name, null),
)
