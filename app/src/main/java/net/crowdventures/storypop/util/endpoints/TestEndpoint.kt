package net.crowdventures.storypop.util.endpoints

import net.crowdventures.storypop.dto.ServerVersion
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TestEndpoint {
    @GET("test")
    suspend fun getClusterStatus(): Response<ServerVersion>
}