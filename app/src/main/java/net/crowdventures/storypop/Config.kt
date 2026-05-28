package net.crowdventures.storypop

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Patterns
import androidx.security.crypto.MasterKeys
import coil.ImageLoader
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import net.crowdventures.storypop.models.ApiEndpoint
import net.crowdventures.storypop.util.RefreshToken
import net.crowdventures.storypop.util.RetrofitUtil
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Config {
    companion object {
        val firebaseAnalytics = Firebase.analytics
        val MAX_USERNAME_LENGTH =20
        val MAX_HASHTAG_LENGTH =15
        val MIN_HASHTAG_LENGTH =4
        val LARGE_FONT_SIZE =20 //device independent pixels
        val defaultPhotoFile = "photo.jpeg"
        val logTag = "SPOP"
        val IMAGE_START_SEPARATOR ="\u0000\u0000\n"
        val STYLE_TERMINATOR_OPERATOR ="\u0000"
        val TIMESTAMP_PARSER = ISODateTimeFormat.dateTimeParser()
        //HD MAX Resolution
        val MAX_IMAGE_FULL_WIDTH = 1024f
        val MAX_IMAGE_FULL_HEIGHT = 1024f
        val MAX_IMAGE_SCALED_WIDTH = 576f
        val MAX_IMAGE_SCALED_HEIGHT = 576f
        val MINIATURE_BITMAP_PREFIX= "m_"
        val GROQ_ENDPOINT="https://api.groq.com/openai/"
        val TOKEN_SECRET ="Your16LengthKey0"
        val ENCODED_TOKEN_SECRET = TOKEN_SECRET.toByteArray(Charsets.US_ASCII)
        const val NTP_SERVER = "time.google.com"
        val apiEndpoints = listOf(
            ApiEndpoint("ICPress Global", "https://api.icpress.org", isDefault = true),
            ApiEndpoint("Custom", "")
        )
        @Volatile var APP_ENDPOINT=apiEndpoints[0].url
        @Volatile var refreshToken:RefreshToken? = null
        @Volatile var imageLoader:ImageLoader? = null
        fun getOrSetImageLoader(context: Context):ImageLoader{
            val newImageLoader = imageLoader ?: ImageLoader.Builder(context)
                .crossfade(true)
                .okHttpClient(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
             if (imageLoader == null) imageLoader = newImageLoader
            return imageLoader?:newImageLoader
        }

        val dtf: DateTimeFormatter =
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
        val sameYearDtf: DateTimeFormatter =
            DateTimeFormat.forPattern("dd MMMM HH:mm")
        val sameWeekDtf: DateTimeFormatter =
            DateTimeFormat.forPattern("EEEE HH:mm")
        val sameDayDtf: DateTimeFormatter =
            DateTimeFormat.forPattern("HH:mm")

        fun getStandardLocalTime() :Date = Calendar.getInstance(Locale.getDefault()).time


        fun getStandardLocalTimeFromUTCString(utcString:String): Date {
            return TIMESTAMP_PARSER.parseDateTime(utcString).toDate()
        }
        fun getStandardTimeUTCString(): String {
            return getStandardTimeUTCString(getStandardLocalTime())
        }
        fun getStandardTimeUTCString(date: Date): String {
            val localDate = DateTime(date)
            return localDate.toDateTime(DateTimeZone.UTC).toString()
        }
        fun isValidEmail(email: String) = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

        fun getEncryptionKeyAlias():String{
            // Custom Advanced Master Key
            val advancedSpec = KeyGenParameterSpec.Builder(
                "_androidx_security_master_key_", //DEFAULT_MASTER_KEY_ALIAS
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).apply {
                setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                setKeySize(256)
            }.build()

            return MasterKeys.getOrCreate(advancedSpec)
        }
        fun getShortWalletString(wallet:String):String{
           return wallet.substring(
                0,
                4
            ) + "...." + wallet.substring(
                wallet.length - 4,
                wallet.length
            )

        }
    }
}