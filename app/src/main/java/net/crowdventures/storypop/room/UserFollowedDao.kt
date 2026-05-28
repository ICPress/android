package net.crowdventures.storypop.room

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.UUID

@Dao
interface UserFollowedDao {
    @Query("SELECT * FROM userfollowed WHERE username == :username AND authorUUID = :authorUUID")
    fun getFollowedForUser( authorUUID: UUID,username: String): LiveData<UserFollowed?>
    @Query("SELECT * FROM userfollowed WHERE username == :username AND authorUUID = :authorUUID")
    fun getFollowedForUserImmediate(authorUUID: UUID,username: String): UserFollowed?
    @Query("SELECT fw.* FROM userfollowed fw left join storypendingupload pu on pu.associatedID = fw.username and pu.resourceType = :pendingUploadTypeExclude  WHERE fw.authorUUID = :authorUUID and pu.associatedID is null ")
    fun getAllFollowedUnfollowedExluded(authorUUID: UUID, pendingUploadTypeExclude:PendingUploadType = PendingUploadType.UNFOLLOW): LiveData<List<UserFollowed>>
    @Query("SELECT * FROM userfollowed fw WHERE authorUUID = :authorUUID")
    fun getAllFollowedImmediate(authorUUID: UUID): List<UserFollowed>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(userFollowed: UserFollowed)
    @Delete
    fun delete(userFollowed: UserFollowed)
    @Query("DELETE FROM userfollowed WHERE authorUUID = :authorUUID and username == :username")
    fun deleteFollowedId(authorUUID: UUID, username: String)

    @Query("SELECT up.associatedID FROM storypendingupload up WHERE up.resourceType = :type and authorUUID=:authorUUID  ORDER BY up.entryTimeStamp DESC")
    fun getAllPendingFollowPagingSource(authorUUID: UUID,type:PendingUploadType = PendingUploadType.FOLLOW): PagingSource<Int, String>

    @Query("SELECT fw.* FROM userfollowed fw left join storypendingupload pu on pu.associatedID = fw.username and pu.resourceType = :pendingUploadTypeExclude  WHERE fw.authorUUID = :authorUUID and pu.associatedID is null ")
    fun getAllFollowedUnfollowedExludedPagingSource(authorUUID: UUID, pendingUploadTypeExclude:PendingUploadType = PendingUploadType.UNFOLLOW):  PagingSource<Int, UserFollowed>
}