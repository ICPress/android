package net.crowdventures.storypop.composable

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.BuildConfig
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.ArticleCommentLikeReplyNotification
import net.crowdventures.storypop.dto.Notification
import net.crowdventures.storypop.dto.NotificationType
import net.crowdventures.storypop.dto.TransactionDescriptionType
import net.crowdventures.storypop.dto.UserMessagePublishedNotification
import net.crowdventures.storypop.dto.UsersFollowingInfo
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryComment
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.util.MessageUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.UUID

class NotificationsDashboard {
    companion object {
        @Composable
        fun ShowNotifications(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            storySavedViewModel: StorySavedViewModel,
            registerViewModel: RegisterViewModel,
            sharedPreferenceManager: SharedPreferenceManager,
            paddingValues: PaddingValues,
            bottomBarVisibility: MutableState<Boolean>,
            navHostController: NavHostController
        ) {
            var refreshed by remember { mutableStateOf(false) }
            val dtf: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM")
            val currentUser = Constants.loggedInUser ?: return
            var notificationsLoaded by remember { mutableStateOf(true) }

            // 1. Collect paging items
            val notificationItems: LazyPagingItems<Notification> =
                storySavedViewModel.notificationSource.collectAsLazyPagingItems()

            // 2. Observe the state change, now notifications
            val consumedState by registerViewModel.userNotificationsConsumed.observeAsState()

            LaunchedEffect(consumedState) {
                // When consumedState changes (e.g., becomes true), refresh the list
                if (consumedState == true) {
                    notificationItems.refresh()
                    // Reset the state if your logic requires it to prevent infinite refreshes
                    // registerViewModel.resetConsumedState()
                }
            }



            registerViewModel.userNotificationsConsumed.observe(lifecycleOwner) { x ->
                if (x != null) {
                    if (x == false && !notificationsLoaded && !refreshed) {
                        refreshed = true
                        Log.v(Config.logTag, "Request refresh notifications")
                    }
                    if (notificationsLoaded != x) {
                        Log.v(Config.logTag, "Reset notifications loaded")
                        notificationsLoaded = x
                    }
                }
            }

            val lazyListState = rememberLazyListState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
            ) {
                // Header - Left Aligned
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colors.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notifications",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colors.onSurface
                        )
                    }
                }

                Divider(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                val notificationItemsCached = if (notificationsLoaded && !refreshed) {
                    val notificationItemsCachedJson =
                        sharedPreferenceManager.getLatestTopNotificationsCached()
                    if (notificationItemsCachedJson != null) {
                        Gson().fromJson<List<Notification>>(
                            notificationItemsCachedJson,
                            object : TypeToken<List<Notification>>() {}.type
                        )
                    } else listOf()
                } else listOf()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    // Cached Items
                    items(
                        items = notificationItemsCached,
                        key = { "${it.notificationId}_${it.timestamp}" }
                    ) { item ->
                        NotificationItem(
                            item = item,
                            context = context,
                            dtf = dtf,
                            bottomBarVisibility = bottomBarVisibility,
                            lifecycleOwner = lifecycleOwner,
                            storySavedViewModel = storySavedViewModel,
                            loggedInUser = currentUser,
                            navHostController = navHostController
                        )
                    }

                    // Live Items
                    items(
                        count = notificationItems.itemCount,
                        key = { index -> "notification_${notificationItems[index]?.notificationId ?: index}" }
                    ) { index ->
                        val item = notificationItems[index]
                        if (item != null && !notificationItemsCached.any { it.notificationId == item.notificationId }) {
                            NotificationItem(
                                item = item,
                                context = context,
                                dtf = dtf,
                                bottomBarVisibility = bottomBarVisibility,
                                lifecycleOwner = lifecycleOwner,
                                storySavedViewModel = storySavedViewModel,
                                loggedInUser = currentUser,
                                navHostController = navHostController
                            )
                        }
                    }
                    // Loading State
                    val isRefreshing = notificationItems.loadState.refresh is LoadState.Loading
                    val isEmpty =
                        notificationItems.itemCount == 0 && notificationItemsCached.isEmpty()

                    if (isRefreshing && isEmpty) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colors.secondary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    } else if (!isRefreshing && isEmpty && notificationItems.loadState.append.endOfPaginationReached) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notifications yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "We'll notify you when something happens",
                                fontSize = 14.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                }
            }
        }

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun NotificationItem(
            item: Notification,
            context: Context,
            dtf: DateTimeFormatter,
            bottomBarVisibility: MutableState<Boolean>,
            lifecycleOwner: LifecycleOwner,
            storySavedViewModel: StorySavedViewModel,
            loggedInUser: AccountInfoFull,
            navHostController: NavHostController
        ) {
            val showMore = remember { mutableStateOf(false) }

            val usernameData = item.triggerAuthor ?: item.additionalData

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp,
                backgroundColor = MaterialTheme.colors.surface,
                onClick = {
                    when {
                        item.notificationType == null -> {
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
                                    )
                                )
                            } catch (e: ActivityNotFoundException) {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                                    )
                                )
                            }
                        }

                        item.notificationType == NotificationType.COMMENT_REPLY_RECEIVED ||
                                item.notificationType == NotificationType.COMMENT_RECEIVED -> {
                            showMore.value = !showMore.value
                        }

                        item.notificationType == NotificationType.MESSAGE_RECEIVED && item.triggerAuthor != null -> {
                            ViewModelUtil.goToMessages(
                                navHostController,
                                storySavedViewModel,
                                UsersFollowingInfo(item.triggerAuthor, item.profileIcon, listOf())
                            )
                        }

                        usernameData != null -> {
                            StoryUtil.showAuthorProfilePage(context, usernameData)
                        }
                    }
                }
            ) {
                NotificationContent(
                    item = item,
                    context = context,
                    dtf = dtf,
                    showMore = showMore,
                    bottomBarVisibility = bottomBarVisibility,
                    lifecycleOwner = lifecycleOwner,
                    storySavedViewModel = storySavedViewModel,
                    loggedInUser = loggedInUser,
                    navHostController = navHostController
                )
            }
        }

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun NotificationContent(
            item: Notification,
            context: Context,
            dtf: DateTimeFormatter,
            showMore: MutableState<Boolean>,
            bottomBarVisibility: MutableState<Boolean>,
            lifecycleOwner: LifecycleOwner,
            storySavedViewModel: StorySavedViewModel,
            loggedInUser: AccountInfoFull,
            navHostController: NavHostController
        ) {
            var loading by remember { mutableStateOf(false) }

            if (loading) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.secondary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            val triggerUserImageInfoMetadata = if (item.profileIcon != null) {
                Gson().fromJson(item.profileIcon, ImageInfoMetadata::class.java)
            } else null

            val focusManager = LocalFocusManager.current

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Main Row with Icon and Content
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Icon Column
                    Column(
                        modifier = Modifier
                            .width(40.dp)
                    ) {
                        when {
                            // User Avatar for interactions
                            item.notificationType == NotificationType.FOLLOW_RECEIVED ||
                                    item.notificationType == NotificationType.COMMENT_LIKE_RECEIVED ||
                                    item.notificationType == NotificationType.LIKE_RECEIVED ||
                                    item.notificationType == NotificationType.COMMENT_REPLY_RECEIVED ||
                                    item.notificationType == NotificationType.COMMENT_RECEIVED ||
                                    item.notificationType == NotificationType.MESSAGE_RECEIVED -> {

                                Card(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.CenterHorizontally),
                                    shape = CircleShape,
                                    elevation = 2.dp
                                ) {
                                    if (triggerUserImageInfoMetadata != null) {
                                        Profile.UserBadge(
                                            triggerUserImageInfoMetadata,
                                            context,
                                            loggedInUser,
                                            size = 36.dp
                                        )
                                    } else {
                                        Profile.DefaultBadge(size = 36.dp)
                                    }
                                }

                                // Action Icon Overlay
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.CenterHorizontally)
                                        .offset(y = (-4).dp)
                                        .background(
                                            color = MaterialTheme.colors.surface,
                                            shape = CircleShape
                                        )
                                        .clip(CircleShape)
                                ) {
                                    when (item.notificationType) {
                                        NotificationType.LIKE_RECEIVED,
                                        NotificationType.COMMENT_LIKE_RECEIVED -> {
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .align(Alignment.Center),
                                                tint = MaterialTheme.colors.error
                                            )
                                        }

                                        NotificationType.COMMENT_REPLY_RECEIVED -> {
                                            Icon(
                                                imageVector = Icons.Filled.Reply,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .scale(-1f, 1f)
                                                    .align(Alignment.Center),
                                                tint = MaterialTheme.colors.secondary
                                            )
                                        }

                                        NotificationType.COMMENT_RECEIVED -> {
                                            Icon(
                                                imageVector = Icons.Filled.Comment,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .align(Alignment.Center),
                                                tint = MaterialTheme.colors.secondary
                                            )
                                        }

                                        NotificationType.MESSAGE_RECEIVED -> {
                                            Icon(
                                                imageVector = Icons.Filled.Mail,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .align(Alignment.Center),
                                                tint = MaterialTheme.colors.secondary
                                            )
                                        }

                                        else -> {}
                                    }
                                }
                            }

                            // Gift Icon
                            item.notificationType == NotificationType.GIFT_UNWRAPPED -> {
                                Icon(
                                    painter = painterResource(id = R.drawable.present2),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }

                            // Article Rejected Icon
                            item.notificationType == NotificationType.ARTICLE_REJECTED -> {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.CenterHorizontally)
                                        .background(
                                            color = Color(0xFFE53E3E).copy(alpha = 0.1f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFE53E3E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Generic Icons
                            else -> {
                                val icon = when (item.notificationType) {
                                    NotificationType.REWARD -> {
                                        if (item.transactionDescriptionType == TransactionDescriptionType.LIKE_RECEIVED)
                                            Icons.Filled.Favorite
                                        else
                                            Icons.Filled.Star
                                    }

                                    else -> Icons.Outlined.Info
                                }

                                val iconColor = when (item.notificationType) {
                                    NotificationType.INFORMATION -> MaterialTheme.colors.onSurface.copy(
                                        alpha = 0.5f
                                    )

                                    NotificationType.REWARD -> {
                                        when (item.transactionDescriptionType) {
                                            TransactionDescriptionType.LIKE_RECEIVED -> MaterialTheme.colors.error
                                            TransactionDescriptionType.SPECIAL_REWARD -> MaterialTheme.colors.secondary
                                            else -> Color(0xFF28a745)
                                        }
                                    }

                                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                }

                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .align(Alignment.CenterHorizontally)
                                )

                                if (item.notificationType == NotificationType.REWARD &&
                                    item.transactionDescriptionType == TransactionDescriptionType.LIKE_RECEIVED
                                ) {
                                    val likes =
                                        ViewModelUtil.parseLikesMultipleFromNotification(item.additionalData)
                                    val likesNum = likes.toString().toIntOrNull()
                                    val displayLikes = if (likesNum != null) {
                                        StoryUtil.likesToStringNumber(likesNum)
                                    } else likes.toString()

                                    Text(
                                        text = "×$displayLikes",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.secondary,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Content Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        // Title and Timestamp Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Title
                                Text(
                                    text = when (item.notificationType) {
                                        NotificationType.REWARD -> {
                                            if (item.transactionDescriptionType == TransactionDescriptionType.LIKE_RECEIVED) {
                                                "Likes Reward"
                                            } else {
                                                "Reward Received"
                                            }
                                        }

                                        NotificationType.FOLLOW_RECEIVED -> "New Follower"
                                        NotificationType.COMMENT_REPLY_RECEIVED -> "Reply to Your Comment"
                                        NotificationType.COMMENT_RECEIVED -> "New Comment"
                                        NotificationType.COMMENT_LIKE_RECEIVED -> "Comment Liked"
                                        NotificationType.LIKE_RECEIVED -> "Article Liked"
                                        NotificationType.MESSAGE_RECEIVED -> "New Message"
                                        NotificationType.GIFT_UNWRAPPED -> "Gift Box Opened"
                                        NotificationType.ARTICLE_REJECTED -> "Article Rejected"
                                        null -> "App Update"
                                        else -> "Information"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (item.notificationType) {
                                        NotificationType.ARTICLE_REJECTED -> Color(0xFFE53E3E)
                                        else -> MaterialTheme.colors.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                // Check for user's own reply - checkmark only shows when user has replied
                                if (item.notificationType == NotificationType.COMMENT_REPLY_RECEIVED ||
                                    item.notificationType == NotificationType.COMMENT_RECEIVED
                                ) {

                                    val commentNotification = remember {
                                        Gson().fromJson<ArticleCommentLikeReplyNotification>(
                                            item.additionalData,
                                            ArticleCommentLikeReplyNotification::class.java
                                        )
                                    }

                                    // Only show checkmark if the user has replied (notificationReply exists)
                                    if (commentNotification.notificationReply != null) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Replied",
                                            tint = MaterialTheme.colors.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Timestamp
                            val localDateTime = DateTime(item.timestamp, DateTimeZone.UTC)
                                .withZone(DateTimeZone.getDefault())

                            Text(
                                text = localDateTime.toString(dtf),
                                fontSize = 10.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Description
                        when {
                            item.notificationType == NotificationType.FOLLOW_RECEIVED -> {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colors.secondary
                                            )
                                        ) {
                                            append(item.triggerAuthor ?: "")
                                        }
                                        append(" started following you")
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            item.notificationType == NotificationType.LIKE_RECEIVED -> {
                                val storyTitle = item.additionalData.split(":").lastOrNull()
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colors.secondary
                                            )
                                        ) {
                                            append(item.triggerAuthor ?: "")
                                        }
                                        append(" liked your article")
                                        if (storyTitle != null) {
                                            append(": $storyTitle")
                                        }
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            item.notificationType == NotificationType.COMMENT_LIKE_RECEIVED -> {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colors.secondary
                                            )
                                        ) {
                                            append(item.triggerAuthor ?: "")
                                        }
                                        append(" liked your comment")
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            item.notificationType == NotificationType.COMMENT_REPLY_RECEIVED -> {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colors.secondary
                                            )
                                        ) {
                                            append(item.triggerAuthor ?: "")
                                        }
                                        append(" replied to your comment")
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            item.notificationType == NotificationType.COMMENT_RECEIVED -> {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colors.secondary
                                            )
                                        ) {
                                            append(item.triggerAuthor ?: "")
                                        }
                                        append(" commented on your article")
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            item.notificationType == NotificationType.MESSAGE_RECEIVED -> {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colors.secondary
                                            )
                                        ) {
                                            append(item.triggerAuthor ?: "")
                                        }
                                        append(" sent you a message")
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            item.notificationType == NotificationType.GIFT_UNWRAPPED -> {
                                Text(
                                    text = "You received: ${item.additionalData}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.secondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            item.notificationType == NotificationType.ARTICLE_REJECTED -> {
                                var data =item.additionalData.split(":")
                                val slugTitle = data.firstOrNull()
                                val rejectionReason = data.lastOrNull()
                                Column {
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(
                                                style = SpanStyle(
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFE53E3E)
                                                )
                                            ) {
                                                append(item.storyTitle)
                                            }
                                            append(" was not approved for publication")
                                        },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Show rejection reason preview
                                    if ( item.additionalData.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = rejectionReason?:"",
                                            fontSize = 12.sp,
                                            color = Color(0xFFE53E3E).copy(alpha = 0.7f),
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                // Show detailed rejection dialog
                                                loading = true
                                                storySavedViewModel.viewModelScope.launch(Dispatchers.IO) {
                                                    ViewModelUtil.fetchArticle(
                                                        storySavedViewModel.storySavedViewModelRepository.context,
                                                        storySavedViewModel.viewModelScope,
                                                        slugTitle ?: return@launch
                                                    ) { story ->
                                                        storySavedViewModel.viewModelScope.launch(Dispatchers.Main) {
                                                            loading = false
                                                            if (story != null) {
                                                                StoryUtil.startEditActivity(story, context)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(
                                                "Edit Article",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colors.secondary
                                            )
                                        }
                                    }
                                }
                            }

                            item.notificationType == null -> {
                                Text(
                                    text = "Update available - tap to download",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colors.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            else -> {
                                Text(
                                    text = StoryUtil.getDescriptionFromNotificationTransactionDescriptionType(
                                        context,
                                        item
                                    ),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Comment Preview Section
                if (item.storyTitle != null && (item.notificationType == NotificationType.COMMENT_REPLY_RECEIVED ||
                            item.notificationType == NotificationType.COMMENT_RECEIVED ||
                            item.notificationType == NotificationType.COMMENT_LIKE_RECEIVED)
                ) {

                    val commentNotification = remember {
                        Gson().fromJson<ArticleCommentLikeReplyNotification>(
                            item.additionalData,
                            ArticleCommentLikeReplyNotification::class.java
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Determine if there's a user reply (from notificationReply field)
                    val hasUserReply = commentNotification.notificationReply != null
                    // Determine if there's a reply from another user (for COMMENT_REPLY_RECEIVED)
                    val hasOtherReply = commentNotification.replyToComment != null

                    // For COMMENT_LIKE_RECEIVED, always show the original comment and nothing else
                    if (item.notificationType == NotificationType.COMMENT_LIKE_RECEIVED) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Card(
                                modifier = Modifier
                                    .size(20.dp),
                                shape = CircleShape,
                                elevation = 1.dp
                            ) {
                                if (triggerUserImageInfoMetadata != null) {
                                    Profile.UserBadge(
                                        triggerUserImageInfoMetadata,
                                        context,
                                        loggedInUser,
                                        size = 20.dp
                                    )
                                } else {
                                    Profile.DefaultBadge(size = 20.dp)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Card(
                                modifier = Modifier
                                    .weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = MaterialTheme.colors.surface,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    text = commentNotification.comment.comment,
                                    modifier = Modifier.padding(10.dp),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colors.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        // Don't add any buttons or additional content for comment likes
                    }
                    // For COMMENT_REPLY_RECEIVED and COMMENT_RECEIVED, handle with full logic
                    else {
                        // For COMMENT_REPLY_RECEIVED, always show the user's original comment
                        if (item.notificationType == NotificationType.COMMENT_REPLY_RECEIVED) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 52.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Card(
                                    modifier = Modifier
                                        .size(20.dp),
                                    shape = CircleShape,
                                    elevation = 1.dp
                                ) {
                                    // User's own avatar
                                    Profile.UserBadge(
                                        loggedInUser = loggedInUser,
                                        context = context,
                                        size = 20.dp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Card(
                                    modifier = Modifier
                                        .weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    backgroundColor = MaterialTheme.colors.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Text(
                                        text = commentNotification.comment.comment,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colors.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // For COMMENT_RECEIVED, always show the other user's comment
                        if (item.notificationType == NotificationType.COMMENT_RECEIVED) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 52.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Card(
                                    modifier = Modifier
                                        .size(20.dp),
                                    shape = CircleShape,
                                    elevation = 1.dp
                                ) {
                                    if (triggerUserImageInfoMetadata != null) {
                                        Profile.UserBadge(
                                            triggerUserImageInfoMetadata,
                                            context,
                                            loggedInUser,
                                            size = 20.dp
                                        )
                                    } else {
                                        Profile.DefaultBadge(size = 20.dp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Card(
                                    modifier = Modifier
                                        .weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    backgroundColor = MaterialTheme.colors.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Text(
                                        text = commentNotification.comment.comment,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colors.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Show additional content only when expanded (showMore = true)
                        if (showMore.value) {
                            // For COMMENT_REPLY_RECEIVED, show the other user's reply
                            if (item.notificationType == NotificationType.COMMENT_REPLY_RECEIVED && hasOtherReply) {
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 52.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .size(20.dp),
                                        shape = CircleShape,
                                        elevation = 1.dp
                                    ) {
                                        if (triggerUserImageInfoMetadata != null) {
                                            Profile.UserBadge(
                                                triggerUserImageInfoMetadata,
                                                context,
                                                loggedInUser,
                                                size = 20.dp
                                            )
                                        } else {
                                            Profile.DefaultBadge(size = 20.dp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Card(
                                        modifier = Modifier
                                            .weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        backgroundColor = MaterialTheme.colors.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Text(
                                            text = commentNotification.replyToComment?.comment
                                                ?: "",
                                            modifier = Modifier.padding(10.dp),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colors.onSurface,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Show user's reply if it exists (for any notification type)
                            if (hasUserReply) {
                                Spacer(modifier = Modifier.height(4.dp))

                                // "You replied" indicator
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 52.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "You replied",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colors.secondary,
                                        fontStyle = FontStyle.Italic
                                    )
                                }

                                // Show the user's reply
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 52.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .size(20.dp),
                                        shape = CircleShape,
                                        elevation = 1.dp
                                    ) {
                                        Profile.UserBadge(
                                            loggedInUser = loggedInUser,
                                            context = context,
                                            size = 20.dp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Card(
                                        modifier = Modifier
                                            .weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        backgroundColor = if (commentNotification.notificationReply!!.deleted)
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                                        else
                                            MaterialTheme.colors.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (commentNotification.notificationReply!!.deleted)
                                                MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                                            else
                                                MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        if (commentNotification.notificationReply!!.deleted) {
                                            Text(
                                                text = "Comment was deleted",
                                                modifier = Modifier.padding(10.dp),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                                fontStyle = FontStyle.Italic,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = commentNotification.notificationReply!!.comment,
                                                modifier = Modifier.padding(10.dp),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colors.onSurface,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 52.dp, top = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    ViewStoryButton(storySavedViewModel, item, navHostController)
                                }
                            }
                        }

                        // Determine what button to show (for COMMENT_REPLY_RECEIVED and COMMENT_RECEIVED only)
                        val shouldShowButton =
                            // Show button if there's additional content to view
                            (item.notificationType == NotificationType.COMMENT_REPLY_RECEIVED && hasOtherReply) || // Other user's reply exists
                                    hasUserReply || // User has replied
                                    (!hasUserReply && !hasOtherReply) // No replies yet, can reply

                        if (shouldShowButton) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 52.dp, top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                // Decide button text based on state
                                val buttonText = when {
                                    showMore.value -> "Hide thread"
                                    hasUserReply || hasOtherReply -> "View thread"
                                    else -> "Reply"
                                }

                                TextButton(
                                    onClick = { showMore.value = !showMore.value },
                                    modifier = Modifier.heightIn(24.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = buttonText,
                                        color = MaterialTheme.colors.secondary,
                                        fontSize = 11.sp
                                    )
                                    if (buttonText == "Reply" && !showMore.value) {
                                        Icon(
                                            imageVector = Icons.Default.Reply,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.secondary,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .padding(start = 2.dp)
                                        )
                                    } else if (showMore.value) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandLess,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.secondary,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .padding(start = 2.dp)
                                        )
                                    } else if (!showMore.value && (hasUserReply || hasOtherReply)) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.secondary,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .padding(start = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Reply Input Section (only when showMore is true AND user hasn't replied yet)
                        if (showMore.value && !hasUserReply) {
                            Spacer(modifier = Modifier.height(8.dp))

                            var replyText by remember { mutableStateOf("") }

                            CompositionLocalProvider(LocalTextSelectionColors provides ComposableUtil.GetDefaultTextSelectionColors()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 52.dp)
                                ) {
                                    // Reply input with subtle background
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        TextField(
                                            value = replyText,
                                            onValueChange = { replyText = it.take(500) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 36.dp)
                                                .onFocusChanged { z ->
                                                    bottomBarVisibility.value = !z.hasFocus
                                                }
                                                .onKeyEvent { event ->
                                                    if (event.key.nativeKeyCode == KeyEvent.KEYCODE_BACK) {
                                                        focusManager.clearFocus()
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                },
                                            keyboardOptions = KeyboardOptions.Default.copy(
                                                imeAction = ImeAction.Send
                                            ),
                                            keyboardActions = KeyboardActions(onSend = {
                                                focusManager.clearFocus()
                                            }),
                                            colors = TextFieldDefaults.textFieldColors(
                                                backgroundColor = Color.Transparent,
                                                cursorColor = MaterialTheme.colors.secondary,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                textColor = MaterialTheme.colors.onSurface,
                                                placeholderColor = MaterialTheme.colors.onSurface.copy(
                                                    alpha = 0.4f
                                                )
                                            ),
                                            placeholder = {
                                                Text(
                                                    text = "Write a reply...",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colors.onSurface.copy(
                                                        alpha = 0.4f
                                                    )
                                                )
                                            },
                                            singleLine = true
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        // View Story button when replying
                                        ViewStoryButton(
                                            storySavedViewModel,
                                            item,
                                            navHostController
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                storySavedViewModel.viewModelScope.launch(
                                                    Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler
                                                ) {
                                                    val commentUUID = UUID.randomUUID().toString()
                                                    val storyDatabase = Constants.getStoryDatabase(
                                                        storySavedViewModel.storySavedViewModelRepository.context
                                                    )
                                                    val entryTimestamp =
                                                        Config.getStandardTimeUTCString()
                                                    val comment = StoryComment(
                                                        item.storyTitle,
                                                        loggedInUser.username,
                                                        replyText,
                                                        commentUUID,
                                                        commentNotification.comment.commentUUID,
                                                        entryTimestamp
                                                    )
                                                    storyDatabase.storyCommentDao().update(comment)

                                                    val commentPendingUpload = StoryPendingUpload(
                                                        UUID.randomUUID(),
                                                        entryTimestamp,
                                                        commentUUID,
                                                        PendingUploadType.COMMENT,
                                                        item.storyTitle,
                                                        UUID.fromString(loggedInUser.user_uuid)
                                                    )
                                                    storyDatabase.storyPendingUploadDao()
                                                        .update(commentPendingUpload)
                                                    ViewModelUtil.startResumeUploads(
                                                        this,
                                                        storySavedViewModel.storySavedViewModelRepository.context
                                                    )

                                                    withContext(Dispatchers.Main) {
                                                        showMore.value = false
                                                    }
                                                }
                                            },
                                            enabled = replyText.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = MaterialTheme.colors.secondary,
                                                contentColor = MaterialTheme.colors.onSecondary
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.height(32.dp)  // Changed from 28.dp to 32.dp to match other buttons
                                        ) {
                                            Text(
                                                text = "Send",
                                                fontSize = 12.sp,  // Changed from 11.sp to 12.sp to match other buttons
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Additional content for other notification types
                when (item.notificationType) {
                    NotificationType.MESSAGE_RECEIVED -> {
                        MessageNotificationContent(
                            item = item,
                            context = context,
                            loggedInUser = loggedInUser,
                            storySavedViewModel = storySavedViewModel,
                            navHostController = navHostController,
                            bottomBarVisibility = bottomBarVisibility,
                            lifecycleOwner = lifecycleOwner,
                            focusManager = focusManager
                        )
                    }

                    else -> {
                        if (item.storyTitle != null &&
                            (item.notificationType == NotificationType.LIKE_RECEIVED ||
                                    item.notificationType == NotificationType.COMMENT_LIKE_RECEIVED)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                ViewStoryButton(storySavedViewModel, item, navHostController)
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun MessageNotificationContent(
            item: Notification,
            context: Context,
            loggedInUser: AccountInfoFull,
            storySavedViewModel: StorySavedViewModel,
            navHostController: NavHostController,
            bottomBarVisibility: MutableState<Boolean>,
            lifecycleOwner: LifecycleOwner,
            focusManager: androidx.compose.ui.focus.FocusManager
        ) {
            val messageNotification = remember {
                Gson().fromJson<UserMessagePublishedNotification>(
                    item.additionalData,
                    UserMessagePublishedNotification::class.java
                )
            }

            val userFollowedInfo = UsersFollowingInfo(
                item.triggerAuthor ?: return,
                item.profileIcon,
                listOf()
            )

            var showLoading by remember { mutableStateOf(false) }
            var approved by remember {
                mutableStateOf(
                    messageNotification.contactApproved ||
                            storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager.getUserContactBlocked(
                                item.triggerAuthor
                            ) == false
                )
            }
            var blocked by remember {
                mutableStateOf(
                    storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager.getUserContactBlocked(
                        item.triggerAuthor
                    ) ?: false
                )
            }

            val latestRepliedId =
                storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager
                    .getLatestLatestMessageIdRepliedForUser(item.triggerAuthor)
            var replied by remember {
                mutableStateOf(latestRepliedId != null && latestRepliedId >= messageNotification.messageId)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp, top = 8.dp)
            ) {
                // Show the message content
                Text(
                    text = messageNotification.content,
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (messageNotification.additionalMessages > 0) {
                    Text(
                        text = "+${messageNotification.additionalMessages} more",
                        fontSize = 11.sp,
                        color = MaterialTheme.colors.secondary,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                when {
                    showLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colors.secondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    replied -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = MaterialTheme.colors.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "You replied",
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.secondary,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    ViewModelUtil.goToMessages(
                                        navHostController,
                                        storySavedViewModel,
                                        userFollowedInfo
                                    )
                                },
                                modifier = Modifier.heightIn(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "View conversation",
                                    color = MaterialTheme.colors.secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    !blocked && !approved -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Allow this user to contact you?",
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showLoading = true
                                        storySavedViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                                            val success = MessageUtil.approveBlockContact(
                                                context,
                                                loggedInUser,
                                                item.triggerAuthor,
                                                false
                                            )
                                            storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager
                                                .setUserContactBlocked(item.triggerAuthor, true)
                                            withContext(Dispatchers.Main) {
                                                showLoading = false
                                                if (success) {
                                                    blocked = true
                                                }
                                            }
                                        }
                                    },
                                    border = BorderStroke(1.dp, MaterialTheme.colors.error),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colors.error
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Block",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Button(
                                    onClick = {
                                        showLoading = true
                                        storySavedViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                                            val success = MessageUtil.approveBlockContact(
                                                context,
                                                loggedInUser,
                                                item.triggerAuthor,
                                                true
                                            )
                                            storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager
                                                .setUserContactBlocked(item.triggerAuthor, false)
                                            withContext(Dispatchers.Main) {
                                                showLoading = false
                                                if (success) {
                                                    approved = true
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = MaterialTheme.colors.secondary,
                                        contentColor = MaterialTheme.colors.onSecondary
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Allow",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    blocked -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colors.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "User blocked",
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.error,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    approved -> {
                        if (messageNotification.additionalMessages == 0) {
                            var replyText by remember { mutableStateOf("") }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                // Reply input with subtle background
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    TextField(
                                        value = replyText,
                                        onValueChange = { replyText = it.take(500) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 36.dp)
                                            .onFocusChanged { z ->
                                                bottomBarVisibility.value = !z.hasFocus
                                            }
                                            .onKeyEvent { event ->
                                                if (event.key.nativeKeyCode == android.view.KeyEvent.KEYCODE_BACK) {
                                                    focusManager.clearFocus()
                                                    true
                                                } else {
                                                    false
                                                }
                                            },
                                        keyboardOptions = KeyboardOptions.Default.copy(
                                            imeAction = ImeAction.Send
                                        ),
                                        keyboardActions = KeyboardActions(onSend = {
                                            focusManager.clearFocus()
                                        }),
                                        colors = TextFieldDefaults.textFieldColors(
                                            backgroundColor = Color.Transparent,
                                            cursorColor = MaterialTheme.colors.secondary,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            textColor = MaterialTheme.colors.onSurface,
                                            placeholderColor = MaterialTheme.colors.onSurface.copy(
                                                alpha = 0.4f
                                            )
                                        ),
                                        placeholder = {
                                            Text(
                                                text = "Reply...",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                                            )
                                        },
                                        singleLine = true
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            ViewModelUtil.goToMessages(
                                                navHostController,
                                                storySavedViewModel,
                                                userFollowedInfo
                                            )
                                        },
                                        modifier = Modifier.height(32.dp),  // Changed from heightIn to fixed height
                                        contentPadding = PaddingValues(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        )  // Match horizontal padding with Send button
                                    ) {
                                        Text(
                                            text = "All messages",
                                            color = MaterialTheme.colors.secondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            MessageUtil.sendMessage(
                                                messageNotification.messageId,
                                                storySavedViewModel,
                                                loggedInUser,
                                                replyText,
                                                userFollowedInfo
                                            )
                                            replied = true
                                        },
                                        enabled = replyText.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = MaterialTheme.colors.secondary,
                                            contentColor = MaterialTheme.colors.onSecondary
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = "Send",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        ViewModelUtil.goToMessages(
                                            navHostController,
                                            storySavedViewModel,
                                            userFollowedInfo
                                        )
                                    },
                                    modifier = Modifier
                                        .heightIn(32.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 8.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Text(
                                        text = "View all messages",
                                        color = MaterialTheme.colors.secondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun ViewStoryButton(
            storySavedViewModel: StorySavedViewModel,
            item: Notification,
            navHostController: NavHostController
        ) {
            var loading by remember { mutableStateOf(false) }

            if (loading) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.secondary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                TextButton(
                    onClick = {
                        loading = true
                        storySavedViewModel.viewModelScope.launch(Dispatchers.IO) {
                            ViewModelUtil.fetchArticle(
                                storySavedViewModel.storySavedViewModelRepository.context,
                                storySavedViewModel.viewModelScope,
                                item.storyTitle ?: return@launch
                            ) { story ->
                                storySavedViewModel.viewModelScope.launch(Dispatchers.Main) {
                                    loading = false
                                    if (story != null) {
                                        StoryUtil.startContentActivity(
                                            story,
                                            navHostController.context
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.height(32.dp),  // Fixed height instead of heightIn
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "View story",
                        color = MaterialTheme.colors.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}