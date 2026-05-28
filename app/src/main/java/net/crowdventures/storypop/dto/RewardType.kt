package net.crowdventures.storypop.dto

import com.google.gson.annotations.SerializedName

enum class RewardType {
    @SerializedName("0")
    VOUCHER,
    @SerializedName("1")
    TOKEN,
    @SerializedName("2")
    COIN,
    @SerializedName("3")
    OTHER,
    @SerializedName("4")
    CREDITS,
    @SerializedName("5")
    BADGE,
    @SerializedName("6")
    EMOJI
}