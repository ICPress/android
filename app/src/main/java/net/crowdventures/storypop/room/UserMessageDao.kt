package net.crowdventures.storypop.room

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import java.util.UUID

@Dao
interface UserMessageDao {
    @Transaction
    @Query("SELECT * FROM usermessage um WHERE um.targetUsername == :targetUsername and um.authorName = :authorName order by entryTimeStamp desc")
    fun getMessagesForUserWithPendingUpload(targetUsername:String, authorName:String): PagingSource<Int, UserMessageWithPendingUploads>

    @Query("SELECT * FROM usermessage WHERE messageUUID == :messageUUID")
    fun getMessageImmediate(messageUUID: UUID):UserMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(userMessage: UserMessage)
    @Delete
    fun delete(userMessage: UserMessage)
}