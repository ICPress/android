package net.crowdventures.storypop.viewmodels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.ArticleCommentPublished
import net.crowdventures.storypop.dto.TransferRequestItem
import net.crowdventures.storypop.paging.ArticleCommentReplySource
import net.crowdventures.storypop.paging.ArticleCommentSource

class RegisterViewModel(var userName : String = "", var email:String ="",
                        var isRequestingSignOnLink:Boolean = false,
                        var sentRegisterRequest:MutableLiveData<Boolean> = MutableLiveData(false), var isConfirmed:MutableLiveData<Boolean> = MutableLiveData(false),
                        var isVerifyingToken :MutableLiveData<Boolean> = MutableLiveData(false ),
                        var loggedInUser :MutableLiveData<AccountInfoFull?> = MutableLiveData(null ),
                        var errorMessage :MutableLiveData<String?> = MutableLiveData(null ),
                        var claimedRewardsSelectedForTransfer :MutableLiveData<ArrayList<TransferRequestItem>?> = MutableLiveData(null ),
                        var userFollowedNotificationsPending: MutableLiveData<Boolean> = MutableLiveData(false ),
                        var userFollowedNotificationsConsumed: MutableLiveData<Boolean> = MutableLiveData(true ),
                        var userFollowedNotificationsLoaded: MutableLiveData<Boolean> = MutableLiveData(false ),
                        var userNotificationsConsumed: MutableLiveData<Boolean> = MutableLiveData(true ),
                        val userCommentReply : MutableLiveData<MutableMap<String,MutableList<ArticleCommentPublished>>> = MutableLiveData(mutableMapOf()),
                        var showProgressbar: MutableLiveData<Boolean> = MutableLiveData(false )
) : ViewModel(){
    fun getArticleComments(loggedInUser:AccountInfoFull?,slugTitle: String,applicationContext: Context):Flow<PagingData<ArticleCommentPublished>> {
        return Pager(
            PagingConfig(pageSize = 6)
        ) {
            ArticleCommentSource(loggedInUser,slugTitle,applicationContext)
        }.flow.cachedIn(viewModelScope)
    }
    fun getArticleCommentsReplies(loggedInUser: AccountInfoFull?,commentUUID: String,slugTitle: String,applicationContext: Context):Flow<PagingData<ArticleCommentPublished>> {
        return Pager(
            PagingConfig(pageSize = 6)
        ) {
            ArticleCommentReplySource(loggedInUser,commentUUID,slugTitle,applicationContext)
        }.flow.cachedIn(viewModelScope)
    }

}