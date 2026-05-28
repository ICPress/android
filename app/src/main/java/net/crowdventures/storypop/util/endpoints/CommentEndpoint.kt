package net.crowdventures.storypop.util.endpoints

import net.crowdventures.storypop.dto.ArticleCommentPublished
import net.crowdventures.storypop.room.StoryComment
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CommentEndpoint {
    @POST("comment") //{authorJwt}
    @Headers(
        "Content-Type: application/json;charset=UTF-8"
    )
    suspend fun publishComment(
        @Header("authorization") authorization:String,
        @Body() body: StoryComment
    ): Response<ResponseBody>

    @DELETE("comment/{storySlugTitle}/{commentUUID}")
    suspend fun deleteComment(
        @Header("authorization") authorization:String,
        @Path("storySlugTitle") storySlugTitle: String,
        @Path("commentUUID") commentUUID: String
    ): Response<ResponseBody>

    @DELETE("comment/like/{slugTitle}/{commentUUID}")
    suspend fun deleteLikeComment(
        @Header("authorization") authorization:String,
        @Path("slugTitle") slugTitle: String,
        @Path("commentUUID") commentUUID: String
    ): Response<ResponseBody>

    @POST("comment/like/{slugTitle}/{commentUUID}")
    suspend fun likeComment(
        @Header("authorization") authorization:String,
        @Path("slugTitle") slugTitle: String,
        @Path("commentUUID") commentUUID: String
    ): Response<ResponseBody>

    @GET("comment/story/{storySlugTitle}")
    suspend fun getComments(
        @Path("storySlugTitle") storySlugTitle: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ):MutableList<ArticleCommentPublished>

    @GET("comment/user/{username}/{storySlugTitle}")
    suspend fun getCommentsForUser(
        @Header("authorization") authorization:String,
        @Path("storySlugTitle") storySlugTitle: String,
        @Path("username") username: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ):MutableList<ArticleCommentPublished>

    @GET("comment/replies/{storySlugTitle}/{commentUUID}")
    suspend fun getCommentReplies(
        @Path("storySlugTitle") storySlugTitle: String,
        @Path("commentUUID") commentUUID: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ):MutableList<ArticleCommentPublished>

    @GET("comment/user/{username}/replies/{storySlugTitle}/{commentUUID}")
    suspend fun getCommentReplies(
        @Header("authorization") authorization:String,
        @Path("username") username: String,
        @Path("storySlugTitle") storySlugTitle: String,
        @Path("commentUUID") commentUUID: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ):MutableList<ArticleCommentPublished>

}