package file

import domain.file.UploadedFileId
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

// Stub implementation — Phase 3 will replace this with AsyncAiProcessingTrigger
@Component
class StubAiProcessingTrigger : TriggerAiProcessing {
    override fun trigger(uploadedFileId: UploadedFileId) {
        log.info { "AI processing triggered for file ${uploadedFileId.value} (stub)" }
    }
}
