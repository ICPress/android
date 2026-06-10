package net.crowdventures.storypop

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Constants.Companion.EDIT_MODE_ENABLED_EXTRA
import net.crowdventures.storypop.Constants.Companion.PUBLISHED_STORY_EXTRA
import net.crowdventures.storypop.Constants.Companion.PUBLISHED_STORY_HAS_NEW_LIKE
import net.crowdventures.storypop.Constants.Companion.STORY_MAP
import net.crowdventures.storypop.Constants.Companion.STORY_PUBLIC_SOURCES
import net.crowdventures.storypop.Constants.Companion.STORY_PUBLISHED
import net.crowdventures.storypop.Constants.Companion.loggedInUser
import net.crowdventures.storypop.dto.ArticleCommentPublished
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.adapters.ArticleCommentAdapter
import net.crowdventures.storypop.dto.StoryMap
import net.crowdventures.storypop.room.PendingUploadType
import net.crowdventures.storypop.room.StoryComment
import net.crowdventures.storypop.room.StoryPendingUpload
import net.crowdventures.storypop.util.EndpointAdminHandler
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.SuccessCallback
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StoryPublishedModel
import net.crowdventures.storypop.viewmodels.StylingInfo
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import java.util.UUID
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.google.android.material.appbar.CollapsingToolbarLayout
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.sources.GeoJsonSource
import net.crowdventures.storypop.Constants.Companion.STORY_PRIVATE_SOURCES
import net.crowdventures.storypop.dto.ArticlePrivateSource
import net.crowdventures.storypop.map.models.LineStringGeometry
import net.crowdventures.storypop.map.models.MarkerType
import net.crowdventures.storypop.map.models.PointGeometry
import net.crowdventures.storypop.map.models.StoryMapGeoJson
import net.crowdventures.storypop.map.util.MapDrawingUtil

class ArticleContentActivity : AppCompatActivity() {
    lateinit var sharedPreferenceManager: SharedPreferenceManager
    private var oldHighLightSpan: BackgroundColorSpan? = null
    private lateinit var tagsSelectedFlex: FlexboxLayout
    private var publishedModel: StoryPublishedModel? = null
    private var storyIsPublished = false

    // Admin moderation FABs
    private lateinit var adminFabContainer: LinearLayout
    private lateinit var acceptFab: ExtendedFloatingActionButton
    private lateinit var rejectFab: ExtendedFloatingActionButton

    // Map views
    private lateinit var mapContainer: FrameLayout
    private lateinit var mapPreview: MapView
    private lateinit var btnExpandMap: ImageButton
    private var mapboxMap: MapLibreMap? = null
    private var isFullscreenMapVisible = false

    // Comment sheet views
    private lateinit var bottomCommentSheet: LinearLayout
    private lateinit var commentEditText: EditText
    private lateinit var commentSubmitBtn: Button
    private lateinit var commentUserAvatarIv: ImageView
    private lateinit var commentUsernameHint: TextView
    private lateinit var btnWriteComment: Button
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var commentsHeader: TextView
    private lateinit var commentsCountTv: TextView

    // Reply functionality
    private lateinit var replyContextLayout: RelativeLayout
    private lateinit var replyContextTv: TextView
    private lateinit var cancelReplyBtn: ImageView
    private var replyingToComment: ArticleCommentPublished? = null
    private var replyParentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContentView(R.layout.activity_scrolling)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        initializeViews()
        setupCommentSheet()
        loadArticleData(savedInstanceState)
        setupAdminModerationFABs()
    }

    /**
     * Initializes all view references
     */
    private fun initializeViews() {
        sharedPreferenceManager = SharedPreferenceManager(this)
        tagsSelectedFlex = findViewById(R.id.tags_selected_flex)

        // Admin FABs
        adminFabContainer = findViewById(R.id.admin_fab_container)
        acceptFab = findViewById(R.id.accept_fab)
        rejectFab = findViewById(R.id.reject_fab)

        // Map views
        mapContainer = findViewById(R.id.map_container)
        mapPreview = findViewById(R.id.map_preview)
        btnExpandMap = findViewById(R.id.btn_expand_map)

        // Comment sheet views
        bottomCommentSheet = findViewById(R.id.bottom_comment_sheet)
        commentEditText = findViewById(R.id.comment_et)
        commentSubmitBtn = findViewById(R.id.comment_submit_btn)
        commentUserAvatarIv = findViewById(R.id.comment_user_avatar_iv)
        commentUsernameHint = findViewById(R.id.comment_username_hint)
        btnWriteComment = findViewById(R.id.btn_write_comment)
        nestedScrollView = findViewById(R.id.nested_scroll_view)
        commentsHeader = findViewById(R.id.comments_header_tv)
        commentsCountTv = findViewById(R.id.comments_count_tv)

        // Reply views
        replyContextLayout = findViewById(R.id.reply_context_layout)
        replyContextTv = findViewById(R.id.reply_context_tv)
        cancelReplyBtn = findViewById(R.id.cancel_reply_btn)

        cancelReplyBtn.setOnClickListener {
            hideCommentSheet()
            hideKeyboard()
        }

    }

    /**
     * Shows the map in fullscreen mode
     */
    private fun showFullscreenMap(storyMap: StoryMap) {
        val intent = Intent(this, FullscreenMapActivity::class.java).apply {
            putExtra("map_data", storyMap.geoJson)
            putExtra("center_lat", storyMap.centerLat)
            putExtra("center_lng", storyMap.centerLng)
            putExtra("zoom", storyMap.zoom )
        }
        startActivity(intent)
    }

    /**
     * Sets up the map preview using StoryMapGeoJson model with incident radius circles.*/
    private fun setupMapPreview(storyMap: StoryMap, savedInstanceState: Bundle?) {
        mapContainer.visibility = View.VISIBLE
        mapPreview.onCreate(savedInstanceState)

        mapPreview.getMapAsync { map ->
            mapboxMap = map

            val inputStream = resources.openRawResource(R.raw.dark)
            val mapContent = inputStream.bufferedReader().use { it.readText() }
            val nightStyle = Style.Builder().fromJson(mapContent)

            mapPreview.addOnDidFinishLoadingStyleListener {
                map.getStyle { style ->
                    // Register all marker bitmaps now that the GL texture atlas is ready.
                    MapDrawingUtil.addMarkerImages(style)

                    try {
                        val geoJson = StoryMapGeoJson.fromJson(storyMap.geoJson)
                        val pointFeatures  = mutableListOf<org.maplibre.geojson.Feature>()
                        val lineFeatures   = mutableListOf<org.maplibre.geojson.Feature>()
                        val circleFeatures = mutableListOf<org.maplibre.geojson.Feature>()

                        geoJson.features.forEach { feature ->
                            when (val geom = feature.geometry) {
                                is PointGeometry -> {
                                    val point = org.maplibre.geojson.Point.fromLngLat(geom.longitude, geom.latitude)
                                    val mapboxFeature = org.maplibre.geojson.Feature.fromGeometry(point)
                                    mapboxFeature.addStringProperty("label", feature.properties.label ?: "")
                                    mapboxFeature.addStringProperty(
                                        "icon", when (feature.properties.markerType) {
                                            MarkerType.INCIDENT -> MapDrawingUtil.MARKER_ICON_INCIDENT
                                            MarkerType.CITY     -> MapDrawingUtil.MARKER_ICON_CITY
                                            else                -> MapDrawingUtil.MARKER_ICON_ORDINARY
                                        }
                                    )

                                    if (feature.properties.markerType == MarkerType.INCIDENT) {
                                        val radiusMeters = feature.properties.radiusMeters ?: 5000.0
                                        val pixelRadius  = MapDrawingUtil.metersToPixels(mapboxMap, radiusMeters, storyMap.zoom)
                                        mapboxFeature.addNumberProperty("radius_pixels", pixelRadius.toDouble())
                                        circleFeatures.add(mapboxFeature)
                                    }

                                    pointFeatures.add(mapboxFeature)
                                }
                                is LineStringGeometry -> {
                                    val points = geom.latLngPairs().map {
                                        org.maplibre.geojson.Point.fromLngLat(it.second, it.first)
                                    }
                                    val lineString    = org.maplibre.geojson.LineString.fromLngLats(points)
                                    val mapboxFeature = org.maplibre.geojson.Feature.fromGeometry(lineString)
                                    mapboxFeature.addStringProperty("label", feature.properties.label ?: "Path")
                                    lineFeatures.add(mapboxFeature)
                                }
                                else -> {
                                    Log.d("MapPreview", "Unknown geometry type: ${geom?.javaClass?.simpleName}")
                                }
                            }
                        }

                        Log.d("MapPreview", "Points: ${pointFeatures.size}, Lines: ${lineFeatures.size}, Circles: ${circleFeatures.size}")

                        if (pointFeatures.isNotEmpty()) {
                            val pointCollection = org.maplibre.geojson.FeatureCollection.fromFeatures(pointFeatures)
                            style.addSource(GeoJsonSource("story-map-points", pointCollection))
                            // createSymbolLayer sets text-font explicitly so a non-empty
                            // text-field never causes MapLibre to drop the whole symbol.
                            style.addLayer(MapDrawingUtil.createSymbolLayer("story-map-points", "markers"))
                        }

                        if (lineFeatures.isNotEmpty()) {
                            val lineCollection = org.maplibre.geojson.FeatureCollection.fromFeatures(lineFeatures)
                            style.addSource(GeoJsonSource("story-map-lines", lineCollection))
                            style.addLayer(MapDrawingUtil.createLineLayer("story-map-lines", "paths"))
                            Log.d("MapPreview", "Line layer added with ${lineFeatures.size} features")
                        }

                        if (circleFeatures.isNotEmpty()) {
                            val circleCollection = org.maplibre.geojson.FeatureCollection.fromFeatures(circleFeatures)
                            style.addSource(GeoJsonSource("story-map-circles", circleCollection))

                            val circleLayer = org.maplibre.android.style.layers.CircleLayer("incident-circles", "story-map-circles")
                            circleLayer.setProperties(
                                org.maplibre.android.style.layers.PropertyFactory.circleColor("#ff4757"),
                                org.maplibre.android.style.layers.PropertyFactory.circleOpacity(0.15f),
                                org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor("#ff4757"),
                                org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(2f),
                                org.maplibre.android.style.layers.PropertyFactory.circleStrokeOpacity(0.6f),
                                org.maplibre.android.style.layers.PropertyFactory.circleRadius(
                                    org.maplibre.android.style.expressions.Expression.get("radius_pixels")
                                )
                            )
                            style.addLayer(circleLayer)
                            Log.d("MapPreview", "Circle layer added with ${circleFeatures.size} incident circles")
                        }

                        val target = if (storyMap.centerLat != 0.0) LatLng(storyMap.centerLat, storyMap.centerLng) else LatLng(20.0, 0.0)
                        map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(target, storyMap.zoom))

                        map.addOnMapClickListener {
                            showFullscreenMap(storyMap)
                            true
                        }

                    } catch (e: Exception) {
                        Log.e("MapPreview", "Setup error", e)
                        Toast.makeText(this, "Error loading map: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            map.setStyle(nightStyle)
        }
    }
    override fun onStart() {
        super.onStart()
        mapPreview.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapPreview.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapPreview.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapPreview.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapPreview.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapPreview.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapPreview.onSaveInstanceState(outState)
    }
    /**
     * Sets up admin moderation FABs for pending review articles
     */
    private fun setupAdminModerationFABs() {
        val loggedInUser = Constants.loggedInUser
        val isAdmin = loggedInUser?.getRoleFromToken() == Constants.ADMIN_ROLE
        val isPendingReview = publishedModel != null && !publishedModel!!.isReviewed && publishedModel!!.rejectionReason.isNullOrEmpty()

        if (isAdmin && isPendingReview) {
            adminFabContainer.visibility = View.VISIBLE

            acceptFab.setOnClickListener {
                showAcceptConfirmationDialog()
            }

            rejectFab.setOnClickListener {
                showRejectReasonDialog()
            }
        } else {
            adminFabContainer.visibility = View.GONE
        }
    }

    /**
     * Shows confirmation dialog for accepting an article
     */
    private fun showAcceptConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_accept_confirmation, null)
        val closeBtn = dialogView.findViewById<ImageView>(R.id.close_btn)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_btn)
        val acceptBtn = dialogView.findViewById<Button>(R.id.accept_btn)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        closeBtn.setOnClickListener { dialog.dismiss() }
        cancelBtn.setOnClickListener { dialog.dismiss() }
        acceptBtn.setOnClickListener {
            dialog.dismiss()
            acceptArticle()
        }

        dialog.show()
    }

    /**
     * Shows dialog to enter rejection reason
     */
    private fun showRejectReasonDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rejection_reason, null)
        val closeBtn = dialogView.findViewById<ImageView>(R.id.close_btn)
        val reasonEditText = dialogView.findViewById<EditText>(R.id.rejection_reason_et)
        val charCountText = dialogView.findViewById<TextView>(R.id.char_count_tv)
        val submitBtn = dialogView.findViewById<Button>(R.id.submit_btn)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_btn)

        reasonEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                charCountText.text = "$length/190"

                val hasColon = s?.contains(":") == true
                val isValid = length in 10..190 && !hasColon
                submitBtn.isEnabled = isValid

                when {
                    hasColon -> {
                        charCountText.setTextColor(ContextCompat.getColor(this@ArticleContentActivity, R.color.colorError))
                        submitBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@ArticleContentActivity, R.color.colorSurface))
                    }
                    length < 10 -> {
                        charCountText.setTextColor(ContextCompat.getColor(this@ArticleContentActivity, R.color.warning))
                        submitBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@ArticleContentActivity, R.color.colorSurface))
                    }
                    else -> {
                        charCountText.setTextColor(ContextCompat.getColor(this@ArticleContentActivity, R.color.success))
                        submitBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@ArticleContentActivity, R.color.colorError))
                    }
                }
            }
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        closeBtn.setOnClickListener { dialog.dismiss() }
        cancelBtn.setOnClickListener { dialog.dismiss() }

        submitBtn.setOnClickListener {
            val reason = reasonEditText.text.toString().trim()
            if (reason.length in 10..190 && !reason.contains(":")) {
                dialog.dismiss()
                rejectArticle(reason)
            } else {
                Toast.makeText(this, "Reason must be 10-190 characters and cannot contain ':'", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    /**
     * Accepts the article for publication
     */
    private fun acceptArticle() {
        val article = publishedModel ?: return
        val loggedInUser = Constants.loggedInUser ?: return
        adminFabContainer.visibility = View.GONE
        lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
            EndpointAdminHandler.acceptRejectArticle(this@ArticleContentActivity, article.authorName, article.slugTitle, false, "", object :
                SuccessCallback<Unit>{
                override fun onSuccess(vararg param: Unit) {
                    Toast.makeText(this@ArticleContentActivity, "Article accepted", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(reason: Any?) {
                    adminFabContainer.visibility = View.VISIBLE
                    Toast.makeText(this@ArticleContentActivity, "Failed to accept article: ${reason}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /**
     * Rejects the article with a reason
     */
    private fun rejectArticle(reason: String) {
        val article = publishedModel ?: return
        val loggedInUser = Constants.loggedInUser ?: return
        adminFabContainer.visibility = View.GONE
        lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
            EndpointAdminHandler.acceptRejectArticle(this@ArticleContentActivity, article.authorName, article.slugTitle, true, reason, object :
                SuccessCallback<Unit>{
                override fun onSuccess(vararg param: Unit) {
                    Toast.makeText(this@ArticleContentActivity, "Article rejected", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(reason: Any?) {
                    adminFabContainer.visibility = View.VISIBLE
                    Toast.makeText(this@ArticleContentActivity, "Failed to reject article: ${reason}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /**
     * Sets up the TikTok-style comment sheet
     */
    private fun setupCommentSheet() {
        val loggedInUser = Constants.loggedInUser

        if (loggedInUser == null) {
            btnWriteComment.visibility = View.GONE
            return
        }

        updateUserInfoInCommentSheet(loggedInUser)

        commentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                commentSubmitBtn.isEnabled = hasText
                commentSubmitBtn.backgroundTintList = if (hasText) {
                    ColorStateList.valueOf(ContextCompat.getColor(this@ArticleContentActivity, R.color.secondaryColor))
                } else {
                    ColorStateList.valueOf(ContextCompat.getColor(this@ArticleContentActivity, R.color.gray))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        commentEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitComment()
                true
            } else {
                false
            }
        }

        commentSubmitBtn.setOnClickListener {
            submitComment()
        }

        btnWriteComment.setOnClickListener {
            showCommentSheet()
        }

        commentEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (bottomCommentSheet.visibility != View.VISIBLE) {
                    showCommentSheet()
                }
            }
        }

        val bottomSheetHolder = findViewById<FrameLayout>(R.id.standard_bottom_sheet)
        val behavior = BottomSheetBehavior.from(bottomSheetHolder)

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN ||
                    newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    val editText = bottomSheet.findViewById<EditText>(R.id.comment_et)
                    editText?.clearFocus()
                    hideKeyboard()
                    hideCommentSheet()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset < -0.5f) {
                    hideKeyboard()
                    hideCommentSheet()
                }
            }
        })
    }

    /**
     * Updates user info in the comment sheet
     */
    private fun updateUserInfoInCommentSheet(loggedInUser: AccountInfoFull) {
        commentUsernameHint.text = "Commenting as ${loggedInUser.username}"

        val authorBadgeMetadata = loggedInUser.profileIcon
        if (authorBadgeMetadata != null) {
            val localImage = ImageUtil.getLocalImageBitmapFromMetaData(this, false, authorBadgeMetadata, false)
            if (localImage != null) {
                commentUserAvatarIv.setImageBitmap(localImage)
            } else {
                ImageUtil.loadMinBitmapImageViewRemote(authorBadgeMetadata, this, commentUserAvatarIv, loggedInUser)
            }
        }
    }

    /**
     * Shows the comment sheet with animation and optional reply context
     */
    private fun showCommentSheet(commentToReply: ArticleCommentPublished? = null) {
        if (commentToReply != null) {
            setReplyContext(commentToReply)
        }

        if (bottomCommentSheet.visibility != View.VISIBLE) {
            bottomCommentSheet.visibility = View.VISIBLE
            bottomCommentSheet.alpha = 0f
            bottomCommentSheet.animate()
                .alpha(1f)
                .setDuration(300)
                .start()

            commentEditText.postDelayed({
                commentEditText.requestFocus()
                showKeyboard()
            }, 100)
        } else {
            commentEditText.requestFocus()
            showKeyboard()
        }
    }

    /**
     * Sets up reply context
     */
    private fun setReplyContext(comment: ArticleCommentPublished) {
        replyingToComment = comment
        replyParentId = comment.commentUUID

        replyContextTv.text = "Replying to @${comment.authorName}"
        replyContextLayout.visibility = View.VISIBLE

        commentEditText.hint = "Write your reply..."
        commentEditText.requestFocus()
    }

    /**
     * Cancels reply mode
     */
    private fun cancelReply() {
        replyingToComment = null
        replyParentId = null
        replyContextLayout.visibility = View.GONE
        commentEditText.hint = "Add a comment..."
        commentEditText.text?.clear()
    }

    /**
     * Hides the comment sheet with animation
     */
    private fun hideCommentSheet() {
        if (bottomCommentSheet.visibility == View.VISIBLE) {
            bottomCommentSheet.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    bottomCommentSheet.visibility = View.GONE
                    cancelReply()
                }
                .start()
        }
    }

    /**
     * Shows the keyboard
     */
    private fun showKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(commentEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Hides the keyboard
     */
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(commentEditText.windowToken, 0)
    }

    /**
     * Submits a comment or reply
     */
    private fun submitComment() {
        val loggedInUser = Constants.loggedInUser ?: return
        val commentText = commentEditText.text.toString().trim()
        if (commentText.isEmpty()) return

        val slugTitle = publishedModel?.slugTitle ?: return

        commentEditText.clearFocus()
        hideKeyboard()

        lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
            val commentUUID = UUID.randomUUID().toString()
            val entryTimeStamp = Config.getStandardTimeUTCString()

            val comment = if (replyingToComment != null) {
                StoryComment(
                    slugTitle,
                    loggedInUser.username,
                    commentText,
                    commentUUID,
                    replyParentId,
                    entryTimeStamp
                )
            } else {
                StoryComment(
                    slugTitle,
                    loggedInUser.username,
                    commentText,
                    commentUUID,
                    null,
                    entryTimeStamp
                )
            }

            Config.firebaseAnalytics.logEvent("story_comment_publish_started", null)
            val storyDatabase = Constants.getStoryDatabase(this@ArticleContentActivity)
            storyDatabase.storyCommentDao().update(comment)

            val commentPendingUpload = StoryPendingUpload(
                UUID.randomUUID(),
                Config.getStandardTimeUTCString(),
                commentUUID,
                PendingUploadType.COMMENT,
                slugTitle,
                UUID.fromString(loggedInUser.user_uuid)
            )
            storyDatabase.storyPendingUploadDao().update(commentPendingUpload)
            ViewModelUtil.startResumeUploads(this, this@ArticleContentActivity.applicationContext)

            withContext(Dispatchers.Main) {
                commentEditText.text?.clear()
                cancelReply()
                Snackbar.make(bottomCommentSheet, if (replyingToComment != null) "Reply posted!" else "Comment posted!", Snackbar.LENGTH_SHORT).show()
                updateCommentsCount()
                (findViewById<RecyclerView>(R.id.rv_comments).adapter as? ArticleCommentAdapter)?.refresh()
            }
        }
    }

    /**
     * Updates the comments count display
     */
    private fun updateCommentsCount() {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_comments)
        val adapter = recyclerView.adapter as? ArticleCommentAdapter
        val count = adapter?.itemCount ?: 0
        commentsCountTv.text = "($count)"
    }

    /**
     * Loads and displays article data from intent
     */
    private fun loadArticleData(savedInstanceState: Bundle?) {
        val registerViewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
        val loggedInUser = Constants.loggedInUser
        val recyclerView = findViewById<RecyclerView>(R.id.rv_comments)
        val titleImageView = findViewById<ImageView>(R.id.content_title_iv)
        val contentTextView = findViewById<ClickableImageSpanTextView>(R.id.content_tv)
        val headerImageTextView = findViewById<TextView>(R.id.content_title_tv)
        val locationTextView = findViewById<TextView>(R.id.location_tv)
        val authorRL = findViewById<RelativeLayout>(R.id.user_info_rv)
        val authorCV = findViewById<CardView>(R.id.avatar_crop_cv)
        val authorBigCV = findViewById<CardView>(R.id.avatar_crop_big_cv)
        val authorBigRL = findViewById<RelativeLayout>(R.id.author_info)
        val authorFollowersTextView = findViewById<TextView>(R.id.author_followers_tv)
        val timePassedTextView = findViewById<TimeSinceTextView>(R.id.time_passed_tv)
        val usernameAuthorTextView = findViewById<TextView>(R.id.username_post_tv)
        val usernameAuthorBigTextView = findViewById<TextView>(R.id.username_post_big_tv)
        val authorBadgeImageView = findViewById<ImageView>(R.id.avatar_iv)
        val subscribeBtn = findViewById<MaterialButton>(R.id.subscribe_btn)
        val authorBadgeBigImageView = findViewById<ImageView>(R.id.avatar_crop_big_iv)
        val btnShare = findViewById<Button>(R.id.btn_share)

        publishedModel = if (intent.hasExtra(PUBLISHED_STORY_EXTRA)) {
            intent.getParcelableExtra(PUBLISHED_STORY_EXTRA)
        } else null

        storyIsPublished = intent.getBooleanExtra(STORY_PUBLISHED, false) || publishedModel != null

        if (intent.hasExtra(PUBLISHED_STORY_HAS_NEW_LIKE) &&
            intent.getBooleanExtra(PUBLISHED_STORY_HAS_NEW_LIKE, false)) {
            setResult(Activity.RESULT_OK)
        }

        val intentMapExtra: StoryMap? = if (intent.hasExtra(STORY_MAP)) {
            intent.getParcelableExtra<StoryMap>(STORY_MAP)
        } else {
            null
        }

        val storyMap = publishedModel?.storyMap ?: intentMapExtra

        // Check if story has map data
        if (storyMap != null && !storyMap.geoJson.isNullOrEmpty()) {
            btnExpandMap.setOnClickListener {
                showFullscreenMap(storyMap)
            }
            titleImageView.visibility = View.GONE
            mapContainer.visibility = View.VISIBLE
            // Small delay to ensure view is properly laid out
            mapContainer.postDelayed({
                setupMapPreview(storyMap, savedInstanceState)
            }, 100)
        } else {
            titleImageView.visibility = View.VISIBLE
            mapContainer.visibility = View.GONE
        }

        if (headerImageTextView != null && (intent.hasExtra(EDIT_MODE_ENABLED_EXTRA) || publishedModel != null)) {
            setupArticleContent(
                registerViewModel,
                recyclerView,
                titleImageView,
                contentTextView,
                headerImageTextView,
                locationTextView,
                authorRL,
                authorCV,
                authorBigCV,
                authorBigRL,
                authorFollowersTextView,
                timePassedTextView,
                usernameAuthorTextView,
                usernameAuthorBigTextView,
                authorBadgeImageView,
                authorBadgeBigImageView,
                subscribeBtn,
                btnShare,
                loggedInUser
            )
        }

        setupLikeButtonAndFollow(loggedInUser, subscribeBtn)

        val publicSources =  publishedModel?.publicSources?.toList() ?: (if (intent.hasExtra(STORY_PUBLIC_SOURCES)) intent.getStringArrayListExtra(STORY_PUBLIC_SOURCES) else listOf())

        if (publicSources != null && publicSources.isNotEmpty()) updatePublicSourcesDisplay(publicSources)

        val privateSources = publishedModel?.privateSources
        val privateSourcesArray =  if (privateSources==null) (if (intent.hasExtra(STORY_PRIVATE_SOURCES)) intent.getParcelableArrayListExtra<ArticlePrivateSource>(STORY_PRIVATE_SOURCES) else arrayListOf<ArticlePrivateSource>()) else arrayListOf<ArticlePrivateSource>(*privateSources)
        if (privateSourcesArray != null && privateSourcesArray.isNotEmpty()) updatePrivateSourcesDisplay(privateSourcesArray)

        // After the view is laid out, force the ImageView to match the CTL height exactly
        (findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)).addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val toolbarHeight = bottom - top
            titleImageView.layoutParams = titleImageView.layoutParams.apply {
                height = toolbarHeight
            }
            titleImageView.requestLayout()
        }
    }

    /**
     * Sets up the article content based on whether it's published or in edit mode
     */
    private fun setupArticleContent(
        registerViewModel: RegisterViewModel,
        recyclerView: RecyclerView,
        titleImageView: ImageView,
        contentTextView: ClickableImageSpanTextView,
        headerImageTextView: TextView,
        locationTextView: TextView,
        authorRL: RelativeLayout,
        authorCV: CardView,
        authorBigCV: CardView,
        authorBigRL: RelativeLayout,
        authorFollowersTextView: TextView,
        timePassedTextView: TimeSinceTextView,
        usernameAuthorTextView: TextView,
        usernameAuthorBigTextView: TextView,
        authorBadgeImageView: ImageView,
        authorBadgeBigImageView: ImageView,
        subscribeBtn: Button,
        btnShare: Button,
        loggedInUser: AccountInfoFull?
    ) {
        val spanStringTitle = SpannableStringBuilder(
            publishedModel?.storyTitle ?: intent.getStringExtra(Constants.EDIT_CONTENT_TITLE)
        )
        val spanString = SpannableStringBuilder(
            publishedModel?.contentText ?: intent.getStringExtra(Constants.EDIT_CONTENT_TEXT)
        )
        val locationString = publishedModel?.location ?: intent.getStringExtra(Constants.EDIT_CONTENT_LOCATION)

        if (locationString.isNullOrEmpty()) {
            findViewById<ImageView>(R.id.locationImg).visibility = View.GONE
        } else {
            findViewById<LinearLayout>(R.id.location_container).visibility = View.VISIBLE
            locationTextView.text = locationString
        }

        val stylingInfo: StylingInfo = publishedModel?.stylingInfo
            ?: intent.getParcelableExtra(Constants.EDIT_CONTENT_TEXT_STYLES)!!

        val selectedTags = publishedModel?.tags?.toList()
            ?: intent.getStringArrayListExtra(Constants.EDIT_CONTENT_TAGS)

        selectedTags?.forEach { tag -> inflateTagView(tag) }

        StoryUtil.restoreTitleBackgroundColorFromStylingInfo(this, stylingInfo, titleImageView)

        if (publishedModel != null && loggedInUser != null) {
            setupPublishedArticle(
                registerViewModel,
                recyclerView,
                titleImageView,
                contentTextView,
                headerImageTextView,
                spanStringTitle,
                spanString,
                stylingInfo,
                authorRL,
                authorCV,
                authorBigCV,
                authorBigRL,
                authorFollowersTextView,
                timePassedTextView,
                usernameAuthorTextView,
                usernameAuthorBigTextView,
                authorBadgeImageView,
                authorBadgeBigImageView,
                subscribeBtn,
                btnShare,
                loggedInUser
            )
        } else if (loggedInUser != null) {
            setupEditMode(
                registerViewModel,
                titleImageView,
                stylingInfo,
                authorRL,
                authorCV,
                authorBigRL,
                authorBigCV,
                authorFollowersTextView,
                usernameAuthorTextView,
                usernameAuthorBigTextView,
                authorBadgeImageView,
                authorBadgeBigImageView,
                loggedInUser
            )
        } else {
            finish()
            return
        }

        // Restore text styling
        val newHighlightSpan = StoryUtil.restoreTitleHighlightColorFromStylingInfo(stylingInfo, spanStringTitle)
        if (newHighlightSpan != null) {
            if (oldHighLightSpan != null) headerImageTextView.text = null
            oldHighLightSpan = BackgroundColorSpan(stylingInfo.titleHighlightColor)
        }

        val enabledStylesAndSpansAdded = StoryUtil.restoreSpannableFromStylingInfo(
            this, stylingInfo.spans, spanString, storyIsPublished, contentTextView, loggedInUser
        )

        contentTextView.text = spanString
        contentTextView.imageSpans = enabledStylesAndSpansAdded.second
        contentTextView.loggedInUser = loggedInUser
        contentTextView.movementMethod = LinkMovementMethod.getInstance()
        contentTextView.linksClickable = true
        if (spanStringTitle.isNotEmpty()){
            headerImageTextView.text = spanStringTitle
            headerImageTextView.post {
                val gradient = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        android.graphics.Color.TRANSPARENT,
                        0x99000000.toInt(),   // 60% black mid-way
                        0xCC000000.toInt()    // 80% black at bottom
                    )
                )
                headerImageTextView.background = gradient
            }
        }else{
            headerImageTextView.visibility = View.GONE
        }

    }

    /**
     * Sets up a published article view
     */
    private fun setupPublishedArticle(
        registerViewModel: RegisterViewModel,
        recyclerView: RecyclerView,
        titleImageView: ImageView,
        contentTextView: ClickableImageSpanTextView,
        headerImageTextView: TextView,
        spanStringTitle: SpannableStringBuilder,
        spanString: SpannableStringBuilder,
        stylingInfo: StylingInfo,
        authorRL: RelativeLayout,
        authorCV: CardView,
        authorBigCV: CardView,
        authorBigRL: RelativeLayout,
        authorFollowersTextView: TextView,
        timePassedTextView: TimeSinceTextView,
        usernameAuthorTextView: TextView,
        usernameAuthorBigTextView: TextView,
        authorBadgeImageView: ImageView,
        authorBadgeBigImageView: ImageView,
        subscribeBtn: Button,
        btnShare: Button,
        loggedInUser: AccountInfoFull
    ) {
        val publishedModel = publishedModel ?: return

        btnShare.setOnClickListener {
            StoryUtil.shareArticle(
                publishedModel.slugTitle,
                this,
                publishedModel.authorName == loggedInUser?.username
            )
        }

        StoryUtil.restoreTitleBackgroundImageFromStylingInfoFromRemote(
            this, stylingInfo, titleImageView, loggedInUser
        )

        val localDateTime = DateTime(publishedModel.timestamp, DateTimeZone.UTC)
            .withZone(DateTimeZone.getDefault())
        timePassedTextView.timestamp = localDateTime

        val openAuthorPageClickListener = View.OnClickListener {
            StoryUtil.showAuthorProfilePage(this@ArticleContentActivity, publishedModel.authorName)
        }

        usernameAuthorTextView.text = publishedModel.authorName
        usernameAuthorTextView.setOnClickListener(openAuthorPageClickListener)
        usernameAuthorBigTextView.text = publishedModel.authorName
        usernameAuthorBigTextView.setOnClickListener(openAuthorPageClickListener)
        authorRL.setOnClickListener(openAuthorPageClickListener)
        authorCV.setOnClickListener(openAuthorPageClickListener)
        authorBigRL.setOnClickListener(openAuthorPageClickListener)
        authorBigCV.setOnClickListener(openAuthorPageClickListener)

        val authorBadgeMetadata = publishedModel.authorBadge
        if (authorBadgeMetadata != null) {
            ImageUtil.loadMinBitmapImageViewRemote(authorBadgeMetadata, this, authorBadgeImageView, loggedInUser)
            ImageUtil.loadMinBitmapImageViewRemote(authorBadgeMetadata, this, authorBadgeBigImageView, loggedInUser)
        }

        if (authorFollowersTextView.visibility == View.INVISIBLE) {
            StoryUtil.getNumberOfFollowers(
                registerViewModel,
                publishedModel.authorName,
                authorFollowersTextView,
                this
            )
        }

        setupCommentsPaging(registerViewModel, recyclerView, publishedModel, loggedInUser)

        // Show comment bar for logged in users
        if (loggedInUser != null) {
            updateUserInfoInCommentSheet(loggedInUser)
        }
    }

    /**
     * Sets up the comments paging adapter with reply functionality
     */
    private fun setupCommentsPaging(
        registerViewModel: RegisterViewModel,
        recyclerView: RecyclerView,
        publishedModel: StoryPublishedModel,
        loggedInUser: AccountInfoFull
    ) {
        val pagingAdapter = ArticleCommentAdapter(
            this,
            registerViewModel,
            false,
            loggedInUser.username == publishedModel.authorName,
            onReplyClicked = { comment, targetView ->
                // 1. Show the sheet
                showCommentSheet(comment)

                // 2. Delay to allow the keyboard to "push" the layout
                nestedScrollView.postDelayed({
                    val insets = ViewCompat.getRootWindowInsets(nestedScrollView)
                    val imeBottom = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                    val sysBottom = insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
                    val keyboardHeight = imeBottom - sysBottom

                    if (keyboardHeight > 0) {
                        // Get the view's current position on the actual SCREEN
                        val screenPos = IntArray(2)
                        targetView.getLocationOnScreen(screenPos)
                        val viewBottomOnScreen = screenPos[1] + targetView.height

                        // Get the screen's bottom "cutoff" point (above the keyboard)
                        val displayMetrics = nestedScrollView.resources.displayMetrics
                        val screenHeight = displayMetrics.heightPixels
                        val visibleCutoff = screenHeight - keyboardHeight

                        // If the view's bottom is below the cutoff (hidden by keyboard)
                        if (viewBottomOnScreen > visibleCutoff) {
                            // Calculate exactly how many pixels it is hidden by
                            val gap = viewBottomOnScreen - visibleCutoff

                            // Add a small margin so it's not touching the keyboard
                            val scrollAmount = gap + 140

                            // Use smoothScrollBy (RELATIVE) to move only the missing distance
                            nestedScrollView.smoothScrollBy(0, scrollAmount)
                        }
                    }
                }, 350)
            }
        )

        recyclerView.adapter = pagingAdapter

        lifecycleScope.launch(Dispatchers.IO) {
            registerViewModel.getArticleComments(
                loggedInUser,
                publishedModel.slugTitle,
                applicationContext
            ).collectLatest { pagingData ->
                pagingAdapter.submitData(pagingData)
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            pagingAdapter.loadStateFlow.collectLatest { loadStates ->
                when (loadStates.refresh) {
                    is LoadState.Error -> {
                        withContext(Dispatchers.Main) {
                            findViewById<LinearLayout>(R.id.comments_error_ll).visibility = View.VISIBLE
                            findViewById<ProgressBar>(R.id.progress_loader).visibility = View.GONE
                        }
                    }
                    is LoadState.NotLoading -> {
                        withContext(Dispatchers.Main) {
                            findViewById<ProgressBar>(R.id.progress_loader).visibility = View.GONE
                            updateCommentsCount()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Sets up edit mode for drafts
     */
    private fun setupEditMode(
        registerViewModel: RegisterViewModel,
        titleImageView: ImageView,
        stylingInfo: StylingInfo,
        authorRL: RelativeLayout,
        authorCV: CardView,
        authorBigRL: RelativeLayout,
        authorBigCV: CardView,
        authorFollowersTextView: TextView,
        usernameAuthorTextView: TextView,
        usernameAuthorBigTextView: TextView,
        authorBadgeImageView: ImageView,
        authorBadgeBigImageView: ImageView,
        loggedInUser: AccountInfoFull
    ) {
        val progressBar = findViewById<ProgressBar>(R.id.progress_loader)
        progressBar.visibility = View.GONE
        findViewById<Button>(R.id.btn_share).visibility = View.GONE

        StoryUtil.restoreTitleBackgroundImageFromStylingInfo(
            this, stylingInfo, titleImageView, loggedInUser
        )

        val openAuthorPageClickListener = View.OnClickListener {
            StoryUtil.showAuthorProfilePage(this@ArticleContentActivity, loggedInUser.username)
        }

        usernameAuthorTextView.text = loggedInUser.username
        usernameAuthorTextView.setOnClickListener(openAuthorPageClickListener)
        usernameAuthorBigTextView.text = loggedInUser.username
        authorCV.setOnClickListener(openAuthorPageClickListener)
        authorRL.setOnClickListener(openAuthorPageClickListener)
        usernameAuthorBigTextView.setOnClickListener(openAuthorPageClickListener)
        authorBigRL.setOnClickListener(openAuthorPageClickListener)
        authorBigCV.setOnClickListener(openAuthorPageClickListener)

        if (authorFollowersTextView.visibility == View.INVISIBLE) {
            StoryUtil.getNumberOfFollowers(
                registerViewModel,
                loggedInUser.username,
                authorFollowersTextView,
                this
            )
        }

        val authorBadgeMetadata = loggedInUser.profileIcon
        if (authorBadgeMetadata != null) {
            val localImage = ImageUtil.getLocalImageBitmapFromMetaData(this, false, authorBadgeMetadata, false)
            if (localImage != null) {
                authorBadgeImageView.setImageBitmap(localImage)
                authorBadgeBigImageView.setImageBitmap(localImage)
            } else {
                ImageUtil.loadMinBitmapImageViewRemote(authorBadgeMetadata, this, authorBadgeImageView, loggedInUser)
                ImageUtil.loadMinBitmapImageViewRemote(authorBadgeMetadata, this, authorBadgeBigImageView, loggedInUser)
            }
        }
    }
    private fun updatePrivateSourcesDisplay(sources:ArrayList<ArticlePrivateSource>) {
        val linearLayout = findViewById<LinearLayout>(R.id.sources_ll)
        linearLayout.visibility = View.VISIBLE
        val container = findViewById<LinearLayout>(R.id.private_sources_container)
        container.visibility = View.VISIBLE
        findViewById<TextView>(R.id.private_sources_tv).visibility = View.VISIBLE
        container.removeAllViews()
        sources.forEachIndexed { index, source ->
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
            view.findViewById<ImageView>(R.id.remove_source_btn).visibility = View.GONE
            container.addView(view)
        }
    }
    private fun updatePublicSourcesDisplay(sources:List<String>) {
        val container = findViewById<LinearLayout>(R.id.public_sources_container)
        val linearLayout = findViewById<LinearLayout>(R.id.sources_ll)
        linearLayout.visibility = View.VISIBLE
        container.removeAllViews()
        (sources).forEachIndexed { index, source ->
            val view = layoutInflater.inflate(R.layout.item_public_source, container, false)
            view.findViewById<TextView>(R.id.source_title).text =  StoryUtil.extractTitleFromUrl(source)
            val linkTv = view.findViewById<TextView>(R.id.source_url)
            StoryUtil.makeUrlClickable(linkTv,source,source,false)
            view.findViewById<ImageView>(R.id.remove_source_btn).visibility  = View.GONE
            container.addView(view)
        }
    }

    /**
     * Sets up like button and follow button functionality
     */
    private fun setupLikeButtonAndFollow(
        loggedInUser: AccountInfoFull?,
        subscribeBtn: MaterialButton
    ) {
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        val publishedModel = publishedModel ?: return

        if (loggedInUser == null || loggedInUser.username == publishedModel.authorName) {
            subscribeBtn.isEnabled = false
            if (loggedInUser == null) {
                fab.setOnClickListener {
                    Snackbar.make(fab, "Login or create an account to like an article.", Snackbar.LENGTH_LONG)
                        .setAction("OK") { }
                        .setActionTextColor(getColor(android.R.color.holo_red_light))
                        .show()
                }
            } else {
                fab.visibility = View.GONE
            }
        } else {
            setupLikeButton(loggedInUser, fab, publishedModel)
            setupFollowButton(loggedInUser, subscribeBtn, publishedModel)
        }
    }

    /**
     * Sets up the like button functionality
     */
    private fun setupLikeButton(
        loggedInUser: AccountInfoFull,
        fab: FloatingActionButton,
        publishedModel: StoryPublishedModel
    ) {
        val storyDatabase = Constants.getStoryDatabase(this@ArticleContentActivity)
        val authorUUID = UUID.fromString(loggedInUser.user_uuid)

        fab.setOnClickListener { view ->
            lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
                val pendingLikes = storyDatabase.storyPendingUploadDao()
                    .getAllPendingForStoryImmediate(publishedModel.slugTitle, authorUUID)
                val actualLike = storyDatabase.storyLikedDao()
                    .getLikeForStoryImmediate(publishedModel.slugTitle, authorUUID)
                val likePendingUpload: StoryPendingUpload

                if (pendingLikes.isNotEmpty() || actualLike != null) {
                    setResult(Activity.RESULT_CANCELED)
                    likePendingUpload = StoryPendingUpload(
                        UUID.randomUUID(),
                        Config.getStandardTimeUTCString(),
                        publishedModel.storyTitle,
                        PendingUploadType.REMOVE_LIKE,
                        publishedModel.slugTitle,
                        authorUUID
                    )
                    withContext(Dispatchers.Main) {
                        fab.setImageDrawable(
                            ContextCompat.getDrawable(
                                this@ArticleContentActivity,
                                R.drawable.ic_baseline_favorite_border_24
                            )
                        )
                        fab.imageTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                this@ArticleContentActivity,
                                R.color.textSecondary
                            )
                        )
                    }
                } else {
                    setResult(Activity.RESULT_OK)
                    likePendingUpload = StoryPendingUpload(
                        UUID.randomUUID(),
                        Config.getStandardTimeUTCString(),
                        publishedModel.storyTitle,
                        PendingUploadType.LIKE,
                        publishedModel.slugTitle,
                        authorUUID
                    )
                    withContext(Dispatchers.Main) {
                        fab.setImageDrawable(
                            ContextCompat.getDrawable(
                                this@ArticleContentActivity,
                                R.drawable.ic_baseline_favorite_24
                            )
                        )
                        fab.imageTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                this@ArticleContentActivity,
                                R.color.heart
                            )
                        )
                    }
                }
                storyDatabase.storyPendingUploadDao().update(likePendingUpload)
                ViewModelUtil.startResumeUploads(this, this@ArticleContentActivity.applicationContext)
            }
        }

        // Observe existing likes
        val pendingLikes = storyDatabase.storyPendingUploadDao()
            .getAllPendingForStory(publishedModel.slugTitle, authorUUID)
        val actualLike = storyDatabase.storyLikedDao()
            .getLikeForStory(publishedModel.slugTitle, authorUUID)

        pendingLikes.observe(this) { x ->
            if (x.isNotEmpty() && x.any { z -> z.resourceType == PendingUploadType.LIKE }) {
                fab.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_baseline_favorite_24)
                )
                fab.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.heart))
            }
        }

        actualLike.observe(this) { x ->
            if (x != null) {
                fab.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_baseline_favorite_24)
                )
                fab.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.heart))
            }
        }
    }

    /**
     * Sets up the follow button functionality
     */
    private fun setupFollowButton(
        loggedInUser: AccountInfoFull,
        subscribeBtn: MaterialButton,
        publishedModel: StoryPublishedModel
    ) {
        val storyDatabase = Constants.getStoryDatabase(this@ArticleContentActivity)
        val authorUUID = UUID.fromString(loggedInUser.user_uuid)

        subscribeBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
                val pendingFollow = storyDatabase.storyPendingUploadDao()
                    .getAllForAssociatedIDImmediate(authorUUID, publishedModel.authorName)
                val actualFollow = storyDatabase.userFollowedDao()
                    .getFollowedForUserImmediate(authorUUID, publishedModel.authorName)
                val followingPendingUpload: StoryPendingUpload

                if (pendingFollow.isNotEmpty() || actualFollow != null) {
                    followingPendingUpload = StoryPendingUpload(
                        UUID.randomUUID(),
                        Config.getStandardTimeUTCString(),
                        publishedModel.authorBadge ?: "",
                        PendingUploadType.UNFOLLOW,
                        publishedModel.authorName,
                        authorUUID
                    )
                    withContext(Dispatchers.Main) {
                        setupEnableFollowButton(loggedInUser,subscribeBtn, false)
                    }

                } else {
                    followingPendingUpload = StoryPendingUpload(
                        UUID.randomUUID(),
                        Config.getStandardTimeUTCString(),
                        publishedModel.authorBadge ?: "",
                        PendingUploadType.FOLLOW,
                        publishedModel.authorName,
                        authorUUID
                    )
                    withContext(Dispatchers.Main) {
                        setupEnableFollowButton(loggedInUser,subscribeBtn, true)
                    }
                }
                storyDatabase.storyPendingUploadDao().update(followingPendingUpload)
                ViewModelUtil.startResumeUploads(this, this@ArticleContentActivity.applicationContext)
            }
        }

        // Observe follow status
        val pendingFollow = storyDatabase.storyPendingUploadDao()
            .getAllForAssociatedID(authorUUID, publishedModel.authorName)
        pendingFollow.observe(this) { x ->
            if (x.isNotEmpty() && x.any { z -> z.resourceType == PendingUploadType.FOLLOW }) {
                setupEnableFollowButton(loggedInUser,subscribeBtn, true)
            }
        }

        val actualFollow = storyDatabase.userFollowedDao()
            .getFollowedForUser(authorUUID, publishedModel.authorName)
        actualFollow.observe(this) { x ->
            setupEnableFollowButton(loggedInUser,subscribeBtn, x != null)
        }
    }

    fun setupEnableFollowButton(loggedInUser: AccountInfoFull,subscribeBtn: MaterialButton, isFollowing: Boolean){
        if (loggedInUser.username == publishedModel?.authorName) {
            subscribeBtn.visibility = View.GONE
            return
        }
        if (!isFollowing){
            subscribeBtn.setText("FOLLOW");
            subscribeBtn.setIconResource(R.drawable.ic_add);
            subscribeBtn.setIconTintResource(android.R.color.white);
            subscribeBtn.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this@ArticleContentActivity, R.color.secondaryLightColor)
            ));
            subscribeBtn.setTextColor(ContextCompat.getColor(this@ArticleContentActivity, R.color.white));

        }else{
            subscribeBtn.setText("FOLLOWING");
            subscribeBtn.setIconResource(R.drawable.ic_check_circle);
            subscribeBtn.setIconTintResource(android.R.color.white);
            subscribeBtn.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this@ArticleContentActivity, R.color.secondaryColor_20)
            ));
            subscribeBtn.setTextColor(ContextCompat.getColor(this@ArticleContentActivity, R.color.primaryColor));
        }

    }

    /**
     * Inflates and adds a tag view to the tags container
     */
    fun inflateTagView(tag: String) {
        val newView = layoutInflater.inflate(R.layout.tag_layout, tagsSelectedFlex, false)
        val inflatedText = newView.findViewById<TextView>(R.id.hashtag_tv)
        if (inflatedText != null) {
            inflatedText.text = tag
            inflatedText.setOnClickListener {
                val intent = Intent(this, ArticleListActivity::class.java)
                intent.putExtra(ArticleListActivity.HASHTAG_FILTER_INTENT_NAME, tag)
                startActivity(intent)
            }
        }
        tagsSelectedFlex.addView(newView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (replyingToComment != null || bottomCommentSheet.visibility == View.VISIBLE) {
            // If in reply mode, cancel reply first
            cancelReply()
            commentEditText.clearFocus()
            hideKeyboard()
            hideCommentSheet()
        }else {
            super.onBackPressed()
        }
    }
}