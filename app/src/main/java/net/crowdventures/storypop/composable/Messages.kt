package net.crowdventures.storypop.composable

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import net.crowdventures.storypop.BuildConfig
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.UserMessagePublished
import net.crowdventures.storypop.dto.UsersFollowingInfo
import net.crowdventures.storypop.room.MessageType
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.room.UserMessage
import net.crowdventures.storypop.room.UserMessageWithPendingUploads
import net.crowdventures.storypop.util.BottomBarManagerCallback
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.MessageUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.SuccessCallback
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.UUID

class Messages {
    companion object {
        // Professional blue theme colors
        private val LightOwnMessageBg = Color(0xFFE8F0FE)  // Light blue for own messages
        private val LightOtherMessageBg = Color(0xFFF5F7FA) // Light gray for others
        private val DarkOwnMessageBg = Color(0xFF1E293B)    // Dark blue-gray for own messages
        private val DarkOtherMessageBg = Color(0xFF121826)  // Dark surface for others
        private val LightDeletedMessageBg = Color(0xFFF0F0F0) // Light gray for deleted
        private val DarkDeletedMessageBg = Color(0xFF2A2A2A)  // Dark gray for deleted

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun ShowMessages(
            lifecycleOwner: LifecycleOwner,
            storySavedViewModel: StorySavedViewModel,
            bottomBarVisible: MutableState<Boolean>,
            loggedInUser: AccountInfoFull,
            paddingValues: PaddingValues,
            sharedPreferenceManager: SharedPreferenceManager,
            navHostController: NavHostController
        ) {
            ComposableUtil.HandleImeShown(object : BottomBarManagerCallback {
                override fun hide() {
                    bottomBarVisible.value = true
                }

                override fun show() {
                    bottomBarVisible.value = false
                }
            })
            val defaultAppTextSelectionColors = ComposableUtil.GetDefaultTextSelectionColors()
            CompositionLocalProvider(LocalTextSelectionColors provides defaultAppTextSelectionColors) {
                var userFollowingInfoNull by remember {
                    mutableStateOf(storySavedViewModel.messagesTargetUsername.value)
                }
                storySavedViewModel.messagesTargetUsername.observe(lifecycleOwner) {
                    userFollowingInfoNull = it
                }
                LaunchedEffect(Unit) {
                    storySavedViewModel.setUserMessagesFromDatabase(storySavedViewModel.storySavedViewModelRepository.context,
                        storySavedViewModel.messagesTargetUsername.value ?: return@LaunchedEffect,
                        loggedInUser
                    )
                }
                if (userFollowingInfoNull == null) ViewModelUtil.goToHome(navHostController)
                val userFollowingInfo = userFollowingInfoNull ?: return@CompositionLocalProvider
                val userBlocked = storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager.getUserContactBlocked(userFollowingInfo.username)
                var showImageSelectionLoading by remember {
                    mutableStateOf(false)
                }
                var attachedImage: ImageInfoMetadata? by remember {
                    mutableStateOf(storySavedViewModel.userMessageDraftAttachedImage)
                }
                val getProfileImageTask = ImageUtil.getImageForResult(storySavedViewModel, false,
                    navHostController.context, true, false,
                    object : SuccessCallback<ImageInfoMetadata> {
                        override fun onSuccess(vararg param: ImageInfoMetadata) {
                            attachedImage = param.first()
                            storySavedViewModel.userMessageDraftAttachedImage = attachedImage
                            showImageSelectionLoading = false
                        }

                        override fun onFailure(reason: Any?) {
                            showImageSelectionLoading = false
                        }

                    })
                val userRemoteMessages: LazyPagingItems<UserMessagePublished> = if (userBlocked == true) flowOf(
                    PagingData.from(listOf<UserMessagePublished>())).cachedIn(storySavedViewModel.viewModelScope).collectAsLazyPagingItems() else
                    storySavedViewModel.userRemoteMessages.collectAsLazyPagingItems()

                val userMessages = if (userBlocked == true) flowOf(
                    PagingData.from(listOf<UserMessageWithPendingUploads>())).cachedIn(storySavedViewModel.viewModelScope).collectAsLazyPagingItems()
                else storySavedViewModel.userMessages.collectAsLazyPagingItems()

                val lazyColumnListState = rememberLazyListState()
                if (userMessages.loadState.refresh is LoadState.NotLoading && userRemoteMessages.loadState.refresh is LoadState.NotLoading && userMessages.itemCount > 0 && userMessages[0] != null) {
                    LaunchedEffect(userMessages.itemCount) {
                        lazyColumnListState.scrollToItem(0)
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.background)
                ) {
                    // User info header - with fixed visibility
                    AnimatedVisibility(
                        visible = bottomBarVisible.value,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.surface)
                                .padding(8.dp)
                        ) {
                            Row(
                                Modifier
                                    .padding(0.dp, 5.dp)
                                    .align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Profile icon - fixed visibility
                                Card(
                                    onClick = {
                                        StoryUtil.showAuthorProfilePage(
                                            storySavedViewModel.storySavedViewModelRepository.context,
                                            userFollowingInfo.username
                                        )
                                    },
                                    Modifier
                                        .size(40.dp),
                                    shape = CircleShape,
                                    elevation = 2.dp
                                ) {
                                    if (userFollowingInfo.profileIcon != null) {
                                        val imageInfoMetadata = Gson().fromJson(
                                            userFollowingInfo.profileIcon,
                                            ImageInfoMetadata::class.java
                                        )
                                        Profile.UserBadge(
                                            imageInfoMetadata,
                                            storySavedViewModel.storySavedViewModelRepository.context,
                                            loggedInUser,
                                            size = 40.dp
                                        )
                                    } else {
                                        Profile.DefaultBadge(size = 40.dp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                // Username with theme-aware colors
                                Text(
                                    text = userFollowingInfo.username,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSystemInDarkTheme())
                                        MaterialTheme.colors.secondary
                                    else
                                        MaterialTheme.colors.primary,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    modifier = Modifier.clickable {
                                        StoryUtil.showAuthorProfilePage(
                                            storySavedViewModel.storySavedViewModelRepository.context,
                                            userFollowingInfo.username
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Column(Modifier.padding(paddingValues)) {
                        Divider(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                            thickness = 1.dp
                        )

                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            reverseLayout = true,
                            state = lazyColumnListState
                        ) {
                            if (userBlocked == true) {
                                item(key = "nothing_to_show") {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(0.dp, 0.dp, 0.dp, 100.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Block,
                                            contentDescription = "User blocked",
                                            modifier = Modifier
                                                .size(48.dp)
                                                .align(Alignment.CenterHorizontally),
                                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "User is blocked from contacting you",
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .widthIn(max = 240.dp)
                                                .align(Alignment.CenterHorizontally),
                                            textAlign = TextAlign.Center,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Unblock to continue chatting",
                                            color = MaterialTheme.colors.secondary,
                                            modifier = Modifier
                                                .clickable {
                                                    storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager.setUserContactBlocked(userFollowingInfo.username, false)
                                                }
                                                .align(Alignment.CenterHorizontally)
                                                .padding(8.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else if ((userMessages.loadState.refresh is LoadState.Loading && userMessages.itemCount == 0 || userRemoteMessages.loadState.refresh is LoadState.Loading && userRemoteMessages.itemCount == 0)) {
                                item(key = "progress") {
                                    Box(
                                        modifier = Modifier
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
                            } else if (userRemoteMessages.loadState.refresh is LoadState.Error && userRemoteMessages.itemCount == 0) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Filled.Block,
                                                contentDescription = "Error",
                                                tint = MaterialTheme.colors.error,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Could not load messages",
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else if (userMessages.loadState.refresh is LoadState.NotLoading && userRemoteMessages.loadState.refresh is LoadState.NotLoading &&
                                userMessages.loadState.append.endOfPaginationReached &&
                                userRemoteMessages.loadState.append.endOfPaginationReached &&
                                userMessages.itemCount == 0 && userRemoteMessages.itemCount == 0) {
                                item(key = "nothing_to_show") {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(0.dp, 0.dp, 0.dp, 100.dp)
                                    ) {
                                        Text(
                                            text = "No messages yet",
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .widthIn(max = 240.dp)
                                                .align(Alignment.CenterHorizontally),
                                            textAlign = TextAlign.Center,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Send a message to start the conversation",
                                            color = MaterialTheme.colors.secondary,
                                            modifier = Modifier.align(Alignment.CenterHorizontally),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else {
                                items(
                                    userMessages.itemCount,
                                    key = { index -> "local_${userMessages[index]?.message?.messageUUID ?: index}" }
                                ) { index ->
                                    val item = userMessages[index]
                                    if (item != null && !userRemoteMessages.itemSnapshotList.any { x -> x?.messageUUID == item.message.messageUUID } &&
                                        !userRemoteMessages.itemSnapshotList.any { x -> x != null && DateTime(x.timestamp, DateTimeZone.UTC) > DateTime(item.message.entryTimeStamp, DateTimeZone.UTC) }) {
                                        ShowMessage(
                                            navHostController.context,
                                            item.message,
                                            item.storypendingupload.plus(item.storypendinguploadMessage),
                                            userFollowingInfo,
                                            loggedInUser,
                                            storySavedViewModel
                                        )
                                    }
                                }
                                items(
                                    userRemoteMessages.itemCount,
                                    key = { index -> "remote_${userRemoteMessages[index]?.messageUUID ?: index}" }
                                ) { index ->
                                    val item = userRemoteMessages[index]?.toUserMessage()
                                    if (item != null) {
                                        val deletedItem = userMessages.itemSnapshotList.firstOrNull { x -> x != null && x.message.deleted && x.message.messageUUID == item.messageUUID }
                                        ShowMessage(
                                            navHostController.context,
                                            deletedItem?.message ?: item,
                                            listOf(),
                                            userFollowingInfo,
                                            loggedInUser,
                                            storySavedViewModel
                                        )
                                    }
                                }
                            }
                        }

                        // Message input card - compact
                        Card(
                            modifier = Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp)),
                            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
                            backgroundColor = MaterialTheme.colors.surface,
                            elevation = 0.dp
                        ) {
                            val attachedImageNotNull = attachedImage
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var newMessageText by remember {
                                    mutableStateOf(TextFieldValue(storySavedViewModel.userMessageDraftText))
                                }

                                if (showImageSelectionLoading) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .padding(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colors.secondary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else if (attachedImageNotNull != null) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp)
                                            .padding(end = 8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.fillMaxSize(),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colors.secondary.copy(alpha = 0.3f))
                                        ) {
                                            Box {
                                                MessageUtil.ShowAttachedImage(
                                                    imageInfoMetadata = attachedImageNotNull,
                                                    context = navHostController.context,
                                                    loggedInUser
                                                )
                                                IconButton(
                                                    onClick = {
                                                        attachedImage = null
                                                        storySavedViewModel.userMessageDraftAttachedImage = null
                                                    },
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = 4.dp, y = (-4).dp)
                                                        .background(
                                                            MaterialTheme.colors.surface,
                                                            CircleShape
                                                        )
                                                        .shadow(2.dp, CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Close,
                                                        tint = MaterialTheme.colors.secondary,
                                                        contentDescription = "Remove",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    TextField(
                                        value = newMessageText,
                                        onValueChange = { x ->
                                            newMessageText = if (x.text.length >= 500) TextFieldValue(
                                                x.text.subSequence(0, x.text.length.coerceAtMost(499)).toString()
                                            ) else x
                                            storySavedViewModel.userMessageDraftText = newMessageText.text
                                        },
                                        modifier = Modifier
                                            .weight(1f),
                                        keyboardOptions = KeyboardOptions.Default.copy(
                                            imeAction = ImeAction.Send,
                                            capitalization = KeyboardCapitalization.None
                                        ),
                                        keyboardActions = KeyboardActions(onSend = {
                                            val latestMessageReplied = if (userRemoteMessages.itemCount > 0) userRemoteMessages[0]?.messageId else null
                                            MessageUtil.sendMessage(
                                                latestMessageReplied,
                                                storySavedViewModel,
                                                loggedInUser,
                                                newMessageText.text,
                                                userFollowingInfo
                                            )
                                            newMessageText = TextFieldValue()
                                            storySavedViewModel.userMessageDraftText = ""
                                            storySavedViewModel.userMessageDraftAttachedImage = null
                                        }),
                                        placeholder = {
                                            Text(
                                                text = "Message...",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                            )
                                        },
                                        colors = TextFieldDefaults.textFieldColors(
                                            backgroundColor = Color.Transparent,
                                            cursorColor = MaterialTheme.colors.secondary,
                                            focusedIndicatorColor = MaterialTheme.colors.secondary,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            textColor = MaterialTheme.colors.onSurface
                                        ),
                                        textStyle = TextStyle(
                                            fontSize = 14.sp
                                        ),
                                        enabled = userBlocked != true,
                                        singleLine = true
                                    )
                                }

                                // Image attachment button
                                AnimatedVisibility(
                                    visible = newMessageText.text.isEmpty() && attachedImage == null,
                                    modifier = Modifier
                                ) {
                                    IconButton(
                                        onClick = {
                                            showImageSelectionLoading = true
                                            getProfileImageTask.launch(ImageUtil.requestImageIntent(navHostController.context))
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape),
                                        enabled = userBlocked != true && !showImageSelectionLoading && attachedImage == null
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Image,
                                            tint = if (userBlocked == true || showImageSelectionLoading || attachedImage != null)
                                                MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                            else MaterialTheme.colors.secondary,
                                            contentDescription = "Image",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Send button
                                IconButton(
                                    onClick = {
                                        val image = attachedImage
                                        val listOfImages: List<ImageInfoMetadata> = if (image != null) listOf(image) else listOf()
                                        val latestMessageReplied = if (userRemoteMessages.itemCount > 0) userRemoteMessages[0]?.messageId else null
                                        MessageUtil.sendMessage(
                                            latestMessageReplied,
                                            storySavedViewModel,
                                            loggedInUser,
                                            newMessageText.text,
                                            userFollowingInfo,
                                            listOfImages
                                        )
                                        newMessageText = TextFieldValue()
                                        attachedImage = null
                                        storySavedViewModel.userMessageDraftText = ""
                                        storySavedViewModel.userMessageDraftAttachedImage = null
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    enabled = (newMessageText.text.isNotEmpty() || attachedImage != null) && userBlocked != true && !showImageSelectionLoading
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        tint = if ((newMessageText.text.isNotEmpty() || attachedImage != null) && userBlocked != true && !showImageSelectionLoading)
                                            MaterialTheme.colors.secondary
                                        else MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                        contentDescription = "Send",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
        @Composable
        fun ShowMessage(
            context: Context,
            item: UserMessage,
            pendingUploads: List<StoryPendingUpload>,
            userFollowingInfo: UsersFollowingInfo,
            loggedInUser: AccountInfoFull,
            storySavedViewModel: StorySavedViewModel
        ) {
            val clipboard: ClipboardManager? = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val uriHandler = LocalUriHandler.current
            var expandedContextMenu by remember { mutableStateOf(false) }

            val isOwnMessage = loggedInUser.username == item.authorName
            val isDeleted = item.deleted

            // Handle deleted message content
            val displayContent = if (isDeleted) {
                "This message has been deleted"
            } else {
                item.content
            }

            val annotatedString = if (!isDeleted && item.messageType == MessageType.TEXT) {
                MessageUtil.getAnnotatedString(
                    item.messageType,
                    displayContent,
                    false,
                    MaterialTheme.colors.secondary,
                    false
                )
            } else {
                null
            }

            // Message background colors
            val messageBgColor = if (isDeleted) {
                if (isSystemInDarkTheme()) DarkDeletedMessageBg else LightDeletedMessageBg
            } else {
                if (isSystemInDarkTheme()) {
                    if (isOwnMessage) DarkOwnMessageBg else DarkOtherMessageBg
                } else {
                    if (isOwnMessage) LightOwnMessageBg else LightOtherMessageBg
                }
            }

            Box(Modifier.fillMaxWidth()) {
                // Profile badge and dropdown menu - positioned at top corner

                    Column(
                        modifier = Modifier
                            .padding(7.dp, 5.dp, 7.dp, 0.dp)
                            .align(if (isOwnMessage) Alignment.TopEnd else Alignment.TopStart)
                            .width(20.dp)
                            .height(20.dp)
                            .zIndex(1f)
                    ) {
                        if (pendingUploads.any()) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colors.secondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            val profileIcon = if (isOwnMessage) loggedInUser.profileIcon else userFollowingInfo.profileIcon
                            if (profileIcon != null) {
                                val imageImageInfoMetadata = Gson().fromJson(
                                    profileIcon,
                                    ImageInfoMetadata::class.java
                                )
                                Profile.UserBadge(
                                    imageImageInfoMetadata,
                                    context,
                                    loggedInUser,
                                    size = 20.dp
                                )
                            } else {
                                Profile.DefaultBadge(size = 20.dp)
                            }
                        }
                        if (!isDeleted) {
                        DropdownMenu(
                            modifier = Modifier
                                .width(160.dp)
                                .background(MaterialTheme.colors.surface),
                            expanded = expandedContextMenu,
                            onDismissRequest = { expandedContextMenu = false }
                        ) {
                            DropdownMenuItem(onClick = {
                                expandedContextMenu = false
                                val clip = ClipData.newPlainText("Message from ${item.authorName}", item.content)
                                clipboard?.setPrimaryClip(clip)
                            }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Copy",
                                        color = MaterialTheme.colors.onSurface,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            if (isOwnMessage) {
                                DropdownMenuItem(onClick = {
                                    expandedContextMenu = false
                                    storySavedViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable) {
                                        item.deleted = true
                                        Config.firebaseAnalytics.logEvent("story_message_deleted_started", null)
                                        val storyDatabase = Constants.getStoryDatabase(storySavedViewModel.storySavedViewModelRepository.context)
                                        storyDatabase.userMessageDao().update(item)
                                        val messagePendingUpload = StoryPendingUpload(
                                            UUID.randomUUID(),
                                            Config.getStandardTimeUTCString(),
                                            item.messageUUID,
                                            PendingUploadType.MESSAGE_DELETE,
                                            userFollowingInfo.username,
                                            UUID.fromString(loggedInUser.user_uuid)
                                        )
                                        storyDatabase.storyPendingUploadDao().update(messagePendingUpload)
                                        ViewModelUtil.startResumeUploads(this, storySavedViewModel.storySavedViewModelRepository.context)
                                    }
                                }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Delete",
                                            color = MaterialTheme.colors.error,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Message card
                var messageCardModifier = Modifier
                    .wrapContentWidth()
                    .align(if (isOwnMessage) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(0.dp, 0.dp, 0.dp, 10.dp)

                if (!isDeleted) {
                    messageCardModifier = messageCardModifier.combinedClickable(
                        onLongClick = { expandedContextMenu = true },
                        onClick = { expandedContextMenu = true }
                    )
                }

                // Optional gradient border for own messages (if needed)
                var contentModifier = Modifier.wrapContentWidth()
                Card(
                    modifier = messageCardModifier,
                    shape = RoundedCornerShape(15.dp),
                    backgroundColor = messageBgColor,
                    elevation = if (isDeleted) 0.dp else 2.dp
                ) {
                    Column(modifier = contentModifier) {
                        if (isDeleted) {
                            // Deleted message indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = if (isOwnMessage) 30.dp else 10.dp,
                                        end = if (isOwnMessage) 10.dp else 30.dp,
                                        top = 5.dp,
                                        bottom = 5.dp
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Message deleted",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        } else if (item.messageType == MessageType.TEXT) {
                            val onClick = { offset: Int ->
                                annotatedString?.getStringAnnotations(tag = "h", start = offset, end = offset)?.firstOrNull()?.let {
                                    Log.d("open URL", it.item)
                                    uriHandler.openUri(it.item)
                                }
                                annotatedString?.getStringAnnotations(tag = "@", start = offset, end = offset)?.firstOrNull()?.let {
                                    Log.d("open profile", it.item)
                                    StoryUtil.showAuthorProfilePage(context, it.item.substring(1))
                                }
                                annotatedString?.getStringAnnotations(tag = "#", start = offset, end = offset)?.firstOrNull()?.let {
                                    Log.d("open search", it.item)
                                    StoryUtil.startListSearchActivity(context, it.item.substring(1))
                                }
                            }

                            val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
                            val pressIndicator = Modifier.pointerInput(onClick) {
                                detectTapGestures(
                                    onLongPress = { expandedContextMenu = true },
                                    onPress = { pos ->
                                        layoutResult.value?.let { layoutResult ->
                                            onClick(layoutResult.getOffsetForPosition(pos))
                                        }
                                    }
                                )
                            }

                            BasicText(
                                modifier = Modifier
                                    .padding(
                                        start = if (isOwnMessage) 10.dp else 30.dp,
                                        top = 5.dp,
                                        end = if (isOwnMessage) 30.dp else 10.dp,
                                        bottom = 0.dp
                                    )
                                    .then(pressIndicator),
                                text = annotatedString ?: androidx.compose.ui.text.buildAnnotatedString { append(displayContent) },
                                style = TextStyle(
                                    textAlign = if (isOwnMessage) TextAlign.End else TextAlign.Start,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colors.onSurface
                                ),
                                onTextLayout = { layoutResult.value = it }
                            )
                        } else if (item.messageType == MessageType.IMAGES) {
                            val images = Gson().fromJson<List<ImageInfoMetadata>>(
                                item.content,
                                object : TypeToken<List<ImageInfoMetadata>>() {}.type
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .padding(
                                        start = if (isOwnMessage) 10.dp else 30.dp,
                                        top = 5.dp,
                                        end = if (isOwnMessage) 30.dp else 10.dp,
                                        bottom = 5.dp
                                    )
                            ) {
                                images.forEach { image ->
                                    val isPending = pendingUploads.any { it.resourceIdentifier.contains(image.name) }

                                    if (isPending) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                        ) {
                                            MessageUtil.ShowAttachedImage(
                                                imageInfoMetadata = image,
                                                context = context,
                                                loggedInUser
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(Color.Black.copy(alpha = 0.3f))
                                            )
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colors.secondary,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    } else {
                                        Card(
                                            onClick = {
                                                StfalconImageViewer.Builder<ImageInfoMetadata>(
                                                    context,
                                                    listOf(image),
                                                    object : ImageLoader<ImageInfoMetadata> {
                                                        override fun loadImage(imageView: ImageView?, img: ImageInfoMetadata?) {
                                                            if (imageView == null || img == null) return
                                                            val imageLoader = Config.getOrSetImageLoader(context)
                                                            val imageRequest = ImageUtil.createImageRequestWithRetryOnce(
                                                                context,
                                                                imageView,
                                                                imageLoader,
                                                                loggedInUser.cdnMessageRequestPath + img.name
                                                            )
                                                            imageLoader.enqueue(imageRequest)
                                                        }
                                                    }
                                                ).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            backgroundColor = MaterialTheme.colors.surface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .padding(vertical = 2.dp)
                                        ) {
                                            MessageUtil.ShowAttachedImage(
                                                imageInfoMetadata = image,
                                                context = context,
                                                loggedInUser
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Timestamp
                        val localDateTime = DateTime(item.entryTimeStamp, DateTimeZone.UTC)
                            .withZone(DateTimeZone.getDefault())
                        Text(
                            text = ViewModelUtil.getTimeAgo(localDateTime.toDateTime().millis, localDateTime) ?: "",
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(5.dp, 0.dp, 10.dp, 5.dp),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}