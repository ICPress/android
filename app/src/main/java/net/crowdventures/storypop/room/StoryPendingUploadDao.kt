package net.crowdventures.storypop.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.UUID

@Dao
interface StoryPendingUploadDao {
    @Query("SELECT * FROM storypendingupload WHERE associatedID == :associatedId and authorUUID == :authorUUID ORDER BY entryTimeStamp ASC")
    fun getAllPendingForStory(associatedId: String, authorUUID: UUID): LiveData<List<StoryPendingUpload>>
    @Query("SELECT * FROM storypendingupload WHERE associatedID == :associatedId  and authorUUID == :authorUUID ORDER BY entryTimeStamp ASC")
    fun getAllPendingForStoryImmediate(associatedId: String, authorUUID: UUID): List<StoryPendingUpload>

    @Query("SELECT * FROM storypendingupload where authorUUID == :authorUUID ORDER BY entryTimeStamp ASC")
    fun getAllPending(authorUUID: UUID): LiveData<List<StoryPendingUpload>>
    @Query("SELECT * FROM storypendingupload where authorUUID == :authorUUID ORDER BY entryTimeStamp ASC")
    fun getAllPendingImmediate(authorUUID: UUID): List<StoryPendingUpload>

    @Query("SELECT * FROM storypendingupload WHERE associatedID == :associatedId and authorUUID == :authorUUID")
    fun getAllForAssociatedID(authorUUID: UUID, associatedId: String): LiveData<List<StoryPendingUpload>>
    @Query("SELECT * FROM storypendingupload WHERE associatedID == :associatedId and authorUUID == :authorUUID")
    fun getAllForAssociatedIDImmediate(authorUUID: UUID, associatedId: String): List<StoryPendingUpload>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(pendingUpload: StoryPendingUpload)
    @Delete
    fun delete(pendingUpload: StoryPendingUpload)

    @Query("DELETE FROM storypendingupload WHERE associatedID == :associatedId")
    fun deleteAllForAssociatedID(associatedId: String)
}