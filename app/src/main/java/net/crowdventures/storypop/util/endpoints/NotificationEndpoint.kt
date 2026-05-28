package net.crowdventures.storypop.util.endpoints

import net.crowdventures.storypop.dto.Notification
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationEndpoint{


@GET("notification/v2/{username}")
suspend fun getV2(@Header("authorization") authorization:String,
                    @Path("username") username: String, @Query("startIndex") startIndex: UInt?,
                    @Query("count") count: Int): List<Notification>
}