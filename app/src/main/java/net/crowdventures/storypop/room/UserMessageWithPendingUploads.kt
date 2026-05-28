package net.crowdventures.storypop.room

import androidx.room.Embedded
import androidx.room.Relation

data class UserMessageWithPendingUploads(
    @Embedded val message: UserMessage,
    @Transient
    @Relation(
        parentColumn = "messageUUID",
        entityColumn = "associatedID"
    )
    var storypendingupload: List<StoryPendingUpload>,
    @Transient
    @Relation(
        parentColumn = "messageUUID",
        entityColumn = "resourceIdentifier"
    )
    var storypendinguploadMessage: List<StoryPendingUpload>
)