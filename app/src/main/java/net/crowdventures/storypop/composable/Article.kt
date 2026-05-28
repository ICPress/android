package net.crowdventures.storypop.composable

import android.content.Context
import android.net.Uri
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.size.OriginalSize
import coil.size.Scale
import com.google.gson.Gson
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import me.saket.extendedspans.ExtendedSpans
import me.saket.extendedspans.drawBehind
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.libs.RoundedCornerSpanPainterCustom
import net.crowdventures.storypop.util.EndpointAccountHandler
import net.crowdventures.storypop.util.EndpointAdminHandler
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.SuccessCallback
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import net.crowdventures.storypop.viewmodels.StorySavedModel
import java.io.File

class Article {
    companion object {
        val fonts = FontFamily(
            Font(net.crowdventures.storypop.R.font.mplus2_regular, weight = FontWeight.Normal),
            Font(net.crowdventures.storypop.R.font.mplus2_medium, weight = FontWeight.Bold)
        )

        // ===== PROFESSIONAL BLUE THEME COLORS =====
        private val LightCardBackground = Color(0xFFFFFFFF)
        private val LightSurface = Color(0xFFF5F7FA)
        private val LightBorder = Color(0xFFE1E5EB)
        private val LightPrimaryText = Color(0xFF1E1E24)
        private val LightSecondaryText = Color(0xFF5A6570)
        private val LightAccent = Color(0xFF3E92CC)
        private val LightTagBackground = Color(0xFFE8F0FE)
        private val LightTagText = Color(0xFF0A2463)

        private val DarkCardBackground = Color(0xFF121826)
        private val DarkSurface = Color(0xFF0A0E17)
        private val DarkBorder = Color(0xFF2A3655)
        private val DarkPrimaryText = Color(0xFFE6EDF3)
        private val DarkSecondaryText = Color(0xFF9FB3C8)
        private val DarkAccent = Color(0xFF58A6FF)
        private val DarkTagBackground = Color(0xFF1E293B)
        private val DarkTagText = Color(0xFFD1DCE6)

        @Composable
        private fun getCardBackground(): Color =
            if (isSystemInDarkTheme()) DarkCardBackground else LightCardBackground

        @Composable
        private fun getBorderColor(): Color =
            if (isSystemInDarkTheme()) DarkBorder else LightBorder

        @Composable
        private fun getPrimaryTextColor(): Color =
            if (isSystemInDarkTheme()) DarkPrimaryText else LightPrimaryText

        @Composable
        private fun getSecondaryTextColor(): Color =
            if (isSystemInDarkTheme()) DarkSecondaryText else LightSecondaryText

        @Composable
        private fun getAccentColor(): Color =
            if (isSystemInDarkTheme()) DarkAccent else LightAccent

        @Composable
        private fun getTagBackground(): Color =
            if (isSystemInDarkTheme()) DarkTagBackground else LightTagBackground

        @Composable
        private fun getTagTextColor(): Color =
            if (isSystemInDarkTheme()) DarkTagText else LightTagText


        @Composable
        fun GetArticleCardInfo(
            story: StorySavedModel?,
            hasLikedRecently: Boolean,
            context: Context
        ) {
            val likes = if (story is StoryPublishedModel) story.hearts else 0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getCardBackground())
                    .padding(12.dp)
            ) {
                // Author Row - Top section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Author info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(getAccentColor().copy(alpha = 0.15f))
                        ) {
                            if (story?.authorName == Constants.loggedInUser?.username) {
                                if (story == null) {
                                    // Placeholder
                                } else {
                                    Profile.UserBadge(
                                        loggedInUser = Constants.loggedInUser,
                                        context = context,
                                        size = 32.dp
                                    )
                                }
                            } else {
                                if (story is StoryPublishedModel) {
                                    Profile.UserBadgeForStory(
                                        storyPublishedModel = story,
                                        context = context,
                                        size = 32.dp
                                    )
                                } else {
                                    Profile.DefaultBadge(size = 32.dp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Author name
                        if (story != null) {
                            Text(
                                text = story.authorName ?: "Anonymous",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = getPrimaryTextColor(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Location and stats row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Location if available
                        if (!story?.location.isNullOrEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PinDrop,
                                    contentDescription = "Location",
                                    tint = getSecondaryTextColor(),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = story!!.location,
                                    fontSize = 12.sp,
                                    color = getSecondaryTextColor(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }

                        // Likes
                        if (story is StoryPublishedModel && (story.hearts > 0 || hasLikedRecently)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Likes",
                                    tint = if (hasLikedRecently) Color(0xFFE53E3E) else getSecondaryTextColor(),
                                    modifier = Modifier.size(14.dp)
                                )

                                val loggedInUser = Constants.loggedInUser
                                if (loggedInUser != null && hasLikedRecently) {
                                    val infiniteTransition = rememberInfiniteTransition()
                                    val color by infiniteTransition.animateColor(
                                        initialValue = getSecondaryTextColor(),
                                        targetValue = Color(0xFFE53E3E),
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )
                                    Text(
                                        text = StoryUtil.likesToStringNumber(likes + 1),
                                        fontSize = 12.sp,
                                        color = color,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                } else {
                                    Text(
                                        text = StoryUtil.likesToStringNumber(likes),
                                        fontSize = 12.sp,
                                        color = getSecondaryTextColor(),
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }

                        // Comments
                        if (story is StoryPublishedModel && story.comments > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Comment,
                                    contentDescription = "Comments",
                                    tint = getSecondaryTextColor(),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = StoryUtil.likesToStringNumber(story.comments),
                                    fontSize = 12.sp,
                                    color = getSecondaryTextColor(),
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Content preview - only show if there's text
                if (!story?.contentText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = story!!.contentText,
                        fontSize = 14.sp,
                        color = getSecondaryTextColor(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                // Tags
                if (!story?.tags.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tagsToShow = story!!.tags.take(3)
                        tagsToShow.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(getTagBackground(), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = null,
                                        tint = getTagTextColor(),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = getTagTextColor(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (story.tags.size > 3) {
                            Text(
                                text = "+${story.tags.size - 3}",
                                fontSize = 11.sp,
                                color = getSecondaryTextColor(),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun ArticleItem(
            viewModel: ViewModel,
            storyModel: StorySavedModel,
            onClick: () -> Unit,
            hasLikedRecently: Boolean,
            context: Context,
            loggedInUser: AccountInfoFull,
            showShadow: Boolean = true
        ) {
            var showProgressDialog by remember { mutableStateOf(false) }
            if (showProgressDialog) ComposableUtil.ShowProgressDialog()

            var requestedTermination by remember { mutableStateOf(false) }
            var expandedContextMenu by remember { mutableStateOf(false) }
            var showDeleteTerminateDialog by remember { mutableStateOf(false) }
            var showRejectionDetailsDialog by remember { mutableStateOf(false) }

            val terminationDeleteCallback = object : SuccessCallback<Unit> {
                override fun onSuccess(vararg param: Unit) {
                    showProgressDialog = false
                }
                override fun onFailure(reason: Any?) {
                    showProgressDialog = false
                }
            }

            if (showDeleteTerminateDialog && storyModel is StoryPublishedModel) {
                ComposableUtil.QuestionDialog(
                    title = if (requestedTermination) "Terminate account: ${storyModel.authorName}" else "Delete Story?",
                    question = if (requestedTermination) "Terminate account ${storyModel.authorName} for policy breach?" else "Delete Story ${storyModel.slugTitle} and send warning email to ${storyModel.authorName}?",
                    confirmText = "Confirm",
                    onDismissRequest = {
                        showDeleteTerminateDialog = false
                        requestedTermination = false
                    }) {
                    showDeleteTerminateDialog = false
                    showProgressDialog = true
                    viewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                        if (requestedTermination) {
                            EndpointAdminHandler.deleteArticleTerminateAccount(
                                context,
                                storyModel.authorName,
                                terminationDeleteCallback
                            )
                        } else {
                            EndpointAdminHandler.deleteArticleTerminateAccount(
                                context,
                                storyModel.authorName,
                                terminationDeleteCallback,
                                storyModel.slugTitle
                            )
                        }
                    }
                }
            }

            val configuration = LocalConfiguration.current
            val extendedSpans = remember {
                ExtendedSpans(
                    RoundedCornerSpanPainterCustom(
                        padding = RoundedCornerSpanPainterCustom.TextPaddingValues(0.sp),
                        topMargin = 0.sp,
                        bottomMargin = 0.sp
                    )
                )
            }

            val isAdmin = storyModel is StoryPublishedModel && Constants.loggedInUser?.getRoleFromToken() == Constants.ADMIN_ROLE

            // Check if article is pending review or rejected
            val isPublished = storyModel is StoryPublishedModel
            val isPendingReview = !storyModel.isReviewed && !(storyModel is StoryPublishedModel && !storyModel.rejectionReason.isNullOrEmpty())
            val isRejected = storyModel is StoryPublishedModel && !storyModel.rejectionReason.isNullOrEmpty()
            val rejectionReason = if (storyModel is StoryPublishedModel) storyModel.rejectionReason else ""
            val articleCategory = if (storyModel is StoryPublishedModel) storyModel.category else null

            val cardModifier = if (isAdmin) {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { expandedContextMenu = true }
                    )
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable(onClick = onClick)
            }

            Card(
                modifier = cardModifier,
                shape = RoundedCornerShape(12.dp),
                elevation = if (showShadow) 4.dp else 0.dp,
                backgroundColor = getCardBackground(),
                border = BorderStroke(
                    width = 0.5.dp,
                    color = if (isRejected) Color(0xFFE53E3E).copy(alpha = 0.3f) else getBorderColor()
                )
            ) {
                Column {
                    // Admin context menu
                    DropdownMenu(
                        modifier = Modifier
                            .width(200.dp)
                            .background(getCardBackground()),
                        expanded = expandedContextMenu,
                        onDismissRequest = { expandedContextMenu = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            expandedContextMenu = false
                            showDeleteTerminateDialog = true
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = getAccentColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Delete article",
                                    color = getPrimaryTextColor(),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        DropdownMenuItem(onClick = {
                            expandedContextMenu = false
                            requestedTermination = true
                            showDeleteTerminateDialog = true
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = null,
                                    tint = getAccentColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Terminate Account",
                                    color = getPrimaryTextColor(),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Status Banner (Pending Review or Rejected)
                    if (isPendingReview) {
                        // Pending Review Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(getAccentColor().copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clickable { /* Optional: show info dialog */ }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pulsing animation for the icon
                                val infiniteTransition = rememberInfiniteTransition()
                                val iconColor by infiniteTransition.animateColor(
                                    initialValue = getAccentColor(),
                                    targetValue = getAccentColor().copy(alpha = 0.5f),
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = "Pending Review",
                                    tint = iconColor,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Pending Review",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = getAccentColor(),
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .background(getAccentColor().copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Awaiting Moderation",
                                        fontSize = 10.sp,
                                        color = getAccentColor(),
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
                                .padding(horizontal = 12.dp, vertical = 8.dp)
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
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "Article Rejected",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE53E3E),
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                // View Details button
                                Text(
                                    text = "View Details",
                                    fontSize = 11.sp,
                                    color = getAccentColor(),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { showRejectionDetailsDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Header image if exists - TILE PATTERN: Prominent image at top
                    if (storyModel.stylingInfo.titleBackgroundImage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(getCardBackground())
                        ) {
                            val imageInfoMetadata = Gson().fromJson(
                                storyModel.stylingInfo.titleBackgroundImage,
                                ImageInfoMetadata::class.java
                            )

                            if (storyModel is StoryPublishedModel && storyModel.imageRequest != null) {
                                AsyncImage(
                                    model = storyModel.imageRequest,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val imageName = if (imageInfoMetadata?.minWidth != null)
                                    Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name
                                else imageInfoMetadata.name

                                val imageRequestLocalPath =
                                    ImageUtil.getCreateImageRootDir(LocalContext.current)
                                        .absolutePath + "/" + imageName
                                val file = File(imageRequestLocalPath)
                                val imageRequestPath = if (imageInfoMetadata.minWidth != null) loggedInUser.cdnSmallRequestPath
                                else loggedInUser.cdnLargeRequestPath
                                val image = rememberAsyncImagePainter(
                                    model = if (file.exists()) imageRequestLocalPath else imageRequestPath + imageName,
                                    contentScale = ContentScale.Crop
                                )

                                Image(
                                    painter = image,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Add subtle overlay for rejected articles
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
                                        .background(getAccentColor().copy(alpha = 0.15f))
                                )
                            }

                            if (storyModel.storyTitle.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomStart)
                                        .background(
                                            if (isRejected)
                                                Color(0xFF8B0000).copy(alpha = 0.7f)
                                            else
                                                Color.Black.copy(alpha = 0.6f)
                                        )
                                        .padding(12.dp)
                                ) {
                                    val annotatedString = buildAnnotatedString {
                                        if (storyModel.stylingInfo.titleHighlightColor != 0) {
                                            withStyle(
                                                style = SpanStyle(
                                                    background = Color(storyModel.stylingInfo.titleHighlightColor)
                                                )
                                            ) {
                                                append(storyModel.storyTitle)
                                            }
                                        } else {
                                            append(storyModel.storyTitle)
                                        }
                                    }

                                    Text(
                                        modifier = if (storyModel.stylingInfo.titleHighlightColor != 0) {
                                            Modifier.drawBehind(extendedSpans)
                                        } else Modifier,
                                        text = if (storyModel.stylingInfo.titleHighlightColor != 0) {
                                            remember(annotatedString) {
                                                extendedSpans.extend(annotatedString)
                                            }
                                        } else annotatedString,
                                        style = TextStyle(
                                            fontFamily = fonts,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        fontSize = 20.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        onTextLayout = { result ->
                                            if (storyModel.stylingInfo.titleHighlightColor != 0) {
                                                extendedSpans.onTextLayout(result)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // NO IMAGE - Compact text-only layout with prominent title
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    when {
                                        isRejected -> Color(0xFFE53E3E).copy(alpha = 0.05f)
                                        storyModel.stylingInfo.titleBackgroundColor != 0 -> Color(storyModel.stylingInfo.titleBackgroundColor)
                                        else -> getCardBackground()
                                    }
                                )
                                .padding(16.dp)
                        ) {
                            val annotatedString = buildAnnotatedString {
                                if (storyModel.stylingInfo.titleHighlightColor != 0) {
                                    withStyle(
                                        style = SpanStyle(
                                            background = Color(storyModel.stylingInfo.titleHighlightColor)
                                        )
                                    ) {
                                        append(storyModel.storyTitle)
                                    }
                                } else {
                                    append(storyModel.storyTitle)
                                }
                            }

                            Text(
                                modifier = if (storyModel.stylingInfo.titleHighlightColor != 0) {
                                    Modifier.drawBehind(extendedSpans)
                                } else Modifier,
                                text = if (storyModel.stylingInfo.titleHighlightColor != 0) {
                                    remember(annotatedString) {
                                        extendedSpans.extend(annotatedString)
                                    }
                                } else annotatedString,
                                style = TextStyle(
                                    fontFamily = fonts,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRejected) Color(0xFFE53E3E) else getPrimaryTextColor()
                                ),
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { result ->
                                    if (storyModel.stylingInfo.titleHighlightColor != 0) {
                                        extendedSpans.onTextLayout(result)
                                    }
                                }
                            )
                        }
                    }

                    // Article metadata and content - with category display for published articles
                    Column(Modifier.padding(bottom=8.dp)) {
                        // Category chip for published articles
                        if (isPublished && articleCategory != null && articleCategory.isNotEmpty()) {

                                Column(
                                    modifier = Modifier
                                        .offset(x = 8.dp, y = 4.dp)
                                        .background(
                                            getAccentColor().copy(alpha = 0.1f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)

                                ) {
                                    Text(
                                        text = articleCategory,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = getAccentColor(),
                                        letterSpacing = 0.3.sp
                                    )
                                }
                        }
                    }
                    GetArticleCardInfo(storyModel, hasLikedRecently, context)

                    // Bottom indicator for pending review or rejected
                    if (isPendingReview && storyModel.stylingInfo.titleBackgroundImage == null) {
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = getAccentColor().copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Under review",
                                fontSize = 10.sp,
                                color = getAccentColor().copy(alpha = 0.7f),
                                letterSpacing = 0.3.sp
                            )
                        }
                    } else if (isRejected && storyModel.stylingInfo.titleBackgroundImage == null) {
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = Color(0xFFE53E3E).copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Rejected",
                                    tint = Color(0xFFE53E3E),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Needs revision",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE53E3E),
                                    letterSpacing = 0.3.sp
                                )
                            }

                            Text(
                                text = "Tap to view feedback",
                                fontSize = 10.sp,
                                color = getAccentColor(),
                                modifier = Modifier.clickable { showRejectionDetailsDialog = true }
                            )
                        }
                    }
                }
            }

            // Rejection Details Dialog
            if (showRejectionDetailsDialog && isRejected && storyModel is StoryPublishedModel) {
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
                                color = getPrimaryTextColor(),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "Rejection Reason:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = getPrimaryTextColor()
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                backgroundColor = Color(0xFFE53E3E).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = rejectionReason ?: "",
                                    fontSize = 13.sp,
                                    color = getPrimaryTextColor(),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            Text(
                                text = "\nPlease revise your article according to the feedback above and resubmit for review.",
                                fontSize = 12.sp,
                                color = getSecondaryTextColor(),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showRejectionDetailsDialog = false }
                        ) {
                            Text("Got it", color = getAccentColor())
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showRejectionDetailsDialog = false
                                StoryUtil.startEditActivity(storyModel, context)
                            }
                        ) {
                            Text("Edit Article", color = getAccentColor())
                        }
                    }
                )
            }
        }

        @Composable
        fun ArticleItemPlaceholder(context: Context) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp,
                backgroundColor = getCardBackground()
            ) {
                Column {
                    // Image placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(getBorderColor().copy(alpha = 0.3f))
                            .shimmer()
                    )

                    // Content placeholder
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Author row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(getBorderColor())
                                        .shimmer()
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(12.dp)
                                        .background(getBorderColor())
                                        .shimmer()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(12.dp)
                                    .background(getBorderColor())
                                    .shimmer()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(getBorderColor())
                                .shimmer()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Excerpt placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .background(getBorderColor())
                                .shimmer()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                                .background(getBorderColor())
                                .shimmer()
                        )
                    }
                }
            }
        }
    }
}