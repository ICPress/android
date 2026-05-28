package net.crowdventures.storypop.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
   indices = [Index("authorName", name = "authorNameIndex", unique = false),Index("targetUsername", name = "targetUsernameIndex", unique = false)]
)
 data class UserMessage (
    val authorName: String,
    val messageType:MessageType,
    val content:String,
    @PrimaryKey
    val messageUUID:String ,
    val targetUsername: String,
    var deleted:Boolean,
    var entryTimeStamp:String
)

