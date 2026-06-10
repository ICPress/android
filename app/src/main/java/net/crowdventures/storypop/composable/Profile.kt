package net.crowdventures.storypop.composable

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HotelClass
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import coil.size.OriginalSize
import coil.size.Scale
import com.google.gson.Gson
import net.crowdventures.storypop.BuildConfig
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.UpdateProfileInfo
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.PendingUploadUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.SuccessCallback
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import java.io.File
import java.util.UUID

class Profile {
    companion object {
        private val VerticalScrollConsumer = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource) =
                available.copy(x = 0f, y = -100f)

            override suspend fun onPreFling(available: Velocity) = available.copy(x = 0f, y = -100f)
        }

        private val HorizontalScrollConsumer = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource) =
                available.copy(y = 0f)

            override suspend fun onPreFling(available: Velocity) = available.copy(y = 0f)
        }

        fun Modifier.disabledVerticalPointerInputScroll(disabled: Boolean = true) =
            if (disabled) this.nestedScroll(VerticalScrollConsumer) else this

        fun Modifier.disabledHorizontalPointerInputScroll(disabled: Boolean = true) =
            if (disabled) this.nestedScroll(HorizontalScrollConsumer) else this

        @Composable
        fun MyTab(text: String, selected: Boolean, icon: ImageVector) {
            Column(
                Modifier
                    .padding(8.dp)
                    .height(48.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    icon,
                    text,
                    Modifier
                        .size(22.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = if (selected) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text,
                    Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 12.sp,
                    color = if (selected) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        @Composable
        fun ProfileHomeView(activity: Activity,
                            lazyItemScope: LazyItemScope,
                            storySavedViewModel: StorySavedViewModel,
                            context: Context,
                            lazyDraftItemsState: LazyListState,
                            lazyPublishedItemsState: LazyListState,
                            lazyPendingPublishedItemsListState: LazyListState,
                            lazyLikedItemsState: LazyListState,
                            lifecycleOwner: LifecycleOwner,
                            sharedPreferenceManager: SharedPreferenceManager,
                            registerViewModel: RegisterViewModel,
                            paddingValues: PaddingValues
        ) {
            var tabNum by remember { mutableStateOf(0) }
            lazyItemScope.run {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                ) {
                    // Tab Row - Fixed height
                    Surface(
                        color = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.fillParentMaxHeight()
                            // .verticalScroll(rememberScrollState())
                        ) {
                            // Tab Row - Fixed height
                            Surface(
                                color = MaterialTheme.colors.surface,
                                shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
                                elevation = 2.dp
                            ) {
                                TabRow(
                                    selectedTabIndex = tabNum,
                                    modifier = Modifier.fillMaxWidth(),
                                    indicator = { tabPositions: List<TabPosition> ->
                                        TabRowDefaults.Indicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabNum]),
                                            height = 3.dp,
                                            color = MaterialTheme.colors.secondary
                                        )
                                    },
                                    backgroundColor = Color.Transparent,
                                    contentColor = MaterialTheme.colors.onSurface
                                ) {
                                    Tab(tabNum == 0, { tabNum = 0 }) {
                                        MyTab("Articles", tabNum == 0, Icons.Default.Article)
                                    }
                                    Tab(tabNum == 1, { tabNum = 1 }) {
                                        MyTab("Transactions", tabNum == 1, Icons.Default.Paid)
                                    }
                                    Tab(tabNum == 2, { tabNum = 2 }) {
                                        MyTab("Rewards", tabNum == 2, Icons.Default.HotelClass)
                                    }
                                }
                            }




                            Surface(
                                // color = MaterialTheme.colors.secondary,
                                elevation = (-8).dp,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                            ) {
                                Column {
                                    when (tabNum) {
                                        0 -> {
                                            ProfileStoriesHeader.DrawHeaderStoriesToggleButtonGroup(
                                                activity,
                                                storySavedViewModel,
                                                context,
                                                lazyDraftItemsState,
                                                lazyPublishedItemsState,
                                                lazyPendingPublishedItemsListState,
                                                lazyLikedItemsState,
                                                lifecycleOwner,
                                                paddingValues,
                                                registerViewModel,
                                                sharedPreferenceManager
                                            )
                                        }

                                        1 -> {
                                            // bottomBarOffsetEnabled.value = true // always true for transactions
                                            TransactionsDashboard.ShowTransactions(
                                                context,
                                                storySavedViewModel,
                                                paddingValues
                                            )
                                        }

                                        2 -> {
                                            RewardsHeader.DrawHeaderRewardsToggleButtonGroup(
                                                storySavedViewModel,
                                                context,
                                                lifecycleOwner,
                                                sharedPreferenceManager,
                                                registerViewModel,
                                                paddingValues
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

        @Composable
        fun DefaultEditIcon(loggedInUser: AccountInfoFull?) {
            Icon(
                if (loggedInUser?.profileIcon == null) Icons.Default.Add else Icons.Default.Edit,
                tint = MaterialTheme.colors.onSecondary,
                contentDescription = null,
                modifier = Modifier
                    .zIndex(22f)
                    .size(14.dp)
            )
        }

        @Composable
        fun StoryPopIcon(size: Dp = 80.dp) {
            Card(
                modifier = Modifier
                    .size(size),
                shape = CircleShape,
                elevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.size(size)
                    )
                }
            }
        }

        @Composable
        fun DefaultBadge(size: Dp = 80.dp) {
            Card(
                modifier = Modifier
                    .size(size),
                shape = CircleShape,
                elevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.user_solid_full),
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        @Composable
        fun UserBadge(loggedInUser: AccountInfoFull?, context: Context, size: Dp = 80.dp) {
            if (loggedInUser?.profileIcon != null) {
                val imageInfoMetadata = Gson().fromJson(loggedInUser.profileIcon, ImageInfoMetadata::class.java)
                UserBadge(imageInfoMetadata, context = context, loggedInUser, size)
            } else {
                DefaultBadge(size)
            }
        }

        @Composable
        fun UserBadge(imageInfoMetadata: ImageInfoMetadata, context: Context, loggedInUser: AccountInfoFull, size: Dp = 80.dp) {
            val imageName = if (imageInfoMetadata.minWidth != null)
                Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name else imageInfoMetadata.name
            val imageRequestPath = if (imageInfoMetadata.minWidth != null) loggedInUser.cdnSmallRequestPath
            else loggedInUser.cdnLargeRequestPath
            val imagePath = ImageUtil.getCreateImageRootDir(context).absolutePath + "/" + imageName
            val file = File(imagePath)
            Image(
                painter = rememberImagePainter(
                    data = if (file.exists()) Uri.fromFile(file) else imageRequestPath + imageName,
                    builder = { crossfade(true).size(OriginalSize).scale(Scale.FILL) }
                ),
                contentDescription = "Profile Badge",
                modifier = Modifier
                    .size(size)
                    .clip(shape = CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        @Composable
        fun UserBadgeForStory(storyPublishedModel: StoryPublishedModel, context: Context, size: Dp = 80.dp) {
            if (storyPublishedModel.badgeImageRequest == null) return DefaultBadge(size)
            AsyncImage(
                storyPublishedModel.badgeImageRequest,
                contentDescription = "Profile Badge",
                modifier = Modifier
                    .size(size)
                    .clip(shape = CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        @Composable
        fun RenderProfileBackgroundImageIfExist(context: Context, loggedInUser: AccountInfoFull?) {
            val profileBackgroundMetadata = loggedInUser?.profileBackgroundImage
            if (profileBackgroundMetadata != null) {
                val imageInfoMetadata = Gson().fromJson(profileBackgroundMetadata, ImageInfoMetadata::class.java)
                RenderProfileBackgroundImageIfExist(context, imageInfoMetadata, loggedInUser)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.primaryVariant)
                )
            }
        }

        @Composable
        fun RenderProfileBackgroundImageIfExist(context: Context, imageInfoMetadata: ImageInfoMetadata, loggedInUser: AccountInfoFull) {
            val imageName = if (imageInfoMetadata.minWidth != null)
                Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name else imageInfoMetadata.name
            val imageRequestPath = if (imageInfoMetadata.minWidth != null) loggedInUser.cdnSmallRequestPath
            else loggedInUser.cdnLargeRequestPath
            val imagePath = ImageUtil.getCreateImageRootDir(context).absolutePath + "/" + imageName
            val file = File(imagePath)
            val image = rememberImagePainter(
                data = if (file.exists()) Uri.fromFile(file) else imageRequestPath + imageName,
                builder = { crossfade(true).size(OriginalSize).scale(Scale.FILL) }
            )
            Image(
                painter = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        @OptIn(ExperimentalMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
        @Composable
        fun ShowEditProfileInformation(
            context: Context,
            activity: Activity,
            sharedPreferenceManager: SharedPreferenceManager,
            loggedInUser: AccountInfoFull,
            storySavedViewModel: StorySavedViewModel,
            registerViewModel: RegisterViewModel,
            showEditProfileInformation: MutableState<Boolean>
        ) {
            val defaultAppTextSelectionColors = ComposableUtil.GetDefaultTextSelectionColors()
            CompositionLocalProvider(LocalTextSelectionColors provides defaultAppTextSelectionColors) {
                var loggedInUserNew by remember { mutableStateOf(loggedInUser) }
                var showLoadingProfileImage by remember { mutableStateOf(false) }
                var showLoadingProfileBackgroundImage by remember { mutableStateOf(false) }

                val getProfileImageTask = ImageUtil.getImageForResult(
                    storySavedViewModel,
                    true,
                    context,
                    false, true,
                    object : SuccessCallback<ImageInfoMetadata> {
                        override fun onSuccess(vararg param: ImageInfoMetadata) {
                            showLoadingProfileImage = false
                            val currentUser = loggedInUserNew
                            val profileIconMetadataString = Gson().toJson(param.first())
                            loggedInUserNew = currentUser.copy(profileIcon = profileIconMetadataString)
                        }

                        override fun onFailure(reason: Any?) {
                            showLoadingProfileImage = false
                        }
                    })

                val getBackgroundImageTask = ImageUtil.getImageForResult(
                    storySavedViewModel,
                    false,
                    context,
                    false, true,
                    object : SuccessCallback<ImageInfoMetadata> {
                        override fun onSuccess(vararg param: ImageInfoMetadata) {
                            val currentUser = loggedInUserNew
                            showLoadingProfileBackgroundImage = false
                            val backgroundImageMetadataString = Gson().toJson(param.first())
                            loggedInUserNew = currentUser.copy(profileBackgroundImage = backgroundImageMetadataString)
                        }

                        override fun onFailure(reason: Any?) {
                            showLoadingProfileBackgroundImage = false
                        }
                    })

                var profileDescription by remember {
                    mutableStateOf(
                        TextFieldValue(loggedInUser.profileText ?: "")
                    )
                }

                fun goBack() {
                    showEditProfileInformation.value = false
                }

                BackHandler(enabled = true) {
                    goBack()
                }

                DisposableEffect(Unit) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    onDispose {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                ) {
                    // Header with back button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .zIndex(10f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { goBack() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colors.surface)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colors.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Edit Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.size(40.dp)) // Empty box for balance
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 56.dp) // Space for header
                    ) {
                        item {
                            // Profile Header Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = 2.dp,
                                backgroundColor = MaterialTheme.colors.surface
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Background Image Section
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                    ) {
                                        RenderProfileBackgroundImageIfExist(context, loggedInUserNew)

                                        // Background Image Edit Button
                                        IconButton(
                                            onClick = {
                                                showLoadingProfileBackgroundImage = true
                                                getBackgroundImageTask.launch(
                                                    ImageUtil.requestImageIntent(context)
                                                )
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .size(32.dp)
                                                .background(
                                                    color = MaterialTheme.colors.secondary,
                                                    shape = CircleShape
                                                )
                                                .clip(CircleShape)
                                        ) {
                                            if (showLoadingProfileBackgroundImage) {
                                                CircularProgressIndicator(
                                                    color = MaterialTheme.colors.onSecondary,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Image,
                                                    tint = MaterialTheme.colors.onSecondary,
                                                    contentDescription = "Change background",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Profile Picture Section
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset(y = (-32).dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Shared container for Card and Button to keep them anchored
                                        Box(modifier = Modifier.size(88.dp)) {

                                            // 1. The Avatar Card (No longer contains the button)
                                            Card(
                                                shape = CircleShape,
                                                elevation = 4.dp,
                                                modifier = Modifier.fillMaxSize(), onClick = {
                                                    showLoadingProfileImage = true
                                                    getProfileImageTask.launch(ImageUtil.requestImageIntent(context))
                                                }
                                            ) {
                                                UserBadge(loggedInUserNew, context, size = 88.dp)
                                            }

                                            // 2. The Edit Button (Placed on top of the Card)
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(26.dp) // Your original size
                                                    .background(
                                                        color = MaterialTheme.colors.secondary,
                                                        shape = CircleShape
                                                    )
                                                    .clip(CircleShape)
                                                    .clickable { // Replaces IconButton to avoid extra padding/size
                                                        showLoadingProfileImage = true
                                                        getProfileImageTask.launch(ImageUtil.requestImageIntent(context))
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (showLoadingProfileImage) {
                                                    CircularProgressIndicator(
                                                        color = MaterialTheme.colors.onSecondary,
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                } else {
                                                    // Wrap your icon to ensure it stays small
                                                    Box(modifier = Modifier.size(16.dp)) {
                                                        DefaultEditIcon(loggedInUserNew)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Username
                                    Text(
                                        text = "@${loggedInUserNew.username}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colors.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        item {
                            // Bio Section
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                elevation = 1.dp,
                                backgroundColor = MaterialTheme.colors.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Bio",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                    }

                                    TextField(
                                        value = profileDescription,
                                        onValueChange = { x ->
                                            profileDescription = if (x.text.length >= 250)
                                                TextFieldValue(x.text.substring(0, 249))
                                            else x
                                        },
                                        placeholder = {
                                            Text(
                                                text = "Tell us about yourself...",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clip(RoundedCornerShape(8.dp)),
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colors.onSurface,
                                            fontSize = 14.sp
                                        ),
                                        colors = TextFieldDefaults.textFieldColors(
                                            backgroundColor = Color.Transparent,
                                            cursorColor = MaterialTheme.colors.secondary,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        maxLines = 4
                                    )

                                    Text(
                                        text = "${profileDescription.text.length}/250",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }

                        item {
                            // Action Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { goBack() },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = MaterialTheme.colors.surface,
                                        contentColor = MaterialTheme.colors.onSurface
                                    ),
                                    shape = RoundedCornerShape(22.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        "Cancel",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Button(
                                    onClick = {
                                        val authorUUID = UUID.fromString(loggedInUser.user_uuid)
                                        val associatedID = UUID.randomUUID().toString()
                                        var profileBadgeImageInfo: ImageInfoMetadata? = null

                                        if (loggedInUserNew.profileIcon != null) {
                                            val profileImage = Gson().fromJson(
                                                loggedInUserNew.profileIcon,
                                                ImageInfoMetadata::class.java
                                            )
                                            profileBadgeImageInfo = profileImage
                                            if (loggedInUserNew.profileIcon != loggedInUser.profileIcon) {
                                                PendingUploadUtil.preparePushImagePendingUpload(
                                                    associatedID,
                                                    profileImage,
                                                    context,
                                                    loggedInUser,
                                                    storySavedViewModel
                                                )
                                            }
                                        } else if (loggedInUser.profileIcon != null) {
                                            profileBadgeImageInfo = Gson().fromJson(
                                                loggedInUser.profileIcon,
                                                ImageInfoMetadata::class.java
                                            )
                                        }

                                        var profileBackgroundImageInfo: ImageInfoMetadata? = null
                                        if (loggedInUserNew.profileBackgroundImage != null) {
                                            val profileBackgroundImage = Gson().fromJson(
                                                loggedInUserNew.profileBackgroundImage,
                                                ImageInfoMetadata::class.java
                                            )
                                            profileBackgroundImageInfo = profileBackgroundImage
                                            if (loggedInUserNew.profileBackgroundImage != loggedInUser.profileBackgroundImage) {
                                                PendingUploadUtil.preparePushImagePendingUpload(
                                                    associatedID,
                                                    profileBackgroundImage,
                                                    context,
                                                    loggedInUser,
                                                    storySavedViewModel
                                                )
                                            }
                                        } else if (loggedInUser.profileBackgroundImage != null) {
                                            profileBackgroundImageInfo = Gson().fromJson(
                                                loggedInUser.profileBackgroundImage,
                                                ImageInfoMetadata::class.java
                                            )
                                        }

                                        val updatedProfileInfo = UpdateProfileInfo(
                                            profileBadgeImageInfo,
                                            profileBackgroundImageInfo,
                                            profileDescription.text.ifEmpty { null }
                                        )

                                        val updatedProfileInfoJson = Gson().toJson(updatedProfileInfo)
                                        val pendingUploadAvatar = StoryPendingUpload(
                                            UUID.randomUUID(),
                                            Config.getStandardTimeUTCString(),
                                            updatedProfileInfoJson,
                                            PendingUploadType.PUBLISH_PROFILE_INFO,
                                            associatedID,
                                            authorUUID
                                        )

                                        storySavedViewModel.pushPendingUpload(pendingUploadAvatar, context)

                                        val newAccountInfo = loggedInUserNew.copy(
                                            profileText = updatedProfileInfo.profileDescription
                                        )

                                        registerViewModel.loggedInUser.value = newAccountInfo
                                        Constants.loggedInUser = newAccountInfo

                                        showEditProfileInformation.value = false
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = MaterialTheme.colors.secondary,
                                        contentColor = MaterialTheme.colors.onSecondary
                                    ),
                                    shape = RoundedCornerShape(22.dp)
                                ) {
                                    Text(
                                        "Save",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun ProfileView(
            activity: Activity,
            viewModel: StorySavedViewModel,
            context: Context,
            sharedPreferenceManager: SharedPreferenceManager,
            registerViewModel: RegisterViewModel,
            lifecycleOwner: LifecycleOwner,
            bottomBarVisibility: MutableState<Boolean>,
            paddingValues: PaddingValues
        ) {
            var loggedInUser by remember { mutableStateOf<AccountInfoFull?>(null) }
            registerViewModel.loggedInUser.observe(lifecycleOwner) { x -> loggedInUser = x }
            val loggedInUserNotNull = loggedInUser ?: return
            val showEditProfileInformation = remember { mutableStateOf(false) }

            AnimatedVisibility(visible = showEditProfileInformation.value) {
                ShowEditProfileInformation(
                    context, activity, sharedPreferenceManager, loggedInUserNotNull, viewModel,
                    registerViewModel, showEditProfileInformation
                )
            }

            if (!showEditProfileInformation.value) {
                if (!bottomBarVisibility.value) bottomBarVisibility.value = true

                val lazyListState = rememberLazyListState()
                var expandedContextMenu by remember { mutableStateOf(false) }
                var scrolledY by remember { mutableStateOf(0f) }
                var previousOffset by remember { mutableStateOf(0) }
                val extraHeight = 55

                val lazyDraftItemsListState = rememberLazyListState()
                val lazyPublishedItemsListState = rememberLazyListState()
                val lazyPendingPublishedItemsListState = rememberLazyListState()
                val lazyLikedItemsListState = rememberLazyListState()

                var dialogText = remember { mutableStateOf<String?>(null) }
                var showLoading = remember { mutableStateOf(false) }

                if (showLoading.value) ComposableUtil.ShowProgressDialog()

                val dialogTextNotNull = dialogText.value
                if (dialogTextNotNull != null) {
                    ComposableUtil.ShowErrorDialog(message = dialogTextNotNull) {
                        dialogText.value = null
                    }
                }

                var showDeleteAccountDialog by remember { mutableStateOf(false) }
                if (showDeleteAccountDialog) {
                    ComposableUtil.QuestionDialog(
                        title = "Delete Account",
                        question = "Deleting your account will delete all your posts and all your account information including any rewards that have not been transferred.",
                        confirmText = "Yes, delete",
                        onDismissRequest = { showDeleteAccountDialog = false }
                    ) {
                        showDeleteAccountDialog = false
                        showLoading.value = true
                        ComposableUtil.DeleteAccount(
                            sharedPreferenceManager, viewModel, registerViewModel,
                            loggedInUserNotNull, dialogText, showLoading
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .background(MaterialTheme.colors.background)
                        .fillMaxSize(),
                    state = lazyListState
                ) {
                    // Dynamic Header Item
                    item {
                        val configuration = LocalConfiguration.current
                        Column(
                            modifier = Modifier
                                .height(dimensionResource(id = com.intuit.sdp.R.dimen._130sdp))
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scrolledY += lazyListState.firstVisibleItemScrollOffset - previousOffset
                                    translationY = scrolledY * 0.5f
                                    previousOffset = lazyListState.firstVisibleItemScrollOffset
                                }
                                .layout { measurable, constraints ->
                                    // Allow header to expand beyond normal height
                                    val expandedHeight = constraints.maxHeight + extraHeight
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            maxHeight = expandedHeight,
                                            minHeight = expandedHeight
                                        )
                                    )
                                    layout(placeable.width, placeable.height) {
                                        placeable.place(0, 0)
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Background Image
                                RenderProfileBackgroundImageIfExist(context, loggedInUser)

                                // Content overlay
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
                                ) {
                                    // Profile Picture
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                    ) {
                                        Card(
                                            onClick = {
                                                bottomBarVisibility.value = false
                                                showEditProfileInformation.value = true
                                            },
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .size(88.dp)
                                                .border(
                                                    2.dp,
                                                    MaterialTheme.colors.surface,
                                                    CircleShape
                                                ),
                                            elevation = 4.dp
                                        ) {
                                            UserBadge(loggedInUser, context, size = 88.dp)
                                        }

                                        // Edit Icon
                                        IconButton(
                                            onClick = {
                                                bottomBarVisibility.value = false
                                                showEditProfileInformation.value = true
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(4.dp, 4.dp)
                                                .size(26.dp)
                                                .background(
                                                    color = MaterialTheme.colors.secondary,
                                                    shape = CircleShape
                                                )
                                                .clip(CircleShape)
                                        ) {
                                            Icon(
                                                if (loggedInUser?.profileIcon == null)
                                                    Icons.Default.Add
                                                else
                                                    Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colors.onSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Username with Dropdown
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .wrapContentSize(Alignment.Center)
                                    ) {
                                        Card(
                                            onClick = { expandedContextMenu = !expandedContextMenu },
                                            backgroundColor = MaterialTheme.colors.surface,
                                            shape = RoundedCornerShape(24.dp),
                                            elevation = 2.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "@${loggedInUser?.username}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colors.onSurface
                                                )
                                                Icon(
                                                    imageVector = if (expandedContextMenu)
                                                        Icons.Default.ArrowDropUp
                                                    else
                                                        Icons.Default.ArrowDropDown,
                                                    tint = MaterialTheme.colors.secondary,
                                                    contentDescription = "Options",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            modifier = Modifier
                                                .width(180.dp)
                                                .background(MaterialTheme.colors.surface),
                                            expanded = expandedContextMenu,
                                            onDismissRequest = { expandedContextMenu = false }
                                        ) {
                                            DropdownMenuItem(onClick = {
                                                expandedContextMenu = false
                                                bottomBarVisibility.value = false
                                                showEditProfileInformation.value = true
                                            }) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colors.secondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        "Edit Profile",
                                                        color = MaterialTheme.colors.onSurface,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                            DropdownMenuItem(onClick = {
                                                expandedContextMenu = false
                                                showDeleteAccountDialog = true
                                            }) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Dangerous,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colors.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        "Delete Account",
                                                        color = MaterialTheme.colors.error,
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

                    // Content Items
                    item {
                        ProfileHomeView(
                            activity = activity,
                            lazyItemScope = this,
                            storySavedViewModel = viewModel,
                            context = context,
                            lazyDraftItemsState = lazyDraftItemsListState,
                            lazyPublishedItemsState = lazyPublishedItemsListState,
                            lazyPendingPublishedItemsListState = lazyPendingPublishedItemsListState,
                            lazyLikedItemsState = lazyLikedItemsListState,
                            lifecycleOwner = lifecycleOwner,
                            sharedPreferenceManager = sharedPreferenceManager,
                            registerViewModel = registerViewModel,
                            paddingValues = paddingValues
                        )
                    }
                }
            }
        }
    }
}