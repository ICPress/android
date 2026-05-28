package net.crowdventures.storypop.models

data class ApiEndpoint(
    val name: String,
    val url: String,
    val isDefault: Boolean = false
)
