package net.crowdventures.storypop.room

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface StoryDao {
    @Query("SELECT * FROM storyroom WHERE originalStoryUUID == :originalStoryUUID ORDER BY entryTimeStamp DESC")
    fun getAllVersions(originalStoryUUID: UUID): LiveData<List<StoryRoom>>
    @Query("SELECT * FROM storyroom WHERE authorUserUUID == :authorUserUUID ORDER BY entryTimeStamp DESC")
    fun getAllForUser(authorUserUUID: UUID): LiveData<List<StoryRoom>>

    @Query("SELECT * FROM storyroom WHERE authorUserUUID == :authorUserUUID ORDER BY entryTimeStamp DESC")
    fun getAllStoriesForUserLiveData(authorUserUUID: UUID): LiveData<List<StoryRoom>>
    @Query("SELECT * FROM storyroom WHERE authorUserUUID == :authorUserUUID and isPublished == 0 ORDER BY entryTimeStamp DESC")
    fun getAllNonPublishedStoriesForUserPagedPagingSource(authorUserUUID: UUID): PagingSource<Int, StoryRoom>
    @Query("SELECT * FROM storyroom WHERE authorUserUUID == :authorUserUUID and isPublished == 0 ORDER BY entryTimeStamp DESC")
    fun getAllStoriesForUserPagedFactory(authorUserUUID: UUID): DataSource.Factory<Int, StoryRoom>
    @Query("SELECT * FROM storyroom WHERE storyId == :storyId")
    fun get(storyId:UUID): LiveData<StoryRoom>
    @Query("SELECT * FROM storyroom WHERE storyId == :storyId")
    fun getImmediate(storyId:UUID): StoryRoom
    @Query("SELECT * FROM storyroom WHERE slugTitle == :slugTitle AND isPublished == 1")
    fun getImmediatePublished(slugTitle:String): StoryRoom?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(storyRoom: StoryRoom)
    @Delete
    fun delete(storyRoom: StoryRoom)
    @Query("DELETE FROM storyroom WHERE storyId == :storyId")
    fun delete(storyId:UUID)
    @Query("SELECT * FROM storyroom WHERE isPublished = 1 and isUploaded = 1 AND entryTimeStamp >= :fromTimeStamp ORDER BY entryTimeStamp DESC") //LIMIT 1
    fun getPublishedRecent(fromTimeStamp:String): PagingSource<Int, StoryRoom>
    @Query("SELECT * FROM storyroom WHERE isPublished = 1 and isUploaded = 1 AND entryTimeStamp >= :fromTimeStamp ORDER BY entryTimeStamp DESC") //LIMIT 1
    fun getPublishedRecentLiveData(fromTimeStamp:String):  LiveData<List<StoryRoom>>
    @Query("SELECT A.* FROM storyroom A INNER JOIN storypendingupload B ON A.storyId = B.resourceIdentifier and A.isPublished = 1 AND (B.resourceType = :type OR B.resourceType = :type2) ORDER BY A.entryTimeStamp DESC")
    fun getAllPendingUploadPagingSource(type:PendingUploadType = PendingUploadType.STORY, type2:PendingUploadType = PendingUploadType.UPDATE_STORY): PagingSource<Int, StoryRoom>
    @Query("SELECT A.* FROM storyroom A INNER JOIN storypendingupload B ON A.storyId = B.resourceIdentifier and A.isPublished = 1 AND (B.resourceType = :type OR B.resourceType = :type2)")
    fun getAllPendingCreateUpdate(type:PendingUploadType = PendingUploadType.STORY, type2:PendingUploadType = PendingUploadType.UPDATE_STORY): Flow<List<StoryRoom>>
    @Query("SELECT A.* FROM storyroom A INNER JOIN storypendingupload B ON A.storyId = B.associatedID and A.isPublished = 1 AND B.resourceType = :type ORDER BY A.entryTimeStamp DESC")
    fun getAllPendingUpload(type:PendingUploadType = PendingUploadType.STORY): LiveData<StoryRoom>
    @Query("SELECT A.* FROM storyroom A INNER JOIN storypendingupload B ON A.storyId = B.associatedID and A.isPublished = 1 AND B.resourceType = :type ORDER BY A.entryTimeStamp DESC")
    fun getAllPendingUploadImmediate(type:PendingUploadType = PendingUploadType.STORY): StoryRoom
}