package net.crowdventures.storypop.util.endpoints

import net.crowdventures.storypop.dto.Transaction
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionEndpoint {

    @GET("transaction/{username}")
    suspend fun get(@Header("authorization") authorization:String,
                    @Path("username") username: String, @Query("count") count: Int,
                    @Query("offset") offset: Int): List<Transaction>
}