package file

import domain.file.UploadedFile
import domain.file.UploadedFileId
import domain.user.UserId

interface GetUploadedFile {
    fun byId(id: UploadedFileId): UploadedFile
    fun byIdOrNull(id: UploadedFileId): UploadedFile?
    fun byChecksumAndUserId(checksum: String, userId: UserId): UploadedFile?
}
