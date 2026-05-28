package net.crowdventures.storypop.composable

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.UsersFollowingInfo
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import java.util.UUID

class FollowingDashboard {
    companion object {
        enum class FollowingHeaderItem {
            Stories, Following
        }

        val items = listOf(
            FollowingHeaderItem.Stories,
            FollowingHeaderItem.Following
        )

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun DrawHeaderFollowingToggleButtonGroup(
            storySavedViewModel: StorySavedViewModel,
            context: Context,
            lifecycleOwner: LifecycleOwner,
            usersFollowingInfo: UsersFollowingInfo,
            expandedSubList: MutableState<Int>,
            removedUsers: MutableState<List<String>>,
            navHostController: NavHostController,
            loggedInUser: AccountInfoFull
        ) {
            val storyDatabase = Constants.getStoryDatabase(context)
            val pendingUploadDao = storyDatabase.storyPendingUploadDao()
            val likedDao = storyDatabase.storyLikedDao()
            var newArticlesVisible by remember { mutableStateOf(false) }
            val onClick = fun() {
                newArticlesVisible = !newArticlesVisible
                if (newArticlesVisible) expandedSubList.value++
                else expandedSubList.value--
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp,
                backgroundColor = MaterialTheme.colors.surface,
                border = null
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var expandedContextMenu by remember { mutableStateOf(false) }

                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Wrap the Profile Avatar and Badge in a Box
                        Box(contentAlignment = Alignment.TopEnd) {
                            Card(
                                onClick = { StoryUtil.showAuthorProfilePage(context, usersFollowingInfo.username) },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                elevation = 2.dp
                            ) {
                                if (usersFollowingInfo.profileIcon != null) {
                                    val imageInfoMetadata = Gson().fromJson(
                                        usersFollowingInfo.profileIcon,
                                        ImageInfoMetadata::class.java
                                    )
                                    Profile.UserBadge(imageInfoMetadata, context, loggedInUser, size = 48.dp)
                                } else {
                                    Profile.DefaultBadge(size = 48.dp)
                                }
                            }

                            // 2. Place the badge here (it will now align to the Avatar's TopEnd)
                            if (usersFollowingInfo.newStories.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        // Offset moves it slightly outside the circle to the top-right
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(20.dp)
                                        .background(
                                            color = MaterialTheme.colors.secondary,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = usersFollowingInfo.newStories.size.toString(),
                                        color = MaterialTheme.colors.onSecondary,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Username and Badge
                        Box(modifier = Modifier.weight(1f)) {
                            // Username button
                            TextButton(
                                onClick = { StoryUtil.showAuthorProfilePage(context, usersFollowingInfo.username) },
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Text(
                                    text = usersFollowingInfo.username,
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = MaterialTheme.colors.onSurface,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                        }

                        // Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Message button
                            IconButton(
                                onClick = { ViewModelUtil.goToMessages(navHostController, storySavedViewModel, usersFollowingInfo) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mail,
                                    tint = MaterialTheme.colors.secondary,
                                    contentDescription = "Send message",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // More options button
                            IconButton(
                                onClick = { expandedContextMenu = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    contentDescription = "Options",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Dropdown Menu
                            DropdownMenu(
                                modifier = Modifier
                                    .width(160.dp)
                                    .background(MaterialTheme.colors.surface),
                                expanded = expandedContextMenu,
                                onDismissRequest = { expandedContextMenu = false }
                            ) {
                                // All Stories option
                                DropdownMenuItem(onClick = {
                                    expandedContextMenu = false
                                    StoryUtil.startListSearchActivity(
                                        context,
                                        "@${usersFollowingInfo.username}"
                                    )
                                }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ViewList,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "All Articles",
                                            color = MaterialTheme.colors.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                // Unfollow option
                                DropdownMenuItem(onClick = {
                                    storySavedViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable) {
                                        val authorUUIDString = Constants.loggedInUser?.user_uuid ?: return@launch
                                        val authorUUID = UUID.fromString(authorUUIDString)
                                        val pendingFollow = storyDatabase.storyPendingUploadDao()
                                            .getAllForAssociatedIDImmediate(authorUUID, usersFollowingInfo.username)
                                        val actualFollow = storyDatabase.userFollowedDao()
                                            .getFollowedForUserImmediate(authorUUID, usersFollowingInfo.username)

                                        if (pendingFollow.isNotEmpty() || actualFollow != null) {
                                            val followingPendingUpload = StoryPendingUpload(
                                                UUID.randomUUID(),
                                                Config.getStandardTimeUTCString(),
                                                "",
                                                PendingUploadType.UNFOLLOW,
                                                usersFollowingInfo.username,
                                                authorUUID
                                            )
                                            storyDatabase.storyPendingUploadDao().update(followingPendingUpload)
                                            ViewModelUtil.startResumeUploads(this, context.applicationContext)
                                            removedUsers.value = removedUsers.value + usersFollowingInfo.username
                                            storySavedViewModel.unfollowedUsers.add(usersFollowingInfo.username)
                                        }
                                        expandedContextMenu = false
                                    }
                                }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GroupRemove,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Unfollow",
                                            color = MaterialTheme.colors.error,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // New Stories Section
                    if (usersFollowingInfo.newStories.isNotEmpty()) {
                        Divider(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                            thickness = 1.dp
                        )

                        // Expand/Collapse Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { newArticlesVisible = !newArticlesVisible },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "New Articles (${usersFollowingInfo.newStories.size})",
                                    color = MaterialTheme.colors.secondary,
                                    fontSize = 13.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            }

                            IconButton(
                                onClick = {
                                    newArticlesVisible = !newArticlesVisible
                                    if (newArticlesVisible) expandedSubList.value++
                                    else expandedSubList.value--
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                enabled = usersFollowingInfo.newStories.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = if (newArticlesVisible) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                    tint = MaterialTheme.colors.secondary,
                                    contentDescription = if (newArticlesVisible) "Collapse" else "Expand",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // New Stories List
                        for (article in usersFollowingInfo.newStories) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = newArticlesVisible,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column {
                                    var likedArticle by remember {
                                        mutableStateOf(storySavedViewModel.likedArticleList.contains(article.slugTitle))
                                    }
                                    val activityForResultLauncher = rememberLauncherForActivityResult(
                                        ActivityResultContracts.StartActivityForResult()
                                    ) { result: ActivityResult ->
                                        likedArticle = result.resultCode == Activity.RESULT_OK
                                        if (likedArticle) storySavedViewModel.likedArticleList.add(article.slugTitle)
                                        else storySavedViewModel.likedArticleList.remove(article.slugTitle)
                                    }
                                    Article.Companion.ArticleItem(
                                        viewModel = storySavedViewModel,
                                        storyModel = article,
                                        onClick = { StoryUtil.startContentActivity(activityForResultLauncher, article, context, likedArticle) },
                                        hasLikedRecently = likedArticle,
                                        context = context,
                                        loggedInUser = loggedInUser,
                                        showShadow = false
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty State
                        androidx.compose.animation.AnimatedVisibility(
                            visible = newArticlesVisible,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                Divider(
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                                    thickness = 1.dp
                                )
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    text = "No new articles to show",
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}