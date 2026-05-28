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
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.endpoints.ArticleEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StorySource(val applicationContext:Context,val fetchType: StoryFetchType?, val hashtagFilter: String?,val sharedPreferenceManager: SharedPreferenceManager) :
    PagingSource<Int, StoryPublishedModel>() {
    private var skipUpdatingCache =false
    override fun getRefreshKey(state: PagingState<Int, StoryPublishedModel>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, StoryPublishedModel> {
         try {
            val nextPage = params.key ?: 0
            var nextKey: Int? = null
            val requestCount = 10
            val prevKey = if (nextPage > 0) nextPage - 1 else null
            //val path: Uri = Uri.parse(Config.APP_RESOURCE_PATH + R.drawable._18a64fb_8d00_4de7_ae08_f7504a964da1_0433360909376658504236912253)
            // val userList =  listOf(User(1,"sdddba@jfej.com","neiwfw","ewenfw",path))// RetrofitClient.apiService.getUserList(page = nextPage)
            //"https://m.media-amazon.com/images/M/MV5BMTYwOTEwNjAzMl5BMl5BanBnXkFtZTcwODc5MTUwMw@@._V1_FMjpg_UX1000_.jpg"
            Log.v(Config.logTag, "Fetching published stories..")
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().registerTypeAdapter(UInt::class.java, NullableUintJson()).create()))
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val service: ArticleEndpoint = restAdapter.create(ArticleEndpoint::class.java)
            var storyList = listOf<StoryPublishedModel>()
            val loggedInUser = Constants.loggedInUser
             if (loggedInUser == null) return emptyLoadResult()
            val authToken = Constants.loggedInUser?.refreshToken
            val response: List<StoryPublishedModel>
            if (fetchType == StoryFetchType.PUBLISHED_BY_USER) {
                response = service.getArticlesPublishedByUser(
                    "Bearer $authToken",
                    loggedInUser.username,
                     requestCount,
                    nextPage * requestCount
                )
            } else if (fetchType == StoryFetchType.LIKED) {
                response = service.getLikedArticles(
                    "Bearer $authToken",
                    loggedInUser.username,
                    requestCount,
                    nextPage * requestCount
                )
            }else if (fetchType == null || loggedInUser == null){
                response = listOf() //DO NOTHING, NOT LOGGED IN WHEN REQUIRED
            } else {
                if (authToken != null) {
                    if (hashtagFilter != null) {
                        if (hashtagFilter.length > 1 && hashtagFilter[0] == '@'){
                            response = service.getArticlesPublishedByUser(
                                "Bearer $authToken",
                                hashtagFilter.replace("@",""),
                                requestCount,
                                nextPage * requestCount
                            )
                        } else{
                            response = service.getRecommendedArticlesByTag(
                                "Bearer $authToken",
                                hashtagFilter,
                                loggedInUser.username,
                                requestCount,
                                nextPage * requestCount
                            )
                        }
                    } else {
                        response = service.getRecommendedArticles(
                            "Bearer $authToken",
                            loggedInUser.username,
                            requestCount,
                            nextPage * requestCount
                        )
                        val latestPageCached = sharedPreferenceManager.getLatestStoriesCachedPageNumber()
                        if (response.isNotEmpty() && response.size == requestCount && nextPage != latestPageCached && !skipUpdatingCache){
                            if ((latestPageCached == null && prevKey != null) || latestPageCached != null ) {
                                val latestStoriesJson = Gson().toJson(response)
                                sharedPreferenceManager.setLatestStories(
                                    latestStoriesJson,
                                    nextPage
                                )
                                skipUpdatingCache = true
                            }
                        }
                    }
                } else {
                    if (hashtagFilter != null) {
                        if (hashtagFilter.length > 1 && hashtagFilter[0] == '@'){
                            response = service.getArticlesPublishedByUser(
                                "Bearer $authToken",
                                hashtagFilter.replace("@",""),
                                requestCount,
                                nextPage * requestCount
                            )
                        } else {
                            response = service.getArticlesLatestByTag(
                                hashtagFilter,
                                requestCount,
                                nextPage * requestCount
                            )
                        }
                    } else {
                        response = service.getArticlesLatest(
                             requestCount,
                            nextPage * requestCount
                        )
                        val latestPageCached = sharedPreferenceManager.getLatestStoriesCachedPageNumber()
                        if (response.isNotEmpty() && response.size == requestCount && nextPage != latestPageCached && !skipUpdatingCache){
                            if ((latestPageCached == null && prevKey != null) || latestPageCached != null ) {
                                val latestStoriesJson = Gson().toJson(response)
                                sharedPreferenceManager.setLatestStories(
                                    latestStoriesJson,
                                    nextPage
                                )
                                skipUpdatingCache = true
                            }
                        }
                    }

                }
            }
            if (response.isNotEmpty()) {
                if (response.any{x->x.contentText != null && x.contentText.length > Constants.MAX_CONTENT_LENGTH }){
                    val mutableList : MutableList<StoryPublishedModel> = mutableListOf()
                    for (article in response){
                        if (article.contentText.length > Constants.MAX_CONTENT_LENGTH){
                            mutableList.add(StoryPublishedModel(article.stylingInfo,article.storyTitle,
                                article.emptyTitle, article.contentText.substring(0,
                                    Constants.MAX_CONTENT_LENGTH
                                ), article.location, article.langCode, article.tags,
                                article.authorName, article.publicSources, article.privateSources,article.isReviewed, article.storyMap,article.timestamp, article.slugTitle,article.hearts, article.comments,article.authorBadge, article.rejectionReason,null))
                        }else mutableList.add(article)
                    }
                    Log.e(
                        Config.logTag,
                        "Loaded new set of articles with wrong bounds!"
                    )
                    storyList = mutableList
                }else{
                    Log.v(
                        Config.logTag,
                        "Loaded new set of articles!"
                    )
                    storyList = response
                }
                    for (story in storyList.filter { it.stylingInfo.titleBackgroundImage != null }){
                        val imgRequest = ImageUtil.getImageRequestFromMetadata(applicationContext, story.stylingInfo.titleBackgroundImage?:continue, loggedInUser)
                        Config.getOrSetImageLoader(applicationContext).enqueue(imgRequest)
                        story.imageRequest = imgRequest
                    }
                for (story in storyList.filter { it.authorBadge != null }){
                    val imgRequest = ImageUtil.getImageRequestFromMetadata(applicationContext, story.authorBadge?:continue, loggedInUser)
                    Config.getOrSetImageLoader(applicationContext).enqueue(imgRequest)
                    story.badgeImageRequest = imgRequest
                }
            } else {
                Log.v(
                    Config.logTag,
                    "received empty article list response!"
                )
            }
            nextKey =
                if (storyList.isNotEmpty() && storyList.size == requestCount) nextPage + 1 else null
            return LoadResult.Page(
                data = storyList,
                prevKey = prevKey,
                nextKey = nextKey //if (userList.data.isEmpty()) null else userList.page + 1
            )
        } catch (exception: Exception) {
            Log.e(Config.logTag, "Could fetch article list, error:" + exception.message,exception)
            return LoadResult.Error(exception)
        }
    }

    fun emptyLoadResult(): LoadResult<Int, StoryPublishedModel> {
        return LoadResult.Page(
            data = listOf(),
            prevKey = null,
            nextKey = null
        ) // user must be logged in for the fetchType
    }
}