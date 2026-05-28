package net.crowdventures.storypop.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.util.endpoints.AdminEndpoint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit

class EndpointAdminHandler {
    companion object{
        suspend fun deleteArticleTerminateAccount(applicationContext: Context, usernameAffected: String, callback: SuccessCallback<Unit>, slugTitle: String? =null ) {
            val loggedInUser = Constants.loggedInUser ?: return
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()

            val service: AdminEndpoint = restAdapter.create(AdminEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken

                val response = if (slugTitle != null) service.deleteArticle(
                    "Bearer $authToken", usernameAffected, slugTitle
                ) else service.terminateUser(
                    "Bearer $authToken", usernameAffected
                )
                if (!response.isSuccessful) {
                    Log.e(
                        Config.logTag,
                        "Response from deleteArticleTerminateAccount request is bad, code:${response.code()}"
                    )
                    withContext(Dispatchers.Main){
                        callback.onFailure("Received error code:"+response.code())
                    }
                }else{
                    withContext(Dispatchers.Main){
                        callback.onSuccess()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    callback.onFailure(e.message)
                }
            }
        }

        suspend fun acceptRejectArticle(applicationContext: Context, usernameAffected: String, slugTitle: String,isRejected:Boolean,rejectionReason:String, callback: SuccessCallback<Unit>) {
            val loggedInUser = Constants.loggedInUser ?: return
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()

            val service: AdminEndpoint = restAdapter.create(AdminEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken

                val response = if (isRejected) {
                    // Wrap the reason as a JSON string — server expects [FromBody] string
                    val requestBody = "\"$rejectionReason\""
                        .toRequestBody("application/json".toMediaType())
                    service.rejectArticle(
                        "Bearer $authToken", usernameAffected,  slugTitle,requestBody
                    )
                }else service.acceptArticle(
                    "Bearer $authToken",usernameAffected, slugTitle
                )
                if (!response.isSuccessful) {
                    Log.e(
                        Config.logTag,
                        "Response from acceptRejectArticle request is bad, code:${response.code()}"
                    )
                    withContext(Dispatchers.Main){
                        callback.onFailure("Received error code:"+response.code())
                    }
                }else{
                    withContext(Dispatchers.Main){
                        callback.onSuccess()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    callback.onFailure(e.message)
                }
            }
        }

    }
}