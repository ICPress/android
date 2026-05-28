package net.crowdventures.storypop.util

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.dto.ArticleGuidelines
import net.crowdventures.storypop.dto.GuidelineCategory
import net.crowdventures.storypop.dto.GuidelineRule
import net.crowdventures.storypop.util.endpoints.ArticleEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream

object GuidelinesProvider {
    suspend fun fetchGuidelines(context: Context): ArticleGuidelines {
        return withContext(Dispatchers.IO) {
                    val restAdapter = Retrofit.Builder()
                        .baseUrl(net.crowdventures.storypop.Config.APP_ENDPOINT)
                        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().registerTypeAdapter(UInt::class.java, NullableUintJson()).create()))
                        .client(RetrofitUtil.generateSecureOkHttpClient(context))
                        .build()
                    val service: ArticleEndpoint = restAdapter.create(ArticleEndpoint::class.java)
                    service.getArticleGuidelines()
        }
    }
}