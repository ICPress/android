package net.crowdventures.storypop.viewmodels

import android.content.Context
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.TextStyle
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.ArticlePrivateSource
import net.crowdventures.storypop.dto.Notification
import net.crowdventures.storypop.dto.Reward
import net.crowdventures.storypop.dto.RewardClaimed
import net.crowdventures.storypop.dto.StoryMap
import net.crowdventures.storypop.dto.Transaction
import net.crowdventures.storypop.dto.UserMessagePublished
import net.crowdventures.storypop.dto.UserSearchResult
import net.crowdventures.storypop.dto.UsersFollowingInfo
import net.crowdventures.storypop.paging.MessageSource
import net.crowdventures.storypop.paging.RewardsStorySource
import net.crowdventures.storypop.paging.StoryFetchType
import net.crowdventures.storypop.paging.StorySource
import net.crowdventures.storypop.paging.TransactionsSource
import net.crowdventures.storypop.paging.UsersFollowedStorySource
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryDatabase
import net.crowdventures.storypop.room.StoryLiked
import net.crowdventures.storypop.room.StoryLikedDao
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.room.UserFollowedDao
import net.crowdventures.storypop.room.UserMessageWithPendingUploads
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.ViewModelUtil
import java.util.UUID

class StorySavedViewModel(val storySavedViewModelRepository: StorySavedViewModelRepository) :
    ViewModel() {
    var storyDatabase: StoryDatabase
    var storyLikedDao: StoryLikedDao
    var userFollowedDao: UserFollowedDao
    private var _hashTagFilter: String? = null
    val hashTagFilter: String? get() = _hashTagFilter
    val likedArticleList = ArrayList<String>()
    val hashTagSuggestions = MutableLiveData<List<String>>()
    val userSuggestions = MutableLiveData<List<UserSearchResult>>()
    @Volatile var hasNotificationItems = false
    val unfollowedUsers = ArrayList<String>()
    val storyBarExpanded = MutableLiveData(true)
    var messagesTargetUsername =  MutableLiveData<UsersFollowingInfo>()
    var userMessages = flowOf(PagingData.from(listOf<UserMessageWithPendingUploads>())).cachedIn(
        viewModelScope)
    var userRemoteMessages = flowOf(PagingData.from(listOf<UserMessagePublished>())).cachedIn(
        viewModelScope)
    var userMessageDraftText :String =""
    var userMessageDraftAttachedImage : ImageInfoMetadata? = null

    fun setUserMessagesFromDatabase(applicationContext: Context,userInfo:UsersFollowingInfo, loggedInUser: AccountInfoFull){
        val storyDatabase = Constants.getStoryDatabase(storySavedViewModelRepository.context)
        val pager = Pager(PagingConfig(pageSize = 10)) {
            storyDatabase.userMessageDao()
                .getMessagesForUserWithPendingUpload(userInfo.username, loggedInUser.username )
        }.flow.flowOn(Dispatchers.IO).cachedIn(viewModelScope)

        userMessages = pager
        userRemoteMessages = Pager(androidx.paging.PagingConfig(pageSize = 10)) {
            MessageSource(applicationContext,storySavedViewModelRepository.sharedPreferenceManager, userInfo.username)
        }.flow.cachedIn(viewModelScope)
    }
    init {
        storyDatabase = Constants.getStoryDatabase(storySavedViewModelRepository.context)
        storyLikedDao = storyDatabase.storyLikedDao()
        userFollowedDao = storyDatabase.userFollowedDao()
        storySavedViewModelRepository.registerViewModel.loggedInUser.observe(
            storySavedViewModelRepository.lifecycleOwner
        ) { loggedInUser ->
            if (loggedInUser != null) {
                val loggedInAuthorUUID = UUID.fromString(loggedInUser.user_uuid)
                storyDatabase.storyDao().getAllPendingUpload().observe(
                    storySavedViewModelRepository.lifecycleOwner
                ) { _ -> //refresh pending published story
                    userPublishedStories = Pager(PagingConfig(pageSize = 6)) {
                        StorySource(storySavedViewModelRepository.context,
                            StoryFetchType.PUBLISHED_BY_USER,
                            null,
                            storySavedViewModelRepository.sharedPreferenceManager
                        )
                    }.flow.cachedIn(viewModelScope)
                }
                storyLikedDao.getAllLikes(loggedInAuthorUUID).observe(
                    storySavedViewModelRepository.lifecycleOwner
                ) { _ -> //refresh liked story source
                    userLikedStories = Pager(PagingConfig(pageSize = 6)) {
                        StorySource(storySavedViewModelRepository.context,
                            StoryFetchType.LIKED,
                            null,
                            storySavedViewModelRepository.sharedPreferenceManager
                        )
                    }.flow.cachedIn(viewModelScope)
                }

                userFollowedStoriesCached = storySavedViewModelRepository
                    .getUserFollowedModelFromDatabase(loggedInAuthorUUID,loggedInUser).cachedIn(viewModelScope)

                rewardsStorySource = Pager(androidx.paging.PagingConfig(pageSize = 6)) {
                    RewardsStorySource<Reward>(storySavedViewModelRepository.context, loggedInUser, false)
                }.flow.cachedIn(viewModelScope)

                claimedRewardsStorySource = Pager(androidx.paging.PagingConfig(pageSize = 6)) {
                    RewardsStorySource<RewardClaimed>(storySavedViewModelRepository.context,loggedInUser, true)
                }.flow.cachedIn(viewModelScope)

                transactionsSource = Pager(androidx.paging.PagingConfig(pageSize = 6)) {
                    net.crowdventures.storypop.paging.TransactionsSource(storySavedViewModelRepository.context,loggedInUser)
                }.flow.cachedIn(viewModelScope)
                userPendingUserFollowed = storySavedViewModelRepository.getPendingUserFollowedModelFromDatabase( loggedInAuthorUUID)
                    .cachedIn(viewModelScope)

                notificationSource = Pager(androidx.paging.PagingConfig(pageSize = 6), initialKey =  if (loggedInUser.unreadNotifications == 0u) storySavedViewModelRepository.sharedPreferenceManager.getLatestCachedNotificationKey() else null) {
                        net.crowdventures.storypop.paging.NotificationSource(storySavedViewModelRepository.context,storySavedViewModelRepository.registerViewModel,
                            storySavedViewModelRepository.sharedPreferenceManager
                        )
                    }.flow.cachedIn(viewModelScope)
            }
        }
    }

    val userNonPublishedStories: Flow<PagingData<StorySavedModel>> =
        storySavedViewModelRepository.getNonPublishedSavedStoryModelFromDatabase()
            .cachedIn(viewModelScope)
    val userPendingPublishedStories: Flow<PagingData<StorySavedModel>> =
        storySavedViewModelRepository.getPendingPublishedSavedStoryModelFromDatabase()
            .cachedIn(viewModelScope)
    var userPendingUserFollowed: Flow<PagingData<UsersFollowingInfo>> = flowOf(PagingData.from(listOf<UsersFollowingInfo>())).cachedIn(
        viewModelScope)

    val userPublishedRecentStories: Flow<PagingData<StorySavedModel>> = storySavedViewModelRepository.getPublishedRecentStoryModelFromDatabase()
        .cachedIn(viewModelScope)

    var stories: MutableLiveData<Flow<PagingData<StoryPublishedModel>>> =
        MutableLiveData(Pager(PagingConfig(pageSize = 6)) {
            StorySource(storySavedViewModelRepository.context,
                StoryFetchType.ARTICLES,
                null,
                storySavedViewModelRepository.sharedPreferenceManager
            )
        }.flow.cachedIn(viewModelScope))
    var userFollowedStories: Flow<PagingData<UsersFollowingInfo>> =
        Pager(PagingConfig(pageSize = 6)) {
            UsersFollowedStorySource(storySavedViewModelRepository.context,storyDatabase,storySavedViewModelRepository.registerViewModel,
                storySavedViewModelRepository.sharedPreferenceManager
            )
        }.flow.cachedIn(viewModelScope)
    var userFollowedStoriesCached =
        flowOf(PagingData.from<UsersFollowingInfo>(listOf<UsersFollowingInfo>())).cachedIn(
            viewModelScope
        )


    fun clearArticleFiltering() {
        _hashTagFilter = null
        stories.value = Pager(PagingConfig(pageSize = 6)) {
            StorySource(storySavedViewModelRepository.context,
                StoryFetchType.ARTICLES,
                null,
                storySavedViewModelRepository.sharedPreferenceManager
            )
        }.flow.cachedIn(viewModelScope)
    }

    fun performArticleFiltering(hashtag: String? = null) {
        if (hashtag != null) _hashTagFilter = hashtag
        stories.value = Pager(PagingConfig(pageSize = 6)) {
            StorySource(storySavedViewModelRepository.context,
                StoryFetchType.ARTICLES,
                _hashTagFilter,
                storySavedViewModelRepository.sharedPreferenceManager
            )
        }.flow.cachedIn(viewModelScope)
    }

    var rewardsStorySource: Flow<PagingData<Reward>> = Pager(PagingConfig(pageSize = 6)) {
        RewardsStorySource<Reward>(storySavedViewModelRepository.context,null, false)
    }.flow.cachedIn(viewModelScope)

    var transactionsSource: Flow<PagingData<Transaction>> = Pager(PagingConfig(pageSize = 6)) {
        TransactionsSource(storySavedViewModelRepository.context,null)
    }.flow.cachedIn(viewModelScope)

    var claimedRewardsStorySource: Flow<PagingData<RewardClaimed>> =
        Pager(PagingConfig(pageSize = 6)) {
            RewardsStorySource<RewardClaimed>(storySavedViewModelRepository.context,null, true)
        }.flow.cachedIn(viewModelScope)

    var userPublishedStories: Flow<PagingData<StoryPublishedModel>> =
        Pager(PagingConfig(pageSize = 6)) {
            StorySource(storySavedViewModelRepository.context,
                null,
                null,
                storySavedViewModelRepository.sharedPreferenceManager
            ) //DO NOT USE, THIS IS A STUB REPLACED WHEN LOGGED IN
        }.flow.cachedIn(viewModelScope)

    var userLikedStories: Flow<PagingData<StoryPublishedModel>> =
        Pager(PagingConfig(pageSize = 6)) {
            StorySource(storySavedViewModelRepository.context,
                null,
                null,
                storySavedViewModelRepository.sharedPreferenceManager
            ) //DO NOT USE, THIS IS A STUB REPLACED WHEN LOGGED IN
        }.flow.cachedIn(viewModelScope)

     var notificationSource: Flow<PagingData<Notification>> =  flowOf(PagingData.from<Notification>(listOf<Notification>())).cachedIn(
         viewModelScope
     )


    fun deleteUserStory(storyUUID: UUID) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
            storySavedViewModelRepository.deleteSavedStory(storyUUID)
        }
    }

    fun pushPendingUpload(pendingUpload: StoryPendingUpload, context: Context) {
        viewModelScope.launch(Dispatchers.IO + StoryUtil.coroutineExceptionHandler) {
            storySavedViewModelRepository.pushPendingUpload(pendingUpload)
            ViewModelUtil.startResumeUploads(this, context.applicationContext)
        }
    }

    private suspend fun scheduleStoryUpload(
        context: Context,
        storyUUID: UUID,
        authorUUID: UUID,
        updatingExistingStorySlugTitle: String?
    ) {
        val storyDatabase = Constants.getStoryDatabase(context)
        val pendingUpload = StoryPendingUpload(
            UUID.randomUUID(),
            net.crowdventures.storypop.Config.getStandardTimeUTCString(),
            storyUUID.toString(),
            if (updatingExistingStorySlugTitle == null) PendingUploadType.STORY else PendingUploadType.UPDATE_STORY,
            updatingExistingStorySlugTitle ?: storyUUID.toString(),
            authorUUID
        )
        storyDatabase.storyPendingUploadDao().update(pendingUpload)
    }

    fun updateUserStory(
        context: Context,
        contentText: String,
        contentTitleText: String,
        emptyTitleText: String,
        contentLocation: String,
        originalStoryUUID: UUID,
        authorUUID: UUID,
        stylingInfo: StylingInfo,
        publishedSlugTitle: String,
        selectedTags: ArrayList<String>?,
        publicSources:Array<String>, privateSources: Array<ArticlePrivateSource>,
        storyMap: StoryMap?
    ) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
            val loggedInUserName = Constants.loggedInUser?.username ?: return@launch
            val newStoryUUID = UUID.randomUUID()
            scheduleUploadPictures(context, stylingInfo, publishedSlugTitle, authorUUID, true)
            persist(
                context,
                contentText,
                contentTitleText,
                emptyTitleText,
                contentLocation,
                newStoryUUID,
                originalStoryUUID,
                authorUUID,
                stylingInfo,
                loggedInUserName,
                true,
                selectedTags,
                publicSources,
                privateSources,
                storyMap
            )
            scheduleStoryUpload(context, newStoryUUID, authorUUID, publishedSlugTitle)
            ViewModelUtil.startResumeUploads(this, context.applicationContext)
        }

    }

    private suspend fun persist(
        context: Context,
        contentText: String,
        contentTitleText: String,
        emptyTitleText: String,
        contentLocation: String,
        storyUUID: UUID,
        originalStoryUUID: UUID,
        authorUUID: UUID,
        stylingInfo: StylingInfo,
        loggedInUserName: String?,
        isPublished: Boolean,
        selectedTags: ArrayList<String>?,
        publicSources:Array<String>, privateSources: Array<ArticlePrivateSource>,
        storyMap: StoryMap?
    ) {
        val imm: InputMethodManager? =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        val ims: InputMethodSubtype? = imm?.currentInputMethodSubtype
        val inputLang: String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && ims?.languageTag != null) {
                ims.languageTag
            } else {
                ""
            }
        val selectedTagsEntry = selectedTags?.toTypedArray() ?: arrayOf<String>()
        ViewModelUtil.persistUserStory(
            context,
            stylingInfo,
            contentText,
            contentTitleText,
            emptyTitleText,
            contentLocation,
            inputLang,
            selectedTagsEntry,
            storyUUID,
            originalStoryUUID,
            authorUUID,
            loggedInUserName,
            isPublished,
            publicSources,
            privateSources,
            storyMap
        )
    }

    fun saveUserStory(
        context: Context,
        contentText: String,
        contentTitleText: String,
        contentLocation: String,
        storyUUID: UUID,
        originalStoryUUID: UUID,
        authorUUID: UUID,
        stylingInfo: StylingInfo,
        loggedInUserName: String?,
        isPublished: Boolean,
        selectedTags: ArrayList<String>?,
        publicSources:Array<String>, privateSources: Array<ArticlePrivateSource>,
        storyMap: StoryMap?
    ) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
            persist(
                context,
                contentText,
                contentTitleText,
                "",
                contentLocation,
                storyUUID,
                originalStoryUUID,
                authorUUID,
                stylingInfo,
                loggedInUserName,
                isPublished,
                selectedTags,
                publicSources,
                privateSources,
                storyMap
            )
        }
    }

    private suspend fun scheduleUploadPictures(
        context: Context,
        stylingInfo: StylingInfo,
        associatedID: String,
        authorUUID: UUID,
        isPublished: Boolean
    ) {
        val storyDatabase = Constants.getStoryDatabase(context)

        val publishedImages = ArrayList<String>();
        for (span in stylingInfo.spans.filter { it.style == TextStyle.IMAGE }) {
            if (span.additionalInfoFlag == null) continue
            val imageInfoMetadata =
                Gson().fromJson(span.additionalInfoFlag, ImageInfoMetadata::class.java)
            if (isPublished && !ImageUtil.checkImageExists(imageInfoMetadata, context)) continue
            if (publishedImages.contains(imageInfoMetadata.name)) continue //a copy of image already in queue (from copy/paste op), skip
            if (imageInfoMetadata?.minHeight != null && imageInfoMetadata.minWidth != null) {
                val pendingUploadMin = StoryPendingUpload(
                    UUID.randomUUID(),
                    Config.getStandardTimeUTCString(),
                    Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name,
                    PendingUploadType.IMAGE_SMALL,
                    associatedID,
                    authorUUID
                )
                storyDatabase.storyPendingUploadDao().update(pendingUploadMin)
            }
            val pendingUpload = StoryPendingUpload(
                UUID.randomUUID(), Config.getStandardTimeUTCString(),
                imageInfoMetadata.name, PendingUploadType.IMAGE_LARGE, associatedID, authorUUID
            )
            publishedImages.add(imageInfoMetadata.name)
            storyDatabase.storyPendingUploadDao().update(pendingUpload)
        }
        val backgroundImageUri = stylingInfo.titleBackgroundImage ?: return
        val imageInfoMetadata = Gson().fromJson(backgroundImageUri, ImageInfoMetadata::class.java)
        if (!isPublished || (isPublished && ImageUtil.checkImageExists(
                imageInfoMetadata,
                context
            ))
        ) {
            if (imageInfoMetadata?.minHeight != null && imageInfoMetadata.minWidth != null) {
                val pendingUploadMin = StoryPendingUpload(
                    UUID.randomUUID(),
                    Config.getStandardTimeUTCString(),
                    Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name,
                    PendingUploadType.IMAGE_SMALL,
                    associatedID,
                    authorUUID
                )
                storyDatabase.storyPendingUploadDao().update(pendingUploadMin)
            }
            val pendingUpload = StoryPendingUpload(
                UUID.randomUUID(), Config.getStandardTimeUTCString(),
                imageInfoMetadata.name, PendingUploadType.IMAGE_LARGE, associatedID, authorUUID
            )
            storyDatabase.storyPendingUploadDao().update(pendingUpload)
        }
    }

    fun publishUserStory(
        context: Context,
        contentText: String,
        contentTitleText: String,
        emptyTitleText: String,
        contentLocation: String,
        storyUUID: UUID,
        originalStoryUUID: UUID,
        authorUUID: UUID,
        stylingInfo: StylingInfo,
        selectedTags: ArrayList<String>?,
        publicSources:Array<String>, privateSources: Array<ArticlePrivateSource>,
        storyMap: StoryMap?
    ) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
            val loggedInUserName = Constants.loggedInUser?.username ?: return@launch
            scheduleUploadPictures(context, stylingInfo, storyUUID.toString(), authorUUID, false)
            persist(
                context,
                contentText,
                contentTitleText,
                emptyTitleText,
                contentLocation,
                storyUUID,
                originalStoryUUID,
                authorUUID,
                stylingInfo,
                loggedInUserName,
                true,
                selectedTags,
                publicSources,
                privateSources,
                storyMap
            )
            scheduleStoryUpload(context, storyUUID, authorUUID, null)
            ViewModelUtil.startResumeUploads(this, context.applicationContext)
        }

    }

    fun updateExistingLikedStoriesDatabase(storyPublishedModel: StoryPublishedModel) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
            val loggedInAuthorUUID = Constants.loggedInUser?.user_uuid
            if (loggedInAuthorUUID != null) {
                val existsting =
                    storyLikedDao.getLikeForStoryImmediate(
                        storyPublishedModel.slugTitle,
                        UUID.fromString(loggedInAuthorUUID)
                    )
                if (existsting == null) storyLikedDao.update(
                    StoryLiked(
                        storyPublishedModel.slugTitle,
                        storyPublishedModel.storyTitle,
                        net.crowdventures.storypop.Config.getStandardTimeUTCString(),
                        UUID.fromString(loggedInAuthorUUID)
                    )
                )
            }
        }
    }

    fun findArticleTags(applicationContext: Context,searchTerm: String) {
        ViewModelUtil.fetchArticleTagSuggestions(applicationContext,viewModelScope, hashTagSuggestions, searchTerm)
    }

    fun findUser(applicationContext: Context,loggedInUser: AccountInfoFull,searchTerm: String){
        ViewModelUtil.fetchUsernameSuggestions(applicationContext,loggedInUser,viewModelScope, userSuggestions, searchTerm)
    }

    fun startResumeExistingUploads(context: Context) {
        ViewModelUtil.startResumeUploads(viewModelScope, context.applicationContext)
    }
}