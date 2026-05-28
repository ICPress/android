package net.crowdventures.storypop.viewmodels

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.instacart.library.truetime.TrueTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.UserFollowingCached
import net.crowdventures.storypop.dto.UsersFollowingInfo
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.util.ViewModelUtil
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.UUID


class StorySavedViewModelRepository(
    val context: Context,
    val sharedPreferenceManager: SharedPreferenceManager,
    val lifecycleOwner: LifecycleOwner,
    val registerViewModel: RegisterViewModel
) {
    fun getNonPublishedSavedStoryModelFromDatabase(): Flow<PagingData<StorySavedModel>> {
        val deviceAuthorUUID = sharedPreferenceManager.getDefaultAuthorUUID()
        val storyDatabase = Constants.getStoryDatabase(context)
        val pager = Pager(PagingConfig(pageSize = 6)) {
            storyDatabase.storyDao()
                .getAllNonPublishedStoriesForUserPagedPagingSource(deviceAuthorUUID)
        }.flow.map { pagingData ->
            pagingData.map {
                ViewModelUtil.getStoryRoomAsStorySavedModel(it)
            }
        }.flowOn(Dispatchers.IO)

        return pager
    }

    fun getPendingPublishedSavedStoryModelFromDatabase(): Flow<PagingData<StorySavedModel>> {
        val storyDatabase = Constants.getStoryDatabase(context)
        val pager = Pager(PagingConfig(pageSize = 6)) {
            storyDatabase.storyDao().getAllPendingUploadPagingSource()//deviceAuthorUUID
        }.flow.map { pagingData ->
            pagingData.map {
                ViewModelUtil.getStoryRoomAsStorySavedModel(it)
            }
        }.flowOn(Dispatchers.IO)

        return pager
    }

    fun getPendingUserFollowedModelFromDatabase(authorUUID: UUID): Flow<PagingData<UsersFollowingInfo>> {
        val storyDatabase = Constants.getStoryDatabase(context)
        val pager = Pager(PagingConfig(pageSize = 6)) {
            storyDatabase.userFollowedDao()
                .getAllPendingFollowPagingSource(authorUUID)//deviceAuthorUUID
        }.flow.map { pagingData ->
            pagingData.map {
                UsersFollowingInfo(it, null, listOf())
            }
        }.flowOn(Dispatchers.IO)

        return pager
    }

    fun getUserFollowedModelFromDatabase(
        authorUUID: UUID,
        loggedInUser: AccountInfoFull
    ): Flow<PagingData<UsersFollowingInfo>> {
        // return flowOf(PagingData.from<UsersFollowingInfo>(listOf<UsersFollowingInfo>()))
        val storyDatabase = Constants.getStoryDatabase(context)
        val pager = Pager(PagingConfig(pageSize = 6)) {
            storyDatabase.userFollowedDao().getAllFollowedUnfollowedExludedPagingSource(authorUUID)
        }.flow.map { pagingData ->
            val userFollowingCachedJson = sharedPreferenceManager.getLatestUsersFollowedCached()
            var userFollowingCached = listOf<UserFollowingCached>()
            if (userFollowingCachedJson != null) {
                userFollowingCached = Gson().fromJson<List<UserFollowingCached>>(
                    userFollowingCachedJson,
                    object : TypeToken<List<UserFollowingCached>>() {}.type
                )
            }
            pagingData.map {
                val cachedProfile = userFollowingCached.filter { z -> z.username == it.username }
                if (cachedProfile.isNotEmpty()) {
                    UsersFollowingInfo(it.username, cachedProfile.first().profileIcon, listOf())
                } else {
                    UsersFollowingInfo(it.username, null, listOf())
                }

            }

        }.flowOn(Dispatchers.IO)

        return pager
    }

    fun getPublishedRecentStoryModelFromDatabase(): Flow<PagingData<StorySavedModel>> {
        val storyDatabase = Constants.getStoryDatabase(context)
        val pager = Pager(PagingConfig(pageSize = 6)) {
            val currentTimeUTC = if (TrueTime.isInitialized()) DateTime(
                TrueTime.now()
            ).toDateTime(
                DateTimeZone.UTC
            ) else DateTime(DateTimeZone.UTC)
            val sinceDayAgo = currentTimeUTC.minusDays(1)//.minusMinutes(30)
            storyDatabase.storyDao().getPublishedRecent(sinceDayAgo.toString())//deviceAuthorUUID
        }.flow.map { pagingData ->
            pagingData.map {
                ViewModelUtil.getStoryRoomAsStorySavedModel(it)
            }
        }.flowOn(Dispatchers.IO)

        return pager
    }

    fun deleteSavedStory(storyUUID: UUID) {
        val storyDatabase = Constants.getStoryDatabase(context)
        storyDatabase.storyPendingUploadDao().deleteAllForAssociatedID(storyUUID.toString())
        storyDatabase.storyDao().delete(storyUUID)
    }

    fun pushPendingUpload(pendingUpload: StoryPendingUpload) {
        val storyDatabase = Constants.getStoryDatabase(context)
        storyDatabase.storyPendingUploadDao().update(pendingUpload)
    }
}