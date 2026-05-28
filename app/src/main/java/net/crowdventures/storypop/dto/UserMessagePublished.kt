package net.crowdventures.storypop.dto

import net.crowdventures.storypop.room.MessageType
import net.crowdventures.storypop.room.UserMessage

open class UserMessagePublished (
    val messageId:UInt,
    val authorName: String,
    val messageType: MessageType,
    val content:String,
    val messageUUID:String ,
    val targetUsername: String,
    var deleted: Boolean,
    var timestamp:String,
){
    fun toUserMessage():UserMessage{
        return UserMessage(authorName, messageType, content, messageUUID, targetUsername,deleted,timestamp)
    }
}

