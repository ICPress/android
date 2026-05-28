package net.crowdventures.storypop.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity()
data class StoryLiked (
    @PrimaryKey
    val storyId: String,
    val storyTitle: String,
    val entryTimeStamp:String,
    val authorUUID: UUID
)