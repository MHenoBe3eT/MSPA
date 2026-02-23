package file

import domain.file.UploadedFileId

interface TriggerAiProcessing {
    fun trigger(uploadedFileId: UploadedFileId)
}
