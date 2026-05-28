package net.crowdventures.storypop.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.UUID

@Dao
interface StoryLikedDao {
    @Query("SELECT * FROM storyliked WHERE storyId == :storyID and authorUUID == :authorUUID")
    fun getLikeForStory(storyID: String, authorUUID: UUID): LiveData<StoryLiked?>
    @Query("SELECT * FROM storyliked WHERE storyId == :storyID and authorUUID == :authorUUID")
    fun getLikeForStoryImmediate(storyID: String, authorUUID: UUID): StoryLiked?
    @Query("SELECT * FROM storyliked WHERE authorUUID == :authorUUID")
    fun getAllLikes(authorUUID: UUID): LiveData<List<StoryLiked>>
    @Query("SELECT * FROM storyliked WHERE authorUUID == :authorUUID")
    fun getAllLikesImmediate(authorUUID: UUID): LiveData<List<StoryLiked>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(storyLiked: StoryLiked)
    @Delete
    fun delete(storyLiked: StoryLiked)
    @Query("DELETE FROM storyliked WHERE storyId == :storyID and authorUUID == :authorUUID")
    fun deleteStoryLikedId(storyID: String, authorUUID: UUID)
}