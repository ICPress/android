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
// ArticleCommentSource.kt
class ArticleCommentSource(
    val loggedInUser: AccountInfoFull?,
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

            Log.v(Config.logTag, "Fetching comments page $nextPage")

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
                if (loggedInUser == null) {
                    service.getComments(storySlugTitle, requestCount, nextPage * requestCount)
                } else {
                    val authToken = loggedInUser.refreshToken
                    service.getCommentsForUser(
                        "Bearer $authToken", storySlugTitle,
                        loggedInUser.username, requestCount, nextPage * requestCount
                    )
                }.toMutableList()
            } catch (e: Exception) {
                Log.e(Config.logTag, "Failed to fetch comments: ${e.message}", e)
                return LoadResult.Error(e)
            }

            // Merge local pending comments
            if (loggedInUser != null) {
                withContext(Dispatchers.IO) {
                    val storyDatabase = Constants.getStoryDatabase(applicationContext)
                    val allLocalComments = storyDatabase.storyCommentDao()
                        .getPendingCommentsForStoryImmediate(storySlugTitle, loggedInUser.username)
                        // Only include local comments not already returned by server
                        .filter { local -> response.none { it.commentUUID == local.commentUUID } }

                    if (nextPage == 0) {
                        // Top-level local pending comments go at the front
                        val localTopLevel = allLocalComments
                            .filter { it.replyToCommentUUID == null }
                            .map { it.toArticleCommentPublished(storySlugTitle, loggedInUser) }
                        response.addAll(0, localTopLevel)
                    }

                    // For each loaded comment, attach any local pending replies
                    for (comment in response) {
                        val localReplies = allLocalComments
                            .filter { it.replyToCommentUUID == comment.commentUUID }
                            .map { it.toArticleCommentPublished(storySlugTitle, loggedInUser) }
                        if (localReplies.isNotEmpty()) {
                            comment.replies.addAll(0, localReplies)
                        }
                    }
                }
            }

            Log.v(Config.logTag, "Loaded ${response.size} comments for page $nextPage")

            val nextKey = if (response.size == requestCount) nextPage + 1 else null
            LoadResult.Page(data = response, prevKey = prevKey, nextKey = nextKey)

        } catch (e: Exception) {
            Log.e(Config.logTag, "Failed to load comment page: ${e.message}", e)
            LoadResult.Error(e)
        }
    }
}