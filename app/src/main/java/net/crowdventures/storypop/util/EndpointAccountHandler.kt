package net.crowdventures.storypop.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfo
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.NewTokenData
import net.crowdventures.storypop.util.endpoints.AccountEndpoint
import net.crowdventures.storypop.util.endpoints.AdminEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.endpoints.TestEndpoint
import net.crowdventures.storypop.util.endpoints.UserEndpoint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

public class EndpointAccountHandler {
    companion object {
        suspend fun createAccount(applicationContext:Context,accountInfo: AccountInfo):Int{
            Log.v(Config.logTag,"verifying username..")
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val accountJSON = Gson().toJson(accountInfo)
            val requestBody: RequestBody =accountJSON.toByteArray(Charsets.UTF_8).toRequestBody("application/json; charset=utf-8".toMediaType())
            val service: AccountEndpoint = restAdapter.create(AccountEndpoint::class.java)
            try {
                val response = service.createAccount(requestBody)
                val responseCode = response.code()
                if (responseCode == 201) {
                    Log.v(Config.logTag,"received username OK!")
                    return responseCode
                }else {
                    Log.v(Config.logTag, "received username NOT OK, statuscode:$responseCode")
                    return responseCode;
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(Config.logTag, "error when verifying username:"+e.message,e)
                return 0
            }
            return 0
        }
        suspend fun sendSignInLink(applicationContext:Context,email: String):String{
            Log.v(Config.logTag,"sending sign-in link..")
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val service: AccountEndpoint = restAdapter.create(AccountEndpoint::class.java)
            val generalErrorText = "A sign-on link could not be sent at this time, please try again later."
            try {
                val response = service.getSignInLink(email)
                val responseCode = response.code()
                if (responseCode == 201) {
                    Log.v(Config.logTag,"received get-sign-in link OK!")
                    return ""
                }else if (responseCode == 403){
                    Log.v(Config.logTag, "received sign-in link NOT OK, statuscode:$responseCode")
                    return "Please wait a few minutes before requesting a new link."
                } else if (responseCode == 400){
                    Log.v(Config.logTag, "received sign-in link NOT OK, statuscode:$responseCode")
                    return "There is no account associated with the email provided."
                }else{
                    Log.e(Config.logTag, "received sign-in link NOT OK, statuscode:$responseCode")
                    return generalErrorText
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(Config.logTag, "error when fetching sign-in link"+e.message,e)
                return generalErrorText
            }
        }
        suspend fun verifyUsernameOK(applicationContext: Context,username: String):Boolean{
            Log.v(Config.logTag,"verifying username..")
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val service: AccountEndpoint = restAdapter.create(AccountEndpoint::class.java)
            try {
                val response = service.existsUsername(username)
                val responseCode = response.code()
                if (responseCode == 200) {
                    Log.v(Config.logTag,"received username OK!")
                    return true
                }else {
                    Log.v(Config.logTag, "received username NOT OK, statuscode:$responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(Config.logTag, "error when verifying username"+e.message)
                return false
            }
            return false
        }
        suspend fun verifyEmailOK(applicationContext: Context,email: String):Boolean{
            Log.v(Config.logTag,"verifying username..")
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val service: AccountEndpoint = restAdapter.create(AccountEndpoint::class.java)
            try {
                val response = service.existsEmail(email)
                val responseCode = response.code()
                if (responseCode == 200) {
                    Log.v(Config.logTag,"received username OK!")
                    return true
                }else {
                    Log.v(Config.logTag, "received username NOT OK, statuscode:$responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(Config.logTag, "error when verifying username"+e.message)
                return false
            }
            return false
        }
        suspend fun verifyAccountGetToken(applicationContext: Context,tempToken: String ): String? {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()

            val service: AccountEndpoint = restAdapter.create(AccountEndpoint::class.java)
            try {
                val response: Response<ResponseBody> = service.verifyEmail(tempToken)
                if (!response.isSuccessful) {
                    Log.e(
                        Config.logTag,
                        "Response from service.verifyEmail() request is null, code:${response.code()}"
                    )
                    return null
                }
                val responseString = response.body()?.string()
                Log.v(Config.logTag, "Received login token:$response")
                return responseString
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        suspend fun updateProfile(applicationContext: Context,updatedProfileInfoJsonString: String, authToken: String  ): Boolean {
            val requestBody: RequestBody = updatedProfileInfoJsonString.toByteArray(Charsets.UTF_8).toRequestBody("application/json; charset=utf-8".toMediaType())
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()

            val service: UserEndpoint = restAdapter.create(UserEndpoint::class.java)
            try {
                val response: Response<ResponseBody> = service.updateProfile("Bearer $authToken",requestBody)
                if (!response.isSuccessful) {
                    Config.firebaseAnalytics.logEvent("update_profile_failed_status_${response.code()}",null)
                    Log.e(
                        Config.logTag,
                        "Response from service.updateProfileInfo() request is null, code:${response.code()}"
                    )
                    return false
                }
                Config.firebaseAnalytics.logEvent("update_profile_success",null)
                Log.v(Config.logTag, "Updated profile info!")
                return true
            } catch (e: Exception) {
                Config.firebaseAnalytics.logEvent("update_profile_failed_exception",null)
                e.printStackTrace()
                return false
            }
        }

        suspend fun signIn(applicationContext: Context,token: String , sharedPreferenceManager: SharedPreferenceManager): AccountInfoFull? {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().registerTypeAdapter(UInt::class.java, NullableUintJson()).create()))
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()

            val service: AccountEndpoint = restAdapter.create(AccountEndpoint::class.java)
            try {
                val newTokenPending = !sharedPreferenceManager.hasPushedLatestFCMToken()
                val latestToken = sharedPreferenceManager.latestFCMToken()
                val response: AccountInfoFull? =
                if (newTokenPending && latestToken != null)
                    service.signIn("Bearer $token", NewTokenData(latestToken,sharedPreferenceManager.getDeviceUUID().toString()))
                else service.signIn("Bearer $token")
                if (response == null) {
                    Log.e(
                        Config.logTag,
                        "Response from service.signin is null"
                    )
                    Config.firebaseAnalytics.logEvent("sign_in_failed_null",null)
                    return null
                }
                if (newTokenPending) sharedPreferenceManager.setPushedLatestFCMToken()
                return response
            } catch (e: Exception) {
                if (e is retrofit2.HttpException){
                    if (e.code() == 401) {
                        sharedPreferenceManager.clearLatestAccountInfo() //if unauthorized clear token
                        Config.firebaseAnalytics.logEvent("sign_in_failed_expired",null)
                    }else{
                        Config.firebaseAnalytics.logEvent("sign_in_failed_status_${e.code()}",null)
                    }
                }else{
                    Config.firebaseAnalytics.logEvent("sign_in_failed_exception",null)
                }
                e.printStackTrace()
                Log.e(Config.logTag, "Could not logIn, fetch of credentials failed:"+e.message,e)
                throw e
            }
        }


    }
}