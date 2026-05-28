package net.crowdventures.storypop.composable

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.LocalImageLoader
import coil.compose.rememberImagePainter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.instacart.library.truetime.TrueTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.composable.Article.Companion.ArticleItem
import net.crowdventures.storypop.composable.Article.Companion.ArticleItemPlaceholder
import net.crowdventures.storypop.composable.Profile.Companion.DefaultBadge
import net.crowdventures.storypop.composable.Profile.Companion.StoryPopIcon
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import net.crowdventures.storypop.viewmodels.StorySavedModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

class ArticleList {
    companion object {

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        private fun ShowArticleList(
            lifecycleOwner: LifecycleOwner,
            viewModel: StorySavedViewModel,
            context: Context,
            paddingValues: PaddingValues,
            sharedPreferenceManager: SharedPreferenceManager,
            registerViewModel: RegisterViewModel,
            loggedInUser: AccountInfoFull,
            currentTimeUTC : DateTime
        ) {
            val sinceDayAgo = currentTimeUTC.minusMinutes(1)
            var latestSlugTitlePublished  by remember {
                mutableStateOf<String?>(sharedPreferenceManager.getLatestStoryPublishedSlugTitle())
            }
            var newStoryPublished  by remember {
                mutableStateOf(false)
            }
            viewModel.storyDatabase.storyDao().getPublishedRecentLiveData(sinceDayAgo.toString()).observe(lifecycleOwner) {
                    x->
                if (x?.firstOrNull()?.slugTitle != null && latestSlugTitlePublished != x.firstOrNull()?.slugTitle){
                    newStoryPublished = true
                }
                latestSlugTitlePublished = x?.firstOrNull()?.slugTitle
            }
            val latestStorySlugTitleNotNull = latestSlugTitlePublished
            if (newStoryPublished && latestStorySlugTitleNotNull != null){
                ComposableUtil.QuestionDialog(
                    title = "Share your latest story?",
                    question = "Let your friends see your latest story\uD83D\uDE80",
                    confirmText = "Share✉" ,
                    onDismissRequest = {
                        newStoryPublished = false
                        sharedPreferenceManager.setLatestStoryPublishedSlugTitle(latestStorySlugTitleNotNull)}) {
                    newStoryPublished = false
                    sharedPreferenceManager.setLatestStoryPublishedSlugTitle(latestStorySlugTitleNotNull)
                    StoryUtil.shareArticle(latestStorySlugTitleNotNull,context)
                }

            }
            var fetchedStores by remember { mutableStateOf(viewModel.stories.value) }
            viewModel.stories.observe(lifecycleOwner) { x -> if (x != null) fetchedStores = x }
            val userListItems: LazyPagingItems<StoryPublishedModel> =
                fetchedStores?.collectAsLazyPagingItems() ?: return
            CompositionLocalProvider(
                LocalImageLoader provides net.crowdventures.storypop.Config.getOrSetImageLoader(
                    context
                ),
                LocalContentAlpha provides ContentAlpha.medium
            ) {
                val storiesCachedJson = sharedPreferenceManager.getLatestStoriesCached()
                var storiesCached by remember {
                    mutableStateOf(
                        if (storiesCachedJson != null) Gson().fromJson<List<StoryPublishedModel>>(
                            storiesCachedJson,
                            object : TypeToken<List<StoryPublishedModel>>() {}.type
                        )
                        else listOf()
                    )
                }
                Log.v(Config.logTag, "Has articles to show, rendering articles..")

                val refreshScope = rememberCoroutineScope()
                var refreshing by remember { mutableStateOf(false) }

                val userListPendingItems: LazyPagingItems<StorySavedModel> =
                    viewModel.userPendingPublishedStories.collectAsLazyPagingItems()

                val recentFlow = if (viewModel.hashTagFilter == null) {
                    viewModel.userPublishedRecentStories
                } else {
                    flowOf(PagingData.empty<StorySavedModel>())
                }

                fun refresh() = refreshScope.launch {
                    refreshing = true
                    delay(200)
                    refreshing = false
                    storiesCached = listOf()
                    viewModel.performArticleFiltering(null)
                }
                val userListRecentPublishedItems: LazyPagingItems<StorySavedModel> =recentFlow.collectAsLazyPagingItems()
                val state = rememberPullRefreshState(refreshing, ::refresh)
                Box(Modifier.pullRefresh(state)) {
                    LazyColumn(Modifier, contentPadding = paddingValues) {
                        val isMainEmpty = userListItems.loadState.refresh is LoadState.NotLoading && userListItems.itemCount == 0 && userListItems.loadState.append.endOfPaginationReached
                        val isRecentEmpty = userListRecentPublishedItems.itemCount == 0
                        val isPendingEmpty = userListPendingItems.itemCount == 0
                        if (isMainEmpty && isRecentEmpty && isPendingEmpty) {
                            item {
                                Log.v(Config.logTag, "No articles to show, showing info page.")
                                ShowErrorItem(false)
                            }
                        } else if (userListItems.loadState.refresh is LoadState.Error && userListItems.itemCount == 0) {
                            item {
                                Log.v(
                                    Config.logTag,
                                    "Error when fetching articles, showing error page."
                                )
                                ShowErrorItem(true)
                            }
                        } else if (userListItems.loadState.refresh is LoadState.Loading && userListItems.itemCount == 0 && (storiesCached.isEmpty() || viewModel.hashTagFilter != null)) {
                            item {
                                ArticleItemPlaceholder(context)
                            }
                            item {
                                ArticleItemPlaceholder(context)
                            }
                            item {
                                ArticleItemPlaceholder(context)
                            }
                            item {
                                ArticleItemPlaceholder(context)
                            }
                            item {
                                ArticleItemPlaceholder(context)
                            }
                        } else {
                            this.items(
                                userListPendingItems.itemCount,
                                contentType = { _ -> "ArticleItem" }) { index ->
                                val item = userListPendingItems[index]
                                if (item != null) {
                                    Box() {
                                        ArticleItem(registerViewModel,storyModel = item, onClick = {
                                            StoryUtil.startPreviewActivity(item, context)
                                        }, false, context, loggedInUser)
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colors.secondary,
                                            modifier = Modifier.align(
                                                Alignment.Center
                                            )
                                        )
                                    }
                                }
                            }
                            this.items(
                                userListRecentPublishedItems.itemCount,
                                contentType = { _ -> "ArticleItem" }) { index ->
                                val item = userListRecentPublishedItems[index]
                                if (item != null) {
                                    if (!userListPendingItems.itemSnapshotList.any { it != null && it.authorName == item.authorName && (it.storyTitle == item.storyTitle || it.emptyTitle == item.emptyTitle) }
                                        && !userListItems.itemSnapshotList.any {  it?.slugTitle ==  item.publishedSlugTitle }) {
                                        ArticleItem(registerViewModel,storyModel = item, onClick = {
                                            val slugTitle = item.publishedSlugTitle
                                            if (slugTitle != null){
                                                StoryUtil.startContentActivity(StoryPublishedModel(item.stylingInfo,item.storyTitle,item.emptyTitle, item.contentText,
                                                    item.location,item.langCode,item.tags,item.authorName, item.publicSources, item.privateSources,item.isReviewed,item.storyMap,item.updatedDateTime.toString(),slugTitle,0,0,loggedInUser?.profileIcon,null, null), context)
                                            }else StoryUtil.startPreviewActivity(item, context, true)
                                        }, false, context, loggedInUser)
                                    }
                                }
                            }
                            if (viewModel.hashTagFilter == null) {
                                this.items(
                                    storiesCached,
                                    contentType = { _ -> "ArticleItem" }) { item ->
                                    if (!userListRecentPublishedItems.itemSnapshotList.any {  it != null && it.publishedSlugTitle == item.slugTitle  }
                                        && !userListItems.itemSnapshotList.any {  it?.slugTitle ==  item.slugTitle }) {
                                        val authorBadge = item.authorBadge
                                        if (authorBadge != null){
                                            val imgRequest = ImageUtil.getImageRequestFromMetadata(context, authorBadge,loggedInUser)
                                            Config.getOrSetImageLoader(context).enqueue(imgRequest)
                                            item.badgeImageRequest = imgRequest
                                        }
                                        var likedArticle by remember {
                                            mutableStateOf(
                                                viewModel.likedArticleList.contains(
                                                    item.slugTitle
                                                )
                                            )
                                        }
                                        val activityForResultLauncher =
                                            rememberLauncherForActivityResult(
                                                ActivityResultContracts.StartActivityForResult()
                                            ) { result: ActivityResult ->
                                                likedArticle =
                                                    result.resultCode == Activity.RESULT_OK
                                                if (likedArticle) viewModel.likedArticleList.add(
                                                    item.slugTitle
                                                )
                                                else viewModel.likedArticleList.remove(item.slugTitle)
                                            }
                                        ArticleItem(registerViewModel,storyModel = item, onClick = {
                                            StoryUtil.startContentActivity(
                                                activityForResultLauncher,
                                                item,
                                                context,
                                                likedArticle
                                            )
                                        }, likedArticle, context, loggedInUser)
                                    }
                                }
                            }
                            this.items(
                                userListItems.itemCount,
                                contentType = { _ -> "ArticleItem" }
                            ) { index ->
                                val item = userListItems[index]
                                    if (item != null && !storiesCached.any { it.slugTitle == item.slugTitle && viewModel.hashTagFilter == null }) {
                                        var likedArticle by remember {
                                            mutableStateOf(
                                                viewModel.likedArticleList.contains(
                                                    item.slugTitle
                                                )
                                            )
                                        }
                                        val activityForResultLauncher =
                                            rememberLauncherForActivityResult(
                                                ActivityResultContracts.StartActivityForResult()
                                            ) { result: ActivityResult ->
                                                likedArticle =
                                                    result.resultCode == Activity.RESULT_OK
                                                if (likedArticle) viewModel.likedArticleList.add(
                                                    item.slugTitle
                                                )
                                                else viewModel.likedArticleList.remove(item.slugTitle)
                                            }
                                        ArticleItem(registerViewModel,storyModel = item, onClick = {
                                            StoryUtil.startContentActivity(
                                                activityForResultLauncher,
                                                item,
                                                context,
                                                likedArticle
                                            )
                                        }, likedArticle, context, loggedInUser)
                                    }

                            }
                        }
                    }
                    //standard Pull-Refresh indicator. You can also use a custom indicator
                    PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.TopCenter))

                }
            }
        }

        @OptIn(ExperimentalMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
        @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
        @Composable
        fun ShowArticles(
            viewModel: StorySavedViewModel,
            context: Activity,
            lifecycleOwner: LifecycleOwner,
            bottomBarVisible: Boolean,
            sharedPreferenceManager: SharedPreferenceManager,
            paddingValues: PaddingValues,
            loggedInUser: AccountInfoFull,
            navHostController: NavHostController,
            registerViewModel: RegisterViewModel
        ) {
            val currentTimeUTC = if (TrueTime.isInitialized()) DateTime(TrueTime.now()).toDateTime(DateTimeZone.UTC) else  DateTime(DateTimeZone.UTC)
            val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            val focusRequester = remember { FocusRequester() }
            val interactionSource = remember { MutableInteractionSource() }
            var currentFilter by remember { mutableStateOf(viewModel.hashTagFilter) }

            var hashTagFliterInputVisible by remember { mutableStateOf(false) }
            var textStateHashtagFilter by remember { mutableStateOf(viewModel.hashTagFilter ?: "") }
            val keyboardController = LocalSoftwareKeyboardController.current
            var searchImageVector by remember { mutableStateOf(if (currentFilter?.contains('@') == true) Icons.Default.Person else Icons.Default.Tag) }

            var barExpanded by remember {
                mutableStateOf(viewModel.storyBarExpanded.value ?: true)
            }
            val unexpandedOffset = -ImageUtil.convertDpToPixel(51, context).toFloat()
            val bottomBarOffsetHeightPx =
                remember { mutableStateOf(if (barExpanded) 0f else unexpandedOffset) }
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (barExpanded) {
                            val delta = available.y.dp
                            val newOffset = bottomBarOffsetHeightPx.value + delta.value
                            bottomBarOffsetHeightPx.value =
                                newOffset.coerceIn(
                                    unexpandedOffset,
                                    0f
                                ) //(-bottomBarHeightPx.value * 2.5).toFloat() <- prev minimum
                        }
                        return Offset.Zero
                    }
                }
            }
            //MOVE ALL OUT OF SCAFFOLD CONTENT IF NOT USING FAB
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                floatingActionButton = {
                    Box(
                        if (hashTagFliterInputVisible || currentFilter != null) Modifier
                            .width(200.dp)
                            .height(50.dp) else Modifier.size(50.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (currentFilter != null && currentFilter != "") {
                                    currentFilter = null
                                    viewModel.clearArticleFiltering()
                                } else hashTagFliterInputVisible = true
                            },
                            modifier = if (bottomBarVisible && !hashTagFliterInputVisible && currentFilter == null) Modifier
                                .offset(y = (-50).dp)
                                .size(50.dp) else if (hashTagFliterInputVisible || currentFilter != null)
                                Modifier
                                    .offset(y = (-50).dp)
                                    .width(200.dp)
                                    .height(50.dp)
                            else Modifier
                                .offset(y = (-50).dp)
                                .size(0.dp),
                            shape = AbsoluteRoundedCornerShape(50),
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = MaterialTheme.colors.onSecondary,
                        ) {
                            if (!hashTagFliterInputVisible && currentFilter == null) {
                                AnimatedVisibility(visible = bottomBarVisible) {
                                    Icon(
                                        Icons.Default.Tag,
                                        contentDescription = "Filter by tag",
                                        tint = MaterialTheme.colors.onPrimary
                                    )

                                }
                            } else {
                                var exp by remember { mutableStateOf(false) }
                                var hasFocus by remember { mutableStateOf(false) }
                                var hashTagSuggestion by remember {
                                    mutableStateOf<List<String>>(
                                        listOf()
                                    )
                                }

                                viewModel.hashTagSuggestions.observe(lifecycleOwner) { x ->
                                    if (x != null) hashTagSuggestion = x
                                }
                                fun clearShowHashtagInputField() {
                                    hashTagFliterInputVisible = false
                                    textStateHashtagFilter = ""
                                    hasFocus = false
                                }

                                if (!bottomBarVisible && hasFocus) clearShowHashtagInputField()
                                ExposedDropdownMenuBox(
                                    expanded = exp,
                                    onExpandedChange = { exp = !exp }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            searchImageVector,
                                            contentDescription = "Search icon",
                                            modifier = Modifier
                                                .padding(10.dp, 5.dp, 5.dp, 5.dp)
                                                .size(20.dp),
                                            tint = if (hashTagFliterInputVisible) {
                                                MaterialTheme.colors.onSecondary
                                            } else MaterialTheme.colors.onSecondary.copy(alpha = 0.7f)
                                        )
                                        if (currentFilter != "" && currentFilter != null) {
                                            Text(
                                                text = currentFilter ?: "",
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .align(Alignment.CenterVertically),
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = MaterialTheme.colors.onSecondary  // White text on colored FAB
                                                )
                                            )
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear filter",
                                                modifier = Modifier
                                                    .padding(10.dp, 5.dp, 5.dp, 5.dp)
                                                    .size(20.dp),
                                                tint = MaterialTheme.colors.onSecondary
                                            )
                                        } else {
                                            LaunchedEffect(key1 = Unit, block = {
                                                focusRequester.requestFocus()
                                                hasFocus = true
                                            })
                                            BasicTextField(
                                                value = textStateHashtagFilter,
                                                onValueChange = { x ->
                                                    val clearedText =
                                                        x.replace(
                                                            Regex("((?![^@#])|[^_A-Za-z0-9])"),
                                                            ""
                                                        )
                                                            .lowercase()
                                                    if (clearedText.length >= Config.MIN_HASHTAG_LENGTH && clearedText.length <= Config.MAX_HASHTAG_LENGTH) {
                                                        if (textStateHashtagFilter != clearedText) {
                                                            textStateHashtagFilter = clearedText
                                                            exp = true
                                                            viewModel.findArticleTags(context,
                                                                textStateHashtagFilter
                                                            )
                                                        }
                                                    } else if (clearedText.length <= Config.MAX_HASHTAG_LENGTH) {
                                                        textStateHashtagFilter =
                                                            clearedText
                                                        if (clearedText.contains('@'))
                                                            searchImageVector = Icons.Default.Person
                                                        else if (searchImageVector.name != "Tag") searchImageVector =
                                                            Icons.Default.Tag
                                                    }
                                                },
                                                modifier = Modifier
                                                    .focusRequester(focusRequester)
                                                    .onFocusChanged { z ->
                                                        if (!z.hasFocus && hasFocus && hashTagFliterInputVisible && textStateHashtagFilter.length < Config.MIN_HASHTAG_LENGTH) {
                                                            clearShowHashtagInputField()
                                                        }
                                                    }
                                                    .onKeyEvent {
                                                        if (it.key == Key.Back) {
                                                            clearShowHashtagInputField()
                                                            true
                                                        } else if (it.key == Key.Backspace) {
                                                            exp = false
                                                            true
                                                        } else if (it.key == Key.Enter) { //perform filtering
                                                            if (textStateHashtagFilter != "") currentFilter =
                                                                textStateHashtagFilter
                                                            performSearchFiltering(
                                                                viewModel,
                                                                keyboardController,
                                                                focusRequester,
                                                                currentFilter ?: ""
                                                            ) {
                                                                clearShowHashtagInputField()
                                                            }
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    }
                                                    .indicatorLine(
                                                        enabled = true,
                                                        isError = false,
                                                        colors = TextFieldDefaults.textFieldColors(
                                                            focusedIndicatorColor = MaterialTheme.colors.onSecondary,
                                                            unfocusedIndicatorColor = MaterialTheme.colors.onSecondary.copy(alpha = 0.5f)
                                                        ),
                                                        interactionSource = interactionSource
                                                    )
                                                    .width(140.dp)
                                                    .align(Alignment.CenterVertically),
                                                singleLine = true,
                                                keyboardOptions = keyboardOptions,
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        if (textStateHashtagFilter != "") currentFilter =
                                                            textStateHashtagFilter
                                                        performSearchFiltering(
                                                            viewModel,
                                                            keyboardController,
                                                            focusRequester,
                                                            currentFilter ?: ""
                                                        ) {
                                                            clearShowHashtagInputField()
                                                        }
                                                    }
                                                ),
                                                textStyle = TextStyle(
                                                    color = MaterialTheme.colors.onSecondary,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            )
                                            if (hashTagSuggestion.isNotEmpty()) {
                                                ExposedDropdownMenu(
                                                    expanded = exp,
                                                    onDismissRequest = { exp = false },
                                                    modifier = Modifier.background(MaterialTheme.colors.surface),
                                                ) {
                                                    hashTagSuggestion.forEach { option ->
                                                        DropdownMenuItem(
                                                            modifier = Modifier.padding(0.dp),
                                                            onClick = {
                                                                Log.v(
                                                                    Config.logTag,
                                                                    "Selected autocomplete option:$option"
                                                                )
                                                                textStateHashtagFilter = option
                                                                exp = false
                                                                if (textStateHashtagFilter != "") currentFilter =
                                                                    textStateHashtagFilter
                                                                performSearchFiltering(
                                                                    viewModel,
                                                                    keyboardController,
                                                                    focusRequester,
                                                                    currentFilter ?: ""
                                                                ) {
                                                                    clearShowHashtagInputField()
                                                                }
                                                            },
                                                            contentPadding = PaddingValues(0.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .padding(15.dp)
                                                                    .fillMaxWidth()
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Tag,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colors.secondary,  // Accent blue for suggestions
                                                                    modifier = Modifier
                                                                        .size(18.dp)
                                                                        .align(Alignment.CenterVertically)
                                                                )
                                                                Text(
                                                                    text = option,
                                                                    maxLines = 1,
                                                                    style = TextStyle(
                                                                        fontFamily = FontFamily.SansSerif,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 16.sp,  // Slightly smaller for suggestions
                                                                        color = MaterialTheme.colors.onSurface
                                                                    ),
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier
                                                                        .padding(5.dp, 0.dp, 5.dp, 0.dp)
                                                                        .weight(1f)
                                                                        .align(Alignment.CenterVertically),
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
                },
                floatingActionButtonPosition = FabPosition.End,
                isFloatingActionButtonDocked = false,
                content = { _ ->
                    Box() {
                        Column(
                            Modifier
//                                .padding(0.dp, 25.dp, 0.dp, 0.dp) //FOR REWARD DRAWER
                                .fillMaxHeight()
                        ) {
                            ShowArticleList(
                                lifecycleOwner,
                                viewModel,
                                context,
                                paddingValues,
                                sharedPreferenceManager,
                                registerViewModel,
                                loggedInUser,
                                currentTimeUTC
                            )
                        }
                    }
                }
            )
        }

        @OptIn(ExperimentalComposeUiApi::class)
        fun performSearchFiltering(
            viewModel: StorySavedViewModel, keyboardController: SoftwareKeyboardController?,
            focusRequester: FocusRequester, hashtagFilter: String,
            clearShowHashtagInputField: () -> Unit
        ) {
            if (hashtagFilter != "") viewModel.performArticleFiltering(hashtagFilter)
            clearShowHashtagInputField()
            keyboardController?.hide()
            focusRequester.freeFocus()
        }

        @Composable
        fun ShowErrorItem(isConnectionError: Boolean) {
            Column(
                modifier = Modifier
                    .padding(0.dp, 100.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.CenterVertically)
            ) {

                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.CenterVertically)
                ) {
                    Text(
                        text = if (isConnectionError) "There was a problem with the communication, please check your connection." else "You are reading fast! We have nothing new to show you.",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .width(180.dp)
                            .align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                ) {
                    if (isConnectionError) {
                        Image(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = "Connection Problem",
                            modifier = Modifier
                                .width(200.dp)
                                .height(200.dp)
                                .align(Alignment.CenterHorizontally),
                            colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                        )
                    } else {
                        Column(Modifier.align(Alignment.CenterHorizontally)){
                            StoryPopIcon(80.dp)
                        }

                    }

                }
            }

        }
    }

}