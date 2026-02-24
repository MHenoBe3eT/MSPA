package controller

import domain.file.UploadedFile
import domain.file.UploadedFileId
import domain.user.UserId
import file.FileStorage
import file.GetUploadedFile
import file.UploadFileUseCase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/uploaded-files")
class UploadController(
    private val uploadFileUseCase: UploadFileUseCase,
    private val getUploadedFile: GetUploadedFile,
    private val fileStorage: FileStorage,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestPart("file") file: MultipartFile,
        authentication: Authentication,
    ): ResponseEntity<UploadedFileResponse> {
        val userId = authentication.principal as UserId
        val uploadedFile = uploadFileUseCase(
            userId = userId,
            originalFileName = file.originalFilename ?: file.name,
            contentType = file.contentType ?: "application/octet-stream",
            data = file.bytes,
        )
        return ResponseEntity(UploadedFileResponse.fromDomain(uploadedFile), HttpStatus.ACCEPTED)
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<UploadedFileResponse> {
        val file = getUploadedFile.byId(UploadedFileId(id))
        return ResponseEntity.ok(UploadedFileResponse.fromDomain(file))
    }

    @GetMapping("/{id}/download")
    fun download(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<ByteArray> {
        val file = getUploadedFile.byId(UploadedFileId(id))
        val data = fileStorage.retrieve(file.storageKey)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${file.originalFileName}\"")
            .contentType(MediaType.parseMediaType(file.contentType))
            .body(data)
    }
}

data class UploadedFileResponse(
    val id: String,
    val userId: String,
    val originalFileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val status: String,
    val uploadedAt: String,
) {
    companion object {
        fun fromDomain(f: UploadedFile) = UploadedFileResponse(
            id = f.id.value.toString(),
            userId = f.userId.value.toString(),
            originalFileName = f.originalFileName,
            contentType = f.contentType,
            sizeBytes = f.sizeBytes,
            status = f.status.name,
            uploadedAt = f.uploadedAt.toString(),
        )
    }
}
