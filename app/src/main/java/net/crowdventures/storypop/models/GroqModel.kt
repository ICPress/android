package net.crowdventures.storypop.models

data class GroqModel(
    val id: String,
    val displayName: String,
    val description: String,
    val rpm: Int,  // Requests per minute
    val tpm: Int,   // Tokens per minute
    val contextWindow: Int,
    val isFree: Boolean = true
)