package net.crowdventures.storypop.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.UUID

@Dao
interface StoryCommentDao {
    @Query("SELECT * FROM storycomment WHERE commentUUID == :commentUUID and slugTitle == :slugTitle ")
    fun getCommentForStory(commentUUID: UUID, slugTitle:String): LiveData<StoryComment?>
    @Query("SELECT * FROM storycomment WHERE replyToCommentUUID == :commentUUID and slugTitle == :slugTitle ")
    fun getCommentRepliesForComment(commentUUID: UUID, slugTitle:String): LiveData<StoryComment?>

    @Query("SELECT * FROM storycomment WHERE commentUUID == :commentUUID and slugTitle == :slugTitle ")
    fun getCommentForStoryImmediate(commentUUID: UUID, slugTitle:String):StoryComment?

    @Query("SELECT * FROM storycomment WHERE slugTitle == :slugTitle and authorName = :authorName")
    fun getCommentsForStory(slugTitle:String, authorName:String): LiveData<List<StoryComment>>

    @Query("SELECT * FROM storycomment WHERE slugTitle == :slugTitle and authorName = :authorName")
    fun getCommentsForStoryImmediate(slugTitle:String, authorName:String):List<StoryComment>

    @Query("SELECT A.* FROM storycomment A INNER JOIN storypendingupload B ON A.commentUUID = B.resourceIdentifier  AND B.resourceType = :type WHERE slugTitle == :slugTitle and authorName = :authorName ORDER BY B.entryTimeStamp DESC ")
    fun getPendingCommentsForStoryImmediate(slugTitle:String, authorName:String,type:PendingUploadType = PendingUploadType.COMMENT):List<StoryComment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(storyComment: StoryComment)
    @Delete
    fun delete(storyComment: StoryComment)
}