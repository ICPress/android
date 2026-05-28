package net.crowdventures.storypop.room
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    indices = [Index("originalStoryUUID", name = "OriginalStoryIdIndex", unique = false), Index("authorUserUUID", name = "AuthorUserUUIDIndex", unique = false)]
)
data class StoryRoom (
    @PrimaryKey val storyId:UUID,
    var entryTimeStamp:String,
    var jsonStoryData:String,
    var originalStoryUUID:UUID,
    var authorUserUUID:UUID,
    var isPublished:Boolean,
    var isUploaded:Boolean = false,
    var slugTitle:String? = null
        )
