package net.crowdventures.storypop.paging

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.Notification
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.util.endpoints.NotificationEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NotificationSource(val applicationContext:Context,
    val registerViewModel: RegisterViewModel,
    val sharedPreferenceManager: SharedPreferenceManager
) : PagingSource<UInt, Notification>() {
    var isRefreshed = false
    var prevIndex : UInt? = null

        override fun getRefreshKey(state: PagingState<UInt, Notification>): UInt? {
       //return state.anchorPosition
             isRefreshed = true
             prevIndex = null
        return null //for refresh
    }

    override suspend fun load(params: LoadParams<UInt>): LoadResult<UInt, Notification> {
        try {
            val loggedInUser = Constants.loggedInUser ?: return ViewModelUtil.emptyLoadResult()
            var nextPage =  params.key
            val requestCount = 10
            val prevKey = if (isRefreshed) prevIndex else null // do not fetch page 0 if not refreshed
            Log.v(Config.logTag, "Fetching notifications, page:$nextPage")
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
                .build() //addConverterFactory: https://youtrack.jetbrains.com/issue/KT-28420/Gson-Invalid-JSON-serialization-of-nullable-unsigned-integer-property
            val service: NotificationEndpoint = restAdapter.create(NotificationEndpoint::class.java)
            val authToken = Constants.loggedInUser?.refreshToken
            val response = service.getV2(
                "Bearer $authToken",
                loggedInUser.username,
                nextPage,
                requestCount
            )
            if (response.isNotEmpty()) {
                Log.v(
                    Config.logTag,
                    "Loaded new set of notifications!"
                )
            } else {
                Log.v(
                    Config.logTag,
                    "received empty notifications list response!"
                )
            }
            if (response.isNotEmpty() && nextPage == null) { // save to cache first results
                val firstNotifications = Gson().toJson(response)
                sharedPreferenceManager.setLatestNotifications(firstNotifications)
                sharedPreferenceManager.setLatestCachedNotificationKey(response.last().notificationId.toLong())
                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val areNotificationsEnabled: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationManager.areNotificationsEnabled()
                } else {
                    true
                }
                if (areNotificationsEnabled) {
                    notificationManager.cancelAll() //clear any notifications
                }
            }
            val zeroNotificationLoggedInUser = loggedInUser.copy(unreadNotifications = 0u)
            Constants.loggedInUser = zeroNotificationLoggedInUser
            val accountInfoJsonString = Gson().toJson(zeroNotificationLoggedInUser)
            sharedPreferenceManager.setLatestAccountInfo(accountInfoJsonString)
            if (nextPage == null) {
                withContext(Dispatchers.Main) {
                    registerViewModel.userNotificationsConsumed.value = true
                }
            }
            prevIndex = nextPage
            nextPage =
                if (response.isNotEmpty() && response.size == requestCount) response.last().notificationId else null
            return LoadResult.Page(
                data = response,
                prevKey = prevKey,
                nextKey = nextPage
            )
        } catch (exception: Exception) {
            Log.e(
                Config.logTag,
                "Could fetch notifications list, error:" + exception.message,
                exception
            )
            return LoadResult.Error(exception)
        }
    }
}