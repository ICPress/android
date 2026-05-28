package net.crowdventures.storypop.util.endpoints

import net.crowdventures.storypop.dto.UserMessagePublished
import net.crowdventures.storypop.room.UserMessage
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

interface MessageEndpoint {
    @GET("message/{fromUsername}")
    suspend fun get(
        @Header("authorization") authorization: String,
        @Path("fromUsername") fromUsername: String, @Query("count") count: Int,
        @Query("startIndex") startIndex: UInt?
    ): List<UserMessagePublished>

    @POST("message")
    @Headers(
        "Content-Type: application/json;charset=UTF-8"
    )
    suspend fun send(
        @Header("authorization") authorization: String,
        @Body() body: UserMessage
    ): Response<ResponseBody>


    @DELETE("message/{messageUUID}")
    suspend fun delete(
        @Header("authorization") authorization: String,
        @Path("messageUUID") messageUUID: String
    ): Response<ResponseBody>
}