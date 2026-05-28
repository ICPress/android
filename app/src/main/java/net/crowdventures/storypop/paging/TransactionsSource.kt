package net.crowdventures.storypop.paging

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.gson.GsonBuilder
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.Transaction
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.endpoints.TransactionEndpoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TransactionsSource(val applicationContext:Context,val loggedInUser: AccountInfoFull?) : PagingSource<Int, Transaction>() {


    override fun getRefreshKey(state: PagingState<Int, Transaction>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
         try {
            val nextPage = params.key ?: 0
            var nextKey: Int? = null
            val requestCount = 10
            val prevKey = if (nextPage > 0) nextPage - 1 else null
            Log.v(Config.logTag, "Fetching transactions..")
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().registerTypeAdapter(UInt::class.java, NullableUintJson()).create()))
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val service: TransactionEndpoint = restAdapter.create(TransactionEndpoint::class.java)
            val authToken = Constants.loggedInUser?.refreshToken
            var response  = listOf<Transaction>()
            if (loggedInUser != null && authToken != null) {
                response = service.get("Bearer $authToken",loggedInUser.username,requestCount,nextPage * requestCount)
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