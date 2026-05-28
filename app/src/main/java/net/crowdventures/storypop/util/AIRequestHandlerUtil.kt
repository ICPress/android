package net.crowdventures.storypop.util

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.dto.AIMessageDTO
import net.crowdventures.storypop.dto.AIRequestDTO
import net.crowdventures.storypop.dto.AIResponseDTO
import net.crowdventures.storypop.util.endpoints.AIEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AIRequestHandlerUtil {
    companion object {
        fun suggest(groqKey:String, model:String,prompt:String, applicationContext: Context, viewModelScope: CoroutineScope, topic: String, callback: SuccessCallback<List<String>>) {
            viewModelScope.launch(Dispatchers.IO + StoryUtil.coroutineExceptionHandler) {
                    val restAdapter = Retrofit.Builder()
                        .baseUrl(Config.GROQ_ENDPOINT)
                        .addConverterFactory(
                            GsonConverterFactory.create(
                                GsonBuilder().registerTypeAdapter(
                                    UInt::class.java,
                                    NullableUintJson()
                                ).create()
                            )
                        )
                        .build()

                    val service: AIEndpoint = restAdapter.create(AIEndpoint::class.java)
                    try {
                        val request = AIRequestDTO(model, listOf(AIMessageDTO("user","{$prompt} ${topic}")))

                        val response: Response<AIResponseDTO> = service.getCompletion( "Bearer ${groqKey}", request)
                        val responseBody = response.body()
                        if (response.isSuccessful && responseBody != null && responseBody.choices.any()) {
                            withContext(Dispatchers.Main) {
                                callback.onSuccess(responseBody.choices.map {z-> z.message.content})
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                callback.onFailure(response.code())
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(
                            Config.logTag,
                            "Could not load SuggestTitle, exception:" + e.message,
                            e
                        )
                        withContext(Dispatchers.Main) {
                            callback.onFailure(500)
                        }
                    }
                }
        }

    }
}