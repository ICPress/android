package net.crowdventures.storypop.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.instacart.library.truetime.TrueTime
import net.crowdventures.storypop.BuildConfig
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.UserFollowingCached
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryLiked
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.room.UserFollowed
import net.crowdventures.storypop.util.endpoints.CommentEndpoint
import net.crowdventures.storypop.util.endpoints.FeedbackEndpoint
import net.crowdventures.storypop.util.endpoints.MessageEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.endpoints.UserEndpoint
import net.crowdventures.storypop.util.s3.FlowStatus
import net.crowdventures.storypop.util.s3.S3Handler
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.UUID

class PendingUploadUtil {
    companion object {
        enum class ArticleFeedbackType {
            HEART, READ
        }

        private suspend fun startImageUpload(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull,
            isSmallImage: Boolean
        ): FlowStatus {
            val imagePath =
                ImageUtil.getCreateImageRootDir(context).absolutePath + "/" + current.resourceIdentifier
            val imageFile = File(imagePath)
            val imageUri = Uri.fromFile(imageFile)
            val extension = current.resourceIdentifier.substring(
                current.resourceIdentifier.lastIndexOf(".") + 1
            )
            var contentType = "image/jpg"
            if (extension != "jpg") contentType = "image/" + extension
            try {
                val inputStream: InputStream? =
                    context.contentResolver.openInputStream(imageUri)
                if (inputStream == null) {
                    Log.e(
                        Config.logTag,
                        "Could not resolve input stream for resource: ${current.resourceIdentifier}, clearing all associated uploads!"
                    )
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .deleteAllForAssociatedID(current.associatedID)
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .delete(current)
                }
                inputStream?.use { inputStreamNotNull ->
                    val requestFile: RequestBody =
                        inputStreamNotNull.readBytes().toRequestBody(contentType.toMediaType())
                    val token =
                        S3Handler.getAuthToken() ?: S3Handler.getUpdatedAuthToken(
                            context,
                            loggedInUser
                        )
                    if (token == null) return FlowStatus.FAIL
                    val bucketName = if (current.resourceType == PendingUploadType.MESSAGE_IMAGE) loggedInUser.cdnMessagePublishPath
                    else if (isSmallImage) loggedInUser.cdnSmallPublishPath
                    else loggedInUser.cdnLargePublishPath
                    val uploadStatus =
                        S3Handler.uploadImage(
                            context,
                            requestFile,
                            imageFile.name,
                            token,
                            bucketName
                        )
                    if (uploadStatus == FlowStatus.SUCCESS) {
                        imageFile.delete()
                        Log.v(
                            Config.logTag,
                            "Upload of resource ${current.resourceIdentifier} is successful!"
                        )
                        Constants.getStoryDatabase(context).storyPendingUploadDao()
                            .delete(current)
                    } else {
                        Log.e(
                            Config.logTag,
                            "Could not perform upload of data at this time, skipping future uploads.."
                        )
                        return FlowStatus.FAIL
                    }
                }
            } catch (ex: FileNotFoundException) {
                Config.firebaseAnalytics.logEvent("image_upload_failed_resource_not_found", null)
                Log.e(
                    Config.logTag,
                    "Resource is missing, clearing all associated uploads, ex:" + ex.message
                )
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .deleteAllForAssociatedID(current.associatedID)
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .delete(current)
            }
            return FlowStatus.SUCCESS
        }

        private suspend fun startStoryUpload(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull
        ): FlowStatus {
            val storyUUID = UUID.fromString(current.resourceIdentifier)
            val storyDao = Constants.getStoryDatabase(context).storyDao()
            val currentStory = storyDao.getImmediate(storyUUID)
            val uploadResult =
                EndpointStoryHandler.publishUserStory(context, currentStory, loggedInUser, null)
            if (uploadResult.first == FlowStatus.SUCCESS) {
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .deleteAllForAssociatedID(current.associatedID)
                val existingStory = Constants.getStoryDatabase(context).storyDao().getImmediatePublished(uploadResult.second)
                if (existingStory != null && existingStory.storyId != storyUUID) storyDao.delete(currentStory) //clear duplicate upload attempts of same article
                else if (!currentStory.isPublished || !currentStory.isUploaded)
                {
                        val currentTime =
                            if (TrueTime.isInitialized()) DateTime(TrueTime.now()).toDateTime(
                                DateTimeZone.UTC
                            ) else DateTime(DateTimeZone.UTC)
                        currentStory.isPublished = true
                        currentStory.isUploaded = true
                        currentStory.entryTimeStamp = currentTime.toString()
                        currentStory.slugTitle = uploadResult.second
                        storyDao.update(currentStory)
                }
            }
            return uploadResult.first
        }

        private suspend fun startStoryUpdate(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull
        ): FlowStatus {
            val storyUUID = UUID.fromString(current.resourceIdentifier)
            val storyDao = Constants.getStoryDatabase(context).storyDao()
            val currentStory = storyDao.getImmediate(storyUUID)
            if (currentStory == null) { //SHOULD NOT OCCUR!
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .deleteAllForAssociatedID(current.associatedID)
                return FlowStatus.SUCCESS
            }
            val uploadResult = EndpointStoryHandler.publishUserStory(
                context,
                currentStory,
                loggedInUser,
                current.associatedID
            )
            if (uploadResult.first == FlowStatus.SUCCESS) {
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .deleteAllForAssociatedID(current.associatedID)
            }
            return uploadResult.first
        }

        private suspend fun updateProfileInfo(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull
        ): FlowStatus {
            val uploadResult = EndpointAccountHandler.updateProfile(
                context,
                current.resourceIdentifier,
                loggedInUser.refreshToken
            )
            if (uploadResult) {
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .deleteAllForAssociatedID(current.associatedID)
                return FlowStatus.SUCCESS
            } else return FlowStatus.FAIL

        }

        suspend fun startResumePendingUploads(context: Context) {
            Log.v(Config.logTag, "Fetching current pending uploads..")
            val loggedInUser = Constants.loggedInUser ?: return
            var pending =
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .getAllPendingImmediate(UUID.fromString(loggedInUser.user_uuid))
            var prevFailure = false
            while (pending.isNotEmpty()) {
                val current = pending.first()
                val uploadStatus: FlowStatus = when (current.resourceType) {
                    PendingUploadType.MESSAGE_IMAGE -> {
                        startImageUpload(current, context, loggedInUser, false)
                    }

                    PendingUploadType.IMAGE_SMALL -> {
                        startImageUpload(current, context, loggedInUser, true)
                    }

                    PendingUploadType.IMAGE_LARGE -> {
                        startImageUpload(current, context, loggedInUser, false)
                    }

                    PendingUploadType.STORY -> {
                        startStoryUpload(current, context, loggedInUser)
                    }

                    PendingUploadType.PUBLISH_PROFILE_INFO -> {
                        updateProfileInfo(current, context, loggedInUser)
                    }

                    PendingUploadType.LIKE -> {
                        publishArticleFeedback(
                            current,
                            context,
                            loggedInUser,
                            ArticleFeedbackType.HEART
                        )
                    }

                    PendingUploadType.READ -> {
                        publishArticleFeedback(
                            current,
                            context,
                            loggedInUser,
                            ArticleFeedbackType.READ
                        )
                    }

                    PendingUploadType.REMOVE_LIKE -> {
                        removeArticleLike(current, context, loggedInUser)
                    }

                    PendingUploadType.UPDATE_STORY -> {
                        startStoryUpdate(current, context, loggedInUser)
                    }

                    PendingUploadType.FOLLOW -> {
                        updateUserFollow(current, context, loggedInUser, false)
                    }

                    PendingUploadType.UNFOLLOW -> {
                        updateUserFollow(current, context, loggedInUser, true)
                    }

                    PendingUploadType.COMMENT -> {
                        publishComment(current, context, loggedInUser)
                    }

                    PendingUploadType.COMMENT_HIDE_DELETE -> {
                        publishCommentHideDelete(current, context, loggedInUser)
                    }

                    PendingUploadType.COMMENT_LIKE, PendingUploadType.COMMENT_REMOVE_LIKE -> {
                        publishCommentLikeUnlike(current, context, loggedInUser)
                    }

                    PendingUploadType.MESSAGE -> {
                        publishMessage(current, context, loggedInUser)
                    }

                    PendingUploadType.MESSAGE_DELETE -> {
                        deleteMessage(current, context, loggedInUser)
                    }

                    else -> {
                        FlowStatus.SUCCESS
                    } // NOT MAPPED,INGORE
                }
                prevFailure = if (uploadStatus == FlowStatus.FAIL) {
                    if (prevFailure) return
                    else true
                } else false
                Log.v(Config.logTag, "Fetching next pending uploads..")
                pending = Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .getAllPendingImmediate(UUID.fromString(loggedInUser.user_uuid))
            }
            Log.v(Config.logTag, "Nothing to upload, stopping upload service.")
        }

        fun preparePushImagePendingUpload(
            associatedID: String, imageInfoMetadata: ImageInfoMetadata,
            context: Context, currentUser: AccountInfoFull, viewModel: StorySavedViewModel
        ) {
            val imageName = if (imageInfoMetadata.minWidth != null)
                Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name else imageInfoMetadata.name
            val authorUUID = UUID.fromString(currentUser.user_uuid)
            if (imageInfoMetadata.minHeight != null && imageInfoMetadata.minWidth != null) {
                val pendingUploadMin = StoryPendingUpload(
                    UUID.randomUUID(),
                    Config.getStandardTimeUTCString(),
                    Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name,
                    PendingUploadType.IMAGE_SMALL,
                    associatedID, authorUUID
                )
                viewModel.pushPendingUpload(pendingUploadMin, context)
            }
            val pendingUpload = StoryPendingUpload(
                UUID.randomUUID(), Config.getStandardTimeUTCString(),
                imageInfoMetadata.name, PendingUploadType.IMAGE_LARGE, associatedID, authorUUID
            )
            viewModel.pushPendingUpload(pendingUpload, context)
        }

        private suspend fun publishArticleFeedback(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull,
            articleFeedbackType: ArticleFeedbackType
        ): FlowStatus {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
            val service: FeedbackEndpoint = restAdapter.create(FeedbackEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken
                val response = service.feedbackArticle(
                    "Bearer $authToken",
                    articleFeedbackType.name,
                    loggedInUser.username,
                    current.associatedID
                )
                if (response.isSuccessful) {
                    if (articleFeedbackType == ArticleFeedbackType.HEART) {
                        val storyLikedDao = Constants.getStoryDatabase(context).storyLikedDao()
                        storyLikedDao.update(
                            StoryLiked(
                                current.associatedID,
                                current.resourceIdentifier,
                                Config.getStandardTimeUTCString(),
                                UUID.fromString(loggedInUser.user_uuid)
                            )
                        )
                    }
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .deleteAllForAssociatedID(current.associatedID)
                    return FlowStatus.SUCCESS
                } else if (response.code() == 405) {
                    //invalid formatting of associatedID, bug where emoji-only title causes sql-lite columns miss-aligned
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .deleteAllForAssociatedID(current.associatedID)
                    Config.firebaseAnalytics.logEvent("push_feedback_failed_wrong_identifier", null)
                    return FlowStatus.SUCCESS
                } else {
                    Log.e(
                        Config.logTag,
                        "Publishing of like of article received code:" + response.code()
                    )
                    Config.firebaseAnalytics.logEvent(
                        "push_feedback_failed_status_${response.code()}",
                        null
                    )
                    return FlowStatus.FAIL
                }

            } catch (exHandle: java.lang.IllegalArgumentException) {
                Config.firebaseAnalytics.logEvent("push_feedback_failed_exception", null)
                Log.e(
                    Config.logTag,
                    "Could not push like of story, setting story as liked even if it failed, associatedId:" + current.associatedID,
                    exHandle
                )
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .deleteAllForAssociatedID(current.associatedID)
                return FlowStatus.SUCCESS
            } catch (ex: Exception) {
                Config.firebaseAnalytics.logEvent("push_feedback_failed_exception", null)
                Log.e(Config.logTag, "Could not push like of story!")
                return FlowStatus.FAIL
            }
        }

        private suspend fun updateUserFollow(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull,
            isDelete: Boolean
        ): FlowStatus {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
            val service: UserEndpoint = restAdapter.create(UserEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken
                val response = if (isDelete) service.unfollowUser(
                    "Bearer $authToken",
                    current.associatedID
                ) else service.followUser("Bearer $authToken", current.associatedID)
                if (response.isSuccessful) {
                    val userFollowedDao = Constants.getStoryDatabase(context).userFollowedDao()
                    if (isDelete)
                        userFollowedDao.deleteFollowedId(
                            UUID.fromString(loggedInUser.user_uuid),
                            current.associatedID
                        )
                    else userFollowedDao.update(
                        UserFollowed(
                            current.associatedID,
                            UUID.fromString(loggedInUser.user_uuid)
                        )
                    )
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .deleteAllForAssociatedID(current.associatedID)
                    val sharedPreferenceManager = SharedPreferenceManager(context)
                    val userFollowingCachedJson =
                        sharedPreferenceManager.getLatestUsersFollowedCached()
                    val userFollowingCached =
                        if (userFollowingCachedJson != null) Gson().fromJson<MutableList<UserFollowingCached>>(
                            userFollowingCachedJson,
                            object : TypeToken<MutableList<UserFollowingCached>>() {}.type
                        ) else mutableListOf()
                    if (isDelete) userFollowingCached.removeAll { it.username == current.associatedID } else userFollowingCached.add(
                        UserFollowingCached(
                            current.associatedID,
                            current.resourceIdentifier.ifEmpty { null })
                    )
                    val userFollowedCached = Gson().toJson(userFollowingCached)
                    sharedPreferenceManager.setLatestUserFollowedCached(userFollowedCached)
                    Config.firebaseAnalytics.logEvent("follow_user_success", null)
                    return FlowStatus.SUCCESS
                } else {
                    Log.e(
                        Config.logTag,
                        "Publishing of user " + current.associatedID + " follow failed, received code:" + response.code()
                    )
                    Config.firebaseAnalytics.logEvent(
                        "follow_user_failed_status_${response.code()}",
                        null
                    )
                    return FlowStatus.FAIL
                }
            } catch (ex: Exception) {
                Config.firebaseAnalytics.logEvent("follow_user_failed_exception", null)
                Log.e(Config.logTag, "Could not push follow of user: " + current.associatedID)
                return FlowStatus.FAIL
            }
        }

        private suspend fun removeArticleLike(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull
        ): FlowStatus {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
            val service: FeedbackEndpoint = restAdapter.create(FeedbackEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken
                val response = service.removeLike(
                    "Bearer $authToken",
                    loggedInUser.username,
                    current.associatedID
                )
                if (response.isSuccessful) {
                    val storyLikedDao = Constants.getStoryDatabase(context).storyLikedDao()
                    storyLikedDao.deleteStoryLikedId(
                        current.associatedID,
                        UUID.fromString(loggedInUser.user_uuid)
                    )
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .deleteAllForAssociatedID(current.associatedID)
                    return FlowStatus.SUCCESS
                } else {
                    Log.e(
                        Config.logTag,
                        "Publishing removal of liked article, received code:" + response.code()
                    )
                    return FlowStatus.FAIL
                }
            } catch (ex: Exception) {
                Log.e(Config.logTag, "Could not push like of story!")
                return FlowStatus.FAIL
            }
        }

        private suspend fun publishComment(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull
        ): FlowStatus {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().registerTypeAdapter(
                            UInt::class.java,
                            NullableUintJson()
                        ).create()
                    )
                )
                .client(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
            val service: CommentEndpoint = restAdapter.create(CommentEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken
                val comment = Constants.getStoryDatabase(context).storyCommentDao()
                    .getCommentForStoryImmediate(
                        UUID.fromString(current.resourceIdentifier),
                        current.associatedID
                    )
                    ?: return FlowStatus.SUCCESS
                val response = service.publishComment(
                    "Bearer $authToken", comment
                )
                if (response.isSuccessful) {
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .delete(current)
                    Config.firebaseAnalytics.logEvent("publish_comment_success", null)
                    return FlowStatus.SUCCESS
                } else if (response.code() == 405) {
                    //invalid formatting of associatedID, bug where emoji-only title causes sql-lite columns miss-aligned
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .delete(current)
                    Config.firebaseAnalytics.logEvent(
                        "publish_comment_failed_status_identifier",
                        null
                    )
                    return FlowStatus.SUCCESS
                } else {
                    Log.e(
                        Config.logTag,
                        "Publishing comment for article received code:" + response.code()
                    )
                    Config.firebaseAnalytics.logEvent(
                        "publish_comment_failed_status_${response.code()}",
                        null
                    )
                    return FlowStatus.FAIL
                }

            } catch (exHandle: java.lang.IllegalArgumentException) {
                Log.e(
                    Config.logTag,
                    "Could not push comment of story, setting comment as uploaded even if it failed, associatedId:" + current.associatedID,
                    exHandle
                )
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .delete(current)
                Config.firebaseAnalytics.logEvent("publish_comment_failed_wrong_identifier", null)
                return FlowStatus.SUCCESS
            } catch (ex: Exception) {
                Config.firebaseAnalytics.logEvent("publish_comment_failed_exception", null)
                Log.e(Config.logTag, "Could not push comment of story!")
                return FlowStatus.FAIL
            }
        }

        private suspend fun publishMessage(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull
        ): FlowStatus {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().registerTypeAdapter(
                            UInt::class.java,
                            NullableUintJson()
                        ).create()
                    )
                )
                .client(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
            val service: MessageEndpoint = restAdapter.create(MessageEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken
                val userMessage = Constants.getStoryDatabase(context).userMessageDao()
                    .getMessageImmediate(
                        UUID.fromString(current.resourceIdentifier)
                    ) ?: return FlowStatus.SUCCESS
                val response = service.send(
                    "Bearer $authToken", userMessage
                )
                return if (response.isSuccessful) {
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .delete(current)
                    Config.firebaseAnalytics.logEvent("publish_message_success", null)
                    FlowStatus.SUCCESS
                } else {
                    Log.e(
                        Config.logTag,
                        "Publishing message for ${userMessage.targetUsername} received code:" + response.code()
                    )
                    Config.firebaseAnalytics.logEvent(
                        "publish_message_failed_status_${response.code()}",
                        null
                    )
                    FlowStatus.FAIL
                }

            } catch (exHandle: java.lang.IllegalArgumentException) {
                Log.e(
                    Config.logTag,
                    "Could not push message, setting comment as uploaded even if it failed, associatedId:" + current.associatedID,
                    exHandle
                )
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .delete(current)
                Config.firebaseAnalytics.logEvent("publish_message_wrong_identifier", null)
                return FlowStatus.SUCCESS
            } catch (ex: Exception) {
                Config.firebaseAnalytics.logEvent("publish_message_failed_exception", null)
                Log.e(Config.logTag, "Could not push message!")
                return FlowStatus.FAIL
            }
        }

        private suspend fun deleteMessage(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull
        ): FlowStatus {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().registerTypeAdapter(
                            UInt::class.java,
                            NullableUintJson()
                        ).create()
                    )
                )
                .client(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
            val service: MessageEndpoint = restAdapter.create(MessageEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken
                val userMessage = Constants.getStoryDatabase(context).userMessageDao()
                    .getMessageImmediate(
                        UUID.fromString(current.resourceIdentifier)
                    ) ?: return FlowStatus.SUCCESS
                val response = service.delete(
                    "Bearer $authToken", userMessage.messageUUID
                )
                return if (response.isSuccessful) {
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .delete(current)
                    Config.firebaseAnalytics.logEvent("publish_message_success", null)
                    FlowStatus.SUCCESS
                } else {
                    Log.e(
                        Config.logTag,
                        "Publishing message for ${userMessage.targetUsername} received code:" + response.code()
                    )
                    Config.firebaseAnalytics.logEvent(
                        "publish_message_failed_status_${response.code()}",
                        null
                    )
                    FlowStatus.FAIL
                }
            } catch (ex: Exception) {
                Config.firebaseAnalytics.logEvent("delete_message_failed_exception", null)
                Log.e(Config.logTag, "Could not delete message!")
                return FlowStatus.FAIL
            }
        }

        private suspend fun publishCommentHideDelete(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull
        ): FlowStatus {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().registerTypeAdapter(
                            UInt::class.java,
                            NullableUintJson()
                        ).create()
                    )
                )
                .client(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
            val service: CommentEndpoint = restAdapter.create(CommentEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken
                val response = service.deleteComment(
                    "Bearer $authToken", current.associatedID, current.resourceIdentifier
                )
                if (response.isSuccessful) {
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .delete(current)
                    return FlowStatus.SUCCESS
                } else if (response.code() == 405) {
                    //invalid formatting of associatedID, bug where emoji-only title causes sql-lite columns miss-aligned
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .delete(current)
                    return FlowStatus.SUCCESS
                } else {
                    Log.e(
                        Config.logTag,
                        "Publishing delete of comment received code:" + response.code()
                    )
                    return FlowStatus.FAIL
                }

            } catch (exHandle: java.lang.IllegalArgumentException) {
                Log.e(
                    Config.logTag,
                    "Could not comment delete, associatedId:" + current.associatedID,
                    exHandle
                )
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .delete(current)
                return FlowStatus.SUCCESS
            } catch (ex: Exception) {
                Log.e(Config.logTag, "Could not comment delete!")
                return FlowStatus.FAIL
            }
        }

        private suspend fun publishCommentLikeUnlike(
            current: StoryPendingUpload,
            context: Context,
            loggedInUser: AccountInfoFull
        ): FlowStatus {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().registerTypeAdapter(
                            UInt::class.java,
                            NullableUintJson()
                        ).create()
                    )
                )
                .client(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
            val service: CommentEndpoint = restAdapter.create(CommentEndpoint::class.java)
            try {
                val authToken = loggedInUser.refreshToken
                val response =
                    if (current.resourceType == PendingUploadType.COMMENT_LIKE) service.likeComment(
                        "Bearer $authToken", current.associatedID, current.resourceIdentifier
                    )
                    else service.deleteLikeComment(
                        "Bearer $authToken", current.associatedID, current.resourceIdentifier
                    )
                if (response.isSuccessful) {
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .delete(current)
                    Config.firebaseAnalytics.logEvent("comment_feedback_success", null)
                    return FlowStatus.SUCCESS
                } else if (response.code() == 405) {
                    //invalid formatting of associatedID, bug where emoji-only title causes sql-lite columns miss-aligned
                    Constants.getStoryDatabase(context).storyPendingUploadDao()
                        .delete(current)
                    Config.firebaseAnalytics.logEvent("comment_feedback_failed_identifier", null)
                    return FlowStatus.SUCCESS
                } else {
                    Log.e(
                        Config.logTag,
                        "Publishing like of comment received code:" + response.code()
                    )
                    Config.firebaseAnalytics.logEvent(
                        "comment_feedback_failed_status_${response.code()}",
                        null
                    )
                    return FlowStatus.FAIL
                }

            } catch (exHandle: java.lang.IllegalArgumentException) {
                Log.e(
                    Config.logTag,
                    "Could not like comment, associatedId:" + current.associatedID,
                    exHandle
                )
                Constants.getStoryDatabase(context).storyPendingUploadDao()
                    .delete(current)
                Config.firebaseAnalytics.logEvent("comment_feedback_failed_wrong_identifier", null)
                return FlowStatus.SUCCESS
            } catch (ex: Exception) {
                Config.firebaseAnalytics.logEvent("comment_feedback_failed_exception", null)
                Log.e(Config.logTag, "Could not like comment!")
                return FlowStatus.FAIL
            }
        }


    }
}