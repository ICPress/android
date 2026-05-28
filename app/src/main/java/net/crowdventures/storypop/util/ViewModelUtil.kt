package net.crowdventures.storypop.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.paging.PagingSource
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.composable.Screen
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.ArticlePrivateSource
import net.crowdventures.storypop.dto.StoryMap
import net.crowdventures.storypop.dto.UserSearchResult
import net.crowdventures.storypop.dto.UsersFollowingInfo
import net.crowdventures.storypop.room.StoryRoom
import net.crowdventures.storypop.util.endpoints.ArticleEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.endpoints.TagEndpoint
import net.crowdventures.storypop.util.endpoints.UserEndpoint
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import net.crowdventures.storypop.viewmodels.StorySavedModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import net.crowdventures.storypop.viewmodels.StylingInfo
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID


class ViewModelUtil {
    companion object{
        private val SECOND_MILLIS : Long = 1000
        val MINUTE_MILLIS = 60 * SECOND_MILLIS
        private val HOUR_MILLIS = 60 * MINUTE_MILLIS
        private val DAY_MILLIS = 24 * HOUR_MILLIS
        fun parseLikesMultipleFromNotification(additionalData: String):CharSequence{
            val indexSeparator = additionalData.indexOf(":")
            return additionalData.subSequence(indexSeparator+1,additionalData.length)
        }
        fun getStoryRoomAsStorySavedModel(storyRoom: StoryRoom ):StorySavedModel{
                val storySavedModel: StorySavedModel =
                    Gson().fromJson(storyRoom.jsonStoryData, StorySavedModel::class.java)
                storySavedModel.updatedDateTime = DateTime(storyRoom.entryTimeStamp, DateTimeZone.UTC).withZone(DateTimeZone.getDefault())
                storySavedModel.storyUUID = storyRoom.storyId
                storySavedModel.storyOriginalUUID = storyRoom.originalStoryUUID
                storySavedModel.publishedSlugTitle = storyRoom.slugTitle
                return storySavedModel
        }

        @Deprecated("https://github.com/google/tink/issues/504")
        fun getStoryRoomAsStorySavedModelSecure(context: Context, storyRoom: StoryRoom ):StorySavedModel?{
                val secureFile = SecureFileUtil.createSecureTmpContentFile(context)
                if (secureFile != null) {
                    SecureFileUtil.writeRawContentToFile(secureFile, storyRoom.jsonStoryData)
                    val secureContent = SecureFileUtil.readSecureTmpContent(context, secureFile)
                    secureFile.delete()
                    val stylingInfo: StorySavedModel =
                        Gson().fromJson(secureContent, StorySavedModel::class.java)
                    return stylingInfo
                } else Log.e("StoryViewModelFact", "Could not get/create tmp secure file!")
            return null
        }
        suspend fun persistUserStory(context: Context,stylingInfo: StylingInfo, contentText:String, titleText:String, emptyTitleDesription:String,
                                     location:String, languageCode: String, tags:Array<String>, storyUUID: UUID, originalStoryUUID: UUID,
                                     authorUUID: UUID, loggedInUsername:String?, isPublished: Boolean, publicSources:Array<String>, privateSources: Array<ArticlePrivateSource>,  storyMap: StoryMap?
        ){
            Log.v(Config.logTag,"Saving state to db..")
            if (loggedInUsername == null && isPublished) throw  IllegalArgumentException("loggedInUsername cannot be null when story has isPublished value!")
            val storySavedModel = StorySavedModel(stylingInfo ,titleText,
                emptyTitleDesription,contentText,location,languageCode, tags,loggedInUsername.toString(), publicSources,privateSources,Constants.loggedInUser?.requireArticleReview == false,storyMap,
                DateTime.now(DateTimeZone.UTC), storyUUID, originalStoryUUID
            )
            saveStorySavedModelDatabase(storySavedModel,context,storyUUID,originalStoryUUID,authorUUID,isPublished)
            Log.v(Config.logTag,"Done saving state!")
        }

        fun startResumeUploads(viewModelScope:CoroutineScope,context: Context){
            if (Constants.uploadJob == null || Constants.uploadJob?.isActive == false){
                Log.v(Config.logTag, "Starting upload job!")
                Constants.uploadJob = viewModelScope.launch(Dispatchers.IO + StoryUtil.coroutineExceptionHandler) {
                    PendingUploadUtil.startResumePendingUploads(context)
                }
            }
        }

        fun <T : Any,A : Any> emptyLoadResult(): PagingSource.LoadResult.Page<A, T> {
            return PagingSource.LoadResult.Page<A, T>(
                data = listOf<T>(),
                prevKey = null,
                nextKey = null
            ) // user must be logged in for the fetchType
        }

        fun goToHome(navController: NavHostController) {
            Screen.Stories.route?.let {
                navController.navigate(it) {
                    // Pop up to the start destination of the graph to
                    // avoid building up a large stack of destinations
                    // on the back stack as users select items
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    // Avoid multiple copies of the same destination when
                    // reselecting the same item
                    launchSingleTop = true
                    // Restore state when reselecting a previously selected item
                    restoreState = true
                }
            }
        }

        fun goToRegister(navController: NavHostController) : ()->Unit = {
            Screen.Profile.route?.let {
                navController.navigate(it) {
                    // Pop up to the start destination of the graph to
                    // avoid building up a large stack of destinations
                    // on the back stack as users select items
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    // Avoid multiple copies of the same destination when
                    // reselecting the same item
                    launchSingleTop = true
                    // Restore state when reselecting a previously selected item
                    restoreState = true
                }
            }
        }

        fun goToMessages(navController: NavHostController,storyViewModel: StorySavedViewModel, userFollowingInfo: UsersFollowingInfo){
            storyViewModel.messagesTargetUsername.value = userFollowingInfo
            storyViewModel.userMessageDraftText = ""
            storyViewModel.userMessageDraftAttachedImage = null
            Screen.Messages.route?.let {
                navController.navigate(it) {

                    // Pop up to the start destination of the graph to
                    // avoid building up a large stack of destinations
                    // on the back stack as users select items
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    // Avoid multiple copies of the same destination when
                    // reselecting the same item
                    launchSingleTop = true
                    // Restore state when reselecting a previously selected item
                    restoreState = true
                }
            }
        }

        @Deprecated("https://github.com/google/tink/issues/504")
        private suspend fun saveStorySavedModelDatabaseSecure(userStory: StorySavedModel,context: Context, storyUUID: UUID,originalStoryUUID:UUID, authorUserUUID: UUID, isPublished:Boolean){
            coroutineScope {
                async {
                    val secureFile = SecureFileUtil.createSecureTmpContentFile(context)
                    val stylingInfoJson = Gson().toJson(userStory)
                    if (secureFile != null){
                        SecureFileUtil.writeSecureTmpContent(context,secureFile,stylingInfoJson)
                        val base64Secure = SecureFileUtil.readSecureTmpContentAsBase64(context,secureFile)
                        secureFile.delete()
                        val storyRoom = StoryRoom(storyUUID, Config.getStandardTimeUTCString(),base64Secure, originalStoryUUID,authorUserUUID,isPublished)
                        Constants.getStoryDatabase(context).storyDao().update(storyRoom)
                    }
                }
            }
        }

        fun fetchArticleTagSuggestions(applicationContext: Context, viewModelScope:CoroutineScope, hashTagSuggestions: MutableLiveData<List<String>>, searchTerm:String){
            viewModelScope.launch (Dispatchers.IO) {
                try {
                    val restAdapter = Retrofit.Builder()
                        .baseUrl(net.crowdventures.storypop.Config.APP_ENDPOINT)
                        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().registerTypeAdapter(UInt::class.java, NullableUintJson()).create()))
                        .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                        .build()
                    val service: TagEndpoint = restAdapter.create(TagEndpoint::class.java)
                    val response = service.getTags(searchTerm)
                    withContext(Dispatchers.Main) {
                        hashTagSuggestions.value = response
                    }
                } catch (ex:Exception){
                    Log.e(net.crowdventures.storypop.Config.logTag,"Could not fetch tags:"+ex.message)
                }
            }
        }

        fun fetchArticle(applicationContext: Context,viewModelScope:CoroutineScope, slugTitle:String, launchStoryCallback: (article:StoryPublishedModel?) -> Unit){
            viewModelScope.launch (Dispatchers.IO) {
                try {
                    val restAdapter = Retrofit.Builder()
                        .baseUrl(net.crowdventures.storypop.Config.APP_ENDPOINT)
                        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().registerTypeAdapter(UInt::class.java, NullableUintJson()).create()))
                        .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                        .build()
                    val service: ArticleEndpoint = restAdapter.create(ArticleEndpoint::class.java)
                    val response = service.getArticle(slugTitle)
                    withContext(Dispatchers.Main) {
                        launchStoryCallback(response)
                    }
                } catch (ex:Exception){
                    Log.e(net.crowdventures.storypop.Config.logTag,"Could not fetch story:"+ex.message)
                    withContext(Dispatchers.Main) {
                        launchStoryCallback(null)
                    }
                }
            }
        }

        fun fetchUsernameSuggestions(applicationContext:Context,loggedInUser: AccountInfoFull,viewModelScope:CoroutineScope, hashTagSuggestions: MutableLiveData<List<UserSearchResult>>, searchTerm:String){
            viewModelScope.launch (Dispatchers.IO) {
                try {
                    val restAdapter = Retrofit.Builder()
                        .baseUrl(net.crowdventures.storypop.Config.APP_ENDPOINT)
                        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().registerTypeAdapter(UInt::class.java, NullableUintJson()).create()))
                        .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                        .build()
                    val service: UserEndpoint = restAdapter.create(UserEndpoint::class.java)
                    val response = service.searchUser("Bearer ${loggedInUser.refreshToken}", searchTerm)
                    withContext(Dispatchers.Main) {
                        hashTagSuggestions.value = response
                    }
                } catch (ex:Exception){
                    Log.e(net.crowdventures.storypop.Config.logTag,"Could not fetch username suggestions:"+ex.message)
                }
            }
        }

        private suspend fun saveStorySavedModelDatabase(userStory: StorySavedModel,context: Context, storyUUID: UUID,originalStoryUUID:UUID, authorUserUUID: UUID, isPublished: Boolean){
            coroutineScope {
                async {
                    val jsonStoryData = Gson().toJson(userStory)
                    val storyRoom = StoryRoom(storyUUID, userStory.updatedDateTime.toDateTime(DateTimeZone.UTC).toString(),jsonStoryData, originalStoryUUID,authorUserUUID, isPublished)
                    Constants.getStoryDatabase(context).storyDao().update(storyRoom)
                }
            }
        }
        fun getTimeAgo(time: Long, timestamp:DateTime): String? {
            val now: Long = System.currentTimeMillis()
            if (time > now || time <= 0) {
                return null
            }

            // TODO: localize
            val diff = now - time
            return if (diff < MINUTE_MILLIS) {
                "just now"
            } else if (diff < 2 * MINUTE_MILLIS) {
                "a minute ago"
            } else if (diff < 50 * MINUTE_MILLIS) {
                (diff / MINUTE_MILLIS).toString() + " minutes ago"
            } else if (diff < 90 * MINUTE_MILLIS) {
                "an hour ago"
            } else if (diff < 24 * HOUR_MILLIS) {
                (diff / HOUR_MILLIS).toString() + " hours ago"
            } else if (diff < 48 * HOUR_MILLIS) {
                "yesterday"
            } else {
                val localPublishedDateTime = DateTime(timestamp, DateTimeZone.UTC).withZone(
                    DateTimeZone.getDefault())
                val currentDft = StoryUtil.getDateTimeFormatter(localPublishedDateTime)
                return timestamp.toString(currentDft)
                // (diff / DAY_MILLIS).toString() + " days ago"
            }
        }
    }
}