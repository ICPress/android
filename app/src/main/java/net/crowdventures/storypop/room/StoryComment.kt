package net.crowdventures.storypop.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
   indices = [Index("slugTitle", name = "storySlugTitleIndex", unique = false),Index("replyToCommentUUID", name = "replyToCommentIndex", unique = false)]
)
 data class StoryComment (
    val slugTitle: String,
    val authorName: String,
    val comment:String,
    @PrimaryKey
    val commentUUID:String ,
    val replyToCommentUUID: String?,
    var entryTimeStamp:String,
)
