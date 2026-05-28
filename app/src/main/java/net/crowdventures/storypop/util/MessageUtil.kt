package net.crowdventures.storypop.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewModelScope
import coil.compose.rememberImagePainter
import coil.size.OriginalSize
import coil.size.Scale
import com.google.gson.Gson
import com.instacart.library.truetime.TrueTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import net.crowdventures.storypop.BuildConfig
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.UsersFollowingInfo
import net.crowdventures.storypop.room.MessageType
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.room.UserMessage
import net.crowdventures.storypop.util.endpoints.ContactEndpoint
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import retrofit2.Retrofit
import java.io.File
import java.util.UUID

class MessageUtil {
    companion object{
        fun sendMessage (latestRemoteMessageId:UInt?, storySavedViewModel:StorySavedViewModel, loggedInUser:AccountInfoFull,newMessageText:String, userFollowingInfo: UsersFollowingInfo, imagesAttached: List<ImageInfoMetadata> = listOf()){
            if (latestRemoteMessageId != null) storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager.setLatestMessageIdRepliedForUser(userFollowingInfo.username,latestRemoteMessageId)
            val currentTimeUTC = if (TrueTime.isInitialized()) DateTime(
                TrueTime.now()).toDateTime(DateTimeZone.UTC) else  DateTime(DateTimeZone.UTC)
            val messageUUID = UUID.randomUUID().toString()
            val entryTimeStamp = currentTimeUTC.toString()
            val content = if (imagesAttached.isNotEmpty()) Gson().toJson(imagesAttached) else newMessageText
            val message = UserMessage(
                loggedInUser.username,
                if (imagesAttached.isEmpty()) MessageType.TEXT else MessageType.IMAGES,
                content,
                messageUUID,
                userFollowingInfo.username,
                false,
                entryTimeStamp
            )
            storySavedViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable) {
                Config.firebaseAnalytics.logEvent(
                    "story_message_publish_started",
                    null
                )
                val storyDatabase =
                    Constants.getStoryDatabase(storySavedViewModel.storySavedViewModelRepository.context)
                for (image in imagesAttached){
                    val messagePendingUpload = StoryPendingUpload(
                        UUID.randomUUID(),
                        entryTimeStamp,
                        image.name,
                        PendingUploadType.MESSAGE_IMAGE,messageUUID,
                        UUID.fromString(loggedInUser.user_uuid))
                    storyDatabase.storyPendingUploadDao().update(messagePendingUpload)
                }
                storyDatabase.userMessageDao().update(message)
                val messagePendingUpload = StoryPendingUpload(
                    UUID.randomUUID(),
                    entryTimeStamp,
                    messageUUID,
                    PendingUploadType.MESSAGE,userFollowingInfo.username,
                    UUID.fromString(loggedInUser.user_uuid))
                storyDatabase.storyPendingUploadDao().update(messagePendingUpload)
                ViewModelUtil.startResumeUploads(this,storySavedViewModel.storySavedViewModelRepository.context)
            }
        }
        suspend fun approveBlockContact(
            context: Context,
            loggedInUser: AccountInfoFull,
            targetUsername:String,
            isApproval: Boolean
        ): Boolean {
            val approvalOrBlock =  if (isApproval) "approve" else "block"
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
            val service: ContactEndpoint = restAdapter.create(ContactEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken
                val response = if (isApproval) service.approve(
                    "Bearer $authToken",
                    targetUsername
                ) else service.block(
                    "Bearer $authToken",
                    targetUsername
                )
                return if (response.isSuccessful) {
                    true
                } else {
                    val responseCode = response.code()
                    Log.e(Config.logTag, "Contact $approvalOrBlock failed, received code:$responseCode")
                    false
                }
            } catch (ex: Exception) {
                Log.e(Config.logTag, "Contact $approvalOrBlock failed, exception:" + ex.message,ex)
                return false
            }
        }

        fun getAnnotatedString(messageType: MessageType,content:String,deleted: Boolean, linkColor:Color, isNotification:Boolean): AnnotatedString{
           return buildAnnotatedString {
                if (deleted)
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)) {
                        append("Message deleted")
                    }
                else if (messageType == MessageType.TEXT){
                    var idxFirstHyperlink = content.indexOf( "http://")
                    var idxFirstSSLHyperlink = content.indexOf("https://")
                    var idxFirstUsername = content.indexOf("@")
                    var idxFirstHashtag = content.indexOf("#")
                    var maxIdx = -1
                    var endIndex = -1
                    var prevEndIndex = -1
                    while (idxFirstHyperlink >= 0 || idxFirstSSLHyperlink >= 0 || idxFirstUsername >= 0 || idxFirstHashtag>= 0 ){
                        if (idxFirstHyperlink == -1) idxFirstHyperlink = 999
                        if (idxFirstSSLHyperlink == -1) idxFirstSSLHyperlink = 999
                        if (idxFirstUsername == -1) idxFirstUsername = 999
                        if (idxFirstHashtag == -1) idxFirstHashtag = 999
                        maxIdx = idxFirstHyperlink.coerceAtMost(idxFirstSSLHyperlink).coerceAtMost(idxFirstUsername).coerceAtMost(idxFirstHashtag)
                        endIndex = content.indexOf(" ",maxIdx+2)
                        //if (endIndex == -1) endIndex = content.length - maxIdx-2
                        if (maxIdx >=0){
                            if (prevEndIndex > 0) append(content.subSequence(prevEndIndex,maxIdx)) else append(content.subSequence(0,maxIdx))
                            if (!isNotification) {
                                val annotation = if (endIndex ==-1) content.substring(maxIdx) else content.substring(maxIdx,endIndex)
                                pushStringAnnotation(tag = content.substring(maxIdx,maxIdx+1), annotation = annotation)
                                withStyle(style = SpanStyle(color = linkColor)) {
                                    append(annotation)
                                }
                                pop()
                            }else{
                                withStyle(style = SpanStyle(color = Color.DarkGray ,fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal)) {
                                    append("(hidden link)")
                                }
                            }
                        }
                        maxIdx = if (endIndex > 0) endIndex else maxIdx +1
                        idxFirstHyperlink = content.indexOf( "http://",maxIdx)
                        idxFirstSSLHyperlink = content.indexOf("https://",maxIdx)
                        idxFirstUsername = content.indexOf("@",maxIdx)
                        idxFirstHashtag = content.indexOf("#",maxIdx)
                        prevEndIndex = endIndex
                    }
                    if (endIndex != -1){
                        append(content.substring(endIndex))
                    }else if (maxIdx == -1){
                        append(content)
                    }
                } else if (messageType == MessageType.IMAGES) {
                    withStyle(style = SpanStyle(color = Color.DarkGray ,fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal)) {
                        append("(hidden image)")
                    }
                } else{
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)) {
                        append("Unknown message type")
                    }
                }
            }
        }
        @Composable
        fun ShowAttachedImage(imageInfoMetadata: ImageInfoMetadata,context: Context, loggedInUser: AccountInfoFull) {
            val imageName = imageInfoMetadata.name
            val imageRequestPath = loggedInUser.cdnMessageRequestPath
            val imagePath = ImageUtil.getCreateImageRootDir(context).absolutePath + "/"+ imageName
            val file = File(imagePath)
            Image(
                painter = rememberImagePainter(
                    // rememberImagePainter
                    data = if (file.exists()) Uri.fromFile(file) else imageRequestPath + imageName,
                    builder = {
                        crossfade(true).size(OriginalSize).scale(Scale.FILL)
                    },
                ),
                contentDescription = "Profile Badge",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            )
        }
    }
}