package net.crowdventures.storypop.dto

import com.google.gson.annotations.SerializedName

data class AIResponseChoice(
    val index: Long,
    val message: AIMessageDTO,
    @SerializedName("finish_reason")
    val finishReason: String,
)