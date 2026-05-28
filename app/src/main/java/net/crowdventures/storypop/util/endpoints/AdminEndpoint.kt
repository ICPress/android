package net.crowdventures.storypop.util.endpoints

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AdminEndpoint {
    @DELETE("admin/article/{usernameAffected}/{articleToDelete}")
    suspend fun deleteArticle(
        @Header("authorization") authorization:String,
        @Path("usernameAffected") usernameAffected: String,
        @Path("articleToDelete") articleToDelete: String,
    ): Response<ResponseBody>

    @DELETE("admin/user/{userToTerminate}")
    suspend fun terminateUser(
        @Header("authorization") authorization:String,
        @Path("userToTerminate") userToTerminate: String
    ): Response<ResponseBody>

    @POST("admin/article/{usernameAffected}/accept/{slugTitle}")
    suspend fun acceptArticle(
        @Header("authorization") authorization:String,
        @Path("usernameAffected") usernameAffected: String,
        @Path("slugTitle") slugTitle: String
    ): Response<ResponseBody>

    @POST("admin/article/{usernameAffected}/reject/{slugTitle}")
    suspend fun rejectArticle(
        @Header("authorization") authorization:String,
        @Path("usernameAffected") usernameAffected: String,
        @Path("slugTitle") slugTitle: String,
        @Body rejectionReason:RequestBody
    ): Response<ResponseBody>
}