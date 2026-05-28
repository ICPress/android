package net.crowdventures.storypop.util.endpoints


import net.crowdventures.storypop.dto.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AccountEndpoint {
    @GET("account/exists/{username}")
    suspend fun existsUsername(
        @Path("username") username: String): Response<ResponseBody>
    @GET("account/existsEmail/{email}")
    suspend fun existsEmail(
        @Path("email") email: String): Response<ResponseBody>

    @POST("account/getSignInLink/{email}")
    suspend fun getSignInLink(
        @Path("email") email: String): Response<ResponseBody>

    @POST("account/verify/{tempToken}")
    suspend fun verifyEmail(
        @Path("tempToken") tempToken: String): Response<ResponseBody>

    @POST("account")
    @Headers(
        "Content-Type: application/json;charset=UTF-8"
    )
    suspend fun createAccount(
        @Body() body: RequestBody): Response<ResponseBody>

    @POST("account/signin")
    @Headers(
        "Content-Type: application/json;charset=UTF-8"
    )
    suspend fun signIn(@Header("authorization") authorization:String, @Body() tokenData: NewTokenData): AccountInfoFull?

    @POST("account/signin")
    @Headers(
        "Content-Type: application/json;charset=UTF-8"
    )
    suspend fun signIn(@Header("authorization") authorization:String): AccountInfoFull?

    @POST("account/wallet")
    suspend fun updateWallet(@Header("authorization") authorization:String,
                             @Query("address") address: String,
                             @Query("deviceId") deviceId: String):  Response<ResponseBody>

    @DELETE("account/wallet")
    suspend fun deleteWallet(@Header("authorization") authorization:String,
                             @Query("address") address: String,
                             @Query("deviceId") deviceId: String):  Response<ResponseBody>

    @DELETE("account")
    suspend fun deleteAccount(@Header("authorization") authorization:String):  Response<ResponseBody>

}