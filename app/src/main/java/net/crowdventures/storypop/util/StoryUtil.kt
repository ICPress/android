package net.crowdventures.storypop.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Parcelable
import android.text.Layout
import android.text.ParcelableSpan
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BulletSpan
import android.text.style.QuoteSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.ArticleContentActivity
import net.crowdventures.storypop.ArticleContentEditActivity
import net.crowdventures.storypop.ArticleListActivity
import net.crowdventures.storypop.BuildConfig
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.ImageClickSpan
import net.crowdventures.storypop.ProfileActivity
import net.crowdventures.storypop.R
import net.crowdventures.storypop.TextStyle
import net.crowdventures.storypop.TextStyleManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.Notification
import net.crowdventures.storypop.dto.NotificationType
import net.crowdventures.storypop.dto.RewardClaimed
import net.crowdventures.storypop.dto.Transaction
import net.crowdventures.storypop.dto.TransactionDescriptionType
import net.crowdventures.storypop.libs.RoundedBackgroundSpan
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.endpoints.RewardEndpoint
import net.crowdventures.storypop.util.endpoints.UserEndpoint
import net.crowdventures.storypop.viewmodels.EnabledStyle
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.LinkType
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.SpanInfo
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import net.crowdventures.storypop.viewmodels.StorySavedModel
import net.crowdventures.storypop.viewmodels.StylingInfo
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Days
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StoryUtil {
    companion object {
        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
            throwable.printStackTrace()
        }
        fun restoreTitleHighlightColorFromStylingInfo(
            stylingInfo: StylingInfo,
            spanStringTitle: SpannableStringBuilder
        ): RoundedBackgroundSpan? {
            if (stylingInfo.titleHighlightColor != 0) {
                val newSpan = RoundedBackgroundSpan(stylingInfo.titleHighlightColor)
                spanStringTitle.setSpan(
                    newSpan,
                    0,
                    spanStringTitle.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                return newSpan
            }
            return null
        }

        fun restoreTitleBackgroundImageFromStylingInfo(
            context: Context,
            stylingInfo: StylingInfo,
            titleImageView: ImageView,
            loggedInUser: AccountInfoFull
        ) {
            if (stylingInfo.titleBackgroundImage != null) {
                val backgroundBitmap = ImageUtil.getLocalImageBitmapFromMetaData(
                    context,
                    false,
                    stylingInfo.titleBackgroundImage ?: return,
                    true
                )
                if (backgroundBitmap != null) {
                    titleImageView.setImageDrawable(backgroundBitmap.toDrawable(context.resources))
                    val imageInfoMetadata = Gson().fromJson(stylingInfo.titleBackgroundImage, ImageInfoMetadata::class.java)
                    val layoutParams = RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ImageUtil.convertDpToPixel(imageInfoMetadata.minHeight?:imageInfoMetadata.height,context)
                    )
                    titleImageView.layoutParams = layoutParams
                } else { //try loading image from coil/remote
                    restoreTitleBackgroundImageFromStylingInfoFromRemote(
                        context,
                        stylingInfo,
                        titleImageView,
                        loggedInUser
                    )
                }
            }
        }

        fun getNumberOfFollowers(
            registerViewModel: ViewModel,
            authorUsername: String,
            authorFollowersTextView: TextView,
            context: Context
        ) {
            registerViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                val restAdapter = Retrofit.Builder()
                    .baseUrl(Config.APP_ENDPOINT)
                    .client(RetrofitUtil.generateSecureOkHttpClient(context))
                    .build()

                val service: UserEndpoint = restAdapter.create(UserEndpoint::class.java)
                try {
                    val response = service.getFollowers(authorUsername)
                    if (!response.isSuccessful) {
                        Log.e(
                            Config.logTag,
                            "Response from service.getFollowers is not successful, code:" + response.code()
                        )
                    } else {
                        withContext(Dispatchers.Main) {
                            val followers = response.body()
                            authorFollowersTextView.text =
                                if (followers != null) "${followers.string()} Followers" else context.getString(
                                    R.string.new_user
                                )
                            authorFollowersTextView.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        Config.logTag,
                        "Could not load service.getFollowers, exception:" + e.message,
                        e
                    )
                }
            }
        }

        fun restoreTitleBackgroundImageFromStylingInfoFromRemote(
            context: Context,
            stylingInfo: StylingInfo,
            titleImageView: ImageView,
            loggedInUser: AccountInfoFull
        ) {
            // Create a context that explicitly uses an AppCompat theme
            val appCompatContext = androidx.appcompat.view.ContextThemeWrapper(
                context,
                androidx.appcompat.R.style.Theme_AppCompat_NoActionBar
            )

            val metadataString = stylingInfo.titleBackgroundImage ?: return
            val imageLoader = Config.getOrSetImageLoader(context)
            val imageInfoMetadata = Gson().fromJson(metadataString, ImageInfoMetadata::class.java)

            val imageName = imageInfoMetadata.name
            val imageRequestPath = loggedInUser.cdnLargeRequestPath  + imageName//show always large version
            var previewRequestPath:String? = null

            if (imageInfoMetadata?.minWidth != null){
                previewRequestPath = loggedInUser.cdnSmallRequestPath + Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name
                //show preview of compressed version
                val imageRequest =  ImageUtil.createImageRequest(context, imageRequestPath)
                    .target(titleImageView)
                    .listener(
                        ImageRequestListenerRetry(
                            context,
                            titleImageView,
                            imageLoader,
                            imageRequestPath
                        )
                    ).placeholderMemoryCacheKey(previewRequestPath)
                    .crossfade(true).build()
                imageLoader.enqueue(imageRequest)
            }else{
                val imageRequest = ImageUtil.createImageRequestWithRetryOnce(context,
                    titleImageView, imageLoader,imageRequestPath)
                imageLoader.enqueue(imageRequest)
            }

            //add image preview
            if (imageInfoMetadata != null) {
                val layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ImageUtil.convertDpToPixel(imageInfoMetadata.minHeight?:imageInfoMetadata.height,context)
                ) //LayoutParams.MATCH_PARENT
                titleImageView.layoutParams = layoutParams
            }
            titleImageView.setOnClickListener {
                StfalconImageViewer.Builder<String>(
                    appCompatContext, //required for StfalconImageViewer as it does not support material theme
                    listOf(metadataString),
                    object : ImageLoader<String> {
                        override fun loadImage(
                            imageView: ImageView?,
                            image: String?
                        ) {
                            if (imageView == null || image == null) return
                            val enlargeReq = ImageUtil.createImageRequest(context, imageRequestPath)
                                .target(imageView)
                                .listener(
                                    ImageRequestListenerRetry(
                                        context,
                                        imageView,
                                        imageLoader,
                                        imageRequestPath
                                    )
                                ).placeholderMemoryCacheKey(previewRequestPath?:"")
                                .crossfade(true).build()
                            imageLoader.enqueue(enlargeReq)
                        }
                    }).show()
            }
        }

        fun restoreTitleBackgroundColorFromStylingInfo(
            context: Context,
            stylingInfo: StylingInfo,
            titleImageView: ImageView
        ) {
            if (stylingInfo.titleBackgroundColor != 0) titleImageView.setColorFilter(
                stylingInfo.titleBackgroundColor,
                PorterDuff.Mode.SRC
            )
        }

        fun showShowWarningSnackbar(
            rootView: View,
            nestedScrollView: NestedScrollView?,
            targetView: View,
            context: Context,
            message: String,
            anchorView: View?
        ) {
            nestedScrollView?.smoothScrollTo(0, 0)
            targetView.requestFocus()
            val orgBackground = targetView.background
            val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .setActionTextColor(context.getColor(android.R.color.holo_red_light))
                .addCallback(object :
                    BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        //context.window.navigationBarColor = orgColor
                        targetView.background = orgBackground
                    }

                    override fun onShown(transientBottomBar: Snackbar?) {
                        targetView.setBackgroundResource(R.drawable.button_missing_input)
                        //context.window.navigationBarColor = Color.parseColor("#000000")
                    }
                })
            if (anchorView != null) snackbar.anchorView = anchorView
            snackbar.show()
        }

        fun startEditActivity(savedModel: StorySavedModel, context: Context) {
            val intent = Intent(context, ArticleContentEditActivity::class.java)
            intent.setExtrasClassLoader(StorySavedModel::class.java.classLoader)
            intent.putExtra(ArticleContentEditActivity.savedStoryModelKey, savedModel as Parcelable)
            context.startActivity(intent)
        }

        fun getStartEditActivityIntent(savedModel: StorySavedModel, context: Context): Intent {
            val intent = Intent(context, ArticleContentEditActivity::class.java)
            intent.setExtrasClassLoader(StorySavedModel::class.java.classLoader)
            intent.putExtra(ArticleContentEditActivity.savedStoryModelKey, savedModel as Parcelable)
            return intent
        }

        fun likesToStringNumber(likes: Int): String {
            return if (likes < 1000) likes.toString() else (likes / 1000).toString() + "K"
        }

        fun startListSearchActivity(context: Context, tag: String) {
            val intent = Intent(context, ArticleListActivity::class.java)
            intent.flags += FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(ArticleListActivity.HASHTAG_FILTER_INTENT_NAME, tag)
            context.startActivity(intent)
        }

        fun showAuthorProfilePage(context: Context, authorUsername: String) {
            val intent = Intent(context, ProfileActivity::class.java)
            intent.flags += FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(ProfileActivity.USERNAME_INTENT_NAME, authorUsername)
            context.startActivity(intent)
        }

        fun startContentActivity(
            managedActivityResultLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
            storyPublishedModel: StoryPublishedModel, context: Context, hasNewLike: Boolean
        ) {
            val intent = Intent(context, ArticleContentActivity::class.java)
            intent.setExtrasClassLoader(StoryPublishedModel::class.java.classLoader)
            intent.putExtra(Constants.PUBLISHED_STORY_EXTRA, storyPublishedModel as Parcelable)
            intent.putExtra(Constants.PUBLISHED_STORY_HAS_NEW_LIKE, hasNewLike)
            managedActivityResultLauncher.launch(intent)
        }

        fun startContentActivity(storyPublishedModel: StoryPublishedModel, context: Context) {
            val intent = Intent(context, ArticleContentActivity::class.java)
            intent.setExtrasClassLoader(StoryPublishedModel::class.java.classLoader)
            intent.putExtra(Constants.PUBLISHED_STORY_EXTRA, storyPublishedModel as Parcelable)
            context.startActivity(intent)
        }

        fun startPreviewActivity(
            savedModel: StorySavedModel,
            context: Context,
            isPublished: Boolean = false
        ) {
            val intent = Intent(context, ArticleContentActivity::class.java)
            val stylingInfo = savedModel.stylingInfo
            intent.putExtra(Constants.EDIT_CONTENT_TEXT_STYLES, stylingInfo as Parcelable)
            intent.putExtra(Constants.EDIT_MODE_ENABLED_EXTRA, true)
            intent.putExtra(Constants.EDIT_CONTENT_TITLE, savedModel.storyTitle)
            intent.putExtra(Constants.EDIT_CONTENT_TEXT, savedModel.contentText)
            intent.putExtra(Constants.STORY_PUBLISHED, isPublished)
            intent.putStringArrayListExtra(
                Constants.EDIT_CONTENT_TAGS,
                arrayListOf(*savedModel.tags)
            )
            intent.putExtra(
                Constants.EDIT_CONTENT_LOCATION,
                savedModel.location
            )
            intent.putStringArrayListExtra(Constants.STORY_PUBLIC_SOURCES,
                arrayListOf(*savedModel.publicSources)
            )
            intent.putParcelableArrayListExtra(Constants.STORY_PRIVATE_SOURCES, arrayListOf(*savedModel.privateSources))

            intent.putExtra(Constants.STORY_MAP, savedModel.storyMap as Parcelable?)
            context.startActivity(intent)
        }

        fun showAddLinkDialog(contentEditText: EditText, textStyleManager: TextStyleManager) {
            val li: LayoutInflater = LayoutInflater.from(contentEditText.context)
            val view = li.inflate(R.layout.add_link_dialog, null)
            val linkTitleEditText = view.findViewById<EditText>(R.id.content_title_et)
            val linkContentEditText = view.findViewById<EditText>(R.id.content_et)
            val alertDialog: AlertDialog = contentEditText.context.let {
                val builder = AlertDialog.Builder(it)
                builder.setView(view)
                builder.create()
            }
            if (contentEditText.hasSelection())
                linkTitleEditText.setText(
                    contentEditText.text.subSequence(
                        contentEditText.selectionStart,
                        contentEditText.selectionEnd
                    )
                )
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val okBtn = view.findViewById<Button>(R.id.ok_btn)
            okBtn.setOnClickListener { _ ->
                if (!linkTitleEditText.text.isNullOrEmpty()) {
                    if (!linkContentEditText.text.startsWith(
                            "http://",
                            true
                        ) && !linkContentEditText.text.startsWith("https://", true)
                    )
                        linkContentEditText.text.insert(0, "http://")
                    if (Patterns.WEB_URL.matcher(linkContentEditText.text).matches()) {
                        alertDialog.dismiss()
                        textStyleManager.addLinkStyle(
                            URLSpan(linkContentEditText.text.toString()),
                            linkTitleEditText.text.toString(),
                            LinkType.USER_LINK
                        )
                    } else {
                        StoryUtil.showShowWarningSnackbar(
                            view,
                            null,
                            linkContentEditText,
                            contentEditText.context,
                            "Enter a valid web link",
                            null
                        )
                    }
                } else {
                    StoryUtil.showShowWarningSnackbar(
                        view,
                        null,
                        linkTitleEditText,
                        contentEditText.context,
                        "Enter a description for the link",
                        null
                    )
                }
            }
            val closeBtn = view.findViewById<ImageView>(R.id.close_btn)
            closeBtn.setOnClickListener { _ -> alertDialog.dismiss() }
            val cancelBtn = view.findViewById<ImageView>(R.id.cancel_btn)
            cancelBtn.setOnClickListener { _ -> alertDialog.dismiss() }
            alertDialog.show()
        }

        fun getDateTimeFormatter(dateTimeToFormat: DateTime): DateTimeFormatter {
            val days = Days.daysBetween(dateTimeToFormat, DateTime.now(DateTimeZone.UTC)).days
            return if (days >= 6) {
                if (dateTimeToFormat.year().get() == DateTime.now(DateTimeZone.UTC).year().get()
                ) {
                    Config.sameYearDtf
                } else Config.dtf
            } else {
                if (days == 0 && DateTime.now(DateTimeZone.UTC).dayOfWeek == dateTimeToFormat.dayOfWeek) Config.sameDayDtf else Config.sameWeekDtf //dateTimeToFormat.dayOfWeek() == DateTime.now(DateTimeZone.UTC).dayOfWeek()
            }
        }

        fun noTimeFormatDate(dateTimeToFormat: DateTime, context: Context): String {
            val dtf: DateTimeFormatter =
                DateTimeFormat.forPattern("yyyy-MM-dd")
            val sameYearDtf: DateTimeFormatter =
                DateTimeFormat.forPattern("dd MMMM")
            val sameWeekDtf: DateTimeFormatter =
                DateTimeFormat.forPattern("EEEE")
            val daysBetweenDate =
                Days.daysBetween(dateTimeToFormat, DateTime.now(DateTimeZone.UTC)).days
            if (daysBetweenDate >= 6) {
                if (dateTimeToFormat.year() == DateTime.now(DateTimeZone.UTC).year()
                ) {
                    return DateTime(
                        dateTimeToFormat,
                        DateTimeZone.UTC
                    ).withZone(DateTimeZone.getDefault()).toString(sameYearDtf)
                } else return DateTime(
                    dateTimeToFormat,
                    DateTimeZone.UTC
                ).withZone(DateTimeZone.getDefault()).toString(dtf)
            } else {
                if (daysBetweenDate == 0 && DateTime.now(DateTimeZone.UTC).dayOfWeek == dateTimeToFormat.dayOfWeek) return context.getString(
                    R.string.today
                ) //&& dateTimeToFormat.dayOfWeek() == DateTime.now(DateTimeZone.UTC).dayOfWeek()
                else return DateTime(
                    dateTimeToFormat,
                    DateTimeZone.UTC
                ).withZone(DateTimeZone.getDefault()).toString(sameWeekDtf)
            }
            return context.getString(R.string.today)
        }

        fun getLocalTransactionDescription(context: Context, transaction: Transaction): String {
            when (transaction.descriptionType) {
                TransactionDescriptionType.LIKE_RECEIVED -> {
                    var likes =
                        ViewModelUtil.parseLikesMultipleFromNotification(transaction.additionalData)
                    val likesNum = likes.toString().toIntOrNull()
                    if (likesNum != null) {
                        likes = likesToStringNumber(likesNum)
                    }
                    return "Likes X${likes} Reward"
                }
                TransactionDescriptionType.FIRST_LIKE_RECEIVED_REWARD -> return "Reward: First like received"
                TransactionDescriptionType.FIRST_LIKE_SENT_REWARD -> return "Reward: First like of an article"
                TransactionDescriptionType.FIRST_POST_REWARD -> return "Reward: First article published"
                TransactionDescriptionType.REWARD_CLAIMED -> return "Claimed reward"
                TransactionDescriptionType.SPECIAL_REWARD -> return "Special reward: ${transaction.additionalData}"
                else -> return "Unknown"
            }
        }

        fun getDescriptionFromNotificationTransactionDescriptionType(
            context: Context,
            notification: Notification
        ): String {
            if (notification.notificationType == NotificationType.INFORMATION) return notification.additionalData
                ?: ""
            else {
                return when (notification.transactionDescriptionType) {
                    TransactionDescriptionType.FIRST_POST_REWARD ->
                        "Your first published article has been recognised. StoryPoints has been credited to your account."

                    TransactionDescriptionType.FIRST_LIKE_SENT_REWARD ->
                        "Engaged contributors strengthen the platform. StoryPoints have been credited to your account for your first like."

                    TransactionDescriptionType.FIRST_FOLLOW ->
                        "Your article has attracted its first follower. StoryPoints have been credited to your account for this milestone."

                    TransactionDescriptionType.LIKE_RECEIVED -> {
                        val likes = ViewModelUtil.parseLikesMultipleFromNotification(notification.additionalData)
                        "\"${notification.storyTitle?.trim()}\" has reached $likes endorsements. StoryPoints has been credited to your account."
                    }

                    TransactionDescriptionType.FIRST_LIKE_RECEIVED_REWARD ->
                        "Your article has received its first endorsement from ${notification.additionalData}. StoryPoints has been credited to your account."

                    TransactionDescriptionType.SPECIAL_REWARD ->
                        "StoryPoints has been credited to your account for your contribution: ${notification.additionalData}"

                    else -> ""
                }
            }
        }

        fun restoreSpannableFromStylingInfo(
            context: Context, spans: List<SpanInfo>,
            spanString: SpannableStringBuilder,
            isPublished: Boolean, targetTextView: TextView,
            loggedInUser: AccountInfoFull
        ): Pair<MutableList<EnabledStyle>, MutableList<ImageClickSpan>> {
            val mappedEnabledStyle = mutableListOf<EnabledStyle>()
            val imageSpansAdded = arrayListOf<ImageClickSpan>()
            for (style in spans) {

                var spanObj: Any? = null
                when (style.style) {
                    TextStyle.NONE -> spanObj = Any()
                    TextStyle.BOLD -> spanObj = StyleSpan(Typeface.BOLD)
                    TextStyle.ITALIC -> spanObj = StyleSpan(Typeface.ITALIC)
                    TextStyle.UNDERLINE -> spanObj = UnderlineSpan()
                    TextStyle.TEXT_SIZE_LARGE -> spanObj = AbsoluteSizeSpan(Config.LARGE_FONT_SIZE,true)
                    TextStyle.TEXT_ALIGNMENT -> spanObj = AlignmentSpan.Standard(
                        Layout.Alignment.values()[style.additionalInfoFlag.toString().toInt()]
                    )
                    TextStyle.QUOTE -> spanObj =
                        QuoteSpan(ContextCompat.getColor(context, R.color.main))
                    TextStyle.BULLET_LIST ->
                        spanObj = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                            val typedArray = context.obtainStyledAttributes(intArrayOf(R.attr.editTextColor))
                            val textColor = typedArray.getColor(0,0)
                            typedArray.recycle()
                            BulletSpan(15,textColor,8)
                        }
                        else BulletSpan()

                    TextStyle.IMAGE -> {
                        if (style.additionalInfoFlag != null) {
                            val imageClickSpan = ImageClickSpan(
                                context, style,
                                null, isPublished, targetTextView, loggedInUser
                            )
                            imageSpansAdded.add(imageClickSpan)
                            spanObj = imageClickSpan
                        } else spanObj = null
                    }
                    TextStyle.REFER_LINK -> spanObj = URLSpan(style.additionalInfoFlag)

                }
                if (spanObj != null) {
                    if (style.style == TextStyle.IMAGE) {
                        spanString.setSpan(
                            spanObj,
                            (style.start - Config.IMAGE_START_SEPARATOR.length).coerceAtLeast(0), //20220314 before style.start
                            style.end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
//                        val clickableSpan = ImageClickableSpan(style,context)
//
//                        spanString.setSpan(
//                            clickableSpan,
//                            (style.start - Config.IMAGE_START_SEPARATOR.length).coerceAtLeast(0), //20220314 before style.start
//                            style.end,
//                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                        )
                        mappedEnabledStyle.add(EnabledStyle(style, false, null))
                    } else {
                        spanString.setSpan(
                            spanObj,
                            style.start,
                            style.end,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                        mappedEnabledStyle.add(
                            EnabledStyle(
                                style,
                                style.end == spanString.length, //continue spans that are at the end of the text
                                spanObj as ParcelableSpan
                            )
                        )
                    }
                }
            }
            return Pair(mappedEnabledStyle, imageSpansAdded)
        }
        fun shareArticle(slugTitle: String, context: Context, isLoggedInUserAuthor:Boolean = true) {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            val shareMessage = if (isLoggedInUserAuthor)
                "Read my article here\n\nhttps://icpress.org/s/${slugTitle}"
            else "Don´t miss this article \n\nhttps://icpress.org/s/${slugTitle}"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    "choose one"
                )
            )
        }

        fun extractTitleFromUrl(url: String): String {
            // Simple title extraction from URL
            return try {
                val domain = url.replace("https://", "").replace("http://", "").split("/").first()
                val path = url.split("/").lastOrNull() ?: ""
                if (path.isNotEmpty() && path != domain) {
                    path.replace("-", " ").replace("_", " ").split(".").first().capitalize()
                } else {
                    domain.replace("www.", "").split(".").first().capitalize()
                }
            } catch (e: Exception) {
                "Source"
            }
        }
    }
}