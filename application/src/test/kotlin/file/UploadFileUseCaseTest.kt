package file

import domain.file.UploadStatus
import domain.file.UploadedFile
import domain.file.UploadedFileId
import domain.user.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class UploadFileUseCaseTest {

    private lateinit var fileStorage: FileStorage
    private lateinit var saveUploadedFile: SaveUploadedFile
    private lateinit var getUploadedFile: GetUploadedFile
    private lateinit var triggerAiProcessing: TriggerAiProcessing
    private lateinit var useCase: UploadFileUseCase

    private val userId = UserId.generateId()

    @BeforeEach
    fun setUp() {
        fileStorage = mockk(relaxed = true)
        saveUploadedFile = mockk()
        getUploadedFile = mockk()
        triggerAiProcessing = mockk(relaxed = true)
        useCase = UploadFileUseCase(fileStorage, saveUploadedFile, getUploadedFile, triggerAiProcessing)
    }

    // Тест-кейс 21: размер файла превышает 10MB
    @Test
    fun `should throw FileTooLargeException when file exceeds 10MB`() {
        val oversizedData = ByteArray(10 * 1024 * 1024 + 1)

        assertThrows<FileTooLargeException> {
            useCase(userId, "test.pdf", "application/pdf", oversizedData)
        }

        verify(exactly = 0) { fileStorage.store(any(), any(), any()) }
        verify(exactly = 0) { saveUploadedFile.save(any()) }
    }

    // Тест-кейс 22: ровно 10MB — валидный файл
    @Test
    fun `should accept file exactly at 10MB limit`() {
        val exactSizeData = ByteArray(10 * 1024 * 1024)
        val fileSlot = slot<UploadedFile>()

        every { getUploadedFile.byChecksumAndUserId(any(), userId) } returns null
        every { saveUploadedFile.save(capture(fileSlot)) } answers { fileSlot.captured }

        val result = useCase(userId, "max.pdf", "application/pdf", exactSizeData)

        assertNotNull(result)
        assertEquals(10L * 1024 * 1024, result.sizeBytes)
    }

    // Тест-кейс 23: неподдерживаемый формат файла
    @Test
    fun `should throw UnsupportedFileFormatException for unsupported content type`() {
        val data = ByteArray(100)

        assertThrows<UnsupportedFileFormatException> {
            useCase(userId, "test.doc", "application/msword", data)
        }

        verify(exactly = 0) { fileStorage.store(any(), any(), any()) }
        verify(exactly = 0) { saveUploadedFile.save(any()) }
    }

    // Тест-кейс 24: поддерживаемые форматы
    @Test
    fun `should accept PDF content type`() {
        val data = ByteArray(100)
        val fileSlot = slot<UploadedFile>()

        every { getUploadedFile.byChecksumAndUserId(any(), userId) } returns null
        every { saveUploadedFile.save(capture(fileSlot)) } answers { fileSlot.captured }

        val result = useCase(userId, "doc.pdf", "application/pdf", data)

        assertEquals("application/pdf", result.contentType)
        assertEquals(UploadStatus.UPLOADED, result.status)
    }

    @Test
    fun `should accept JPEG content type`() {
        val data = ByteArray(100)
        val fileSlot = slot<UploadedFile>()

        every { getUploadedFile.byChecksumAndUserId(any(), userId) } returns null
        every { saveUploadedFile.save(capture(fileSlot)) } answers { fileSlot.captured }

        val result = useCase(userId, "scan.jpg", "image/jpeg", data)

        assertEquals("image/jpeg", result.contentType)
    }

    @Test
    fun `should accept PNG content type`() {
        val data = ByteArray(100)
        val fileSlot = slot<UploadedFile>()

        every { getUploadedFile.byChecksumAndUserId(any(), userId) } returns null
        every { saveUploadedFile.save(capture(fileSlot)) } answers { fileSlot.captured }

        val result = useCase(userId, "scan.png", "image/png", data)

        assertEquals("image/png", result.contentType)
    }

    // Тест-кейс 25: дубликат файла по checksum — возвращает существующий
    @Test
    fun `should return existing file when duplicate checksum detected for same user`() {
        val data = "same content".toByteArray()
        val existingFile = UploadedFile(
            id = UploadedFileId.generateId(),
            userId = userId,
            originalFileName = "original.pdf",
            contentType = "application/pdf",
            sizeBytes = data.size.toLong(),
            checksum = "some-checksum",
            storageKey = "key/path",
            status = UploadStatus.PROCESSED,
            uploadedAt = Instant.now(),
        )

        every { getUploadedFile.byChecksumAndUserId(any(), userId) } returns existingFile

        val result = useCase(userId, "duplicate.pdf", "application/pdf", data)

        assertEquals(existingFile.id, result.id)
        assertEquals(existingFile.status, result.status)
        verify(exactly = 0) { fileStorage.store(any(), any(), any()) }
        verify(exactly = 0) { saveUploadedFile.save(any()) }
    }

    // Тест-кейс 26: дубликат у другого пользователя не считается дубликатом
    @Test
    fun `should store file even if same checksum exists for different user`() {
        val data = "same content".toByteArray()
        val fileSlot = slot<UploadedFile>()

        every { getUploadedFile.byChecksumAndUserId(any(), userId) } returns null
        every { saveUploadedFile.save(capture(fileSlot)) } answers { fileSlot.captured }

        val result = useCase(userId, "test.pdf", "application/pdf", data)

        assertNotNull(result.id)
        verify(exactly = 1) { fileStorage.store(any(), any(), any()) }
        verify(exactly = 1) { saveUploadedFile.save(any()) }
    }

    // Тест-кейс 27: SHA-256 checksum вычисляется корректно
    @Test
    fun `should compute consistent SHA-256 checksum`() {
        val data = "consistent content".toByteArray()
        val fileSlot = slot<UploadedFile>()

        every { getUploadedFile.byChecksumAndUserId(any(), userId) } returns null
        every { saveUploadedFile.save(capture(fileSlot)) } answers { fileSlot.captured }

        useCase(userId, "file.pdf", "application/pdf", data)

        val checksum = fileSlot.captured.checksum
        assertNotNull(checksum)
        assertEquals(64, checksum.length) // SHA-256 hex is 64 chars
        assert(checksum.matches(Regex("[0-9a-f]+")))
    }

    // Тест-кейс 28: AI processing triggered after successful upload
    @Test
    fun `should trigger AI processing after successful upload`() {
        val data = ByteArray(100)
        val fileSlot = slot<UploadedFile>()

        every { getUploadedFile.byChecksumAndUserId(any(), userId) } returns null
        every { saveUploadedFile.save(capture(fileSlot)) } answers { fileSlot.captured }

        useCase(userId, "doc.pdf", "application/pdf", data)

        verify(exactly = 1) { triggerAiProcessing.trigger(any()) }
    }

    // Тест-кейс 29: storage key contains userId and fileId
    @Test
    fun `should build storage key with userId and fileId`() {
        val data = ByteArray(100)
        val fileSlot = slot<UploadedFile>()

        every { getUploadedFile.byChecksumAndUserId(any(), userId) } returns null
        every { saveUploadedFile.save(capture(fileSlot)) } answers { fileSlot.captured }

        useCase(userId, "report.pdf", "application/pdf", data)

        val storageKey = fileSlot.captured.storageKey
        assert(storageKey.startsWith(userId.value.toString()))
        assert(storageKey.contains(fileSlot.captured.id.value.toString()))
        assert(storageKey.endsWith("report.pdf"))
    }
}
