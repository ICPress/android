package net.crowdventures.storypop.dto

import com.google.gson.annotations.SerializedName

enum class RewardRarity {
    @SerializedName("0")
    STANDARD,
    @SerializedName("1")
    RARE,
    @SerializedName("2")
    EPIC,
    @SerializedName("3")
    LEGENDARY
}