package net.crowdventures.storypop.composable

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.UsersFollowingInfo
import net.crowdventures.storypop.paging.UsersFollowedStorySource
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import java.net.URI
import java.util.UUID

class Following {
    companion object {
        fun sendInvite(loggedInUser: AccountInfoFull, context: Context) {
            val token = Base64.encodeToString(
                loggedInUser.username.toByteArray(Charsets.UTF_8),
                Base64.NO_PADDING
            )
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            val shareMessage =
                context.getString(
                    R.string.you_are_invited_to_follow_the_link_to_join_and_claim_your_reward_https_storypop_net_invite_token,
                    context.getString(R.string.app_name),
                    URI(loggedInUser.imageStaticPath).host,
                    token
                )
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    "choose one"
                )
            )
        }

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun FollowingView(
            lifecycleOwner: LifecycleOwner,
            context: Context,
            storySavedViewModel: StorySavedViewModel,
            registerViewModel: RegisterViewModel,
            paddingValues: PaddingValues,
            navHostController: NavHostController
        ) {
            val currentUser = Constants.loggedInUser ?: return
            var expandedContextMenu by remember { mutableStateOf(false) }
            var showAddUserDialog by remember {
                mutableStateOf(false)
            }
            var refreshFollowedUserList by remember {
                mutableStateOf(false)
            }
            var loggedInUser by remember {
                mutableStateOf(currentUser)
            }
            var followedStoriesLoaded by remember {
                mutableStateOf(registerViewModel.userFollowedNotificationsLoaded.value ?: false)
            }
            var followedStoriesConsumed by remember {
                mutableStateOf(registerViewModel.userFollowedNotificationsConsumed.value ?: false)
            }
            var followedStoriesPending by remember {
                mutableStateOf(registerViewModel.userFollowedNotificationsPending.value ?: false)
            }
            var hasNotificationItems by remember {
                mutableStateOf(storySavedViewModel.hasNotificationItems)
            }

            registerViewModel.userFollowedNotificationsLoaded.observe(lifecycleOwner) { x ->
                followedStoriesLoaded = x
                refreshFollowedUserList = false
            }
            registerViewModel.userFollowedNotificationsConsumed.observe(lifecycleOwner) { x ->
                followedStoriesConsumed = x
            }
            registerViewModel.userFollowedNotificationsPending.observe(lifecycleOwner) { x ->
                followedStoriesPending = x
                if (x) storySavedViewModel.hasNotificationItems = x
                if (x) hasNotificationItems = true
            }

            registerViewModel.loggedInUser.observe(lifecycleOwner) { x ->
                //monitor for user changes only if we dont have pending notifications
                if (!hasNotificationItems || !storySavedViewModel.hasNotificationItems) {
                    if (x != null) {
                        loggedInUser = x
                        storySavedViewModel.userFollowedStories =
                            Pager(PagingConfig(pageSize = 6)) {
                                UsersFollowedStorySource(storySavedViewModel.storySavedViewModelRepository.context,
                                    storySavedViewModel.storyDatabase, registerViewModel,
                                    storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager
                                )
                            }.flow.cachedIn(storySavedViewModel.viewModelScope)
                    }
                    refreshFollowedUserList = false
                }
            }

            if (showAddUserDialog) {
                ComposableUtil.AddFollowedUserDialog(loggedInUser,
                    storySavedViewModel,
                    lifecycleOwner,
                    onDismissRequest = { showAddUserDialog = false }) { userToFollow ->
                    showAddUserDialog = false
                    storySavedViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                        val followingPendingUpload = StoryPendingUpload(
                            UUID.randomUUID(),
                            net.crowdventures.storypop.Config.getStandardTimeUTCString(),
                            userToFollow.profileIcon ?: "",
                            PendingUploadType.FOLLOW,
                            userToFollow.username,
                            UUID.fromString(loggedInUser.user_uuid)
                        )
                        storySavedViewModel.storyDatabase.storyPendingUploadDao()
                            .update(followingPendingUpload)
                        ViewModelUtil.startResumeUploads(
                            storySavedViewModel.viewModelScope,
                            storySavedViewModel.storySavedViewModelRepository.context.applicationContext
                        )
                    }
                }
            }
            val removedUsers = remember {
                mutableStateOf(storySavedViewModel.unfollowedUsers.toList())
            }
            val hasCache =
                storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager.hasUsersFollowedCached()

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Use exact padding as Notifications header
                        .padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp), // Padding for the right side button
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Title - Icon + Text (Left Aligned)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Group,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp), // Exact same size as Notifications
                                tint = MaterialTheme.colors.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Following",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.onSurface
                            )
                        }

                        // Add/Invite Button Container (Right Aligned)
                        Box(
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Button(
                                onClick = { expandedContextMenu = true },
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color.Transparent,
                                    contentColor = MaterialTheme.colors.secondary
                                ),
                                elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colors.secondary.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Add a friend",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Now this menu is anchored to the Box above, which is on the right
                            DropdownMenu(
                                modifier = Modifier
                                    .width(170.dp)
                                    .background(MaterialTheme.colors.surface),
                                expanded = expandedContextMenu,
                                onDismissRequest = { expandedContextMenu = false }
                            ) {
                                DropdownMenuItem(onClick = {
                                    expandedContextMenu = false
                                    showAddUserDialog = true
                                }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.GroupAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Add user",
                                            color = MaterialTheme.colors.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                DropdownMenuItem(onClick = {
                                    expandedContextMenu = false
                                    sendInvite(loggedInUser, context)
                                }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Mail,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Send invite",
                                            color = MaterialTheme.colors.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                Divider(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp
                )



                val userPendingFollowingListItems: LazyPagingItems<UsersFollowingInfo> =
                    storySavedViewModel.userPendingUserFollowed.collectAsLazyPagingItems()

                val userFollowingListItemsCached = storySavedViewModel.userFollowedStoriesCached
                val userFollowingListItemsValue =
                    if (!hasNotificationItems && hasCache) {
                        storySavedViewModel.userFollowedStoriesCached
                    } else {
                        storySavedViewModel.userFollowedStories
                    }
                val userFollowingListCachedItems =
                    userFollowingListItemsCached.collectAsLazyPagingItems()
                val userFollowingListItems = userFollowingListItemsValue.collectAsLazyPagingItems()

                val expandedSubList = remember {
                    mutableStateOf(0)
                }

                LazyColumn(
                    modifier = Modifier,
                    contentPadding = paddingValues
                ) {
                    // New Stories Notification Banner
                    if (!(userFollowingListItems.loadState.refresh is LoadState.Loading && userFollowingListItems.itemCount == 0)
                        && !followedStoriesConsumed && followedStoriesLoaded && !refreshFollowedUserList
                        && followedStoriesPending
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, MaterialTheme.colors.secondary)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "New articles are waiting!",
                                        color = MaterialTheme.colors.secondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            refreshFollowedUserList = true
                                            storySavedViewModel.hasNotificationItems = false
                                            followedStoriesLoaded = false
                                            followedStoriesConsumed = true
                                            hasNotificationItems = false
                                            registerViewModel.loggedInUser.value = loggedInUser
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = MaterialTheme.colors.secondary,
                                            contentColor = MaterialTheme.colors.onSecondary
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            "Load New Articles",
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Loading State
                    if (userFollowingListItems.loadState.refresh is LoadState.Loading && userFollowingListItems.itemCount == 0) {
                        item {
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
                    }
                    // Empty State with Invite Card
                    else if (userFollowingListItems.loadState.refresh is LoadState.NotLoading &&
                        userFollowingListItems.loadState.append.endOfPaginationReached &&
                        userFollowingListItems.itemCount == 0) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "You are currently not following anyone.",
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Invite Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 4.dp,
                                    backgroundColor = MaterialTheme.colors.surface
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Header Text
                                        Text(
                                            stringResource(R.string.support_independent_journalism),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colors.secondary
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            stringResource(R.string.help_us_reach_more_readers_invite_up_to_2_people_to_join_our_platform_both_you_and_each_new_member_will_receive_500_storypoints_upon_their_registration),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = { sendInvite(loggedInUser, context) },
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = MaterialTheme.colors.secondary,
                                                contentColor = MaterialTheme.colors.onSecondary
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        ) {
                                            Text(
                                                "Send Invitation",
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Cached Following Items
                    this.items(
                        userFollowingListCachedItems.itemCount,
                        contentType = { _ -> "FollowedUser" }) { index ->
                        val item = userFollowingListCachedItems[index]
                        if (item != null && !removedUsers.value.contains(item.username) &&
                            !userPendingFollowingListItems.itemSnapshotList.any { z -> z != null && z.username == item.username } &&
                            !userFollowingListItems.itemSnapshotList.any { k -> k != null && k.username == item.username }
                        ) {
                            FollowingDashboard.DrawHeaderFollowingToggleButtonGroup(
                                storySavedViewModel,
                                context,
                                lifecycleOwner,
                                item,
                                expandedSubList,
                                removedUsers,
                                navHostController,
                                loggedInUser
                            )
                        }
                    }

                    // Pending Following Items
                    this.items(
                        userPendingFollowingListItems.itemCount,
                        contentType = { _ -> "FollowedUser" }) { index ->
                        val item = userPendingFollowingListItems[index]
                        if (item != null && !removedUsers.value.contains(item?.username)) {
                            FollowingDashboard.DrawHeaderFollowingToggleButtonGroup(
                                storySavedViewModel,
                                context,
                                lifecycleOwner,
                                item,
                                expandedSubList,
                                removedUsers,
                                navHostController,
                                loggedInUser
                            )
                        }
                    }

                    // Following Items
                    this.items(
                        userFollowingListItems.itemCount,
                        contentType = { _ -> "FollowedUser" }) { index ->
                        val item = userFollowingListItems[index]
                        if (item != null && !removedUsers.value.contains(item?.username)) {
                            FollowingDashboard.DrawHeaderFollowingToggleButtonGroup(
                                storySavedViewModel,
                                context,
                                lifecycleOwner,
                                item,
                                expandedSubList,
                                removedUsers,
                                navHostController,
                                loggedInUser
                            )
                        }
                    }
                }
            }
        }
    }
}