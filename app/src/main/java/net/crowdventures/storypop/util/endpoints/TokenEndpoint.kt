package net.crowdventures.storypop.util.endpoints

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TokenEndpoint {
    @GET("token/{appName}")
    suspend fun get(@Header("authorization") authorization:String,  @Path("appName") appName: String ): Response<ResponseBody>
}