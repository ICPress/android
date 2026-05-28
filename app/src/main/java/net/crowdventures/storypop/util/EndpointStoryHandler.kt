package net.crowdventures.storypop.util

import android.content.Context
import android.util.Log
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.room.StoryRoom
import net.crowdventures.storypop.util.endpoints.ArticleEndpoint
import net.crowdventures.storypop.util.s3.FlowStatus
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit

public class EndpointStoryHandler {
    companion object {
        suspend fun publishUserStory(applicationContext:Context,storyRoom: StoryRoom,loggedInUser:AccountInfoFull, publishedSlugTitle: String?): Pair<FlowStatus,String> {
            Log.v(Config.logTag,"Publishing story online..")
            val isPublishOrUpdate =  if (publishedSlugTitle==null) "publish" else "update" // for logging
            val storyJson = storyRoom.jsonStoryData
            val requestBody: RequestBody =storyJson.toByteArray(Charsets.UTF_8).toRequestBody("application/json; charset=utf-8".toMediaType())
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val service: ArticleEndpoint = restAdapter.create(ArticleEndpoint::class.java)
            try {
                val response = if (publishedSlugTitle==null) {
                    service.publishArticle(
                        "Bearer ${loggedInUser.refreshToken}",
                        requestBody
                    )
                }else{
                    service.updateArticle(
                        publishedSlugTitle,
                        "Bearer ${loggedInUser.refreshToken}",
                        requestBody
                    )
                }
                if (response.code() == 200) {
                    Config.firebaseAnalytics.logEvent("${isPublishOrUpdate}_story_success",null)
                    Log.v(
                        Config.logTag,
                        "Success publish of article, code:" +
                                response.code() + ", message:" + response.message() + ", body:" + response.body()
                    )
                    return Pair(FlowStatus.SUCCESS,response.body()?.string()?:"")
                } else {
                    Config.firebaseAnalytics.logEvent("${isPublishOrUpdate}_story_failed_status_${response.code()}",null)
                    Log.e(
                        Config.logTag,
                        "failed publish of article, code:" + response.code() + ", message:" + response.message()
                    )
                    return Pair(FlowStatus.FAIL,"")
                }
            } catch (e: Exception) {
                Config.firebaseAnalytics.logEvent("${isPublishOrUpdate}_story_failed_exception",null)
                e.printStackTrace()
                return Pair(FlowStatus.FAIL,"")
            }
        }
    }
}