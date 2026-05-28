package net.crowdventures.storypop.util.endpoints


import net.crowdventures.storypop.dto.UserFollowing
import net.crowdventures.storypop.dto.ArticleGuidelines
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ArticleEndpoint {

    @GET("article/guidelines")
    suspend fun getArticleGuidelines(): ArticleGuidelines

    @GET("article/title/{slugTitle}")
    suspend fun getArticle(
        @Path("slugTitle") slugTitle: String
    ): StoryPublishedModel

     @POST("article") //{authorJwt}
     @Headers(
         "Content-Type: application/json;charset=UTF-8"
     )
     suspend fun publishArticle(
         @Header("authorization") authorization:String,
         @Body() body: RequestBody
     ): Response<ResponseBody>

    @PUT("article/{slugTitle}") //{authorJwt}
    @Headers(
        "Content-Type: application/json;charset=UTF-8"
    )
    suspend fun updateArticle(
        @Path("slugTitle") slugTitle: String,
        @Header("authorization") authorization:String,
        @Body() body: RequestBody
    ): Response<ResponseBody>

    @GET("article/recommended/{username}")
    suspend fun getRecommendedArticles(
        @Header("authorization") authorization:String,
        @Path("username") username: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ): List<StoryPublishedModel>

    @GET("article/recommended/{hashtag}/{username}")
    suspend fun getRecommendedArticlesByTag(
        @Header("authorization") authorization:String,
        @Path("hashtag") hashtag: String,
        @Path("username") username: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ): List<StoryPublishedModel>

    @GET("article/liked/{username}")
    suspend fun getLikedArticles(
        @Header("authorization") authorization:String,
        @Path("username") username: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ): List<StoryPublishedModel>

    @GET("article/latest")
    suspend fun getArticlesLatest(
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ): List<StoryPublishedModel>

    @GET("article/tag/{hashtag}")
    suspend fun getArticlesLatestByTag(
        @Path("hashtag") hashtag: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ): List<StoryPublishedModel>

    @GET("article/author/{username}")
    suspend fun getArticlesPublishedByUser(
        @Header("authorization") authorization:String,
        @Path("username") username: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ): List<StoryPublishedModel>

    @GET("article/followed/{username}")
    suspend fun getUsersFollowed(
        @Header("authorization") authorization:String,
        @Path("username") username: String,
        @Query("followingLatestDate") followingPreviousDate: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ): UserFollowing

}