package net.crowdventures.storypop.paging

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.UserMessagePublished
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.util.endpoints.MessageEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MessageSource(val applicationContext: Context,
    val sharedPreferenceManager: SharedPreferenceManager,
    val targetUsername: String
) : PagingSource<UInt, UserMessagePublished>() {
    var isRefreshed = false
    var prevKey :UInt? = null

    override fun getRefreshKey(state: PagingState<UInt, UserMessagePublished>): UInt? {
        //return state.anchorPosition
        isRefreshed = true
        prevKey = null
        return null //for refresh
    }

    override suspend fun load(params: LoadParams<UInt>): LoadResult<UInt, UserMessagePublished> {
        try {
            val loggedInUser = Constants.loggedInUser ?: return ViewModelUtil.emptyLoadResult()
            //val startPage = if (skipFirstPage) 1 else 0
            val nextPage : UInt? =  params.key ?: sharedPreferenceManager.getLatestCachedMessageKeyForUser(targetUsername)
            var nextKey: UInt? = nextPage
            val requestCount = 10
            val prevKey = prevKey // do not fetch page 0 if not refreshed
            Log.v(Config.logTag, "Fetching messages, page:$nextPage")
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
            val service: MessageEndpoint = restAdapter.create(MessageEndpoint::class.java)
            val authToken = Constants.loggedInUser?.refreshToken
            var response = service.get(
                "Bearer $authToken",
                targetUsername,
                requestCount,
                nextKey
            )
            if (response.isNotEmpty()) {
                Log.v(
                    Config.logTag,
                    "Loaded new set of messages!"
                )
            } else {
                Log.v(
                    Config.logTag,
                    "received empty list of messages response!"
                )
            }
            if (response.isNotEmpty() && nextPage == 0u) { // save to cache first results
                val firstNotifications = Gson().toJson(response)
                sharedPreferenceManager.setLatestMessagesForUser(firstNotifications,targetUsername)
                sharedPreferenceManager.setLatestCachedMessageKeyForUser(targetUsername,response.last().messageId.toLong())
            }
            nextKey =
                if (response.isNotEmpty() && response.size == requestCount) response.last().messageId else null
            return LoadResult.Page(
                data = response,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (exception: Exception) {
            Log.e(
                Config.logTag,
                "Could fetch messages for user ${targetUsername}, error:" + exception.message,
                exception
            )
            return LoadResult.Error(exception)
        }
    }
}