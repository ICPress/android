package net.crowdventures.storypop

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Job
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.room.StoryDatabase

class Constants {
    companion object {
        const val PUBLISHED_STORY_EXTRA = "PUBLISHED_STORY_EXTRA"
        const val PUBLISHED_STORY_HAS_NEW_LIKE = "PUBLISHED_STORY_HAS_NEW_LIKE"
        const val EDIT_MODE_ENABLED_EXTRA = "EDIT_ENABLED"
        const val EDIT_CONTENT_TITLE = "EDIT_CONTENT_TITLE"
        const val EDIT_CONTENT_TEXT = "EDIT_CONTENT_TEXT"
        const val EDIT_CONTENT_TEXT_STYLES = "EDIT_CONTENT_TEXT_STYLES"
        const val EDIT_CONTENT_LOCATION = "EDIT_CONTENT_LOCATION"
        const val EDIT_CONTENT_TAGS = "EDIT_CONTENT_TAGS"
        const val STORY_PUBLISHED = "STORY_PUBLISHED"
        const val STORY_PUBLIC_SOURCES = "STORY_PUBLIC_SOURCES"
        const val STORY_PRIVATE_SOURCES = "STORY_PRIVATE_SOURCES"
        const val STORY_MAP  = "STORY_MAP"
        const val GET_IMAGE_CONTENT_INTENT = 2930
        const val GET_IMAGE_TITLE_CONTENT_INTENT = 2342
        const val MAX_CONTENT_LENGTH = 10000
        const val ADMIN_ROLE ="ADMIN_USER"
        @Volatile
        var uploadJob:Job? = null

        @Volatile
        private var storyDatabase: StoryDatabase? = null
        fun getStoryDatabase(context: Context):StoryDatabase{
            if (storyDatabase == null) storyDatabase = Room.databaseBuilder(context, StoryDatabase::class.java, "StoryDatabase").fallbackToDestructiveMigration().build()
            return storyDatabase!!
        }
        @Volatile
        var loggedInUser :AccountInfoFull? = null

    }
}