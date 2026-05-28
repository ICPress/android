package net.crowdventures.storypop.util.s3

import android.content.Context
import android.util.Base64.decode
import android.util.Log
import androidx.annotation.RequiresApi
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.util.RefreshToken
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.endpoints.AdvancedHostingS3
import net.crowdventures.storypop.util.endpoints.TokenEndpoint
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.joda.time.DateTime
import org.joda.time.Minutes
import retrofit2.Response
import retrofit2.Retrofit
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class S3Handler {
    companion object {

        @RequiresApi(22) //AES/GCM/NoPadding available 22>=API
        suspend fun getAuthToken(): String? {
            val currentToken = Config.refreshToken
            if (currentToken != null &&
                Minutes.minutesBetween(
                    currentToken.receivedDateTime,
                    DateTime.now()
                ).minutes <= 40
            ) {
                Log.v(
                    net.crowdventures.storypop.Config.logTag,
                    "Reusing existing authToken.."
                )
                return currentToken.token
            }
            return null
        }

        suspend fun getUpdatedAuthToken(applicationContext:Context,loggedInUser:AccountInfoFull): String? {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val service: TokenEndpoint = restAdapter.create(TokenEndpoint::class.java)
            try {
                val response: Response<ResponseBody> = service.get("Bearer ${loggedInUser.refreshToken}", applicationContext.packageName)
                if (response.body() == null) {
                    Config.firebaseAnalytics.logEvent("get_updated_auth_failed_empty",null)
                    Log.e(
                        Config.logTag,
                        "Response from service.getToken() request is null, code:${response.code()}"
                    )
                    return null
                }
                val responseString = response.body()?.string() ?: return null
                Log.v(Config.logTag, "Received response: $responseString")
                val ivSecretArray = responseString.split('\n')
                val key: SecretKey = SecretKeySpec(
                    Config.ENCODED_TOKEN_SECRET,
                    0,
                    Config.ENCODED_TOKEN_SECRET.size,
                    "AES"
                )
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    IvParameterSpec(ivSecretArray[0].toByteArray(Charsets.UTF_8))
                )
                val ciphertextDecoded: ByteArray = cipher.doFinal(
                    decode(
                        ivSecretArray[2],
                        android.util.Base64.DEFAULT
                    ) + decode(ivSecretArray[1], android.util.Base64.DEFAULT)
                )
                val newToken = ciphertextDecoded.toString(Charsets.UTF_8)
                //Log.v(Config.logTag, "Received token:"+ ciphertextDecoded.toString(Charsets.UTF_8))
                if (response.code() == 200) {
                    Config.firebaseAnalytics.logEvent("get_updated_auth_success",null)
                    Log.v(
                        net.crowdventures.storypop.Config.logTag,
                        "Received new authToken!"
                    )
                    val refreshToken = RefreshToken(newToken)
                    Config.refreshToken = refreshToken
                    return refreshToken.token
                } else {
                    Config.firebaseAnalytics.logEvent("get_updated_auth_failed_status_${response.code()}",null)
                    Log.e(
                        net.crowdventures.storypop.Config.logTag,
                        "failed to get latest refreshToken!"
                    )
                    return null
                }
            } catch (e: Exception) {
                Config.firebaseAnalytics.logEvent("get_updated_auth_exception",null)
                e.printStackTrace()
                return null
            }
        }

        suspend fun uploadImage(applicationContext: Context,
            body: RequestBody,
            fileName: String,
            authToken: String,
            bucketName:String
        ): FlowStatus {
            val restAdapter = Retrofit.Builder()
                .baseUrl(bucketName)
                .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                .build()
            val service: AdvancedHostingS3 = restAdapter.create(AdvancedHostingS3::class.java)
            try {
                val response = service.upload(
                    fileName,
                    body.contentLength(),
                    authToken,
                    body
                )
                if (response.code() == 201) {
                    Config.firebaseAnalytics.logEvent("image_upload_success",null)
                    Log.v(
                        Config.logTag,
                        "Success upload to S3, code:" +
                                response.code() + ", message:" + response.message() + ", body:" + response.body()
                    )
                    return FlowStatus.SUCCESS
                } else {
                    Config.firebaseAnalytics.logEvent("image_upload_failed_status_${response.code()}",null)
                    Log.e(
                        Config.logTag,
                        "failed upload to S3, code:" + response.code() + ", message:" + response.message()
                    )
                    return FlowStatus.FAIL
                }
            } catch (e: Exception) {
                Config.firebaseAnalytics.logEvent("image_upload_exception",null)
                e.printStackTrace()
                return FlowStatus.FAIL
            }
        }



    }
}