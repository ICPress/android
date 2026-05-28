package net.crowdventures.storypop.dto

import net.crowdventures.storypop.room.MessageType

class UserMessagePublishedNotification(
     messageId:UInt,
     authorName: String,
     messageType: MessageType,
     content:String,
     messageUUID:String,
     targetUsername: String,
     deleted: Boolean,
     timestamp:String,
     val contactApproved:Boolean,
     val additionalMessages :Int
):UserMessagePublished( messageId,
    authorName,
    messageType,
    content,
    messageUUID,
    targetUsername,
    deleted,
    timestamp) {
}