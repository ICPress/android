package net.crowdventures.storypop.dto

import com.google.gson.annotations.SerializedName

enum class TransactionDescriptionType {
    @SerializedName("0")
    LIKE_RECEIVED,
    @SerializedName("1")
    FIRST_POST_REWARD,
    @SerializedName("2")
    FIRST_LIKE_RECEIVED_REWARD,
    @SerializedName("3")
    FIRST_LIKE_SENT_REWARD,
    @SerializedName("4")
    REWARD_CLAIMED,
    @SerializedName("5")
    FIRST_FOLLOW,
    @SerializedName("6")
    FOLLOW_RECEIVED,
    @SerializedName("7")
    SPECIAL_REWARD,
    @SerializedName("8")
    COMMENT_LIKE_REWARD,
    @SerializedName("9")
    COMMENT_REPLY_REWARD
}