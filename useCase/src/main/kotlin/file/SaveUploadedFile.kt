package file

import domain.file.UploadedFile

interface SaveUploadedFile {
    fun save(uploadedFile: UploadedFile): UploadedFile
}
