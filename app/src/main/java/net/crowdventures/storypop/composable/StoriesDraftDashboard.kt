package net.crowdventures.storypop.composable

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.composable.ProfileStoriesHeader.Companion.ArticleTile
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StorySavedModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel

class StoriesDraftDashboard {
    companion object {
        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        private fun UserStoryDraftHeader(
            activity: Activity,
            registerViewModel: RegisterViewModel,
            lifecycleOwner: LifecycleOwner,
            sharedPreferenceManager: SharedPreferenceManager
        ) {
            var loggedInUser by remember {
                mutableStateOf(Constants.loggedInUser)
            }
            registerViewModel.loggedInUser.observe(lifecycleOwner) { x ->
                loggedInUser = x
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = rememberImagePainter(
                        data = R.drawable.ic_edit_square,
                        builder = { crossfade(true) }
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Drafts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onSurface
                )
            }

            Divider(
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                thickness = 1.dp
            )
        }

        @Composable
        fun UserStoryDraftsDashboard(
            activity: Activity,
            storySavedViewModel: StorySavedViewModel,
            context: Context,
            draftItemsLazyListState: LazyListState,
            paddingValues: PaddingValues,
            registerViewModel: RegisterViewModel,
            lifecycleOwner: LifecycleOwner,
            sharedPreferenceManager: SharedPreferenceManager
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Fixed header - not part of LazyColumn
                UserStoryDraftHeader(activity, registerViewModel, lifecycleOwner, sharedPreferenceManager)

                // Scrollable content
                val userListItems: LazyPagingItems<StorySavedModel> =
                    storySavedViewModel.userNonPublishedStories.collectAsLazyPagingItems()

                LazyColumn(
                    state = draftItemsLazyListState,
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding()
                    )
                ) {
                    // Loading State
                    if (userListItems.loadState.refresh is LoadState.Loading && userListItems.itemCount == 0) {
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

                    // Empty State
                    else if (userListItems.loadState.refresh is LoadState.NotLoading &&
                        userListItems.loadState.append.endOfPaginationReached &&
                        userListItems.itemCount == 0) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = rememberImagePainter(
                                        data = R.drawable.ic_edit_square,
                                        builder = { crossfade(true) }
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No drafts yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Start writing your first story",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Draft Items
                    items(
                        count = userListItems.itemCount,
                        key = { index -> "draft_${userListItems[index]?.storyUUID ?: index}" }
                    ) { index ->
                        val item = userListItems[index]
                        if (item != null) {
                            var expandedContextMenu by remember { mutableStateOf(false) }
                            val loggedInUser = Constants.loggedInUser

                            val editStoryCallback = {
                                StoryUtil.startEditActivity(item, context)
                                expandedContextMenu = false
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                // Draft Article Tile
                                ArticleTile(
                                    context = context,
                                    article = item,
                                    onClick = editStoryCallback,
                                    isLikedDashboard = false,
                                    isDraftDashboard = true,
                                    editStoryForResult = null
                                )

                                // Options Menu Button - Properly positioned
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                ) {
                                    IconButton(
                                        onClick = { expandedContextMenu = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colors.surface.copy(alpha = 0.9f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                            contentDescription = "Options",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Dropdown Menu - Anchored to the button
                                    DropdownMenu(
                                        modifier = Modifier
                                            .width(180.dp)
                                            .background(MaterialTheme.colors.surface),
                                        expanded = expandedContextMenu,
                                        onDismissRequest = { expandedContextMenu = false }
                                    ) {
                                        // Preview Option
                                        DropdownMenuItem(onClick = {
                                            StoryUtil.startPreviewActivity(item, context)
                                            expandedContextMenu = false
                                        }) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.RemoveRedEye,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colors.secondary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    "Preview",
                                                    color = MaterialTheme.colors.onSurface,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        // Edit Option
                                        DropdownMenuItem(onClick = editStoryCallback) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colors.secondary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    "Edit",
                                                    color = MaterialTheme.colors.onSurface,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        // Delete Option
                                        DropdownMenuItem(onClick = {
                                            storySavedViewModel.deleteUserStory(item.storyUUID)
                                            expandedContextMenu = false
                                        }) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colors.error,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    "Delete",
                                                    color = MaterialTheme.colors.error,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}