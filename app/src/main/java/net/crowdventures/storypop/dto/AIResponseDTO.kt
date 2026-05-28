package net.crowdventures.storypop.dto

import com.google.gson.annotations.SerializedName

data class AIResponseDTO(
    val id: String,
    @SerializedName("object")
    val object_field: String,
    val created: Long,
    val model: String,
    val choices: List<AIResponseChoice>,
)

