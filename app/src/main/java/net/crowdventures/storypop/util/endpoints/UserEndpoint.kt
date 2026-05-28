package net.crowdventures.storypop.util.endpoints

import net.crowdventures.storypop.dto.ProfileInfo
import net.crowdventures.storypop.dto.UserSearchResult
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface UserEndpoint {
    @POST("user/{username}/follow")
    suspend fun followUser(
        @Header("authorization") authorization:String,
        @Path("username") username: String
    ): Response<ResponseBody>

    @DELETE("user/{username}/follow")
    suspend fun unfollowUser(
        @Header("authorization") authorization:String,
        @Path("username") username: String
    ): Response<ResponseBody>

    @POST("user/profile")
    @Headers(
        "Content-Type: application/json;charset=UTF-8"
    )
    suspend fun updateProfile(@Header("authorization") authorization:String, @Body() body: RequestBody): Response<ResponseBody>

    @GET("user/{username}")
    suspend fun getProfile(@Path("username") username: String): Response<ProfileInfo?>

    @GET("user/full/{username}")
    suspend fun getProfileFull(@Header("authorization") authorization:String, @Path("username") username: String): Response<ProfileInfo?>


    @GET("user/followers/{username}")
    suspend fun getFollowers(@Path("username") username: String): Response<ResponseBody>

    @GET("user/search/{username}")
    suspend fun searchUser(@Header("authorization") authorization:String,@Path("username") username: String): List<UserSearchResult>
}