package ai

import domain.file.UploadedFileId
import file.TriggerAiProcessing
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class AsyncAiProcessingTrigger(
    private val processUploadedFileUseCase: ProcessUploadedFileUseCase,
) : TriggerAiProcessing {

    @Async
    override fun trigger(uploadedFileId: UploadedFileId) {
        log.info { "Triggering async AI processing for file ${uploadedFileId.value}" }
        processUploadedFileUseCase(uploadedFileId)
    }
}
