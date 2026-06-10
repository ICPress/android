package net.crowdventures.storypop

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.*
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ContentInfoCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chiralcode.colorpicker.ColorPickerDialog
import com.google.android.flexbox.FlexboxLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImage.isUriRequiresPermissions
import kotlinx.coroutines.*
import net.crowdventures.storypop.adapters.TagSearchAdapter
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.ArticlePrivateSource
import net.crowdventures.storypop.dto.ArticlePublicSource
import net.crowdventures.storypop.dto.StoryMap
import net.crowdventures.storypop.libs.RoundedBackgroundSpan
import net.crowdventures.storypop.map.editor.MapEditorActivity
import net.crowdventures.storypop.util.*
import net.crowdventures.storypop.viewmodels.*
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.properties.Delegates


class ArticleContentEditActivity : BaseActivity() {
    private lateinit var softInputLayout: LinearLayout
    private var titleMenuShown = false
    lateinit var sharedPreferenceManager: SharedPreferenceManager
    private lateinit var publishFab: ExtendedFloatingActionButton
    private lateinit var textStyleManager: TextStyleManager
    lateinit var contentEditText: EditText
    private lateinit var titleEditText: EditText
    //FloatingActionButtons
    private lateinit var titleFabHighlight: FloatingActionButton
    private lateinit var titleFabBackground: FloatingActionButton
    private lateinit var titleFabImage: FloatingActionButton
    private lateinit var titleFabMapBackground: FloatingActionButton
    private lateinit var titleRelativeView: RelativeLayout
    private lateinit var titleBackgroundImageView: ImageView
    private lateinit var storyUUID: UUID
    private lateinit var originalStoryUUID: UUID
    private lateinit var tagsSelectedFlex: FlexboxLayout
    lateinit var storyViewModel: StoryViewModel
    private lateinit var tagsAutoCompleteTextView: AutoCompleteTextView
    private lateinit var modeTextView: TextView
    private lateinit var locationAutoCompleteTextView: AutoCompleteTextView
    private lateinit var locationImg: ImageView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var defaultUUID: UUID
    private lateinit var locationProgressBar: ProgressBar
    private lateinit var sourcesLinearLayout: LinearLayout
    private var publishedModelSlugTitle: String? = null
    private var publishedModelEmptyTitle: String? = null
    private lateinit var nestedScrollView: NestedScrollView
    private var nestedScrollViewRichTextBarOffset: Int by Delegates.notNull<Int>()
    private var loggedInUser: AccountInfoFull? = null
    private var leftInset = 0
    private var rightInset = 0
    private var bottomInset = 0

    companion object {
        val richContentMimeTypes = arrayOf("image/*", "text/plain") //, "video/*"
        const val SELECTED_TAGS_KEY = "selected_tag"
        const val PUBLISH_CODE = 3482
        const val AI_SUGGESTION_CODE = 3220923
        const val AI_CONTENT_RESULT = "AI_CONTENT_RESULT"
        const val UPDATED_STORY_SLUG_TITLE_RESULT = "UPDATED_STORY_SLUG_TITLE_RESULT"
        val savedStoryModelKey = "SavedStoryModelKey"
        val savedInstanceStateKey = "StoryUUIDKey"
        val savedInstanceStateOriginalKey = "OriginalStoryUUIDKey"
        val savedInstanceStateSlugTitle = "OriginalStorySlugtTitle"
        val savedInstanceStatePublishedEmptyTitle = "OriginalStoryEmptyTitle"
        val savedInstanceStateLoggedInUser = "LoggedInUser"
    }

    private val selectColorDialog =
        { view: View ->

            val previousColor =
                if (view.id == R.id.fab_title_menu_background) storyViewModel.titleBackgroundColor.value else storyViewModel.titleHighlightColor.value?.backgroundColor

            // Create theme with app's colors
            val theme = ColorPickerDialog.ColorPickerTheme(
                ContextCompat.getColor(this@ArticleContentEditActivity, R.color.primaryColor),
                ContextCompat.getColor(this@ArticleContentEditActivity, R.color.secondaryColor),
                ContextCompat.getColor(this@ArticleContentEditActivity, R.color.textSecondary),
                ContextCompat.getColor(this@ArticleContentEditActivity, R.color.textPrimary),
                ContextCompat.getColor(this@ArticleContentEditActivity, R.color.colorSurface),
                ContextCompat.getColor(this@ArticleContentEditActivity, R.color.secondaryColor_20),
                ContextCompat.getColor(this@ArticleContentEditActivity, R.color.colorBorder)
            )

            val colorPickerDialog =
                ColorPickerDialog(
                    this@ArticleContentEditActivity,
                    previousColor ?: 0,
                    getColorSelectedListenerForView(view),
                    theme  // Pass the theme here
                )
            colorPickerDialog.show()
        }

    fun getColorSelectedListenerForView(view: View): ColorPickerDialog.OnColorSelectedListener {
        return object : ColorPickerDialog.OnColorSelectedListener {
            override fun onNoColorSelected() {
                if (view.id == R.id.fab_title_menu_background) {
                    storyViewModel.titleBackgroundColor.value = null
                } else if (view.id == R.id.fab_title_menu_text_highlight) {
                    storyViewModel.clearTitleHighlightColor()
                }
            }

            override fun onColorSelected(color: Int) {
                if (view.id == R.id.fab_title_menu_background) {
                    storyViewModel.viewModelScope.async {
                        storyViewModel.clearTitleImageUri()
                        storyViewModel.setTitleBackgroundColor(color)
                    }
                } else if (view.id == R.id.fab_title_menu_text_highlight) {

                    storyViewModel.viewModelScope.async { //<- NOT NEEDED HERE
                        storyViewModel.setTitleHighlightColor(color)
                    }
                }
            }
        }
    }

    private fun showKeyboardSoftInputExtras() {
        //fix text being hidden when soft-input is shown
        nestedScrollView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(leftInset, bottomInset, rightInset, nestedScrollViewRichTextBarOffset)
        } //to show full text when soft-input is gone
        //nestedScrollView.requestLayout()
        //
        softInputLayout.visibility = View.VISIBLE
        publishFab.hide()

    }

    private fun hideKeyboardSoftInputExtras() {
        nestedScrollView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(0, 0, 0, 0)
        }
        softInputLayout.visibility = View.GONE
    }

    private val onContentEditTextInputFocusLostListener =
        View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideKeyboardSoftInputExtras()
            } else {
                showKeyboardSoftInputExtras()
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    private val addImageToTitleHandler = View.OnClickListener { v ->
        startActivityForResult(
            ImageUtil.requestImageIntent(this),
            Constants.GET_IMAGE_TITLE_CONTENT_INTENT
        )
    }
    private val onTextStyleChangeButtonClicked = View.OnClickListener { it ->
        it as ImageButton
        when (it.id) {
            R.id.bold_text_btn -> textStyleManager.enableDisableStyle(
                TextStyle.BOLD,
                it
            )

            R.id.italic_text_btn -> textStyleManager.enableDisableStyle(
                TextStyle.ITALIC,
                it
            )

            R.id.underline_text_btn -> textStyleManager.enableDisableStyle(
                TextStyle.UNDERLINE,
                it
            )

            R.id.text_size_increase_btn -> textStyleManager.enableDisableStyle(
                TextStyle.TEXT_SIZE_LARGE,
                it
            )

            R.id.refer_link_text_btn -> {
                textStyleManager.enableDisableStyle(
                    TextStyle.REFER_LINK,
                    it
                )
            }

            R.id.image_text_btn -> textStyleManager.enableDisableStyle(
                TextStyle.IMAGE,
                it
            )

            R.id.ai_text_btn -> {
                AIArticleEditHelperUtil.handleAIRewritePressed(
                    contentEditText,
                    textStyleManager,
                    storyViewModel.viewModelScope,
                    sharedPreferenceManager
                )
            }
        }
    }


    override fun onBackPressed() {
        if (titleMenuShown) {
            titleMenuShown = false
            titleEditText.clearFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(titleEditText.windowToken, 0)
        } else {
            super.onBackPressed()
            if (publishedModelSlugTitle == null) {
                val contentTitle = titleEditText.text.toString()
                if (contentTitle.equals(resources.getString(R.string.unnamed_story)) ||
                    contentTitle == resources.getString(R.string.default_story_title)
                ) {
                    titleEditText.text.clear()
                }
                saveDraft()
            }
        }
    }

    private fun initGetStorySavedViewModel(): StorySavedViewModel {
        val viewModel = ViewModelProvider(
            this, StorySavedViewModelFactory(
                this,
                StorySavedViewModelRepository(
                    this.applicationContext,
                    sharedPreferenceManager,
                    this,
                    RegisterViewModel()
                )
            )
        ).get(StorySavedViewModel::class.java)
        ArticleListActivity.storySavedViewModel = viewModel
        return viewModel
    }

    private fun saveDraft() {
        val stylingInfo = storyViewModel.generateStylingInfo(contentEditText, true)
        val title =
            if (titleEditText.text.isEmpty()) getString(R.string.unnamed_story) else titleEditText.text.toString()
        var savedViewModel = ArticleListActivity.storySavedViewModel
        if (savedViewModel == null) {
            savedViewModel = initGetStorySavedViewModel()
        }
        savedViewModel.saveUserStory(
            this,
            contentEditText.text.toString(),
            title,
            locationAutoCompleteTextView.text.toString(),
            storyUUID,
            originalStoryUUID,
            defaultUUID,
            stylingInfo,
            Constants.loggedInUser?.username,
            false, storyViewModel.selectedTags.value,
            storyViewModel.publicSources.value ?: arrayOf(),
            storyViewModel.privateSources.value ?: arrayOf(),
            storyViewModel.mapData.value
        )
        Toast.makeText(this, "draft saved", Toast.LENGTH_SHORT).show()
        Config.firebaseAnalytics.logEvent("story_draft_created", null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(savedInstanceStateKey, storyUUID.toString())
        outState.putString(savedInstanceStateOriginalKey, originalStoryUUID.toString())
        outState.putParcelable(savedInstanceStateLoggedInUser, Constants.loggedInUser as Parcelable)
        if (publishedModelSlugTitle != null) outState.putString(
            savedInstanceStateSlugTitle,
            publishedModelSlugTitle
        )
        if (publishedModelEmptyTitle != null) outState.putString(
            savedInstanceStatePublishedEmptyTitle,
            publishedModelEmptyTitle
        )
        storyViewModel.saveUserStory(contentEditText, outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Log.v(Config.logTag, "onRestoreInstanceState called")
        super.onRestoreInstanceState(savedInstanceState)
        val restorableEnabledStyle = storyViewModel.restoreState(savedInstanceState)
        if (savedInstanceState.containsKey(savedInstanceStateKey) &&
            savedInstanceState.containsKey(savedInstanceStateOriginalKey) && restorableEnabledStyle != null
        ) {
            storyUUID = UUID.fromString(savedInstanceState.getString(savedInstanceStateKey))
            originalStoryUUID =
                UUID.fromString(savedInstanceState.getString(savedInstanceStateOriginalKey))
            loggedInUser = savedInstanceState.getParcelable(savedInstanceStateLoggedInUser)
            if (loggedInUser != null) Constants.loggedInUser = loggedInUser
            Log.v(
                Config.logTag,
                "onRestoreInstanceState, restored logged in user:" + loggedInUser?.username
            )
            if (savedInstanceState.containsKey(savedInstanceStateSlugTitle)) publishedModelSlugTitle =
                savedInstanceState.getString(savedInstanceStateSlugTitle)
            if (savedInstanceState.containsKey(savedInstanceStatePublishedEmptyTitle)) publishedModelEmptyTitle =
                savedInstanceState.getString(savedInstanceStatePublishedEmptyTitle)
            val spanList = contentEditText.text.getSpans(
                0,
                contentEditText.text.length - 1,
                Any::class.java
            ).filterIsInstance<ParcelableSpan>()
            val enabledStyles = textStyleManager.enabledStyles.toList()
            mapParcellableKeyValuePairToStyles(restorableEnabledStyle, spanList, enabledStyles)
            updatePrivateSourcesDisplay()
            updatePublicSourcesDisplay()
            Log.v(Config.logTag, "Done mapping restorableEnabledStyle")
        } else {
            Log.e(
                Config.logTag,
                "Restore failed, contains key: storyUUID " + savedInstanceState.containsKey(
                    savedInstanceStateKey
                )
            )
            contentEditText.text.clear()
        }
    }

    fun mapParcellableKeyValuePairToStyles(
        restorableEnabledStyle: ParcellableKeyValuePair,
        spanList: List<ParcelableSpan>,
        enabledStyles: List<EnabledStyle>
    ) {
        val keyValuePairList = restorableEnabledStyle.keyValuePair
        val keysList = keyValuePairList.keys.toIntArray()
        for (currentKey in keysList) {
            Log.v(
                Config.logTag, "trying to map key $currentKey " +
                        "to enabledStyles with size ${enabledStyles.size} and spanlist with size ${spanList.size} "
            )
            if (keyValuePairList[currentKey] == null) Log.e(
                Config.logTag,
                "keyValuePairList[currentKey] is null!!"
            )
            if (spanList.size > currentKey && keyValuePairList[currentKey] != null && enabledStyles.size > keyValuePairList[currentKey]!!) {
                Log.v(
                    Config.logTag,
                    "trying to map index ${keyValuePairList[currentKey]} of enabledStyles to index $currentKey of spanlist," +
                            " enabledStyle type is ${enabledStyles[keyValuePairList[currentKey]!!].spanInfo.style.name}, target spanClass is ${spanList[currentKey].javaClass.name}"
                )
                //correct any possible miss-alignments
                val enabledStyle = enabledStyles[keyValuePairList[currentKey]!!]
                val spanInfo = enabledStyle.spanInfo
                spanInfo.start = spanInfo.start.coerceAtLeast(0).coerceAtMost(spanInfo.end)
                    .coerceAtMost(contentEditText.text.length)
                spanInfo.end = spanInfo.end.coerceAtLeast(spanInfo.start)
                    .coerceAtMost(contentEditText.text.length)
                enabledStyle.span = spanList[currentKey]
            }

        }
        storyViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
            val imageSpansToRestore = enabledStyles.filter { it.spanInfo.style == TextStyle.IMAGE }
            val currentLoggedInUser = loggedInUser
            if (currentLoggedInUser == null) return@launch
            for (imageSpanToRestore in imageSpansToRestore) {
                val span = ImageClickSpan(
                    this@ArticleContentEditActivity, imageSpanToRestore.spanInfo, null,
                    publishedModelSlugTitle != null, contentEditText, currentLoggedInUser
                )
                runOnUiThread {
                    val spanInfoEnd = imageSpanToRestore.spanInfo.end.coerceAtLeast(
                        0
                    ).coerceAtMost(contentEditText.text.length)
                    if (spanInfoEnd != imageSpanToRestore.spanInfo.end) { //there is trimming of text during saving of state
                        imageSpanToRestore.spanInfo.end = spanInfoEnd
                        imageSpanToRestore.spanInfo.start = spanInfoEnd
                    }
                    contentEditText.text.setSpan(
                        span,
                        (imageSpanToRestore.spanInfo.end - Config.IMAGE_START_SEPARATOR.length).coerceAtLeast(
                            0
                        ), //imageSpanToRestore.spanInfo.start
                        imageSpanToRestore.spanInfo.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val ph = ImageUtil.getPhotoFileUri(this)
        val mPhotoUri: Uri? = if (ph.exists())
            FileProvider.getUriForFile(
                this,
                "net.crowdventures.storypop.Fileprovider",
                ph
            ) else null
        if ((requestCode == Constants.GET_IMAGE_CONTENT_INTENT || requestCode == Constants.GET_IMAGE_TITLE_CONTENT_INTENT)
            && resultCode == Activity.RESULT_OK
        ) {
            if (data?.data == null && mPhotoUri == null) return
            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                data?.data != null && isUriRequiresPermissions(this, data.data!!)
            ) {
                // request permissions and handle the result in onRequestPermissionsResult()
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
            } else {
                if (requestCode == Constants.GET_IMAGE_CONTENT_INTENT) {
                    ImageUtil.requestImage(this, (data?.data ?: mPhotoUri) ?: return)
                } else if (requestCode == Constants.GET_IMAGE_TITLE_CONTENT_INTENT) {
                    ImageUtil.requestImageCroppedVersion(
                        this,
                        (data?.data ?: mPhotoUri) ?: return,
                        true,
                        true
                    )
                }
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (data != null) {
                val cropImageActivityResult = CropImage.getActivityResult(data)
                val currentLoggedInUser = loggedInUser
                if (resultCode == RESULT_OK && cropImageActivityResult?.uri != null && currentLoggedInUser != null) {
                    val bitmapUri = cropImageActivityResult.uri
                    val isTitleBackgroundImage = cropImageActivityResult.isTitleBackgroundImage
                    insertImageAsync(bitmapUri, isTitleBackgroundImage, currentLoggedInUser)
                }
            }
        } else if (requestCode == AI_SUGGESTION_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val pairContent = data.getStringArrayListExtra(AI_CONTENT_RESULT)
            if (pairContent != null && pairContent.size == 2) {
                // Set title
                titleEditText.setText(pairContent[0])
                // Process markdown content
                val rawContent = pairContent[1]
                val (processedContent, markdownSpans) = MarkdownUtil.processMarkdownWithCorrectOffsets(
                    rawContent
                )
                val spanString = SpannableStringBuilder(processedContent)
                // Apply all markdown styles
                contentEditText.post {
                    storyViewModel.setEnabledStyles(
                        StoryUtil.restoreSpannableFromStylingInfo(
                            this,
                            markdownSpans,
                            spanString,
                            publishedModelSlugTitle != null,
                            contentEditText,
                            loggedInUser!!
                        ).first
                    )
                    contentEditText.setText(spanString)
                }
            }
        } else if (requestCode == PUBLISH_CODE && resultCode == Activity.RESULT_OK) {
            doPublish(
                loggedInUser ?: return,
                contentEditText.text.toString(),
                titleEditText.text.toString(),
                storyViewModel.alternativeTitle.value ?: "",
                locationAutoCompleteTextView.text.toString(),
                storyViewModel.generateStylingInfo(contentEditText, true),
                storyViewModel.storySavedModel?.storyMap
            )
        } else if (requestCode == MapEditorActivity.REQUEST_CODE && resultCode == RESULT_OK) {
            val centerLat = data?.getDoubleExtra(MapEditorActivity.RESULT_CENTER_LAT, 0.0)
            val centerLng = data?.getDoubleExtra(MapEditorActivity.RESULT_CENTER_LNG, 0.0)
            val zoom = data?.getDoubleExtra(MapEditorActivity.RESULT_ZOOM, 4.0)
            val geoJson = data?.getStringExtra(MapEditorActivity.RESULT_GEOJSON)
            val screenshotUri = data?.getStringExtra(MapEditorActivity.RESULT_SCREENSHOT_URI)

            if (zoom != null && centerLat != null && centerLng != null && geoJson != null){
                // Save GeoJSON to your story model
                storyViewModel.mapData.value = StoryMap(
                    centerLat  = centerLat,
                    centerLng  = centerLng,
                    zoom       = zoom,
                    geoJson    = geoJson
                )

                // Insert screenshot as title background image
                if (!screenshotUri.isNullOrEmpty()) {
                    val uri = Uri.parse(screenshotUri)
                    insertImageAsync(uri, true, loggedInUser!!)
                }
            }

            }
    }

    fun insertImageAsync(uri: Uri, isTitleBackgroundImage: Boolean, loggedInUser: AccountInfoFull) {
        storyViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
            val inputStream: InputStream =
                this@ArticleContentEditActivity.contentResolver.openInputStream(uri)
                    ?: return@launch
            inputStream.use { stream ->
                val bitmapOptions = BitmapFactory.Options()
                bitmapOptions.inMutable = true
                bitmapOptions.inScaled = false
                val bitmap =
                    BitmapFactory.decodeStream(
                        stream,
                        null,
                        bitmapOptions
                    ) // BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) return@launch
                if (isTitleBackgroundImage) {
                    val scaledBitmap = ImageUtil.getScaledBitmap(
                        bitmap,
                        Config.MAX_IMAGE_FULL_WIDTH,
                        Config.MAX_IMAGE_FULL_HEIGHT
                    )
                    val compressedUri = ImageUtil.compressAndReturnBitmapUri(
                        scaledBitmap,
                        this@ArticleContentEditActivity, null
                    )
                    if (compressedUri != null) {
                        val orgFile = File(compressedUri.toString())
                        val minatureBitmap = ImageUtil.getMiniatureBitmapFromDeviceWidth(
                            scaledBitmap,
                            Config.MAX_IMAGE_SCALED_WIDTH,
                            contentEditText.context
                        )
                        if (minatureBitmap.width < scaledBitmap.width || minatureBitmap.height < scaledBitmap.height) {
                            val scaledUri = ImageUtil.compressAndReturnBitmapUri(
                                minatureBitmap,
                                this@ArticleContentEditActivity, orgFile.name
                            )
                            if (scaledUri != null) {
                                storyViewModel.setTitleImageUri(
                                    compressedUri,
                                    scaledBitmap,
                                    minatureBitmap,
                                    this@ArticleContentEditActivity
                                )
                            }
                        } else {
                            storyViewModel.setTitleImageUri(
                                compressedUri,
                                scaledBitmap, null,
                                this@ArticleContentEditActivity
                            )
                        }
                    }
                } else { //below method calls compress indirectly
                    textStyleManager.insertImage(
                        bitmap,
                        this@ArticleContentEditActivity, loggedInUser
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling_edit)
        setSupportActionBar(findViewById(R.id.toolbar))
        val dpRatio = resources.displayMetrics.density
        val rootView = findViewById<View>(android.R.id.content).rootView
        publishFab = findViewById<ExtendedFloatingActionButton>(R.id.reviewFab)
        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { v: View, windowInsets: WindowInsetsCompat ->
            val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. This solution sets only the
            // bottom, left, and right dimensions, but you can apply whichever insets are
            // appropriate to your layout. You can also update the view padding if that's
            // more appropriate.
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            publishFab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
            leftInset = insets.left
            bottomInset = insets.bottom
            rightInset = insets.right
            WindowInsetsCompat.CONSUMED
        }
        nestedScrollViewRichTextBarOffset = (50 * dpRatio).toInt()
        sharedPreferenceManager = SharedPreferenceManager(this)
        defaultUUID = sharedPreferenceManager.getDefaultAuthorUUID()
        softInputLayout = findViewById(R.id.softinput_extra_layout)
        contentEditText = findViewById<EditText>(R.id.content_et)
        val aiSuggestion = findViewById<LinearLayout>(R.id.text_et_suggestion_btn_rl)
        aiSuggestion.setOnClickListener {
            val aiIntent = Intent(this, AIWizardActivity::class.java)
            if (!titleEditText.text.contentEquals(resources.getString(R.string.default_story_title)) && titleEditText.text.isNotEmpty())
                aiIntent.putExtra(
                    AIWizardActivity.EXISTING_TITLE_EXTRA,
                    titleEditText.text.toString()
                )
            val groqKey = sharedPreferenceManager.getGroqKey()
            if (groqKey == null) {
                val onSuccess = object : SuccessCallback<String> {
                    override fun onSuccess(vararg param: String) {
                        sharedPreferenceManager.setGroqKey(param.first())
                        startActivityForResult(aiIntent, AI_SUGGESTION_CODE)
                    }

                    override fun onFailure(reason: Any?) {
                    }
                }
                AIArticleEditHelperUtil.showEnterApiKeyDialog(onSuccess, layoutInflater, this)
            } else {
                startActivityForResult(aiIntent, AI_SUGGESTION_CODE)
            }
        }
        contentEditText.addTextChangedListener { x ->
            if (x?.length != null && x.isNotEmpty()) aiSuggestion.visibility = View.GONE
            else aiSuggestion.visibility = View.VISIBLE
        }
        nestedScrollView = findViewById<NestedScrollView>(R.id.edit_nested_scrollView)
        titleBackgroundImageView = findViewById<ImageView>(R.id.content_title_iv)
        modeTextView = findViewById(R.id.modeTextView)
        modeTextView.text = getString(R.string.unnamed_story) //+DateTime.now().toString(Config.dtf)
        titleEditText = findViewById<EditText>(R.id.content_title_et)
        // Apply dynamic gradient that grows with text
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.TRANSPARENT,
                0x99000000.toInt(),   // 60% black mid-way
                0xCC000000.toInt()    // 80% black at bottom
            )
        )
        titleEditText.post {
            titleEditText.background = gradient
        }
        initSources()

        val titleStyleToolbar = findViewById<LinearLayout>(R.id.title_style_toolbar)
        val appBarEdit = findViewById<AppBarLayout>(R.id.app_bar_edit)

        val titleFocusListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                titleMenuShown = true
                if (titleEditText.text.contentEquals(resources.getString(R.string.default_story_title))) {
                    titleEditText.text.clear()
                }

                titleStyleToolbar.visibility = View.VISIBLE
                titleStyleToolbar.translationY = titleStyleToolbar.height.toFloat().coerceAtLeast(400f)
                titleStyleToolbar.alpha = 0f
                titleStyleToolbar.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

                appBarEdit.setExpanded(true, true)

            } else {
                titleEditText.background = gradient

                // Only hide the toolbar if it is still visible
                // (drag dismiss sets it to GONE before clearFocus is called)
                if (titleStyleToolbar.visibility == View.VISIBLE) {
                    titleStyleToolbar.animate()
                        .translationY(titleStyleToolbar.height.toFloat())
                        .alpha(0f)
                        .setDuration(250)
                        .setInterpolator(AccelerateInterpolator())
                        .withEndAction { titleStyleToolbar.visibility = View.GONE }
                        .start()
                }
            }
        }

        titleEditText.onFocusChangeListener = titleFocusListener
        findViewById<FloatingActionButton>(R.id.fab_title_menu_open).setOnClickListener {
            titleStyleToolbar.visibility = View.GONE
        }

        titleStyleToolbar.setOnTouchListener(object : View.OnTouchListener {
            var dragStartY = 0f
            var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartY = event.rawY
                        isDragging = false
                        return true  // ← true here is the key fix, consumes the event
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dragDistance = event.rawY - dragStartY
                        if (dragDistance > 20f) {
                            isDragging = true
                            titleStyleToolbar.translationY = dragDistance
                        }
                        return isDragging
                    }
                    MotionEvent.ACTION_UP -> {
                        val dragDistance = event.rawY - dragStartY
                        if (dragDistance > 120f) {
                            titleStyleToolbar.animate()
                                .translationY(titleStyleToolbar.height.toFloat())
                                .alpha(0f)
                                .setDuration(200)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction {
                                    titleStyleToolbar.visibility = View.GONE
                                    titleStyleToolbar.translationY = 0f
                                    titleStyleToolbar.alpha = 1f
                                    titleEditText.onFocusChangeListener = null
                                    titleEditText.clearFocus()
                                    titleEditText.onFocusChangeListener = titleFocusListener
                                }
                                .start()
                        } else {
                            titleStyleToolbar.animate()
                                .translationY(0f)
                                .setDuration(150)
                                .setInterpolator(DecelerateInterpolator())
                                .start()
                        }
                        isDragging = false
                        return true  // ← consume UP as well
                    }
                    else -> return false
                }
            }
        })
        val locationAddButton = findViewById<CardView>(R.id.cv_location_btn)
        val locationInputLayout = findViewById<LinearLayout>(R.id.ll_location_text)
        locationAutoCompleteTextView = findViewById(R.id.location_ac_tv)
        locationAddButton.setOnClickListener {
            locationAddButton.visibility = View.GONE
            locationInputLayout.visibility = View.VISIBLE
            locationAutoCompleteTextView.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(locationAutoCompleteTextView, InputMethodManager.SHOW_IMPLICIT)
        }

        titleBackgroundImageView.setOnClickListener {
            titleEditText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(titleEditText, InputMethodManager.SHOW_IMPLICIT)
        }
        titleEditText.addTextChangedListener(MultilineRemover.getMultilineTextRemover(1) { x ->
            if (x.length > 0 && x.length > 30) modeTextView.text = x.substring(0, 30) + ".."
            else if (x.length > 0) modeTextView.text = x
            else {
                modeTextView.text = getString(R.string.unnamed_story)
                storyViewModel.clearTitleHighlightColor()
            } //+DateTime.now().toString(Config.dtf)
        })
        ViewCompat.setOnReceiveContentListener(
            contentEditText,
            richContentMimeTypes,
            richContentReceiver
        );
        attachKeyboardListeners()
        var storySavedModel: StorySavedModel? = null
        if (intent.hasExtra(savedStoryModelKey)) {
            storySavedModel = intent.getParcelableExtra<StorySavedModel>(savedStoryModelKey)
                ?: return
            if (storySavedModel is StoryPublishedModel) {
                publishedModelSlugTitle = storySavedModel.slugTitle
                publishedModelEmptyTitle = storySavedModel.emptyTitle
            }
            storyUUID = storySavedModel.storyUUID
            originalStoryUUID = storySavedModel.storyOriginalUUID
        } else {
            storyUUID = UUID.randomUUID()
            originalStoryUUID = storyUUID
        } //create new UUID for a new story if we do not have reference
        var spanString: SpannableStringBuilder? = null
        var spanStringTitle: SpannableStringBuilder? = null
        var mappedEnabledStyle = mutableListOf<EnabledStyle>()
        if (loggedInUser == null) loggedInUser = Constants.loggedInUser
        val loggedInUserCurrent = loggedInUser
        if (storySavedModel != null && loggedInUserCurrent != null) {
            var storyTitle = storySavedModel.storyTitle
            if (storyTitle.equals(resources.getString(R.string.unnamed_story)) || storyTitle.equals(
                    resources.getString(R.string.default_story_title)
                )
            ) storyTitle = ""
            if (storySavedModel.location.isNotEmpty()) {
                locationAutoCompleteTextView.setText(storySavedModel.location)
                locationAddButton.visibility = View.GONE
                locationInputLayout.visibility = View.VISIBLE
            }
            spanString = SpannableStringBuilder(storySavedModel.contentText.trimEnd('\u0000'))
            spanStringTitle = SpannableStringBuilder(storyTitle)
            mappedEnabledStyle = StoryUtil.restoreSpannableFromStylingInfo(
                this,
                storySavedModel.stylingInfo.spans,
                spanString,
                publishedModelSlugTitle != null,
                contentEditText,
                loggedInUserCurrent
            ).first

            StoryUtil.restoreTitleBackgroundColorFromStylingInfo(
                this,
                storySavedModel.stylingInfo,
                titleBackgroundImageView
            )
            if (publishedModelSlugTitle == null) {
                StoryUtil.restoreTitleBackgroundImageFromStylingInfo(
                    this,
                    storySavedModel.stylingInfo,
                    titleBackgroundImageView,
                    loggedInUserCurrent
                )
            } else {
                StoryUtil.restoreTitleBackgroundImageFromStylingInfoFromRemote(
                    this,
                    storySavedModel.stylingInfo,
                    titleBackgroundImageView,
                    loggedInUserCurrent
                )
            }
            StoryUtil.restoreTitleHighlightColorFromStylingInfo(
                storySavedModel.stylingInfo,
                spanStringTitle
            )
        }
        storyViewModel =
            ViewModelProvider(
                this,
                StoryViewModelFactory(this, storySavedModel, mappedEnabledStyle.toTypedArray())
            ).get(
                StoryViewModel::class.java
            )
        if (spanString != null) contentEditText.setText(spanString) //initialize text after viewmodel
        if (spanStringTitle != null) titleEditText.setText(spanStringTitle) //initialize text after viewmodel
        val toolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)
        toolbarLayout.setExpandedTitleTextAppearance(R.style.ExpandedAppBar)
        toolbarLayout.setCollapsedTitleTextAppearance(R.style.CollapsedAppBar)
        val addTagBtn = findViewById<CardView>(R.id.add_tag_btn)
        titleFabImage = findViewById<FloatingActionButton>(R.id.fab_title_menu_image)
        titleFabHighlight = findViewById<FloatingActionButton>(R.id.fab_title_menu_text_highlight)
        titleFabHighlight.setOnClickListener(selectColorDialog)
        titleFabBackground = findViewById<FloatingActionButton>(R.id.fab_title_menu_background)
        titleFabBackground.setOnClickListener(selectColorDialog)
        titleFabImage.setOnClickListener(addImageToTitleHandler)
        titleFabMapBackground =findViewById<FloatingActionButton>(R.id.fab_add_map)
        titleFabMapBackground.setOnClickListener {
            val mapData  = storyViewModel.mapData.value
            if (mapData ==null) {
                MapEditorActivity.startForResult(
                    context = this,
                    centerLat = 0.0,
                    centerLng = 0.0,
                    zoom = 1.0,     // Show whole world
                    title = titleEditText.text.toString(),
                    articleContent = contentEditText.text.trimEnd('\u0000').toString()
                )
            }else{
                MapEditorActivity.startForResult(
                    context         = this,
                    existingGeoJson = mapData.geoJson,
                    centerLat = mapData.centerLat,
                    centerLng = mapData.centerLng,
                    zoom = mapData.zoom,
                )
            }
        }
        sourcesLinearLayout =findViewById<LinearLayout>(R.id.sources_ll)
        val publishText = if (loggedInUserCurrent?.requireArticleReview == true) getString(R.string.review) else getString(R.string.publish)
        publishFab.text =
            if (publishedModelSlugTitle == null) publishText else getString(R.string.update)
        if (loggedInUserCurrent?.requireArticleSources == true) sourcesLinearLayout.visibility = View.VISIBLE
        contentEditText.onFocusChangeListener = onContentEditTextInputFocusLostListener
        textStyleManager = TextStyleManager(
            contentEditText,
            {
                startActivityForResult(
                    ImageUtil.requestImageIntent(this),
                    Constants.GET_IMAGE_CONTENT_INTENT
                )
            },
            storyViewModel,
            this
        )
        (contentEditText as MonitoringEditText).textStyleManager = textStyleManager
        (contentEditText as MonitoringEditText).editActivity = this
        if (savedInstanceState != null) loggedInUser =
            savedInstanceState.getParcelable(savedInstanceStateLoggedInUser)
        Log.v(Config.logTag, "ArticleContent running onCreate with user:" + loggedInUser?.username)
        val hashtagWarningTextView = findViewById<TextView>(R.id.hashtag_warning_tv)
        hashtagWarningTextView.text =getString(R.string.hashtag_length_info_warning, Config.MIN_HASHTAG_LENGTH)
        tagsSelectedFlex = findViewById(R.id.tags_selected_flex)
        storyViewModel.selectedTags.observe(this) { x ->
            if (x == null) return@observe
            if (x.size > Config.MAX_HASHTAG_COUNT) {
                storyViewModel.selectedTags.value = ArrayList(x.subList(0, Config.MAX_HASHTAG_COUNT-1))
                return@observe
            }
            tagsSelectedFlex.removeAllViews()
            for (tag in x) {
                inflateTagView(tag)
            }
            hashtagWarningTextView.visibility = View.GONE
            if (storyViewModel.selectedTags.value?.size == Config.MAX_HASHTAG_COUNT || publishedModelSlugTitle != null) {
                tagsAutoCompleteTextView.visibility = View.GONE
                tagsAutoCompleteTextView.isEnabled = false
                addTagBtn.visibility = View.GONE
            } else if (storyViewModel.selectedTags.value?.size != Config.MAX_HASHTAG_COUNT) {
                tagsAutoCompleteTextView.visibility = View.VISIBLE
                tagsAutoCompleteTextView.isEnabled = true
                addTagBtn.visibility = View.VISIBLE
            }
        }
        tagsAutoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.hashtags_ac_tv)
        tagsAutoCompleteTextView.hint = "#${getString(R.string.today).lowercase()}"
        if (publishedModelSlugTitle != null) {
            tagsAutoCompleteTextView.visibility = View.GONE
            addTagBtn.visibility = View.GONE
        } else {
            addTagBtn.setOnClickListener {
                val trimmedConstraint =
                    tagsAutoCompleteTextView.text.toString().lowercase().replace("#", "")
                        .replace(" ", "").replace("\n", "")
                if (trimmedConstraint.length >= Config.MIN_HASHTAG_LENGTH) {
                    hashtagWarningTextView.visibility = View.GONE
                    onTagSuggestionSelectedListener.onTagAdded(trimmedConstraint)
                } else {
                    StoryUtil.showShowWarningSnackbar(
                        rootView,
                        nestedScrollView,
                        tagsAutoCompleteTextView,
                        this,
                        getString(R.string.hashtag_minimum_length_warning, Config.MIN_HASHTAG_LENGTH),
                        publishFab
                    )
                }
                //addTagBtn.visibility = View.GONE
            }
            val tagsSearchAdapter = TagSearchAdapter(
                this,
                storyViewModel,
                this,
                onTagSuggestionSelectedListener, addTagBtn
            )
            tagsAutoCompleteTextView.setOnEditorActionListener { tv, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    val trimmedConstraint =
                        tv.text.toString().lowercase().replace("#", "").replace(" ", "")
                            .replace("\n", "")
                    if (trimmedConstraint.length >= Config.MIN_HASHTAG_LENGTH) {
                        onTagSuggestionSelectedListener.onTagAdded(trimmedConstraint)
                    } else {
                        hashtagWarningTextView.visibility = View.VISIBLE
                        StoryUtil.showShowWarningSnackbar(
                            rootView,
                            nestedScrollView,
                            tagsAutoCompleteTextView,
                            this,
                            getString(R.string.hashtag_minimum_length_warning, Config.MIN_HASHTAG_LENGTH),
                            publishFab
                        )
                    }
                    if (storyViewModel.selectedTags.value?.size != Config.MAX_HASHTAG_COUNT) return@setOnEditorActionListener true
                }
                false
            }
            tagsAutoCompleteTextView.addTextChangedListener { x ->
                val trimmedConstraint =
                    x.toString().lowercase().replace("#", "").replace(" ", "").replace("\n", "")
                if (trimmedConstraint.length >= Config.MIN_HASHTAG_LENGTH) {
                    hashtagWarningTextView.visibility = View.GONE
                } else {
                    hashtagWarningTextView.visibility = View.VISIBLE
                }
            }
            tagsAutoCompleteTextView.setAdapter(tagsSearchAdapter)
        }
        tagsAutoCompleteTextView.filters = arrayOf(TagInputFilter())
        val usernameAuthorTextView = findViewById<TextView>(R.id.username_post_tv)
        val usernameAuthorBigTextView = findViewById<TextView>(R.id.username_post_big_tv)
        val authorBadgeImageView = findViewById<ImageView>(R.id.avatar_iv)
        usernameAuthorTextView.text = (loggedInUserCurrent?.username ?: "new_user")
        val authorBadgeMetadata = loggedInUserCurrent?.profileIcon
        if (authorBadgeMetadata != null) {
            val localImage =
                ImageUtil.getLocalImageBitmapFromMetaData(this, false, authorBadgeMetadata, false)
            if (localImage != null) {
                authorBadgeImageView.setImageBitmap(localImage)
            } else {
                ImageUtil.loadMinBitmapImageViewRemote(
                    authorBadgeMetadata,
                    this,
                    authorBadgeImageView,
                    loggedInUserCurrent
                )
            }
        }
        publishFab.setOnClickListener {
            val selectedTags = storyViewModel.selectedTags.value
            if (selectedTags == null || selectedTags.isEmpty()) {
                StoryUtil.showShowWarningSnackbar(
                    rootView,
                    nestedScrollView,
                    tagsAutoCompleteTextView,
                    this,
                    "Select at least one tag",
                    publishFab
                )
                return@setOnClickListener
            }
            val publishedTitle = publishedModelEmptyTitle
            val contentTitle = titleEditText.text.toString()
            val contentText = contentEditText.text.trimEnd('\u0000').toString()
            val contentLocation = locationAutoCompleteTextView.text.toString()
            val stylingInfo = storyViewModel.generateStylingInfo(contentEditText, true)
            val titleIsEmpty =
                contentTitle.equals(resources.getString(R.string.unnamed_story)) || contentTitle == resources.getString(
                    R.string.default_story_title
                ) || contentTitle.isEmpty()
            if (titleIsEmpty && storyViewModel.titleImageUri.value == null) {
                StoryUtil.showShowWarningSnackbar(
                    rootView,
                    nestedScrollView,
                    titleEditText,
                    this,
                    "Add an image or title to your story",
                    publishFab
                )
                return@setOnClickListener
            }
            if (contentEditText.length() < 6) {
                StoryUtil.showShowWarningSnackbar(
                    rootView,
                    nestedScrollView,
                    contentEditText,
                    this,
                    "Text must be longer than 6 characters.",
                    publishFab
                )
                return@setOnClickListener
            }
            if (loggedInUserCurrent?.requireArticleSources == true && storyViewModel.privateSources.value?.count() == 0 && storyViewModel.publicSources.value?.count() == 0) {
                StoryUtil.showShowWarningSnackbar(
                    rootView,
                    nestedScrollView,
                    sourcesLinearLayout,
                    this,
                    "Must supply at least one source",
                    publishFab
                )
                return@setOnClickListener
            }
            var cntSpaces = 0
            var cntSpacesIndexStart = contentText.indexOf(' ')
            while (cntSpacesIndexStart >= 0) {
                cntSpaces++
                cntSpacesIndexStart = contentText.indexOf(' ', cntSpacesIndexStart + 1)
            }
            if (cntSpaces < 5) {
                StoryUtil.showShowWarningSnackbar(
                    rootView,
                    nestedScrollView,
                    contentEditText,
                    this,
                    "Text must have at least one sentence.",
                    publishFab
                )
                return@setOnClickListener
            }
            if (publishedTitle == null && titleIsEmpty) {
                if (loggedInUserCurrent != null) selectDescriptiveTitle(
                    loggedInUserCurrent,
                    contentText,
                    contentLocation,
                    stylingInfo
                )
                return@setOnClickListener
            }
            if (loggedInUserCurrent != null) {
                initiatePublishReview(loggedInUserCurrent,contentText,contentTitle, publishedTitle ?:"", contentLocation, stylingInfo, storyViewModel.mapData.value )
            } else {
                saveDraft()
            }


        }
        findViewById<ImageButton>(R.id.bold_text_btn).setOnClickListener(
            onTextStyleChangeButtonClicked
        )
        findViewById<ImageButton>(R.id.italic_text_btn).setOnClickListener(
            onTextStyleChangeButtonClicked
        )
        findViewById<ImageButton>(R.id.underline_text_btn).setOnClickListener(
            onTextStyleChangeButtonClicked
        )
        findViewById<ImageButton>(R.id.text_size_increase_btn).setOnClickListener(
            onTextStyleChangeButtonClicked
        )

        findViewById<ImageButton>(R.id.refer_link_text_btn).setOnClickListener(
            onTextStyleChangeButtonClicked
        )
        findViewById<ImageButton>(R.id.image_text_btn).setOnClickListener(
            onTextStyleChangeButtonClicked
        )
        findViewById<ImageButton>(R.id.ai_text_btn).setOnClickListener(
            onTextStyleChangeButtonClicked
        )
        textStyleManager.styleButtons = arrayOf(
            findViewById<ImageButton>(R.id.bold_text_btn),
            findViewById<ImageButton>(R.id.italic_text_btn),
            findViewById<ImageButton>(R.id.underline_text_btn),
            findViewById<ImageButton>(R.id.text_size_increase_btn),
            findViewById<ImageButton>(R.id.refer_link_text_btn),
            findViewById<ImageButton>(R.id.image_text_btn),
        )

        titleRelativeView = findViewById<RelativeLayout>(R.id.titleContent_rl)

        storyViewModel.titleHighlightColor.observe(this) { x ->
            if (x != null) {
                titleEditText.text.removeSpan(x) // titleEditText.text.clearSpans()
                titleEditText.text.setSpan(
                    x,
                    0,
                    titleEditText.text.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
            } else {
                val spanList = titleEditText.text.getSpans(
                    0,
                    titleEditText.text.length,
                    Any::class.java
                )?.filterIsInstance<RoundedBackgroundSpan>()
                if (spanList != null && spanList.isNotEmpty())
                    spanList.forEach { titleEditText.text.removeSpan(it) }
            }
        }

        storyViewModel.titleImageUri.observe(this) { x ->
            if (x != null) {
                storyViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                    val backgroundBitmap = ImageUtil.getLocalImageBitmapFromMetaData(
                        this@ArticleContentEditActivity,
                        false,
                        x,
                        true
                    )
                    withContext(Dispatchers.Main) {
                        titleBackgroundImageView.clearColorFilter()
                        titleBackgroundImageView.setImageBitmap(
                            backgroundBitmap ?: return@withContext
                        )
                        storyViewModel.setTitleBackgroundColor(null)
                    }
                }
            }
        }
        storyViewModel.titleBackgroundColor.observe(this) { x ->
            if (x != null && x != 0) {
                titleBackgroundImageView.setColorFilter(x, PorterDuff.Mode.SRC)
                val layoutParams = RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    resources.getDimension(R.dimen._250sdp).toInt()
                )
                titleBackgroundImageView.layoutParams = layoutParams
            } else if (storyViewModel.titleImageUri.value == null) titleBackgroundImageView.setColorFilter(
                getColor(R.color.textSecondary),
                PorterDuff.Mode.SRC
            )
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationImg = findViewById(R.id.locationImg)
        locationProgressBar = findViewById(R.id.location_pb)
        if (Geocoder.isPresent()) {
            locationAutoCompleteTextView.setOnFocusChangeListener { view, b ->
                if (b) {
                    locationImg.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.ic_baseline_my_location_24,
                            null
                        )
                    )
                    locationImg.imageTintList =
                        ColorStateList.valueOf(resources.getColor(R.color.primaryColor))
                    locationImg.setOnClickListener {
                        locationImg.visibility = View.GONE
                        locationProgressBar.visibility = View.VISIBLE
                        locationPermissionRequest.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                } else {
                    locationImg.imageTintList = null
                    locationImg.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.ic_maps_black,
                            null
                        )
                    )
                    locationImg.setOnClickListener(null)
                }

            }
        }
        setupUIHideIMEOnNonEditText(findViewById(android.R.id.content))
        textStyleManager.setTextWatcher()
        updatePrivateSourcesDisplay()
        updatePublicSourcesDisplay()
    }

    private fun initiatePublishReview(loggedInUserCurrent:AccountInfoFull, contentText: String, contentTitle:String, alternativeTitle:String, contentLocation:String, stylingInfo:StylingInfo, storyMap: StoryMap?){
        if (loggedInUser?.requireArticleReview == true && publishedModelSlugTitle == null) {
            val groqKey = sharedPreferenceManager.getGroqKey()
            if (groqKey == null) {
                val onSuccess = object : SuccessCallback<String> {
                    override fun onSuccess(vararg param: String) {
                        sharedPreferenceManager.setGroqKey(param.first())
                        startReviewProcess()
                    }

                    override fun onFailure(reason: Any?) {
                    }
                }
                AIArticleEditHelperUtil.showEnterApiKeyDialog(
                    onSuccess,
                    layoutInflater,
                    this
                )
            }else{
                intent.putExtra(Constants.EDIT_CONTENT_TEXT_STYLES, stylingInfo as Parcelable)
                intent.putExtra(Constants.EDIT_MODE_ENABLED_EXTRA, true)
                intent.putExtra(Constants.EDIT_CONTENT_TITLE, contentTitle.ifEmpty { alternativeTitle })
                intent.putExtra(Constants.EDIT_CONTENT_TEXT, contentText)
                intent.putStringArrayListExtra(Constants.EDIT_CONTENT_TAGS, storyViewModel.selectedTags.value)
                intent.putExtra(
                    Constants.EDIT_CONTENT_LOCATION,
                    contentLocation
                )
                startReviewProcess()
            }
        }
        else{
            doPublish(
                loggedInUserCurrent,
                contentText,
                contentTitle,
                alternativeTitle,
                contentLocation,
                stylingInfo,
                storyMap
            )
        }
    }

    private fun setupUIHideIMEOnNonEditText(view: View) {
        if (view !is EditText
            && (view.parent as View).id != R.id.softinput_extra_layout
            && view.id != R.id.content_title_iv
            && view.id != R.id.title_style_toolbar  // ← exclude the sheet itself
            && (view.parent as? View)?.id != R.id.title_style_toolbar  // ← exclude direct children
            && getParentWithId(view, R.id.title_style_toolbar) == null  // ← exclude all descendants
        ) {
            view.setOnTouchListener(hideIMEOnTouchListener)
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUIHideIMEOnNonEditText(innerView)
            }
        }
    }

    // Helper to walk up the tree and check if any ancestor has a given id
    private fun getParentWithId(view: View, id: Int): View? {
        var current = view.parent
        while (current != null) {
            if (current is View && current.id == id) return current
            current = current.parent
        }
        return null
    }
    val hideIMEOnTouchListener = object : View.OnTouchListener {
        override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
            currentFocus?.let {
                val imm: InputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
            return false
        }
    }

    private fun doPublish(
        loggedInUser: AccountInfoFull,
        contentText: String,
        contentTitle: String,
        emptyTitleText: String,
        contentLocation: String,
        stylingInfo: StylingInfo,
        storyMap: StoryMap?
    ) {
        val publishedTitle = publishedModelSlugTitle
        var storySavedViewModel = ArticleListActivity.storySavedViewModel
        if (storySavedViewModel == null) {
            storySavedViewModel = initGetStorySavedViewModel()
        }
        if (publishedTitle == null) {
            Config.firebaseAnalytics.logEvent("story_publish_started", null)
            storySavedViewModel.publishUserStory(
                this,
                contentText,
                contentTitle,
                emptyTitleText,
                contentLocation,
                storyUUID,
                originalStoryUUID,
                UUID.fromString(loggedInUser.user_uuid),
                stylingInfo, storyViewModel.selectedTags.value,
                storyViewModel.publicSources.value?: arrayOf(),
                storyViewModel.privateSources.value?: arrayOf(),
                storyViewModel.mapData.value
            )
        } else {
            Config.firebaseAnalytics.logEvent("story_update_started", null)
            storySavedViewModel.updateUserStory(
                this,
                contentText,
                contentTitle,
                emptyTitleText,
                contentLocation,
                originalStoryUUID,
                UUID.fromString(loggedInUser.user_uuid),
                stylingInfo,
                publishedTitle, storyViewModel.selectedTags.value,
                storyViewModel.publicSources.value?: arrayOf(),
                storyViewModel.privateSources.value?: arrayOf(),
                storyMap
            )
            this.setResult(
                Activity.RESULT_OK,
                Intent().putExtra(
                    ArticleContentEditActivity.UPDATED_STORY_SLUG_TITLE_RESULT,
                    publishedTitle
                )
            )
        }
        finish()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun selectDescriptiveTitle(
        loggedInUser: AccountInfoFull, contentText: String,
        contentLocation: String, stylingInfo: StylingInfo
    ) {
        saveDraft()
        val li: LayoutInflater = LayoutInflater.from(this)
        val view = li.inflate(R.layout.alternative_title_dialog, null)
        val errorTextTv = view.findViewById<TextView>(R.id.error_text)
        val closeBtn = view.findViewById<ImageView>(R.id.close_btn)
        val contentTitleTv = view.findViewById<EditText>(R.id.content_title_et)
        val alertDialog: AlertDialog = this.let {
            val builder = AlertDialog.Builder(it)
            builder.setView(view)
            builder.create()
        }
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val okBtn = view.findViewById<Button>(R.id.ok_btn)
        okBtn.setOnClickListener { _ ->
            val alternativeTitle = contentTitleTv.text.toString()
            if (alternativeTitle.isNotEmpty()) {
                storyViewModel.alternativeTitle.value = alternativeTitle
                initiatePublishReview(loggedInUser, contentText, "", alternativeTitle, contentLocation, stylingInfo, storyViewModel.mapData.value)
            } else {
                StoryUtil.showShowWarningSnackbar(
                    view,
                    null,
                    contentTitleTv,
                    this,
                    "Enter a title for your story containing at least 3 characters",
                    publishFab
                )
            }

        }
        closeBtn.setOnClickListener { alertDialog.dismiss() }
        val cancelBtn = view.findViewById<Button>(R.id.cancel_btn)
        cancelBtn.setOnClickListener { alertDialog.dismiss() }
        alertDialog.show()
    }

    @SuppressLint("MissingPermission")
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION) && permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                if (isLocationEnabled()) {
                    fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                        val location: Location? = task.result
                        if (location != null) {
                            val geocoder = Geocoder(this, Locale.getDefault())
                            val addresses: MutableList<Address> =
                                geocoder.getFromLocation(
                                    location.latitude,
                                    location.longitude,
                                    1
                                ) as MutableList<Address>
                            locationAutoCompleteTextView.setText(addresses[0].getAddressLine(0))
                        } else {
                            Log.e(Config.logTag, "There was no latest location..")
                            Toast.makeText(
                                this,
                                "Could not find current location",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Location services disabled", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            else -> {
                Toast.makeText(this, "Missing permissions", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        locationImg.visibility = View.VISIBLE
        locationProgressBar.visibility = View.GONE
    }


    val richContentReceiver = object : androidx.core.view.OnReceiveContentListener {
        override fun onReceiveContent(view: View, payload: ContentInfoCompat): ContentInfoCompat {
            for (i in 0 until payload.clip.itemCount) {
                val uriContent = payload.clip.getItemAt(i).uri
                val currentLoggedInUser = loggedInUser
                if (uriContent != null && currentLoggedInUser != null) {
                    insertImageAsync(uriContent, false, currentLoggedInUser)
                }

            }
            return payload
        }

    }

    // Add after existing view initializations
    private fun initSources() {
        val addPublicSourceBtn = findViewById<Button>(R.id.add_public_source_btn)
        val addPrivateSourceBtn = findViewById<Button>(R.id.add_private_source_btn)

        addPublicSourceBtn.setOnClickListener { showAddPublicSourceDialog() }
        addPrivateSourceBtn.setOnClickListener { showAddPrivateSourceDialog() }
    }

    private fun showAddPublicSourceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_public_source, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val closeBtn = dialogView.findViewById<ImageView>(R.id.close_btn)
        val urlInput = dialogView.findViewById<EditText>(R.id.source_url)
        val urlsMultipleInput = dialogView.findViewById<EditText>(R.id.source_urls_multiple)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_btn)
        val addBtn = dialogView.findViewById<Button>(R.id.add_btn)

        closeBtn.setOnClickListener { dialog.dismiss() }
        cancelBtn.setOnClickListener { dialog.dismiss() }

        addBtn.setOnClickListener {
            // Check single URL input first
            val singleUrl = urlInput.text.toString().trim()
            val multipleUrls = urlsMultipleInput.text.toString().trim()

            if (singleUrl.isNotEmpty()) {
                // Add single URL
                storyViewModel.publicSources.value = storyViewModel.publicSources.value?.plus(singleUrl)
                updatePublicSourcesDisplay()
                dialog.dismiss()
            } else if (multipleUrls.isNotEmpty()) {
                // Process multiple URLs (one per line)
                val urls = multipleUrls.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it.startsWith("http") }

                if (urls.isNotEmpty()) {
                    urls.forEach { url ->
                        storyViewModel.publicSources.value = storyViewModel.publicSources.value?.plus(
                        url)
                    }
                    updatePublicSourcesDisplay()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Please enter at least one valid URL", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun startReviewProcess() {
        val contentText = contentEditText.text.toString()
        if (contentText.length < 100) {
            Toast.makeText(this, "Please write at least 100 characters before review", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedTags = storyViewModel.selectedTags.value
        if (selectedTags.isNullOrEmpty()) {
            Toast.makeText(this, "Please add at least one tag", Toast.LENGTH_SHORT).show()
            return
        }

        val contentTitle = titleEditText.text.toString()
        val titleIsEmpty = contentTitle.isEmpty() ||
                contentTitle.equals(resources.getString(R.string.unnamed_story)) ||
                contentTitle == resources.getString(R.string.default_story_title)

        if (titleIsEmpty && storyViewModel.titleImageUri.value == null && storyViewModel.alternativeTitle.value == null) {
            Toast.makeText(this, "Please add a title or image to your story", Toast.LENGTH_SHORT).show()
            return
        }

        val stylingInfo = storyViewModel.generateStylingInfo(contentEditText, true)
        val location = locationAutoCompleteTextView.text.toString()

        val intent = ArticleReviewActivity.startForReview(
            context = this,
            title = contentTitle.ifEmpty { storyViewModel.alternativeTitle.value ?:"" },
            content = contentText,
            tags = selectedTags,
            location = location,
            stylingInfo = stylingInfo,
            publicSources = storyViewModel.publicSources.value?: arrayOf(),
            privateSources = storyViewModel.privateSources.value?: arrayOf(),
            storyUUID = storyUUID,
            originalUUID = originalStoryUUID,
            publishedSlug = publishedModelSlugTitle
        )

        startActivityForResult(intent, PUBLISH_CODE)
    }

    private fun showAddPrivateSourceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_private_source, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val closeBtn = dialogView.findViewById<ImageView>(R.id.close_btn)
        val nameInput = dialogView.findViewById<EditText>(R.id.source_name)
        val addressInput = dialogView.findViewById<EditText>(R.id.source_address)
        val phoneInput = dialogView.findViewById<EditText>(R.id.source_phone)
        val notesInput = dialogView.findViewById<EditText>(R.id.source_notes)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_btn)
        val addBtn = dialogView.findViewById<Button>(R.id.add_btn)

        closeBtn.setOnClickListener { dialog.dismiss() }
        cancelBtn.setOnClickListener { dialog.dismiss() }

        addBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isNotEmpty()) {
                val source = ArticlePrivateSource(
                    name = name,
                    address = addressInput.text.toString().trim(),
                    phone = phoneInput.text.toString().trim(),
                    notes = notesInput.text.toString().trim()
                )
                storyViewModel.privateSources.value = storyViewModel.privateSources.value?.plus(
                    source
                )
                updatePrivateSourcesDisplay()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter source name", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updatePublicSourcesDisplay() {
        val container = findViewById<LinearLayout>(R.id.public_sources_container)
        container.removeAllViews()
        (storyViewModel.publicSources.value?: arrayOf()).forEachIndexed { index, source ->
            val view = layoutInflater.inflate(R.layout.item_public_source, container, false)
            view.findViewById<TextView>(R.id.source_title).text = StoryUtil.extractTitleFromUrl(source)
            view.findViewById<TextView>(R.id.source_url).text = source
            val descriptionView = view.findViewById<TextView>(R.id.source_description)
            view.findViewById<ImageView>(R.id.remove_source_btn).setOnClickListener {
                storyViewModel.publicSources.value  = storyViewModel.publicSources.value?.filter { x-> x!=source  }?.toTypedArray()
                updatePublicSourcesDisplay()
            }
            container.addView(view)
        }
    }

    private fun updatePrivateSourcesDisplay() {
        val container = findViewById<LinearLayout>(R.id.private_sources_container)
        container.removeAllViews()
        (storyViewModel.privateSources.value?: arrayOf()).forEachIndexed { index, source ->
            val view = layoutInflater.inflate(R.layout.item_private_source, container, false)
            view.findViewById<TextView>(R.id.source_name).text = source.name
            val addressView = view.findViewById<TextView>(R.id.source_address)
            if (source.address.isNotEmpty()) {
                addressView.text = source.address
                addressView.visibility = View.VISIBLE
            }
            val phoneView = view.findViewById<TextView>(R.id.source_phone)
            if (source.phone.isNotEmpty()) {
                phoneView.text = source.phone
                phoneView.visibility = View.VISIBLE
            }
            val notesView = view.findViewById<TextView>(R.id.source_notes)
            if (source.notes.isNotEmpty()) {
                notesView.text = source.notes
                notesView.visibility = View.VISIBLE
            }
            view.findViewById<ImageView>(R.id.remove_source_btn).setOnClickListener {
                storyViewModel.privateSources.value = storyViewModel.privateSources.value?.filter { x-> x!= source }?.toTypedArray()
                updatePrivateSourcesDisplay()
            }
            container.addView(view)
        }
    }
    fun inflateTagView(tag: String) {
        val newView = if (publishedModelSlugTitle != null) layoutInflater.inflate(
            R.layout.tag_layout,
            tagsSelectedFlex,
            false
        )
        else layoutInflater.inflate(R.layout.selected_tag_layout, tagsSelectedFlex, false)
        val inflatedText = newView.findViewById<TextView>(R.id.hashtag_tv)
        if (inflatedText != null) {
            inflatedText.text = tag
            inflatedText.tag = SELECTED_TAGS_KEY
        }
        if (publishedModelSlugTitle == null) newView.setOnClickListener {
            onTagSuggestionSelectedListener.onTagRemoved(
                tag
            )
        }
        tagsSelectedFlex.addView(newView)
    }

    val onTagSuggestionSelectedListener = object : TagCollectionChangeCallback {
        override fun onTagAdded(tag: String) {
            runOnUiThread {
                tagsAutoCompleteTextView.text.clear()
                tagsAutoCompleteTextView.background = null
                storyViewModel.addSelectedTag(tag.lowercase())
            }
        }

        override fun onTagRemoved(tag: String) {
            runOnUiThread {
                storyViewModel.removeSelectedTag(tag)
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    // slide the view from below itself to the current position

    override fun onShowKeyboard(keyboardHeight: Int) {
        // do things when keyboard is hidden
        publishFab.hide()
        if (contentEditText.hasFocus()) {
            showKeyboardSoftInputExtras()
        }
    }

    override fun onHideKeyboard() {
        publishFab.show()
        // do things when keyboard is shown
        hideKeyboardSoftInputExtras()
    }

}