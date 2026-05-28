package net.crowdventures.storypop.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = arrayOf(StoryRoom::class,StoryPendingUpload::class, StoryLiked::class, UserFollowed::class, StoryComment::class, UserMessage::class), version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class StoryDatabase  : RoomDatabase()  {
    abstract fun storyDao(): StoryDao
    abstract fun storyPendingUploadDao(): StoryPendingUploadDao
    abstract fun storyLikedDao() : StoryLikedDao
    abstract fun userFollowedDao() : UserFollowedDao
    abstract fun storyCommentDao() : StoryCommentDao
    abstract fun userMessageDao() : UserMessageDao
}