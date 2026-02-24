package config

import file.FileStorage
import file.GetUploadedFile
import file.SaveUploadedFile
import file.TriggerAiProcessing
import file.UploadFileUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FileUploadConfig {

    @Bean
    fun uploadFileUseCase(
        fileStorage: FileStorage,
        saveUploadedFile: SaveUploadedFile,
        getUploadedFile: GetUploadedFile,
        triggerAiProcessing: TriggerAiProcessing,
    ): UploadFileUseCase = UploadFileUseCase(fileStorage, saveUploadedFile, getUploadedFile, triggerAiProcessing)
}
