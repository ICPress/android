package net.crowdventures.storypop.paging

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.UserFollowingCached
import net.crowdventures.storypop.dto.UsersFollowingInfo
import net.crowdventures.storypop.room.StoryDatabase
import net.crowdventures.storypop.room.UserFollowed
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.endpoints.ArticleEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

class UsersFollowedStorySource(val applicationContext:Context,
    val storyDatabase: StoryDatabase,
    val registerViewModel: RegisterViewModel,
    val sharedPreferenceManager: SharedPreferenceManager
) :
    PagingSource<Int, UsersFollowingInfo>() {

    override fun getRefreshKey(state: PagingState<Int, UsersFollowingInfo>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UsersFollowingInfo> {
         try {
            val nextPage = params.key ?: 0
            var nextKey: Int? = null
            val hasCache = sharedPreferenceManager.hasUsersFollowedCached()
            val requestCount = if (hasCache) 10 else 99
            val prevKey = if (nextPage > 0) nextPage - 1 else null
            //val path: Uri = Uri.parse(Config.APP_RESOURCE_PATH + R.drawable._18a64fb_8d00_4de7_ae08_f7504a964da1_0433360909376658504236912253)
            // val userList =  listOf(User(1,"sdddba@jfej.com","neiwfw","ewenfw",path))// RetrofitClient.apiService.getUserList(page = nextPage)
            //"https://m.media-amazon.com/images/M/MV5BMTYwOTEwNjAzMl5BMl5BanBnXkFtZTcwODc5MTUwMw@@._V1_FMjpg_UX1000_.jpg"
            Log.v(Config.logTag, "Fetching followed users..")
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().registerTypeAdapter(
                            UInt::class.java,
                            NullableUintJson()
                        ).create()
                    )
                )
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val service: ArticleEndpoint = restAdapter.create(ArticleEndpoint::class.java)
            val loggedInUser =
                Constants.loggedInUser ?: return LoadResult.Error(Exception("user_uuid is null"))
            val loggedInUserUUID = UUID.fromString(loggedInUser.user_uuid)
            val followingResponse = service.getUsersFollowed(
                "Bearer ${loggedInUser.refreshToken}",
                loggedInUser.username,
                loggedInUser.followingLatestCheck,
                requestCount,
                nextPage * requestCount
            )
            val response = followingResponse.userFollowings
            var userFollowingCached = mutableListOf<UserFollowingCached>()
            if (response.isNotEmpty()) {
                Log.v(
                    Config.logTag,
                    "Loaded new set of followed users!"
                )
                withContext(Dispatchers.IO + NonCancellable) {
                    val userFollowedDao = storyDatabase.userFollowedDao()
                    if (response.isNotEmpty() && nextPage == 0) { // save to cache first results
                        if (!hasCache) {
                            response.forEach {
                                val actualFollow = userFollowedDao
                                    .getFollowedForUserImmediate(loggedInUserUUID, it.username)
                                if (actualFollow == null) {
                                    userFollowedDao.update(
                                        UserFollowed(
                                            it.username,
                                            loggedInUserUUID
                                        )
                                    )
                                }
                            }
                        }
                        userFollowingCached =
                            response.map { UserFollowingCached(it.username, it.profileIcon) }
                                .toMutableList()
                    } else {
                        val userFollowingCachedJson =
                            sharedPreferenceManager.getLatestUsersFollowedCached()
                        userFollowingCached =
                            if (userFollowingCachedJson != null) Gson().fromJson<MutableList<UserFollowingCached>>(
                                userFollowingCachedJson,
                                object : TypeToken<MutableList<UserFollowingCached>>() {}.type
                            ) else mutableListOf()
                        response.forEach { k ->
                            if (!userFollowingCached.any { z -> z.username == k.username }) {
                                userFollowingCached.add(
                                    UserFollowingCached(
                                        k.username,
                                        k.profileIcon
                                    )
                                )
                                val actualFollow = userFollowedDao
                                    .getFollowedForUserImmediate(loggedInUserUUID, k.username)
                                if (actualFollow == null) {
                                    userFollowedDao.update(
                                        UserFollowed(
                                            k.username,
                                            loggedInUserUUID
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Log.v(
                    Config.logTag,
                    "received empty followed users list response!"
                )
            }
            val zeroNotificationLoggedInUser = loggedInUser.copy(unreadFollowedStories = 0u)
            Constants.loggedInUser = zeroNotificationLoggedInUser
            val accountInfoJsonString = Gson().toJson(zeroNotificationLoggedInUser)
            sharedPreferenceManager.setLatestAccountInfo(accountInfoJsonString)
            withContext(Dispatchers.Main) {
                registerViewModel.userFollowedNotificationsPending.value = false
                registerViewModel.userFollowedNotificationsConsumed.value = false
                registerViewModel.userFollowedNotificationsLoaded.value = true
            }
            if (nextPage == 0) {
                val userFollowedCached = Gson().toJson(userFollowingCached)
                sharedPreferenceManager.setLatestUserFollowedCached(userFollowedCached)
            }
            nextKey =
                if (response.isNotEmpty() && response.size == requestCount) nextPage + 1 else null
            return LoadResult.Page(
                data = response,
                prevKey = prevKey,
                nextKey = nextKey //if (userList.data.isEmpty()) null else userList.page + 1
            )
        } catch (exception: Exception) {
            Log.e(
                Config.logTag,
                "Could fetch followed users list, error:" + exception.message,
                exception
            )
            return LoadResult.Error(exception)
        }
    }


}