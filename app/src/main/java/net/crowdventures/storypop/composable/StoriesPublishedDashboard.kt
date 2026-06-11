package net.crowdventures.storypop.composable

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.rememberImagePainter
import net.crowdventures.storypop.ArticleContentEditActivity
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.composable.ProfileStoriesHeader.Companion.ArticleTile
import net.crowdventures.storypop.paging.StoryFetchType
import net.crowdventures.storypop.paging.StorySource
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import net.crowdventures.storypop.viewmodels.StorySavedModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.UUID

class StoriesPublishedDashboard {
    companion object {
        @Composable
        fun UserStoryPublishedHeader() {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = rememberImagePainter(
                        data = R.drawable.ic_tick_symbol,
                        builder = { crossfade(true) }
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Published",
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
        fun UserStoryPublishedDashboard(
            storySavedViewModel: StorySavedViewModel,
            context: Context,
            publishedItemsLazyListState: LazyListState,
            lazyPendingPublishedItemsListState: LazyListState,
            lifecycleOwner: LifecycleOwner,
            paddingValues: PaddingValues
        ) {
            val authorUUIDString = Constants.loggedInUser?.user_uuid ?: return
            val authorUUID = UUID.fromString(authorUUIDString)
            val storyDatabase = Constants.getStoryDatabase(context)
            var updatedStorySlugTitle by remember { mutableStateOf<String?>(null) }

            val editStoryForResult = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK &&
                    result.data?.hasExtra(ArticleContentEditActivity.UPDATED_STORY_SLUG_TITLE_RESULT) == true
                ) {
                    updatedStorySlugTitle = result.data?.getStringExtra(
                        ArticleContentEditActivity.UPDATED_STORY_SLUG_TITLE_RESULT
                    )
                }
            }

            val pendingStoryFlow = remember { storyDatabase.storyDao().getAllPendingCreateUpdate() }
            val userListPendingItems: LazyPagingItems<StorySavedModel> =
                storySavedViewModel.userPendingPublishedStories.collectAsLazyPagingItems()
            val userListItems: LazyPagingItems<StoryPublishedModel> =
                storySavedViewModel.userPublishedStories.collectAsLazyPagingItems()

            LaunchedEffect(pendingStoryFlow) {
                var lastSize = 0
                pendingStoryFlow.collect { list ->
                    val currentSize = list.size
                    if (lastSize > 0 && currentSize == 0) {
                        updatedStorySlugTitle = null
                        userListItems.refresh()
                    }
                    lastSize = currentSize
                }
            }

            LaunchedEffect(updatedStorySlugTitle) {
                if (updatedStorySlugTitle != null) {
                    userListItems.refresh()
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Fixed header
                UserStoryPublishedHeader()

                // Scrollable content
                LazyColumn(
                    state = lazyPendingPublishedItemsListState,
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding()
                    )
                ) {
                    // Pending Items (uploading)
                    if (userListPendingItems.itemCount > 0) {
                        items(
                            count = userListPendingItems.itemCount,
                            key = { index -> "pending_${userListPendingItems[index]?.storyUUID ?: "pending_" +index}" }
                        ) { index ->
                            val item = userListPendingItems[index]
                            if (item != null) {
                                val currentDft = StoryUtil.getDateTimeFormatter(item.updatedDateTime)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 2.dp,
                                    backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.7f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (item.storyTitle.isNullOrEmpty()) item.emptyTitle else item.storyTitle,
                                                color = MaterialTheme.colors.onSurface,
                                                fontWeight = FontWeight.Medium,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 1,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "Published ${item.updatedDateTime.toString(currentDft)} • uploading...",
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colors.secondary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Empty State
                    if (userListItems.loadState.refresh is LoadState.NotLoading &&
                        userListPendingItems.loadState.refresh is LoadState.NotLoading &&
                        userListItems.loadState.append.endOfPaginationReached &&
                        userListItems.itemCount == 0 &&
                        userListPendingItems.itemCount == 0) {

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = rememberImagePainter(
                                        data = R.drawable.ic_tick_symbol,
                                        builder = { crossfade(true) }
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No published articles yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your published articles will appear here",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Loading State
                    else if (userListItems.loadState.refresh is LoadState.Loading && userListItems.itemCount == 0) {
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

                    // Published Items
                    items(
                        count = userListItems.itemCount,
                        key = { index -> "published_${userListItems[index]?.slugTitle ?: index}" }
                    ) { index ->
                        val item = userListItems[index]
                        if (item != null) {
                            var expandedContextMenu by remember { mutableStateOf(false) }

                            val isCurrentlyUpdating = updatedStorySlugTitle == item.slugTitle
                            val editStoryCallback = {
                                editStoryForResult.launch(
                                    StoryUtil.getStartEditActivityIntent(item, context)
                                )
                                expandedContextMenu = false
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                // Published Article Tile
                                ArticleTile(
                                    context = context,
                                    article = item,
                                    onClick = {
                                        if (!isCurrentlyUpdating) {
                                            StoryUtil.startContentActivity(item, context)
                                        }
                                    },
                                    isLikedDashboard = false,
                                    isDraftDashboard = false,
                                    editStoryForResult = editStoryForResult
                                )

                                // Options Menu Button - Properly positioned
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                ) {
                                    if (isCurrentlyUpdating) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colors.secondary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
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
                                    }

                                    // Dropdown Menu - Anchored to the button
                                    DropdownMenu(
                                        modifier = Modifier
                                            .width(180.dp)
                                            .background(MaterialTheme.colors.surface),
                                        expanded = expandedContextMenu,
                                        onDismissRequest = { expandedContextMenu = false }
                                    ) {
                                        // View Option
                                        DropdownMenuItem(onClick = {
                                            StoryUtil.startContentActivity(item, context)
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
                                                    "View",
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

                                        // Share Option
                                        DropdownMenuItem(onClick = {
                                            StoryUtil.shareArticle(item.slugTitle, context)
                                            expandedContextMenu = false
                                        }) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colors.secondary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    "Share",
                                                    color = MaterialTheme.colors.onSurface,
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