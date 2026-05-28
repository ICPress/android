package net.crowdventures.storypop.dto

import com.google.gson.annotations.SerializedName

enum class NotificationType {
    @SerializedName("0")
    INFORMATION,
    @SerializedName("1")
    REWARD,
    @SerializedName("2")
    FOLLOW_RECEIVED,
    @SerializedName("3")
    LIKE_RECEIVED,
    @SerializedName("4")
    COMMENT_LIKE_RECEIVED,
    @SerializedName("5")
    COMMENT_REPLY_RECEIVED,
    @SerializedName("6")
    COMMENT_RECEIVED,
    @SerializedName("7")
    GIFT_UNWRAPPED,
    @SerializedName("8")
    MESSAGE_RECEIVED,
    @SerializedName("9")
    ARTICLE_REJECTED
}