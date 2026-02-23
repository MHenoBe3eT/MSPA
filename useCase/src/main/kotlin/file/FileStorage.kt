package file

interface FileStorage {
    fun store(key: String, data: ByteArray, contentType: String)
    fun retrieve(key: String): ByteArray
    fun delete(key: String)
}
