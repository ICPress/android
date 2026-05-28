package net.crowdventures.storypop.dto

import com.google.gson.annotations.SerializedName

enum class TransactionType {
    @SerializedName("0")
    LIKE_RECEIVED,
    @SerializedName("1")
    STORY_POINTS_REWARD,
    @SerializedName("2")
    REWARD_CLAIMED,

}