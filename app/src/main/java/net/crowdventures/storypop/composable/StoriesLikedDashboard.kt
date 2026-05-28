package net.crowdventures.storypop.composable

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
import androidx.compose.material.icons.filled.Favorite
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
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.composable.ProfileStoriesHeader.Companion.ArticleTile
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

public class StoriesLikedDashboard {
    companion object {
        @Composable
        private fun UserStoryLikedHeader() {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Liked",
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
        fun UserStoryLikedDashboard(
            storySavedViewModel: StorySavedViewModel,
            context: Context,
            likedItemsLazyListState: LazyListState,
            paddingValues: PaddingValues
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Fixed header
                UserStoryLikedHeader()

                // Scrollable content
                val userListItems: LazyPagingItems<StoryPublishedModel> =
                    storySavedViewModel.userLikedStories.collectAsLazyPagingItems()

                LazyColumn(
                    state = likedItemsLazyListState,
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
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No liked articles yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Articles you like will appear here",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Liked Items
                    items(
                        count = userListItems.itemCount,
                        key = { index -> "liked_${userListItems[index]?.slugTitle ?: index}" }
                    ) { index ->
                        val item = userListItems[index]
                        if (item != null) {
                            storySavedViewModel.updateExistingLikedStoriesDatabase(item)

                            val localPublishedDateTime = DateTime(
                                item.timestamp,
                                DateTimeZone.UTC
                            ).withZone(DateTimeZone.getDefault())

                            var expandedContextMenu by remember { mutableStateOf(false) }

                            val viewStoryCallback = {
                                StoryUtil.startContentActivity(item, context)
                                expandedContextMenu = false
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                // Liked Article Tile
                                ArticleTile(
                                    context = context,
                                    article = item,
                                    onClick = viewStoryCallback,
                                    isLikedDashboard = true,
                                    isDraftDashboard =false,
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
                                        // View Option
                                        DropdownMenuItem(onClick = viewStoryCallback) {
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
                                                    "View Story",
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