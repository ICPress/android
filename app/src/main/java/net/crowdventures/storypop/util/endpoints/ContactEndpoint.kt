package net.crowdventures.storypop.util.endpoints

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ContactEndpoint{


@POST("contact/approve/{targetUsername}")
suspend fun approve(@Header("authorization") authorization:String,
                    @Path("targetUsername") targetUsername: String): Response<ResponseBody>


@POST("contact/block/{targetUsername}")
suspend fun block(@Header("authorization") authorization:String,
                    @Path("targetUsername") targetUsername: String): Response<ResponseBody>

}