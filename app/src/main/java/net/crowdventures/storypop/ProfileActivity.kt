package net.crowdventures.storypop

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import coil.compose.rememberImagePainter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.composable.Article
import net.crowdventures.storypop.composable.ComposableUtil
import net.crowdventures.storypop.composable.Profile
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.ProfileInfo
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.MessageUtil
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.endpoints.UserEndpoint
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.ProfileViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

public class ProfileActivity : ComponentActivity() {
    companion object {
        val USERNAME_INTENT_NAME = "USERNAME_PROFILE"
    }

    lateinit var profileViewModel: ProfileViewModel
    lateinit var sharedPreferenceManager: SharedPreferenceManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usernameProfile: String?
        if (intent.hasExtra(USERNAME_INTENT_NAME))
            usernameProfile = intent.getStringExtra(USERNAME_INTENT_NAME)
        else usernameProfile = null
        val loggedInUser = Constants.loggedInUser
        if (usernameProfile == null || loggedInUser == null) {
            finish()
            return
        }
        sharedPreferenceManager = SharedPreferenceManager(this)
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        setContent {
            MaterialTheme(
                colors = if (isSystemInDarkTheme()) ComposableUtil.GetDarkColors(this) else ComposableUtil.GetLightColors(
                    this
                )
            ) {
                var currentProfileInfo by remember { mutableStateOf<ProfileInfo?>(null) }
                profileViewModel.profileInfo.observe(this) { x ->
                    if (x != null) {
                        currentProfileInfo = x
                    }
                }
                ShowProfileInfo(context = this, usernameProfile, profileInfo = currentProfileInfo, loggedInUser)
            }
        }
        if (profileViewModel.profileInfo.value == null) {
            profileViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                val restAdapter = Retrofit.Builder()
                    .baseUrl(Config.APP_ENDPOINT)
                    .addConverterFactory(
                        GsonConverterFactory.create(
                            GsonBuilder().registerTypeAdapter(
                                UInt::class.java,
                                NullableUintJson()
                            ).create()
                        )
                    )
                    .client(RetrofitUtil.generateSecureOkHttpClient(applicationContext))
                    .build()

                val service: UserEndpoint = restAdapter.create(UserEndpoint::class.java)
                try {
                    val response: Response<ProfileInfo?> = service.getProfileFull("Bearer ${loggedInUser.refreshToken}", usernameProfile)
                    val responseBody = response.body()
                    if (responseBody == null) {
                        Log.e(
                            Config.logTag,
                            "Response from service.getProfile is null"
                        )
                        if (response.isSuccessful) {
                            runOnUiThread {
                                profileViewModel.profileInfo.value = ProfileInfo(
                                    usernameProfile, null, null,
                                    null, null, null, null, true
                                )
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this@ProfileActivity,
                                    "Connection issue, check your connection.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                    } else {
                        runOnUiThread {
                            profileViewModel.profileInfo.value = responseBody
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        Config.logTag,
                        "Could not load user profile, exception:" + e.message,
                        e
                    )
                    runOnUiThread {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Connection issue, check your connection.",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun ShowProfileInfo(context: Context, username: String, profileInfo: ProfileInfo?, loggedInUser: AccountInfoFull) {
        val extraHeight = 55
        val lazyListState = rememberLazyListState()
        var scrolledY = 0f
        var previousOffset = 0
        val lazyColumnModifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
        val scrollState = rememberScrollState()
        val configuration = LocalConfiguration.current
        val extraHeightDueToFAB =
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 100 else 0

        val memberSinceDate =
            if (profileInfo?.memberSince != null) DateTime(
                profileInfo.memberSince,
                DateTimeZone.UTC
            ).withZone(
                DateTimeZone.getDefault()
            ) else null
        val memberSinceString =
            if (memberSinceDate == null) ".." else StoryUtil.noTimeFormatDate(memberSinceDate, this)
        val storyDatabase = Constants.getStoryDatabase(context)
        val stringUUID = Constants.loggedInUser?.user_uuid
        var followPossible by remember {
            mutableStateOf(profileInfo?.username != Constants.loggedInUser?.username)
        }

        if (followPossible && stringUUID != null && profileInfo != null && !profileInfo.deleted) {
            val authorUUID = UUID.fromString(stringUUID)
            val pendingFollow = storyDatabase.storyPendingUploadDao()
                .getAllForAssociatedID(authorUUID, profileInfo.username)
            pendingFollow.observe(this) { x ->
                if (x.isNotEmpty() && x.any { z -> z.resourceType == PendingUploadType.FOLLOW }) {
                    followPossible = false
                }
            }
            val actualFollow =
                storyDatabase.userFollowedDao().getFollowedForUser(authorUUID, profileInfo.username)
            actualFollow.observe(this) { x ->
                followPossible = x == null
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Profile",
                            color = MaterialTheme.colors.onPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { finish() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colors.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary,
                    elevation = 4.dp
                )
            },
            floatingActionButton =  {
                if (stringUUID != null && Constants.loggedInUser != null && profileInfo != null && profileInfo.username != Constants.loggedInUser?.username && !profileInfo.deleted) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val authorUUID = UUID.fromString(stringUUID)
                            this.lifecycleScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                                val followingPendingUpload: StoryPendingUpload
                                if (!followPossible) { //remove like
                                    followingPendingUpload = StoryPendingUpload(
                                        UUID.randomUUID(),
                                        Config.getStandardTimeUTCString(),
                                        profileInfo.profileIcon ?: "",
                                        PendingUploadType.UNFOLLOW,
                                        profileInfo.username,
                                        authorUUID
                                    )
                                    storyDatabase.storyPendingUploadDao()
                                        .update(followingPendingUpload)
                                    ViewModelUtil.startResumeUploads(this, this@ProfileActivity.applicationContext)
                                    runOnUiThread {
                                        followPossible = true
                                    }
                                } else {
                                    followingPendingUpload = StoryPendingUpload(
                                        UUID.randomUUID(),
                                        Config.getStandardTimeUTCString(),
                                        profileInfo.profileIcon ?: "",
                                        PendingUploadType.FOLLOW,
                                        profileInfo.username,
                                        authorUUID
                                    )
                                    runOnUiThread {
                                        followPossible = false
                                    }
                                }
                                storyDatabase.storyPendingUploadDao().update(followingPendingUpload)
                                ViewModelUtil.startResumeUploads(this, this@ProfileActivity.applicationContext)
                            }
                        },
                        shape = RoundedCornerShape(15.dp),
                        backgroundColor = if (followPossible) MaterialTheme.colors.secondary else MaterialTheme.colors.surface,
                        text = {
                            Text(
                                if (followPossible) "FOLLOW" else "UNFOLLOW",
                                color = if (isSystemInDarkTheme()) MaterialTheme.colors.primary else (if (followPossible) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface)
                            )
                        },
                        modifier = Modifier,//.width(120.dp)
                        icon = {
                            Icon(
                                if (followPossible) Icons.Filled.GroupAdd else Icons.Filled.GroupRemove,
                                contentDescription = "FOLLOW",
                                tint = if (isSystemInDarkTheme()) MaterialTheme.colors.primary else (if (followPossible) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface)
                            )
                        })
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            backgroundColor = MaterialTheme.colors.background
        ) { paddingValues ->
        LazyColumn(
                modifier = lazyColumnModifier,
                state = lazyListState,
                contentPadding = paddingValues
            ) {
                item {
                    // Profile Header
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
                                val overriddenHeight = constraints.maxHeight + extraHeight
                                val placeable = measurable.measure(constraints.copy(maxHeight = overriddenHeight))
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(Modifier.height(dimensionResource(id = com.intuit.sdp.R.dimen._150sdp))) {
                            // Background Image
                            if (profileInfo?.profileBackgroundImage != null) {
                                val imageInfoMetadata = Gson().fromJson(
                                    profileInfo.profileBackgroundImage,
                                    ImageInfoMetadata::class.java
                                )
                                Profile.RenderProfileBackgroundImageIfExist(
                                    context,
                                    imageInfoMetadata,
                                    loggedInUser
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colors.primaryVariant)
                                )
                            }

                            // Avatar
                            Column {
                                Box(
                                    modifier = Modifier
                                        .offset(0.dp, 40.dp)
                                        .align(Alignment.CenterHorizontally)
                                ) {
                                    Card(
                                        onClick = {
                                            val imageInfoMetadata = profileInfo?.profileIcon
                                            if (imageInfoMetadata != null) {
                                                StfalconImageViewer.Builder<String>(
                                                    context,
                                                    listOf(imageInfoMetadata),
                                                    object : ImageLoader<String> {
                                                        override fun loadImage(
                                                            imageView: ImageView?,
                                                            image: String?
                                                        ) {
                                                            if (imageView == null || image == null) return
                                                            ImageUtil.loadMinBitmapImageViewRemote(
                                                                image,
                                                                context,
                                                                imageView,
                                                                loggedInUser
                                                            )
                                                        }
                                                    }
                                                ).show()
                                            }
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .border(
                                                2.dp,
                                                MaterialTheme.colors.surface,
                                                CircleShape
                                            )
                                    ) {
                                        if (profileInfo?.profileIcon != null) {
                                            val imageInfoMetadata = Gson().fromJson(
                                                profileInfo.profileIcon,
                                                ImageInfoMetadata::class.java
                                            )
                                            Profile.UserBadge(
                                                imageInfoMetadata,
                                                context,
                                                loggedInUser,
                                                size = 80.dp
                                            )
                                        } else {
                                            Profile.DefaultBadge(size = 80.dp)
                                        }
                                    }
                                }

                                // Username
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .offset(0.dp, 50.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Card(
                                        backgroundColor = MaterialTheme.colors.surface,
                                        shape = RoundedCornerShape(20.dp),
                                        elevation = 2.dp
                                    ) {
                                        Text(
                                            text = if (profileInfo?.deleted != true) "@${username}" else stringResource(R.string.account_deleted),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Profile Content Card
                    Column(modifier = Modifier.fillMaxSize()) {
                        Card(
                            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
                            backgroundColor = MaterialTheme.colors.surface,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((configuration.screenHeightDp + extraHeightDueToFAB).dp)
                                    .verticalScroll(scrollState)
                                    .padding(0.dp, 24.dp)
                            ) {
                                // About Section Header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(horizontal = 16.dp),
                                        backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            "About",
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                            color = MaterialTheme.colors.secondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // About Text Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    backgroundColor = MaterialTheme.colors.surface,
                                    elevation = 2.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                                ) {
                                    if (profileInfo?.deleted != true) {
                                        Text(
                                            text = if (profileInfo == null) "..." else profileInfo.profileText
                                                ?: "Hello, I have yet to write anything about myself.",
                                            modifier = Modifier.padding(16.dp),
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                        )
                                    } else {
                                        Text(
                                            text = "This account has been deleted.",
                                            modifier = Modifier.padding(16.dp),
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                // Block/Allow Section
                                var showLoading by remember { mutableStateOf(false) }
                                val loggedInUser = Constants.loggedInUser

                                if (showLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colors.secondary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                } else if (loggedInUser != null && profileInfo != null &&
                                    loggedInUser.username != profileInfo.username &&
                                    profileInfo?.deleted != true) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (profileInfo.contactBlocked) Icons.Default.Block else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colors.secondary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (profileInfo.contactBlocked)
                                                    "User is blocked from contacting you"
                                                else
                                                    "User can contact you",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                showLoading = true
                                                profileViewModel.viewModelScope.launch(
                                                    Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler
                                                ) {
                                                    val claimRewardResult =
                                                        MessageUtil.approveBlockContact(
                                                            context,
                                                            loggedInUser,
                                                            profileInfo.username,
                                                            profileInfo.contactBlocked
                                                        )
                                                    sharedPreferenceManager.setUserContactBlocked(
                                                        profileInfo.username,
                                                        !profileInfo.contactBlocked
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        showLoading = false
                                                        profileViewModel.profileInfo.value = ProfileInfo(
                                                            profileInfo.username,
                                                            profileInfo.profileIcon,
                                                            profileInfo.profileBackgroundImage,
                                                            profileInfo.profileText,
                                                            profileInfo.followerSpan,
                                                            profileInfo.memberSince,
                                                            profileInfo.articlesPublished,
                                                            profileInfo.deleted,
                                                            !profileInfo.contactBlocked
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = if (profileInfo.contactBlocked)
                                                    MaterialTheme.colors.secondary.copy(alpha = 0.1f)
                                                else
                                                    MaterialTheme.colors.error.copy(alpha = 0.1f),
                                                contentColor = if (profileInfo.contactBlocked)
                                                    MaterialTheme.colors.secondary
                                                else
                                                    MaterialTheme.colors.error
                                            ),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (profileInfo.contactBlocked)
                                                    Icons.Default.CheckCircle
                                                else
                                                    Icons.Default.Block,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (profileInfo.contactBlocked) "Allow Contact" else "Block User",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                                    thickness = 1.dp
                                )

                                // Stats Section Header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.align(Alignment.Center),
                                        backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            "Stats",
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                            color = MaterialTheme.colors.secondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Stats Grid
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    // Member Since
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Card(
                                            modifier = Modifier.size(48.dp),
                                            shape = CircleShape,
                                            backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DateRange,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .align(Alignment.CenterHorizontally),
                                                tint = MaterialTheme.colors.secondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Member Since",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = memberSinceString,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                    }

                                    // Followers
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Card(
                                            modifier = Modifier.size(48.dp),
                                            shape = CircleShape,
                                            backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Group,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .align(Alignment.CenterHorizontally),
                                                tint = MaterialTheme.colors.secondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Followers",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = if (profileInfo == null) "..." else profileInfo.followerSpan ?: "0",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Stories
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Card(
                                            modifier = Modifier.size(48.dp),
                                            shape = CircleShape,
                                            backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f)
                                        ) {
                                            Icon(
                                                painter = rememberImagePainter(
                                                    data = R.drawable.ic_edit_square,
                                                    builder = { crossfade(true) }
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .align(Alignment.CenterHorizontally),
                                                tint = MaterialTheme.colors.secondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Articles",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                        )

                                        val storyCount = profileInfo?.articlesPublished?.toInt() ?: 0
                                        if (storyCount > 0) {
                                            TextButton(
                                                onClick = {
                                                    StoryUtil.startListSearchActivity(
                                                        context,
                                                        "@${username}"
                                                    )
                                                },
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colors.secondary
                                                )
                                            ) {
                                                Text(
                                                    text = "$storyCount published",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "No articles",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}