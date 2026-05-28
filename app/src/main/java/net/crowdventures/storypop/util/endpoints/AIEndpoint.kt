package net.crowdventures.storypop.util.endpoints

import net.crowdventures.storypop.dto.AIRequestDTO
import net.crowdventures.storypop.dto.AIResponseDTO
import retrofit2.Response
import retrofit2.http.*

interface AIEndpoint {

    @POST("v1/chat/completions")
    suspend fun getCompletion(
        @Header("authorization") authorization:String,
        @Body() content: AIRequestDTO): Response<AIResponseDTO>

}