package net.crowdventures.storypop.composable

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import android.graphics.Shader
import android.util.Log
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Colors
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.RewardClaimed
import net.crowdventures.storypop.dto.UserSearchResult
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.util.BottomBarManagerCallback
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.SuccessCallback
import net.crowdventures.storypop.util.endpoints.AccountEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.UUID

class ComposableUtil {
    companion object {

        // ===== PROFESSIONAL COLOR PALETTE =====
        // Light Theme Colors (Matches web light mode)
        private val LightPrimary = Color(0xFF0A2463)      // Deep tech blue
        private val LightPrimaryVariant = Color(0xFF061A40) // Darker blue
        private val LightSecondary = Color(0xFF3E92CC)    // Accent blue
        private val LightSecondaryVariant = Color(0xFF2A7CB0) // Hover blue
        private val LightBackground = Color(0xFFF5F7FA)   // Light gray background
        private val LightSurface = Color(0xFFFFFFFF)      // White surface
        private val LightError = Color(0xFFE53E3E)        // Red for errors/badges
        private val LightOnPrimary = Color(0xFFFFFFFF)    // White text on primary
        private val LightOnSecondary = Color(0xFFFFFFFF)  // White text on secondary
        private val LightOnBackground = Color(0xFF1E1E24) // Dark text on background
        private val LightOnSurface = Color(0xFF1E1E24)    // Dark text on surface
        private val LightOnError = Color(0xFFFFFFFF)      // White text on error

        // Dark Theme Colors (Matches web dark mode)
        private val DarkPrimary = Color(0xFF0C1220)       // Dark blue header
        private val DarkPrimaryVariant = Color(0xFF050811) // Darker blue
        private val DarkSecondary = Color(0xFF58A6FF)     // Bright accent blue
        private val DarkSecondaryVariant = Color(0xFF7CB9FF) // Lighter accent
        private val DarkBackground = Color(0xFF0A0E17)    // Very dark blue-gray
        private val DarkSurface = Color(0xFF121826)       // Dark surface
        private val DarkError = Color(0xFFF56565)         // Soft red for errors
        private val DarkOnPrimary = Color(0xFFE6EDF3)     // Light text on primary
        private val DarkOnSecondary = Color(0xFFE6EDF3)   // Light text on secondary
        private val DarkOnBackground = Color(0xFFE6EDF3)  // Light text on background
        private val DarkOnSurface = Color(0xFFE6EDF3)     // Light text on surface
        private val DarkOnError = Color(0xFFFFFFFF)       // White text on error

        @Composable
        fun getFancyBrush(offset:Float,gradient:List<Color>):ShaderBrush{
            return remember(offset) {
                object : ShaderBrush() {
                    override fun createShader(size: Size): Shader {
                        val widthOffset = size.width * offset
                        val heightOffset = size.height * offset
                        return LinearGradientShader(
                            colors = gradient,
                            from = Offset(widthOffset, heightOffset),
                            to = Offset(widthOffset + size.width, heightOffset + size.height),
                            tileMode = TileMode.Mirror
                        )
                    }
                }
            }

        }
        fun DeleteAccount(
            sharedPreferenceManager: SharedPreferenceManager,
            storySavedViewModel: StorySavedViewModel,
            registerViewModel: RegisterViewModel,
            accountInfoFull: AccountInfoFull,
            dialogText: MutableState<String?>,
            loading: MutableState<Boolean>
        ) {
            storySavedViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
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
                    .client(RetrofitUtil.generateSecureOkHttpClient(storySavedViewModel.storySavedViewModelRepository.context))
                    .build()

                val service: AccountEndpoint = restAdapter.create(AccountEndpoint::class.java)
                try {
                    val response: Response<ResponseBody> =
                        service.deleteAccount("Bearer ${accountInfoFull.refreshToken}")

                    if (response.isSuccessful) {
                        sharedPreferenceManager.clearAll()
                        storySavedViewModel.storyDatabase.clearAllTables()
                        Log.v(
                            Config.logTag,
                            "Response from service.deleteAccount is success!"
                        )
                        withContext(Dispatchers.Main) {
                            Constants.loggedInUser = null
                            registerViewModel.loggedInUser.value = null
                            loading.value = false
                            registerViewModel.errorMessage.value =
                                "Your account has now been deleted."
                        }
                    } else {
                        Log.e(
                            Config.logTag,
                            "Response from service.deleteAccount is failure, code:" + response.code()
                        )
                        withContext(Dispatchers.Main) {
                            loading.value = false
                            dialogText.value =
                                "We are experiencing issues right now, try again later."
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        Config.logTag,
                        "Could not delete user account, exception:" + e.message, e
                    )
                    withContext(Dispatchers.Main) {
                        loading.value = false
                        dialogText.value = "Connection issue, check your connection."
                    }
                }
            }
        }

        @Composable
        fun HandleImeShown(callback: BottomBarManagerCallback) {
            val view = LocalView.current
            DisposableEffect(view) {
                val listener = ViewTreeObserver.OnGlobalLayoutListener {
                    val isKeyboardOpen = ViewCompat.getRootWindowInsets(view)
                        ?.isVisible(WindowInsetsCompat.Type.ime()) ?: true
                    if (isKeyboardOpen) callback.show() else callback.hide()
                }

                view.viewTreeObserver.addOnGlobalLayoutListener(listener)
                onDispose {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
            }
        }

        /**
         * Professional Dark Theme that matches the web dark mode
         * Features: Dark blue background, bright blue accents, good contrast
         */
        @Composable
        fun GetDarkColors(context: Context): Colors {
            return darkColors(
                primary = DarkPrimary,
                primaryVariant = DarkPrimaryVariant,
                secondary = DarkSecondary,
                secondaryVariant = DarkSecondaryVariant,
                background = DarkBackground,
                surface = DarkSurface,
                error = DarkError,
                onPrimary = DarkOnPrimary,
                onSecondary = DarkOnSecondary,
                onBackground = DarkOnBackground,
                onSurface = DarkOnSurface,
                onError = DarkOnError
            )
        }

        /**
         * Professional Light Theme that matches the web light mode
         * Features: Clean white surfaces, deep blue accents, excellent readability
         */
        @Composable
        fun GetLightColors(context: Context): Colors {
            return lightColors(
                primary = LightPrimary,
                primaryVariant = LightPrimaryVariant,
                secondary = LightSecondary,
                secondaryVariant = LightSecondaryVariant,
                background = LightBackground,
                surface = LightSurface,
                error = LightError,
                onPrimary = LightOnPrimary,
                onSecondary = LightOnSecondary,
                onBackground = LightOnBackground,
                onSurface = LightOnSurface,
                onError = LightOnError
            )
        }

        /**
         * Text selection colors that match the theme's accent color
         */
        @Composable
        fun GetDefaultTextSelectionColors(): TextSelectionColors {
            return TextSelectionColors(
                handleColor = MaterialTheme.colors.secondary,
                backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.25f) // More subtle background
            )
        }

        @Composable
        fun ShowNoItemsErrorItem(message: String) {
            Column(
                modifier = Modifier
                    .padding(0.dp, 100.dp, 0.dp, 0.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.CenterVertically)
            ) {

                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.CenterVertically)
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f), // Softer text for empty states
                        modifier = Modifier
                            .width(180.dp)
                            .align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center
                    )
                }

            }
        }

        @SuppressLint("WrongConstant")
        @Composable
        fun LockScreenOrientation(activity:Activity) {
            DisposableEffect(activity) {
                // Lock the screen orientation.
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                onDispose {
                    // Release the the screen orientation lock.
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }

        @Composable
        fun ShowProgressDialog() {
            Dialog(
                onDismissRequest = { },
                DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colors.surface, shape = RoundedCornerShape(15.dp))
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.secondary,
                        strokeWidth = 3.dp // Slightly thicker for better visibility
                    )
                }
            }
        }

        @Composable
        fun ShowErrorDialog(message: String, onDismissRequest: () -> Unit) {
            Dialog(
                onDismissRequest = onDismissRequest,
                DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(250.dp)
                        .height(200.dp)
                        .background(MaterialTheme.colors.surface, shape = RoundedCornerShape(15.dp))
                ) {
                    Column() {
                        Column() {
                            Text(
                                message,
                                Modifier,
                                MaterialTheme.colors.onSurface,
                                18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(Modifier.fillMaxWidth()) {
                            Button(
                                onDismissRequest,
                                Modifier
                                    .offset(0.dp, 20.dp)
                                    .align(Alignment.CenterHorizontally),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.secondary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("OK", Modifier)
                            }
                        }
                    }

                }
            }
        }


        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun QuestionDialog(
            title: String,
            question: String,
            confirmText: String,
            onDismissRequest: () -> Unit,
            onConfirmRequest: () -> Unit
        ) {
            Dialog(
                onDismissRequest = onDismissRequest,
                DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(250.dp)
                        .height(200.dp)
                        .background(MaterialTheme.colors.surface, shape = RoundedCornerShape(15.dp))
                ) {
                    Column() {
                        Text(
                            title,
                            Modifier.fillMaxWidth(),
                            MaterialTheme.colors.onSurface,
                            18.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Column(Modifier.padding(15.dp, 10.dp)) {
                            Text(
                                question,
                                Modifier,
                                MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                                14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                Button(
                                    onClick = onDismissRequest,
                                    Modifier
                                        .width(110.dp)
                                        .padding(0.dp, 0.dp, 15.dp, 0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = MaterialTheme.colors.primary,
                                        contentColor = MaterialTheme.colors.onPrimary
                                    )
                                ) {
                                    Text(
                                        modifier = Modifier,
                                        text = "Back"
                                    )
                                }
                                Button(
                                    onConfirmRequest,
                                    Modifier,
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = MaterialTheme.colors.secondary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(confirmText, Modifier)
                                }
                            }
                        }
                    }

                }
            }
        }


        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun AddFollowedUserDialog(
            loggedInUser: AccountInfoFull,
            storySavedViewModel: StorySavedViewModel,
            lifecycleOwner: LifecycleOwner,
            onDismissRequest: () -> Unit,
            onConfirmRequest: (usernameToFollow:UserSearchResult) -> Unit
        ) {
            var textStateUsernameFilter by remember {
                mutableStateOf("")
            }
            var textStateUsername by remember { mutableStateOf(TextFieldValue("")) }
            var exp by remember { mutableStateOf(false) }
            var followPossible by remember { mutableStateOf(false)}
            var textStateUsernameCannotBeYourself =false
            var selectedUser:UserSearchResult? = null
            var userSuggestion by remember {
                mutableStateOf<List<UserSearchResult>>(
                    listOf()
                )
            }
            storySavedViewModel.userSuggestions.observe(lifecycleOwner) { x ->
                if (x != null) userSuggestion = x
            }
            Dialog(
                onDismissRequest = onDismissRequest,
                DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Column(
                    Modifier
                        .width(250.dp)
                        .height(200.dp)
                        .background(
                            MaterialTheme.colors.surface,
                            shape = RoundedCornerShape(15.dp)
                        )
                ) {
                    Text(
                        "Add user to follow",
                        Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        MaterialTheme.colors.onSurface,
                        18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Column(
                        Modifier
                            .padding(15.dp, 10.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = exp,
                            onExpandedChange = { }) {
                            Row(
                                Modifier
                                    .wrapContentHeight(Alignment.CenterVertically)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = "@",
                                    color = MaterialTheme.colors.secondary,
                                    fontSize = 25.sp,
                                    fontFamily = Article.fonts,
                                    modifier = Modifier
                                        .wrapContentWidth(
                                            Alignment.End
                                        )
                                        .offset(5.dp, 10.dp)
                                        .zIndex(10f)
                                )
                                TextField(
                                    textStateUsername.text,
                                    { x ->
                                        if (x.length <= Config.MAX_USERNAME_LENGTH) {
                                            val clearedUsername =
                                                x.replace(Regex("[^._|A-Za-z0-9\\[\\]]"), "")
                                                    .lowercase(Locale.ROOT)
                                            textStateUsername = TextFieldValue(
                                                clearedUsername
                                            )
                                            if (clearedUsername.length >= Config.MIN_HASHTAG_LENGTH && clearedUsername.length <= Config.MAX_USERNAME_LENGTH) {
                                                if (textStateUsernameFilter != clearedUsername) {
                                                    textStateUsernameFilter = clearedUsername
                                                    textStateUsernameCannotBeYourself = false
                                                    followPossible = false
                                                    exp = true
                                                    storySavedViewModel.findUser(storySavedViewModel.storySavedViewModelRepository.context,
                                                        loggedInUser,
                                                        textStateUsernameFilter
                                                    )
                                                }
                                            }else textStateUsernameFilter = ""
                                        }
                                    },
                                    Modifier
                                        .width(290.dp)
                                        .offset(-(10).dp, 0.dp),
                                    keyboardOptions = KeyboardOptions.Default.copy(
                                        imeAction = ImeAction.Next,
                                        capitalization = KeyboardCapitalization.None
                                    ),
                                    isError = textStateUsernameCannotBeYourself,
                                    keyboardActions = KeyboardActions(onNext = {
                                        //search
                                        //focusManager.moveFocus(FocusDirection.Down);
                                    }),
                                    placeholder = {
                                        Text(
                                            text = "Username",
                                            fontSize = 18.sp,
                                            fontFamily = Article.fonts,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                        )
                                    },
                                    colors = TextFieldDefaults.textFieldColors(
                                        backgroundColor = Color.Transparent,
                                        cursorColor = MaterialTheme.colors.secondary,
                                        focusedIndicatorColor = MaterialTheme.colors.secondary,
                                        focusedLabelColor = MaterialTheme.colors.secondary,
                                        textColor = MaterialTheme.colors.onSurface
                                    ),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colors.onSurface,
                                        fontFamily = Article.fonts,
                                        fontSize = 18.sp
                                    )
                                )
                                if (userSuggestion.isNotEmpty()) {
                                    ExposedDropdownMenu(
                                        expanded = exp,
                                        onDismissRequest = { exp = false }) {
                                        userSuggestion.forEach { option ->
                                            DropdownMenuItem(
                                                modifier = Modifier.padding(0.dp),
                                                onClick = {
                                                    Log.v(
                                                        Config.logTag,
                                                        "Selected autocomplete option:$option"
                                                    )
                                                    textStateUsernameFilter = option.username
                                                    textStateUsername =
                                                        TextFieldValue(option.username)
                                                    exp = false
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(15.dp)
                                                        //.background(MaterialTheme.colors.onPrimary)
                                                        .align(Alignment.CenterVertically)
                                                        .wrapContentWidth(Alignment.CenterHorizontally)/// senad 20220316 align does not work with flex
                                                ) {
                                                    Card(
                                                        Modifier
                                                            .size(30.dp), shape = CircleShape
                                                    ) {
                                                        if (option.profileIcon != null) {
                                                            val imageInfoMetadata = Gson().fromJson(
                                                                option.profileIcon,
                                                                ImageInfoMetadata::class.java
                                                            )
                                                            Profile.UserBadge(
                                                                imageInfoMetadata,
                                                                storySavedViewModel.storySavedViewModelRepository.context, loggedInUser
                                                            )
                                                        } else Profile.DefaultBadge()
                                                    }
                                                    Text(
                                                        text = "${option.username}",
                                                        maxLines = 1,
                                                        style = TextStyle(
                                                            fontFamily = FontFamily.SansSerif,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 18.sp
                                                        ),
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier
                                                            .padding(
                                                                5.dp,
                                                                0.dp,
                                                                5.dp,
                                                                0.dp
                                                            )
                                                            .align(Alignment.CenterVertically),
                                                        onTextLayout = { textLayoutResult ->
                                                            if (textLayoutResult.hasVisualOverflow) {

                                                            }
                                                        },
                                                        color = MaterialTheme.colors.secondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        textStateUsernameCannotBeYourself = textStateUsernameFilter == loggedInUser.username
                        if (textStateUsernameCannotBeYourself) Text(
                            "You cannot follow yourself",
                            Modifier.fillMaxWidth(),
                            color = MaterialTheme.colors.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        selectedUser =  userSuggestion.firstOrNull() { x-> x.username == textStateUsernameFilter}
                        selectedUser?.let { userToFollow->
                            if (!followPossible && !textStateUsernameCannotBeYourself && textStateUsernameFilter.length <= Config.MAX_USERNAME_LENGTH && textStateUsernameFilter.length >= Config.MIN_HASHTAG_LENGTH)
                            {
                                val authorUUID = UUID.fromString(loggedInUser.user_uuid)
                                val pendingFollow = storySavedViewModel.storyDatabase.storyPendingUploadDao()
                                    .getAllForAssociatedID(authorUUID, userToFollow.username)
                                pendingFollow.observe(lifecycleOwner) { x ->
                                    if (x.isNotEmpty() && x.any { z -> z.resourceType == PendingUploadType.FOLLOW }) {
                                        followPossible = false
                                    }
                                }
                                val actualFollow =
                                    storySavedViewModel.storyDatabase.userFollowedDao().getFollowedForUser(authorUUID, userToFollow.username)
                                actualFollow.observe(lifecycleOwner) { x ->
                                    followPossible = x == null
                                }
                                if (!followPossible) Text(
                                    "You are already following this user",
                                    Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colors.error,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Button(
                                onClick = onDismissRequest,
                                Modifier
                                    .width(110.dp)
                                    .padding(0.dp, 0.dp, 15.dp, 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = MaterialTheme.colors.onPrimary
                                )
                            ) {
                                Text(
                                    modifier = Modifier,
                                    text = "Back"
                                )
                            }
                            Button(
                                {selectedUser?.let {selected-> onConfirmRequest(selected)  } },
                                Modifier,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.secondary,
                                    contentColor = Color.White
                                ),
                                enabled = followPossible
                            ) {
                                Text("Follow user", Modifier)
                            }
                        }
                    }
                }
            }
        }


        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun ShowConnectWalletDialog(onDismissRequest: () -> Unit, onConnectRequest: () -> Unit) {
            Dialog(
                onDismissRequest = onDismissRequest,
                DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(250.dp)
                        .height(200.dp)
                        .background(MaterialTheme.colors.surface, shape = RoundedCornerShape(15.dp))
                ) {
                    Column() {
                        Text(
                            "Connect Wallet",
                            Modifier.fillMaxWidth(),
                            MaterialTheme.colors.onSurface,
                            18.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Column(Modifier.padding(0.dp, 10.dp)) {
                            Text(
                                "Connect your Phantom wallet to transfer your rewards",
                                Modifier,
                                MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                                14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(
                            Modifier
                                .fillMaxWidth()
                        ) {
                            Card(
                                onClick = onConnectRequest,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .align(Alignment.CenterHorizontally),
                                backgroundColor = Color.Black,
                                shape = RoundedCornerShape(15)
                            ) {
                                Row() {
                                    Image(
                                        painter = rememberImagePainter(
                                            // rememberImagePainter
                                            data = R.drawable.phantom_logo_purple,
                                            builder = {
                                                crossfade(true)
                                            },
                                        ),
                                        contentDescription = "Claimed",
                                        modifier = Modifier
                                            .padding(10.dp, 2.dp, 15.dp, 0.dp)
                                            .height(30.dp)
                                            .zIndex(15f),
                                    )
                                }

                            }
                            Button(
                                onConnectRequest,
                                Modifier
                                    .align(Alignment.CenterHorizontally),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.secondary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("CONNECT", Modifier)
                            }
                        }
                    }

                }
            }
        }
        fun Modifier.customShadow(
            color: Color = Color.Black.copy(alpha = 0.2f), // Softer default shadow
            offsetX: Dp = 0.dp,
            offsetY: Dp = 2.dp, // Slight downward offset by default
            blurRadius: Dp = 4.dp, // Default blur for depth
            isCircle:Boolean = false
        ) =  then(
            drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint()
                    val frameworkPaint = paint.asFrameworkPaint()
                    if (blurRadius != 0.dp) {
                        frameworkPaint.maskFilter = (BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL))
                    }
                    frameworkPaint.color = color.toArgb()

                    val leftPixel = offsetX.toPx()
                    val topPixel = offsetY.toPx()
                    val rightPixel = size.width + topPixel
                    val bottomPixel = size.height + leftPixel

                    if (isCircle) canvas.drawCircle(Offset.Zero,size.width,paint)
                    else canvas.drawRect(
                        left = leftPixel,
                        top = topPixel,
                        right = rightPixel,
                        bottom = bottomPixel,
                        paint = paint,
                    )
                }
            }
        )

    }
}