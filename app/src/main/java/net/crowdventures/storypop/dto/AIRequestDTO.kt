package net.crowdventures.storypop.dto
data class AIRequestDTO(
    val model: String,
    val messages: List<AIMessageDTO>,
)

