package net.crowdventures.storypop.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    indices = [Index("associatedID", name = "AssociatedIDIndex", unique = false), Index("authorUUID", name = "AuthorUUIDIndex", unique = false)]
)
data class StoryPendingUpload (
    @PrimaryKey
    val pendingUploadUUID: UUID,
    val entryTimeStamp:String,
    val resourceIdentifier:String,
    val resourceType :PendingUploadType,
    val associatedID:String,
    val authorUUID: UUID
)