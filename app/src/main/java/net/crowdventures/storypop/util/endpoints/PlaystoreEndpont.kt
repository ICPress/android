package net.crowdventures.storypop.util.endpoints

import net.crowdventures.storypop.dto.AcknowledgeStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PlaystoreEndpont {
    @POST("playstore/acknowledge/{playstoreSubscriptionId}")
    suspend fun acknowledge(
        @Header("authorization") authorization:String,
        @Path("playstoreSubscriptionId") playstoreSubscriptionId: String,
        @Body purchaseToken: String
    ): Response<AcknowledgeStatus>
}