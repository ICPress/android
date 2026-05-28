package net.crowdventures.storypop.util.endpoints

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface FeedbackEndpoint {

    @POST("feedback/{feedbackType}/{username}/{storyId}")
    suspend fun feedbackArticle(
        @Header("authorization") authorization:String,
        @Path("feedbackType") feedbackType: String,
        @Path("username") username: String,
        @Path("storyId") storyId: String
    ): Response<ResponseBody>

    @DELETE("feedback/{username}/{storyId}")
    suspend fun removeLike(
        @Header("authorization") authorization:String,
        @Path("username") username: String,
        @Path("storyId") storyId: String
    ): Response<ResponseBody>

    @POST("feedback/comment/{feedbackType}/{commentUUID}")
    suspend fun feedbackComment(
        @Header("authorization") authorization:String,
        @Path("feedbackType") feedbackType: String,
        @Path("commentUUID") commentUUID: String,
    ): Response<ResponseBody>
}