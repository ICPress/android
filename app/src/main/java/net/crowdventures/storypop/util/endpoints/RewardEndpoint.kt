package net.crowdventures.storypop.util.endpoints

import net.crowdventures.storypop.dto.Reward
import net.crowdventures.storypop.dto.RewardClaimed
import net.crowdventures.storypop.dto.TransferRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface RewardEndpoint {

    @GET("reward")
    suspend fun getRewards(@Query("offset") offset: Int): List<Reward>

    @GET("reward/claimed/{username}")
    suspend fun getClaimedRewards(@Header("authorization") authorization:String, @Path("username") username: String,@Query("offset") offset: Int): List<RewardClaimed>

    @POST("reward/claim/{rewardId}")
    suspend fun claimReward(
        @Header("authorization") authorization:String,
        @Path("rewardId") rewardId: UInt,
        @Query("rewardPrice") rewardPrice: UInt,
        @Query("rewardType") rewardType: UShort): Response<ResponseBody>

    @POST("reward/transfer")
    @Headers(
        "Content-Type: application/json;charset=UTF-8"
    )
    suspend fun transferReward(
        @Header("authorization") authorization:String,
        @Body() transferRequest: TransferRequest): Response<ResponseBody>


    @POST("reward/spin_wheel")
    @Headers(
        "Content-Type: application/json;charset=UTF-8"
    )
    suspend fun spinWheel(@Header("authorization") authorization:String): Response<Int?>

    @POST("reward/unwrap_gift_box")
    @Headers(
        "Content-Type: application/json;charset=UTF-8"
    )
    suspend fun unwrapGiftBox(@Header("authorization") authorization:String): Response<RewardClaimed?>
}