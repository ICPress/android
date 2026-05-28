package net.crowdventures.storypop.paging

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.gson.GsonBuilder
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.Reward
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.endpoints.RewardEndpoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RewardsStorySource< T : Reward>(val applicationContext: Context, val loggedInUser: AccountInfoFull?, private val isClaimedRewardSource:Boolean) : PagingSource<Int, T>() {


        override fun getRefreshKey(state: PagingState<Int, T>): Int? {
            return state.anchorPosition
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
             try {
                val nextPage = params.key ?: 0
                var nextKey: Int? = null
                val requestCount = 10
                val prevKey = if (nextPage > 0) nextPage - 1 else null
                Log.v(Config.logTag, "Fetching rewards..")
                val restAdapter = Retrofit.Builder()
                    .baseUrl(Config.APP_ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create(GsonBuilder().registerTypeAdapter(UInt::class.java, NullableUintJson()).create()))
                    .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                    .build()
                val service: RewardEndpoint = restAdapter.create(RewardEndpoint::class.java)
                val authToken = Constants.loggedInUser?.refreshToken
                var response  = listOf<T>()
                if (loggedInUser != null && authToken != null) {
                    if (isClaimedRewardSource){
                        response = service.getClaimedRewards("Bearer $authToken",loggedInUser.username,nextPage * requestCount).map{ it as T  }.toList()
                    }else response = service.getRewards(nextPage * requestCount).map{ it as T  }.toList()
                }
                if (response.isNotEmpty()) {
                    Log.v(
                        Config.logTag,
                        "Loaded new set of rewards!"
                    )
                } else {
                    Log.v(
                        Config.logTag,
                        "received empty rewards list response!"
                    )
                }
                nextKey =
                    if (response.isNotEmpty() && response.size == requestCount) nextPage + 1 else null
                return LoadResult.Page(
                    data = response,
                    prevKey = prevKey,
                    nextKey = nextKey //if (userList.data.isEmpty()) null else userList.page + 1
                )
            } catch (exception: Exception) {
                Log.e(Config.logTag, "Could fetch rewards list, error:" + exception.message,exception)
                return LoadResult.Error(exception)
            }
        }
}