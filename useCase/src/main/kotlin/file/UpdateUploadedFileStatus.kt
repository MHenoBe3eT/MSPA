package file

import domain.file.UploadedFile
import domain.file.UploadedFileId
import domain.file.UploadStatus

interface UpdateUploadedFileStatus {
    fun update(id: UploadedFileId, status: UploadStatus): UploadedFile
}
