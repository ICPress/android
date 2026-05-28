package net.crowdventures.storypop.util.endpoints

import retrofit2.http.GET
import retrofit2.http.Path

interface TagEndpoint {
    @GET("tag/{search_term}")
    suspend fun getTags(
        @Path("search_term") searchTerm: String
    ): List<String>
}