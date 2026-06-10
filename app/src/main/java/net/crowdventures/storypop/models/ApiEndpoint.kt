package net.crowdventures.storypop.models

data class ApiEndpoint(
    val name: String,
    val url: String,
    val cdnTokenEncryptionSecret:String,
    val isDefault: Boolean = false
){
    fun getTokenEncoded(): ByteArray {
        return cdnTokenEncryptionSecret.toByteArray(Charsets.US_ASCII)
    }
}
