package net.crowdventures.storypop.util.endpoints


import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface AdvancedHostingS3 {
    @PUT("{fileName}")
    suspend fun upload(
        @Path("fileName") fileName: String,
        @Header("Content-Length") length: Long,
        @Header("X-Auth-Token") authorization: String,
        @Body() body: RequestBody
    ): Response<ResponseBody>

}