package net.crowdventures.storypop.composable

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import net.crowdventures.storypop.viewmodels.StorySavedModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import java.io.File

class ProfileStoriesHeader {
    companion object {
        enum class ArticleHeaderItem {
            Draft, Published, Liked
        }

        val items = listOf(
            ArticleHeaderItem.Draft,
            ArticleHeaderItem.Published,
            ArticleHeaderItem.Liked
        )

        @Composable
        fun DrawHeaderStoriesToggleButtonGroup(
            activity: Activity,
            storySavedViewModel: StorySavedViewModel,
            context: Context,
            lazyDraftItemsState: LazyListState,
            lazyPublishedItemsState: LazyListState,
            lazyPendingPublishedItemsListState: LazyListState,
            lazyLikedItemsState: LazyListState,
            lifecycleOwner: LifecycleOwner,
            paddingValues: PaddingValues,
            registerViewModel: RegisterViewModel,
            sharedPreferenceManager: SharedPreferenceManager
        ) {
            var selectedIndex by remember { mutableStateOf(0) }
            val cornerRadius = 8.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Tab buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items.forEachIndexed { index, item ->
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            onClick = { selectedIndex = index },
                            shape = when (index) {
                                0 -> RoundedCornerShape(
                                    topStart = cornerRadius,
                                    topEnd = 0.dp,
                                    bottomStart = cornerRadius,
                                    bottomEnd = 0.dp
                                )

                                items.size - 1 -> RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = cornerRadius,
                                    bottomStart = 0.dp,
                                    bottomEnd = cornerRadius
                                )

                                else -> RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selectedIndex == index) {
                                    MaterialTheme.colors.secondary
                                } else {
                                    MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                }
                            ),
                            colors = if (selectedIndex == index) {
                                ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colors.secondary
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = Color.Transparent,
                                    contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Edit
                                        1 -> Icons.Default.CheckCircle
                                        2 -> Icons.Default.Favorite
                                        else -> Icons.Default.Favorite
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedIndex == index)
                                        MaterialTheme.colors.secondary
                                    else
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedIndex == index) FontWeight.Medium else FontWeight.Normal,
                                    color = if (selectedIndex == index)
                                        MaterialTheme.colors.secondary
                                    else
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedIndex) {
                        ArticleHeaderItem.Draft.ordinal ->
                            StoriesDraftDashboard.UserStoryDraftsDashboard(
                                activity = activity,
                                storySavedViewModel = storySavedViewModel,
                                context = context,
                                draftItemsLazyListState = lazyDraftItemsState,
                                paddingValues = paddingValues,
                                registerViewModel = registerViewModel,
                                lifecycleOwner = lifecycleOwner,
                                sharedPreferenceManager = sharedPreferenceManager
                            )

                        ArticleHeaderItem.Published.ordinal ->
                            StoriesPublishedDashboard.UserStoryPublishedDashboard(
                                storySavedViewModel = storySavedViewModel,
                                context = context,
                                publishedItemsLazyListState = lazyPublishedItemsState,
                                lazyPendingPublishedItemsListState = lazyPendingPublishedItemsListState,
                                lifecycleOwner = lifecycleOwner,
                                paddingValues = paddingValues
                            )

                        ArticleHeaderItem.Liked.ordinal ->
                            StoriesLikedDashboard.UserStoryLikedDashboard(
                                storySavedViewModel = storySavedViewModel,
                                context = context,
                                likedItemsLazyListState = lazyLikedItemsState,
                                paddingValues = paddingValues
                            )
                    }
                }

                Divider(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
            }
        }

        @Composable
        fun ArticleTile(
            context: Context,
            article: StorySavedModel,
            onClick: () -> Unit,
            isLikedDashboard: Boolean,
            isDraftDashboard: Boolean,
            editStoryForResult: ManagedActivityResultLauncher<Intent, ActivityResult>?
        ) {
            val isPublished = article is StoryPublishedModel

            // Check status states
            val isPendingReview = !article.isReviewed && !isDraftDashboard && !(article is StoryPublishedModel && !article.rejectionReason.isNullOrEmpty())
            val isRejected = article is StoryPublishedModel && !article.rejectionReason.isNullOrEmpty()
            val rejectionReason = if (isRejected && article is StoryPublishedModel) article.rejectionReason else ""

            var showRejectionDetailsDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp,
                backgroundColor = MaterialTheme.colors.surface,
                border = if (isRejected) BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFE53E3E).copy(alpha = 0.3f)
                ) else null
            ) {
                Column {
                    // Status Banner (only for pending review or rejected)
                    if (isPendingReview) {
                        // Pending Review Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.secondary.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pulsing animation for the icon
                                val infiniteTransition = rememberInfiniteTransition()
                                val iconColor by infiniteTransition.animateColor(
                                    initialValue = MaterialTheme.colors.secondary,
                                    targetValue = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = "Pending Review",
                                    tint = iconColor,
                                    modifier = Modifier.size(14.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = "Pending Review",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.secondary,
                                    letterSpacing = 0.3.sp
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colors.secondary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Awaiting Moderation",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colors.secondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else if (isRejected) {
                        // Rejected Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE53E3E).copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable { showRejectionDetailsDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Rejected",
                                        tint = Color(0xFFE53E3E),
                                        modifier = Modifier.size(14.dp)
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = "Article Rejected",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE53E3E),
                                        letterSpacing = 0.3.sp
                                    )
                                }

                                // View Details button
                                Text(
                                    text = "View Details",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colors.secondary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { showRejectionDetailsDialog = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp).offset(-30.dp,0.dp)
                                )
                            }
                        }
                    }

                    // Main Content Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Article Image (if available)
                        if (article.stylingInfo.titleBackgroundImage != null) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                            ) {
                                ArticleImage(
                                    storyModel = article,
                                    context = context,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Overlay for rejected articles
                                if (isRejected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFE53E3E).copy(alpha = 0.2f))
                                    )
                                } else if (isPendingReview) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colors.secondary.copy(alpha = 0.15f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        } else {
                            // Placeholder when no image
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isRejected -> Color(0xFFE53E3E).copy(alpha = 0.2f)
                                            isPendingReview -> MaterialTheme.colors.secondary.copy(alpha = 0.2f)
                                            else -> MaterialTheme.colors.primaryVariant
                                        }
                                    )
                            ) {
                                Icon(
                                    imageVector = when {
                                        isRejected -> Icons.Default.Warning
                                        !isPublished -> Icons.Default.Edit
                                        isLikedDashboard -> Icons.Default.Favorite
                                        isPendingReview -> Icons.Default.HourglassEmpty
                                        else -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isRejected -> Color(0xFFE53E3E)
                                        isPendingReview -> MaterialTheme.colors.secondary
                                        else -> MaterialTheme.colors.onPrimary.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .align(Alignment.Center)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        // Article Details
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Title with status indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = article.storyTitle.ifEmpty { article.emptyTitle },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isRejected) Color(0xFFE53E3E) else MaterialTheme.colors.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // Small status indicator for compact view
                                if (isPendingReview) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = "Pending Review",
                                        tint = MaterialTheme.colors.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else if (isRejected) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Rejected",
                                        tint = Color(0xFFE53E3E),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Preview text
                            if (article.contentText.isNotEmpty()) {
                                Text(
                                    text = article.contentText.take(80) +
                                            if (article.contentText.length > 80) "..." else "",
                                    fontSize = 13.sp,
                                    color = if (isRejected)
                                        Color(0xFFE53E3E).copy(alpha = 0.7f)
                                    else
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Tags
                            if (article.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    article.tags.take(2).forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isRejected)
                                                        Color(0xFFE53E3E).copy(alpha = 0.1f)
                                                    else
                                                        MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                fontSize = 10.sp,
                                                color = if (isRejected)
                                                    Color(0xFFE53E3E)
                                                else
                                                    MaterialTheme.colors.secondary
                                            )
                                        }
                                    }
                                    if (article.tags.size > 2) {
                                        Text(
                                            text = "+${article.tags.size - 2}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        // Stats (for published articles)
                        if (isPublished && article is StoryPublishedModel) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Likes
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isRejected)
                                            Color(0xFFE53E3E).copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = article.hearts.toString(),
                                        fontSize = 12.sp,
                                        color = if (isRejected)
                                            Color(0xFFE53E3E).copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                // Comments
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Comment,
                                        contentDescription = null,
                                        tint = if (isRejected)
                                            Color(0xFFE53E3E).copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = article.comments.toString(),
                                        fontSize = 12.sp,
                                        color = if (isRejected)
                                            Color(0xFFE53E3E).copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Rejection Details Dialog
            if (showRejectionDetailsDialog && isRejected && article is StoryPublishedModel) {
                AlertDialog(
                    onDismissRequest = { showRejectionDetailsDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE53E3E),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Article Rejected",
                                color = Color(0xFFE53E3E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Your article did not meet our community guidelines.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colors.onSurface,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "Rejection Reason:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                backgroundColor = Color(0xFFE53E3E).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = rejectionReason?:"",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colors.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            Text(
                                text = "\nPlease revise your article according to the feedback above and resubmit for review.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showRejectionDetailsDialog = false }
                        ) {
                            Text("Got it", color = MaterialTheme.colors.secondary)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showRejectionDetailsDialog = false
                                editStoryForResult?.launch(
                                    StoryUtil.getStartEditActivityIntent(article, context)
                                )
                            }
                        ) {
                            Text("Edit Article", color = MaterialTheme.colors.secondary)
                        }
                    }
                )
            }
        }
        @Composable
        private fun ArticleImage(
            storyModel: StorySavedModel,
            context: Context,
            modifier: Modifier = Modifier,
        ) {
            if (storyModel.stylingInfo.titleBackgroundImage != null) {
                if (storyModel is StoryPublishedModel) {
                    AsyncImage(
                        model = storyModel.imageRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val imageInfoMetadata = Gson().fromJson(
                        storyModel.stylingInfo.titleBackgroundImage,
                        ImageInfoMetadata::class.java
                    )
                    val imageName = if (imageInfoMetadata.minWidth != null)
                        Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name
                    else imageInfoMetadata.name

                    val imageRequestPath =
                        ImageUtil.getCreateImageRootDir(context).absolutePath + "/"
                    val image = rememberAsyncImagePainter(
                        model = imageRequestPath + imageName,
                        contentScale = ContentScale.Crop
                    )
                    Image(
                        painter = image,
                        contentDescription = null,
                        modifier = modifier,
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = modifier
                        .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                )
            }
        }
    }
}