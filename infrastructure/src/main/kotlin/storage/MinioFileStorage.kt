package storage

import file.FileStorage
import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream

@Component
class MinioFileStorage(
    private val minioClient: MinioClient,
    @Value("\${minio.bucket}") private val bucket: String,
) : FileStorage {

    @Volatile
    private var bucketEnsured = false

    override fun store(key: String, data: ByteArray, contentType: String) {
        ensureBucket()
        val stream = ByteArrayInputStream(data)
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(key)
                .stream(stream, data.size.toLong(), -1)
                .contentType(contentType)
                .build()
        )
    }

    override fun retrieve(key: String): ByteArray =
        minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucket)
                .`object`(key)
                .build()
        ).readBytes()

    override fun delete(key: String) {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucket)
                .`object`(key)
                .build()
        )
    }

    private fun ensureBucket() {
        if (bucketEnsured) return
        synchronized(this) {
            if (bucketEnsured) return
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            }
            bucketEnsured = true
        }
    }
}
