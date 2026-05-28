package net.crowdventures.storypop.util

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.endpoints.TestEndpoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EndpointServerHandler {
    companion object{

        suspend fun checkServerReachability(applicationContext: Context):Boolean{
            Log.v(Config.logTag,"verifying server status..")
            if (Config.APP_ENDPOINT.isNullOrEmpty()) return false
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().registerTypeAdapter(UInt::class.java, NullableUintJson()).create()))
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val service: TestEndpoint = restAdapter.create(TestEndpoint::class.java)
            try {
                val response = service.getClusterStatus()
                if (response.isSuccessful) {
                    Log.v(Config.logTag,"received getClusterStatus OK!")
                    return true
                }else {
                    Log.v(Config.logTag, "getClusterStatus failed, status code:${response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(Config.logTag, "error when running checkServerReachability:"+e.message)
                return false
            }
            return false
        }
    }
}