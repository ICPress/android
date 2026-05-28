package net.crowdventures.storypop.paging

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.ArticleCommentPublished
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.endpoints.CommentEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.toArticleCommentPublished
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ArticleCommentReplySource(
    val loggedInUser: AccountInfoFull?,
    val commentUUID: String,
    val storySlugTitle: String,
    val applicationContext: Context
) : PagingSource<Int, ArticleCommentPublished>() {

    override fun getRefreshKey(state: PagingState<Int, ArticleCommentPublished>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ArticleCommentPublished> {
        return try {
            val nextPage = params.key ?: 0
            val requestCount = 10
            val prevKey = if (nextPage > 0) nextPage - 1 else null

            Log.v(Config.logTag, "Fetching comment replies page $nextPage for $commentUUID")

            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().registerTypeAdapter(
                            UInt::class.java, NullableUintJson()
                        ).create()
                    )
                )
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()

            val service: CommentEndpoint = restAdapter.create(CommentEndpoint::class.java)

            val response: MutableList<ArticleCommentPublished> = try {
                if (loggedInUser?.username == null) {
                    service.getCommentReplies(storySlugTitle, commentUUID, requestCount, nextPage * requestCount)
                } else {
                    service.getCommentReplies(
                        "Bearer ${loggedInUser.refreshToken}",
                        loggedInUser.username,
                        storySlugTitle,
                        commentUUID,
                        requestCount,
                        nextPage * requestCount
                    )
                }.toMutableList()
            } catch (e: Exception) {
                Log.e(Config.logTag, "Failed to fetch comment replies: ${e.message}", e)
                return LoadResult.Error(e)
            }

            Log.v(Config.logTag, "Loaded ${response.size} replies for comment $commentUUID")

            // Merge local pending replies on the first page only so they appear
            // at the top while the upload is in progress in the background
            if (nextPage == 0 && loggedInUser != null) {
                withContext(Dispatchers.IO) {
                    val storyDatabase = Constants.getStoryDatabase(applicationContext)
                    val localPendingReplies = storyDatabase.storyCommentDao()
                        .getPendingCommentsForStoryImmediate(storySlugTitle, loggedInUser.username)
                        .filter { local ->
                            local.replyToCommentUUID == commentUUID &&
                                    response.none { it.commentUUID == local.commentUUID }
                        }
                        .map { it.toArticleCommentPublished(storySlugTitle, loggedInUser) }

                    if (localPendingReplies.isNotEmpty()) {
                        // Prepend pending replies so they appear immediately at the top
                        response.addAll(0, localPendingReplies)
                        Log.v(Config.logTag, "Prepended ${localPendingReplies.size} local pending replies")
                    }
                }
            }

            val nextKey = if (response.size >= requestCount) nextPage + 1 else null
            LoadResult.Page(data = response, prevKey = prevKey, nextKey = nextKey)

        } catch (e: Exception) {
            Log.e(Config.logTag, "Failed to load reply page: ${e.message}", e)
            LoadResult.Error(e)
        }
    }
}