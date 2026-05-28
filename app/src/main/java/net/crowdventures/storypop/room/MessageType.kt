package net.crowdventures.storypop.room

import com.google.gson.annotations.SerializedName

enum class MessageType {
    @SerializedName("0")
    TEXT,
    @SerializedName("1")
    IMAGES
}