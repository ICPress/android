package net.crowdventures.storypop.adapters

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.TimeSinceTextView
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.ArticleCommentPublished
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryComment
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.MultilineRemover
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.UUID

class ArticleCommentAdapter(
    val lifecycleOwner: LifecycleOwner,
    val registerViewModel: RegisterViewModel,
    val isReplyAdapter: Boolean,
    val isArticleAuthor: Boolean,
    private val onReplyClicked: ((ArticleCommentPublished, commentView: View) -> Unit)? = null
) : PagingDataAdapter<ArticleCommentPublished, ArticleCommentAdapter.ArticleCommentPublishedViewHolder>(
    differ
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleCommentPublishedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return ArticleCommentPublishedViewHolder(lifecycleOwner, registerViewModel, view, this@ArticleCommentAdapter)
    }

    override fun onBindViewHolder(holder: ArticleCommentPublishedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ArticleCommentPublishedViewHolder(
        val lifecycleOwner: LifecycleOwner,
        val registerViewModel: RegisterViewModel,
        val view: View,
        val adapter: ArticleCommentAdapter
    ) : RecyclerView.ViewHolder(view) {

        fun showItemDeleted() {
            commentText?.text = ""
            buttonLike?.visibility = View.GONE
            buttonDeletedComment?.visibility = View.VISIBLE
            buttonDelete?.visibility = View.GONE
            buttonReply?.visibility = View.GONE
            (buttonReply?.parent as? ViewGroup)?.visibility = View.GONE
            replyToImageView?.visibility = View.GONE
            replyToUsernameTextView?.visibility = View.GONE
        }

        private val pendingAdapter = ArticleCommentAdapter(
            lifecycleOwner, registerViewModel, true, isArticleAuthor, onReplyClicked
        )

        private val serverAdapter = ArticleCommentAdapter(
            lifecycleOwner, registerViewModel, true, isArticleAuthor, onReplyClicked
        )
        var authorCommentImageView: ImageView? = null
        var authorBadgeCardView: CardView? = null
        var commentTimestamp: TimeSinceTextView? = null
        var commentText: TextView? = null
        var authorTextView: TextView? = null
        var buttonLike: MaterialButton? = null
        var buttonShowReplies: MaterialButton? = null
        var buttonDeletedComment: Button? = null
        var buttonReply: MaterialButton? = null
        var buttonDelete: MaterialButton? = null
        var repliesLinearLayout: RelativeLayout? = null
        var repliesRecyclerView: RecyclerView? = null
        var avatarImageCardView: CardView? = null
        var commentReplyRelativeLayout: RelativeLayout? = null
        var commentEditTextView: EditText? = null
        var authorCommentLinearLayout: LinearLayout? = null
        var commentLinearLayout: LinearLayout? = null
        var replyAuthorBadgeImageView: ImageView? = null
        var replyAuthorTextView: TextView? = null
        var showMoreButton: MaterialButton? = null
        var submitReplyButton: MaterialButton? = null
        var replyLinearLayout: LinearLayout? = null
        var commentReplyRecyclerView: RecyclerView? = null
        var replyToImageView: ImageView? = null
        var replyToUsernameTextView: TextView? = null
        var writeReplyToUsername: TextView? = null
        var writeReplyToUserImageView: ImageView? = null
        var cancelReplyButton: Button? = null

        init {
            authorCommentImageView = view.findViewById(R.id.avatar_iv)
            authorBadgeCardView = view.findViewById(R.id.author_badge_cv)
            commentTimestamp = view.findViewById(R.id.time_passed_tv)
            commentText = view.findViewById(R.id.comment_tv)
            authorTextView = view.findViewById(R.id.username_post_tv)
            buttonLike = view.findViewById(R.id.btn_like)
            buttonShowReplies = view.findViewById(R.id.btn_more)
            buttonDeletedComment = view.findViewById(R.id.btn_comment_deleted)
            buttonReply = view.findViewById(R.id.btn_reply)
            buttonDelete = view.findViewById(R.id.btn_delete)
            repliesLinearLayout = view.findViewById(R.id.ll_replies)
            repliesRecyclerView = view.findViewById(R.id.rv_comment_replies)
            avatarImageCardView = view.findViewById(R.id.avatar_crop_cv)
            commentReplyRelativeLayout = view.findViewById(R.id.comment_reply_rl)
            commentEditTextView = view.findViewById(R.id.comment_et)
            commentLinearLayout = view.findViewById(R.id.user_comment_info_lv)
            authorCommentLinearLayout = view.findViewById(R.id.author_comment_info_lv)
            replyAuthorBadgeImageView = view.findViewById(R.id.author_badge_iv)
            replyAuthorTextView = view.findViewById(R.id.username_author_tv)
            showMoreButton = view.findViewById(R.id.show_more)
            submitReplyButton = view.findViewById(R.id.ok_btn)
            replyLinearLayout = view.findViewById(R.id.ll_reply)
            commentReplyRecyclerView = view.findViewById(R.id.rv_comment_reply)
            replyToImageView = view.findViewById(R.id.reply_to_iv)
            replyToUsernameTextView = view.findViewById(R.id.reply_username_post_tv)
            writeReplyToUserImageView = view.findViewById(R.id.write_reply_to_iv)
            writeReplyToUsername = view.findViewById(R.id.write_reply_username_post_tv)
            cancelReplyButton = view.findViewById(R.id.cancel_button)

            commentReplyRecyclerView?.adapter = pendingAdapter
            repliesRecyclerView?.adapter = serverAdapter
        }

        fun bind(item: ArticleCommentPublished?) {
            val itemNotNull = item ?: return
            val loggedInUser = Constants.loggedInUser
            commentText?.text = itemNotNull.comment

            var expandedShowMore = false

            val commentLength = itemNotNull.comment.length
            val commentLines = itemNotNull.comment.lines()

            if (commentLines.size > 5 || commentLength > 250) {
                commentText?.maxLines = 5
                showMoreButton?.visibility = View.VISIBLE
            } else {
                showMoreButton?.visibility = View.GONE
            }

            showMoreButton?.setOnClickListener {
                expandedShowMore = !expandedShowMore
                if (expandedShowMore) {
                    commentText?.maxLines = Int.MAX_VALUE
                    showMoreButton?.text = "Show less"
                    showMoreButton?.icon = ContextCompat.getDrawable(view.context, R.drawable.baseline_expand_less_24)
                } else {
                    commentText?.maxLines = 5
                    showMoreButton?.text = "Show more"
                    showMoreButton?.icon = ContextCompat.getDrawable(view.context, R.drawable.baseline_expand_more_24)
                }
            }

            val localDateTime = DateTime(itemNotNull.timestamp, DateTimeZone.UTC).withZone(DateTimeZone.getDefault())
            commentTimestamp?.timestamp = localDateTime
            authorTextView?.text = itemNotNull.authorName
            authorTextView?.setOnClickListener {
                StoryUtil.showAuthorProfilePage(view.context, itemNotNull.authorName)
            }
            avatarImageCardView?.setOnClickListener {
                StoryUtil.showAuthorProfilePage(view.context, itemNotNull.authorName)
            }

            val replyToAuthor = itemNotNull.reply_to_username
            if (replyToAuthor != null) {
                replyToImageView?.visibility = View.VISIBLE
                replyToUsernameTextView?.text = itemNotNull.reply_to_username
                replyToUsernameTextView?.visibility = View.VISIBLE
                replyToUsernameTextView?.setOnClickListener {
                    StoryUtil.showAuthorProfilePage(view.context, replyToAuthor)
                }
            }

            var authorBadgeMetadata = itemNotNull.authorBadge

            cancelReplyButton?.setOnClickListener {
                commentReplyRelativeLayout?.visibility = View.GONE
                commentEditTextView?.text?.clear()
                commentEditTextView?.clearFocus()
                commentEditTextView?.let { editView ->
                    val imm = view.context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(editView.windowToken, 0)
                }
            }

            if (loggedInUser == null) {
                buttonDelete?.visibility = View.GONE
            }

            buttonDelete?.setOnClickListener {
                if (loggedInUser == null) return@setOnClickListener
                val li = LayoutInflater.from(view.context)
                val dialogView = li.inflate(R.layout.question_dialog, null)
                val errorTextTv = dialogView.findViewById<TextView>(R.id.error_text)
                val infoTextTv = dialogView.findViewById<TextView>(R.id.text2)
                val closeBtn = dialogView.findViewById<ImageView>(R.id.close_btn)
                val alertDialog = AlertDialog.Builder(view.context).setView(dialogView).create()

                infoTextTv.text = itemNotNull.comment.subSequence(0, 100.coerceAtMost(itemNotNull.comment.length))
                infoTextTv.setTypeface(null, Typeface.ITALIC)
                if (loggedInUser.username == itemNotNull.authorName) {
                    errorTextTv.text = "Delete your comment?"
                } else {
                    errorTextTv.text = if (itemNotNull.hidden) "Unhide comment?" else "Hide comment?"
                }
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                dialogView.findViewById<Button>(R.id.ok_btn).setOnClickListener {
                    if (loggedInUser.username == itemNotNull.authorName) {
                        itemNotNull.deleted = true
                        buttonDelete?.isEnabled = false
                        showItemDeleted()
                    } else {
                        itemNotNull.hidden = !itemNotNull.hidden
                        buttonDelete?.icon = if (itemNotNull.hidden)
                            ContextCompat.getDrawable(view.context, R.drawable.baseline_visibility_24)
                        else ContextCompat.getDrawable(view.context, R.drawable.baseline_visibility_off_24)
                        buttonDelete?.text = if (itemNotNull.hidden) "Unhide" else "Hide"
                    }
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
                        val storyDatabase = Constants.getStoryDatabase(view.context)
                        storyDatabase.storyPendingUploadDao().update(
                            StoryPendingUpload(
                                UUID.randomUUID(), Config.getStandardTimeUTCString(),
                                itemNotNull.commentUUID, PendingUploadType.COMMENT_HIDE_DELETE,
                                itemNotNull.slugTitle, UUID.fromString(loggedInUser.user_uuid)
                            )
                        )
                        ViewModelUtil.startResumeUploads(this, view.context.applicationContext)
                    }
                    alertDialog.dismiss()
                }
                dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener { alertDialog.dismiss() }
                closeBtn.setOnClickListener { alertDialog.dismiss() }
                alertDialog.show()
            }

            if (loggedInUser?.username == itemNotNull.authorName) {
                buttonLike?.background = null
                buttonLike?.isEnabled = false
                authorBadgeMetadata = loggedInUser.profileIcon
                buttonLike?.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_baseline_favorite_24)
            } else {
                if (isArticleAuthor) {
                    if (itemNotNull.hidden) {
                        buttonDelete?.icon = ContextCompat.getDrawable(view.context, R.drawable.baseline_visibility_24)
                        buttonDelete?.text = "Unhide"
                    } else {
                        buttonDelete?.icon = ContextCompat.getDrawable(view.context, R.drawable.baseline_visibility_off_24)
                        buttonDelete?.text = "Hide"
                    }
                } else {
                    buttonDelete?.visibility = View.GONE
                }
                if (itemNotNull.liked) {
                    buttonLike?.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_baseline_favorite_24)
                }
                buttonLike?.setOnClickListener {
                    if (loggedInUser == null) {
                        Snackbar.make(view, "Login or create an account to like a comment.", Snackbar.LENGTH_LONG)
                            .setAction("OK") {}
                            .setActionTextColor(view.context.getColor(android.R.color.holo_red_light))
                            .show()
                        return@setOnClickListener
                    }
                    itemNotNull.liked = !itemNotNull.liked
                    if (itemNotNull.liked) {
                        itemNotNull.hearts = itemNotNull.hearts + 1u
                        buttonLike?.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_baseline_favorite_24)
                        buttonLike?.iconTint = ColorStateList.valueOf(view.context.getColor(R.color.heart))
                    } else {
                        itemNotNull.hearts = itemNotNull.hearts - 1u
                        buttonLike?.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_baseline_favorite_border_24)
                        buttonLike?.iconTint = ColorStateList.valueOf(view.context.getColor(R.color.black))
                    }
                    buttonLike?.text = StoryUtil.likesToStringNumber(itemNotNull.hearts.toInt())
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
                        val storyDatabase = Constants.getStoryDatabase(view.context)
                        storyDatabase.storyPendingUploadDao().update(
                            StoryPendingUpload(
                                UUID.randomUUID(), Config.getStandardTimeUTCString(),
                                itemNotNull.commentUUID,
                                if (itemNotNull.liked) PendingUploadType.COMMENT_LIKE else PendingUploadType.COMMENT_REMOVE_LIKE,
                                itemNotNull.slugTitle, UUID.fromString(loggedInUser.user_uuid)
                            )
                        )
                        ViewModelUtil.startResumeUploads(this, view.context.applicationContext)
                    }
                }
            }

            if (authorBadgeMetadata != null && loggedInUser != null) {
                val localImage = ImageUtil.getLocalImageBitmapFromMetaData(view.context, false, authorBadgeMetadata, false)
                if (localImage != null) {
                    authorCommentImageView?.setImageBitmap(localImage)
                } else {
                    val imgViewNotNull = authorCommentImageView
                    if (imgViewNotNull != null)
                        ImageUtil.loadMinBitmapImageViewRemote(authorBadgeMetadata, view.context, imgViewNotNull, loggedInUser)
                }
            }

            buttonLike?.text = itemNotNull.hearts.toString()

            // ── Locally submitted replies (pending upload) ──
            // These are stored in registerViewModel.userCommentReply and shown
            // in the separate rv_comment_reply / ll_reply container so they don't
            // interfere with server-loaded replies in rv_comment_replies / ll_replies
            val replyCommentEntries = registerViewModel.userCommentReply.value?.get(itemNotNull.slugTitle)
            val pendingRepliesForThisComment =
                replyCommentEntries
                    ?.filter { it.replyToCommentUUID == itemNotNull.commentUUID }
                    ?.toList()
                    ?: emptyList()

            if (pendingRepliesForThisComment.isNotEmpty()) {
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    pendingAdapter.submitData(PagingData.from(pendingRepliesForThisComment))
                }
                replyLinearLayout?.visibility = View.VISIBLE
            }

            // ── Server-loaded replies (pre-fetched or fetched on demand) ──
            // These live in rv_comment_replies / ll_replies
            if (itemNotNull.replies.isNotEmpty()) {
                buttonShowReplies?.visibility = View.GONE
                repliesLinearLayout?.visibility = View.VISIBLE
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    serverAdapter.submitData(PagingData.from(itemNotNull.replies))
                    view.findViewById<ProgressBar>(R.id.progress_loader)?.visibility = View.GONE
                }
            } else if (itemNotNull.numReplies != 0u && !itemNotNull.deleted) {
                buttonShowReplies?.visibility = View.VISIBLE
                buttonShowReplies?.text = "Show ${itemNotNull.numReplies} replies"
                buttonShowReplies?.setOnClickListener {
                    buttonShowReplies?.visibility = View.GONE
                    repliesLinearLayout?.visibility = View.VISIBLE
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        registerViewModel.getArticleCommentsReplies(
                            loggedInUser, itemNotNull.commentUUID,
                            itemNotNull.slugTitle, view.context.applicationContext
                        ).collectLatest { pagingData ->
                            serverAdapter.submitData(pagingData)
                        }
                    }
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        serverAdapter.loadStateFlow.collectLatest { loadStates ->
                            when (loadStates.refresh) {
                                is LoadState.Error -> withContext(Dispatchers.Main) {
                                    view.findViewById<LinearLayout>(R.id.comments_error_ll)?.visibility = View.VISIBLE
                                    view.findViewById<ProgressBar>(R.id.progress_loader)?.visibility = View.GONE
                                }
                                is LoadState.NotLoading -> withContext(Dispatchers.Main) {
                                    view.findViewById<ProgressBar>(R.id.progress_loader)?.visibility = View.GONE
                                }
                                else -> {}
                            }
                        }
                    }
                }
            } else {
                buttonShowReplies?.visibility = View.GONE
            }

            if (itemNotNull.deleted) {
                showItemDeleted()
            } else {
                buttonReply?.setOnClickListener {
                    if (loggedInUser == null) {
                        Snackbar.make(view, "Login or create an account to reply to a comment.", Snackbar.LENGTH_LONG)
                            .setAction("OK") {}
                            .setActionTextColor(view.context.getColor(android.R.color.holo_red_light))
                            .show()
                        return@setOnClickListener
                    }
                    if (onReplyClicked != null) {
                        onReplyClicked.invoke(itemNotNull, view)
                    } else {
                        setupLocalReplyUI(itemNotNull, loggedInUser)
                    }
                }

                submitReplyButton?.setOnClickListener {
                    if (loggedInUser == null) return@setOnClickListener
                    val replyText = commentEditTextView?.text?.trim()?.toString() ?: ""
                    if (replyText.isNotEmpty()) {
                        submitReply(itemNotNull, replyText, loggedInUser)
                    }
                }
            }

            if (isReplyAdapter) {
                val dp35 = ImageUtil.convertDpToPixel(35, view.context)
                avatarImageCardView?.layoutParams?.apply { width = dp35; height = dp35 }
                    ?.let { avatarImageCardView?.layoutParams = it }
                authorBadgeCardView?.layoutParams?.apply { width = dp35; height = dp35 }
                    ?.let { authorBadgeCardView?.layoutParams = it }
                buttonShowReplies?.visibility = View.GONE
            }
        }

        private fun setupLocalReplyUI(item: ArticleCommentPublished, loggedInUser: AccountInfoFull) {
            commentReplyRelativeLayout?.visibility = View.VISIBLE
            commentEditTextView?.hint = "Reply to ${item.authorName}"
            commentEditTextView?.requestFocus()
            replyAuthorTextView?.text = loggedInUser.username

            if (loggedInUser.profileIcon != null) {
                val imageView = replyAuthorBadgeImageView ?: return
                val localImage = ImageUtil.getLocalImageBitmapFromMetaData(view.context, false, loggedInUser.profileIcon, false)
                if (localImage != null) {
                    imageView.setImageBitmap(localImage)
                } else {
                    ImageUtil.loadMinBitmapImageViewRemote(loggedInUser.profileIcon, view.context, imageView, loggedInUser)
                }
            }

            if (isReplyAdapter) {
                writeReplyToUserImageView?.visibility = View.VISIBLE
                writeReplyToUsername?.visibility = View.VISIBLE
                writeReplyToUsername?.text = item.authorName
            }

            commentEditTextView?.setText("")
            commentEditTextView?.clearFocus()

            commentEditTextView?.addTextChangedListener(
                MultilineRemover.getMultilineTextRemover(1) { text ->
                    submitReplyButton?.isEnabled = text.isNotEmpty()
                    submitReplyButton?.backgroundTintList = ColorStateList.valueOf(
                        view.context.getColor(
                            if (text.isNotEmpty()) R.color.textSecondary else R.color.gray
                        )
                    )
                }
            )
        }

        private fun submitReply(item: ArticleCommentPublished, replyText: String, loggedInUser: AccountInfoFull) {
            Config.firebaseAnalytics.logEvent("story_comment_reply_publish_started", null)
            commentReplyRelativeLayout?.visibility = View.GONE
            commentEditTextView?.text?.clear()

            val commentUUID = UUID.randomUUID().toString()
            val newReply = ArticleCommentPublished(
                slugTitle = item.slugTitle,
                authorName = loggedInUser.username,
                comment = replyText,
                commentUUID = commentUUID,
                replyToCommentUIID = item.commentUUID,
                hearts = 0u,
                timestamp = Config.getStandardTimeUTCString(),
                langCode = "",
                numReplies = 0u,
                deleted = false,
                hidden = false,
                authorBadge = loggedInUser.profileIcon,
                liked = false,
                replies = mutableListOf(),
                reply_to_username = item.authorName
            )

            // ── Add to the in-memory pending map so it survives rebind ──
            val slugEntries = registerViewModel.userCommentReply.value
                ?: mutableMapOf<String, MutableList<ArticleCommentPublished>>().also {
                    registerViewModel.userCommentReply.value = it
                }
            val commentEntries = slugEntries.getOrPut(item.slugTitle) { mutableListOf() }
            commentEntries.add(newReply)

            // ── Show immediately in the pending-replies container (ll_reply / rv_comment_reply) ──
            // This is separate from the server-loaded replies container (ll_replies / rv_comment_replies)
            // so the two lists never interfere with each other
            replyLinearLayout?.visibility = View.VISIBLE

            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                pendingAdapter.submitData(
                    PagingData.from(commentEntries.filter { it.replyToCommentUUID == item.commentUUID })
                )
            }

            // ── Persist to local DB and queue for background upload ──
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
                val storyDatabase = Constants.getStoryDatabase(view.context)
                storyDatabase.storyCommentDao().update(
                    StoryComment(
                        item.slugTitle, loggedInUser.username, replyText,
                        commentUUID, item.commentUUID, Config.getStandardTimeUTCString()
                    )
                )
                storyDatabase.storyPendingUploadDao().update(
                    StoryPendingUpload(
                        UUID.randomUUID(), Config.getStandardTimeUTCString(),
                        commentUUID, PendingUploadType.COMMENT, item.slugTitle,
                        UUID.fromString(loggedInUser.user_uuid)
                    )
                )
                ViewModelUtil.startResumeUploads(this, view.context.applicationContext)
            }
        }
    }

    companion object {
        val differ = object : DiffUtil.ItemCallback<ArticleCommentPublished>() {
            override fun areItemsTheSame(oldItem: ArticleCommentPublished, newItem: ArticleCommentPublished): Boolean {
                return oldItem.commentUUID == newItem.commentUUID
            }

            override fun areContentsTheSame(oldItem: ArticleCommentPublished, newItem: ArticleCommentPublished): Boolean {
                return oldItem.comment == newItem.comment &&
                        oldItem.liked == newItem.liked &&
                        oldItem.hearts == newItem.hearts &&
                        oldItem.hidden == newItem.hidden &&
                        oldItem.deleted == newItem.deleted
            }
        }
    }
}