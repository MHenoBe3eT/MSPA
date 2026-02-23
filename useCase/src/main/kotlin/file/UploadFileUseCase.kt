package file

import domain.file.UploadedFile
import domain.file.UploadedFileId
import domain.file.UploadStatus
import domain.user.UserId
import java.security.MessageDigest
import java.time.Instant

class UploadFileUseCase(
    private val fileStorage: FileStorage,
    private val saveUploadedFile: SaveUploadedFile,
    private val getUploadedFile: GetUploadedFile,
    private val triggerAiProcessing: TriggerAiProcessing,
) {
    companion object {
        private const val MAX_SIZE_BYTES = 10L * 1024 * 1024
        private val ALLOWED_CONTENT_TYPES = setOf(
            "application/pdf",
            "image/jpeg",
            "image/png",
        )
    }

    operator fun invoke(
        userId: UserId,
        originalFileName: String,
        contentType: String,
        data: ByteArray,
    ): UploadedFile {
        if (data.size > MAX_SIZE_BYTES) {
            throw FileTooLargeException("File exceeds 10MB limit (size: ${data.size} bytes)")
        }
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw UnsupportedFileFormatException(
                "Unsupported file type: $contentType. Allowed: ${ALLOWED_CONTENT_TYPES.joinToString()}"
            )
        }

        val checksum = sha256(data)

        getUploadedFile.byChecksumAndUserId(checksum, userId)?.let { return it }

        val fileId = UploadedFileId.generateId()
        val storageKey = "${userId.value}/${fileId.value}/$originalFileName"

        fileStorage.store(storageKey, data, contentType)

        val uploadedFile = UploadedFile(
            id = fileId,
            userId = userId,
            originalFileName = originalFileName,
            contentType = contentType,
            sizeBytes = data.size.toLong(),
            checksum = checksum,
            storageKey = storageKey,
            status = UploadStatus.UPLOADED,
            uploadedAt = Instant.now(),
        )

        val saved = saveUploadedFile.save(uploadedFile)
        triggerAiProcessing.trigger(saved.id)
        return saved
    }

    private fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }
}
