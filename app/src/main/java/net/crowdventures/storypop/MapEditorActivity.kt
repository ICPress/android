package net.crowdventures.storypop.map.editor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.map.models.*
import net.crowdventures.storypop.map.util.MapDrawingUtil
import net.crowdventures.storypop.util.AIArticleEditHelperUtil
import net.crowdventures.storypop.util.AIRequestHandlerUtil
import net.crowdventures.storypop.util.GroqModelProvider
import net.crowdventures.storypop.util.SuccessCallback
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MapEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GEOJSON        = "extra_geojson"
        const val EXTRA_CENTER_LAT     = "extra_center_lat"
        const val EXTRA_CENTER_LNG     = "extra_center_lng"
        const val EXTRA_ZOOM           = "extra_zoom"
        const val EXTRA_TITLE          = "extra_title"
        const val EXTRA_ARTICLE_CONTENT = "extra_article_content"
        const val RESULT_GEOJSON       = "result_geojson"
        const val RESULT_CENTER_LAT    = "result_center_lat"
        const val RESULT_CENTER_LNG    = "result_center_lng"
        const val RESULT_ZOOM          = "result_zoom"
        const val RESULT_SCREENSHOT_URI = "result_screenshot_uri"
        const val REQUEST_CODE         = 4210

        fun startForResult(
            context: Context,
            existingGeoJson: String? = null,
            centerLat: Double = 0.0,
            centerLng: Double = 0.0,
            zoom: Double = 4.0,
            title: String = "",
            articleContent: String = ""
        ) {
            val intent = Intent(context, MapEditorActivity::class.java).apply {
                putExtra(EXTRA_GEOJSON, existingGeoJson)
                putExtra(EXTRA_CENTER_LAT, centerLat)
                putExtra(EXTRA_CENTER_LNG, centerLng)
                putExtra(EXTRA_ZOOM, zoom)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTICLE_CONTENT, articleContent)
            }
            (context as? AppCompatActivity)?.startActivityForResult(intent, REQUEST_CODE)
        }
    }

    // Views
    private lateinit var mapView: MapView
    private lateinit var toolChipGroup: ChipGroup
    private lateinit var fabUndo: FloatingActionButton
    private lateinit var fabRedo: FloatingActionButton
    private lateinit var fabAiSuggestion: ExtendedFloatingActionButton
    private lateinit var fabCommitPath: MaterialButton
    private lateinit var fabCancelPath: ImageButton
    private lateinit var btnSave: MaterialButton
    private lateinit var pathInProgressBar: CardView
    private lateinit var pathPointsCountTv: TextView
    private lateinit var aiSuggestionDialog: CardView
    private lateinit var aiPromptEt: TextInputEditText
    private lateinit var btnGenerateAi: MaterialButton
    private lateinit var btnCancelAi: MaterialButton
    private lateinit var aiProgressContainer: LinearLayout
    private lateinit var aiProgressText: TextView
    private lateinit var screenshotDialog: CardView
    private lateinit var screenshotPreview: ImageView
    private lateinit var btnCancelScreenshot: MaterialButton
    private lateinit var btnSaveScreenshot: MaterialButton

    // MapLibre
    private var mapboxMap: MapLibreMap? = null
    private var symbolManager: SymbolManager? = null
    private var lineManager: LineManager? = null
    private var circleManager: CircleManager? = null

    // Annotation mappings
    private val symbolById = mutableMapOf<String, Symbol>()
    private val circleById = mutableMapOf<String, Circle>()
    private val lineById = mutableMapOf<String, Line>()

    // ViewModel
    private lateinit var vm: MapEditorViewModel
    private lateinit var sharedPrefs: SharedPreferenceManager

    // For dynamic circle updates on zoom
    private var lastZoomLevel = 0.0
    private var zoomUpdateJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContentView(R.layout.activity_map_editor)

        sharedPrefs = SharedPreferenceManager(this)
        vm = ViewModelProvider(this)[MapEditorViewModel::class.java]

        bindViews()
        setupToolChips()
        setupFabs()
        setupMapView(savedInstanceState)

        intent.getStringExtra(EXTRA_GEOJSON)?.let { vm.loadFromGeoJson(it) }
        observeViewModel()
    }

    private fun bindViews() {
        mapView = findViewById(R.id.mapView)
        toolChipGroup = findViewById(R.id.toolChipGroup)
        fabUndo = findViewById(R.id.fabUndo)
        fabRedo = findViewById(R.id.fabRedo)
        fabAiSuggestion = findViewById(R.id.fabAiSuggestion)
        fabCommitPath = findViewById(R.id.btnCommitPath)
        fabCancelPath = findViewById(R.id.btnCancelPath)
        btnSave = findViewById(R.id.btnSave)
        pathInProgressBar = findViewById(R.id.pathInProgressBar)
        pathPointsCountTv = findViewById(R.id.pathPointsCountTv)
        aiSuggestionDialog = findViewById(R.id.aiSuggestionDialog)
        aiPromptEt = findViewById(R.id.aiPromptEt)
        btnGenerateAi = findViewById(R.id.btnGenerateAi)
        btnCancelAi = findViewById(R.id.btnCancelAi)
        aiProgressContainer = findViewById(R.id.aiProgressContainer)
        aiProgressText = findViewById(R.id.aiProgressText)
        screenshotDialog = findViewById(R.id.screenshotDialog)
        screenshotPreview = findViewById(R.id.screenshotPreview)
        btnCancelScreenshot = findViewById(R.id.btnCancelScreenshot)
        btnSaveScreenshot = findViewById(R.id.btnSaveScreenshot)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { confirmDiscard() }
    }

    private fun setupToolChips() {
        val tools = listOf(
            "Select" to MapEditorViewModel.Tool.SELECT,
            "Incident" to MapEditorViewModel.Tool.ADD_INCIDENT,
            "City" to MapEditorViewModel.Tool.ADD_CITY,
            "Marker" to MapEditorViewModel.Tool.ADD_ORDINARY,
            "Path" to MapEditorViewModel.Tool.DRAW_PATH
        )

        tools.forEach { (label, tool) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                tag = tool
                chipBackgroundColor = ContextCompat.getColorStateList(this@MapEditorActivity, R.color.surfaceLight)
                chipStrokeColor = ContextCompat.getColorStateList(this@MapEditorActivity, R.color.border_color)
                chipStrokeWidth = 1f
                setChipIconResource(getIconForTool(tool))
                chipIconTint = ContextCompat.getColorStateList(this@MapEditorActivity, R.color.primaryColor)
                setTextColor(ContextCompat.getColorStateList(this@MapEditorActivity, R.color.primaryTextColor))
            }
            toolChipGroup.addView(chip)
            chip.setOnClickListener {
                vm.setTool(tool)
                updateChipSelection(tool)
            }
        }
        (toolChipGroup.getChildAt(0) as? Chip)?.isChecked = true
        updateChipSelection(MapEditorViewModel.Tool.SELECT)
    }

    private fun updateChipSelection(selectedTool: MapEditorViewModel.Tool) {
        for (i in 0 until toolChipGroup.childCount) {
            val chip = toolChipGroup.getChildAt(i) as? Chip ?: continue
            val isSelected = chip.tag == selectedTool

            if (isSelected) {
                chip.chipBackgroundColor = ContextCompat.getColorStateList(this, R.color.secondaryColor)
                chip.setTextColor(ContextCompat.getColorStateList(this, R.color.white))
                chip.chipIconTint = ContextCompat.getColorStateList(this, R.color.white)
                chip.chipStrokeColor = ContextCompat.getColorStateList(this, R.color.secondaryColor)
            } else {
                chip.chipBackgroundColor = ContextCompat.getColorStateList(this, R.color.surfaceLight)
                chip.setTextColor(ContextCompat.getColorStateList(this, R.color.primaryTextColor))
                chip.chipIconTint = ContextCompat.getColorStateList(this, R.color.primaryColor)
                chip.chipStrokeColor = ContextCompat.getColorStateList(this, R.color.border_color)
            }
        }
    }

    private fun getIconForTool(tool: MapEditorViewModel.Tool): Int {
        return when (tool) {
            MapEditorViewModel.Tool.SELECT -> R.drawable.ic_select
            MapEditorViewModel.Tool.ADD_INCIDENT -> R.drawable.ic_incident
            MapEditorViewModel.Tool.ADD_CITY -> R.drawable.ic_city
            MapEditorViewModel.Tool.ADD_ORDINARY -> R.drawable.ic_marker
            MapEditorViewModel.Tool.DRAW_PATH -> R.drawable.ic_route
        }
    }

    private fun setupFabs() {
        fabUndo.setOnClickListener { vm.undo() }
        fabRedo.setOnClickListener { vm.redo() }
        btnSave.setOnClickListener { showScreenshotDialog() }
        fabAiSuggestion.setOnClickListener { showAiSuggestionDialog() }
        fabCommitPath.setOnClickListener { showCommitPathDialog() }
        fabCancelPath.setOnClickListener {
            vm.cancelPath()
            updatePathBar(emptyList())
        }
        btnGenerateAi.setOnClickListener { generateAiSuggestions() }
        btnCancelAi.setOnClickListener { aiSuggestionDialog.visibility = View.GONE }
        btnCancelScreenshot.setOnClickListener { screenshotDialog.visibility = View.GONE }
        btnSaveScreenshot.setOnClickListener { captureAndSave() }
    }

    private fun getCurrentCameraPosition(): Triple<Double, Double, Double> {
        val camera = mapboxMap?.cameraPosition
        return Triple(
            camera?.target?.latitude ?: 0.0,
            camera?.target?.longitude ?: 0.0,
            camera?.zoom ?: 4.0
        )
    }

    private fun showScreenshotDialog() {
        captureMapScreenshotPreview { bitmap ->
            if (bitmap != null) {
                screenshotPreview.setImageBitmap(bitmap)
                screenshotDialog.visibility = View.VISIBLE
            } else {
                Snackbar.make(mapView, "Failed to preview screenshot", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun captureMapScreenshotPreview(callback: (Bitmap?) -> Unit) {
        mapView.postDelayed({
            try {
                mapboxMap?.snapshot { snapshotBitmap ->
                    if (snapshotBitmap != null) {
                        callback(snapshotBitmap)
                    } else {
                        val bitmap = Bitmap.createBitmap(mapView.width, mapView.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        mapView.draw(canvas)
                        callback(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }, 500)
    }

    private fun captureAndSave() {
        val (centerLat, centerLng, zoom) = getCurrentCameraPosition()

        mapboxMap?.snapshot { snapshotBitmap ->
            if (snapshotBitmap != null) {
                val uri = saveBitmapToUri(snapshotBitmap)
                if (uri != null) {
                    val geoJson = vm.toGeoJsonString()
                    val resultIntent = Intent().apply {
                        putExtra(RESULT_GEOJSON, geoJson)
                        putExtra(RESULT_CENTER_LAT, centerLat)
                        putExtra(RESULT_CENTER_LNG, centerLng)
                        putExtra(RESULT_ZOOM, zoom)
                        putExtra(RESULT_SCREENSHOT_URI, uri.toString())
                    }
                    setResult(RESULT_OK, resultIntent)
                    screenshotDialog.visibility = View.GONE
                    finish()
                } else {
                    Snackbar.make(mapView, "Failed to save screenshot", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                try {
                    val bitmap = Bitmap.createBitmap(mapView.width, mapView.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    mapView.draw(canvas)

                    val uri = saveBitmapToUri(bitmap)
                    if (uri != null) {
                        val geoJson = vm.toGeoJsonString()
                        val resultIntent = Intent().apply {
                            putExtra(RESULT_GEOJSON, geoJson)
                            putExtra(RESULT_CENTER_LAT, centerLat)
                            putExtra(RESULT_CENTER_LNG, centerLng)
                            putExtra(RESULT_ZOOM, zoom)
                            putExtra(RESULT_SCREENSHOT_URI, uri.toString())
                        }
                        setResult(RESULT_OK, resultIntent)
                        screenshotDialog.visibility = View.GONE
                        finish()
                    } else {
                        Snackbar.make(mapView, "Failed to save screenshot", Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Snackbar.make(mapView, "Failed to capture screenshot: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveBitmapToUri(bitmap: Bitmap): Uri? {
        return try {
            val filename = "map_screenshot_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StoryPop")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                }
                uri
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val storyPopDir = File(imagesDir, "StoryPop")
                if (!storyPopDir.exists()) {
                    storyPopDir.mkdirs()
                }

                val imageFile = File(storyPopDir, filename)
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    imageFile
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showAiSuggestionDialog() {
        aiSuggestionDialog.visibility = View.VISIBLE
    }

    private fun generateAiSuggestions() {
        val articleContent = intent.getStringExtra(EXTRA_ARTICLE_CONTENT) ?: ""
        if (articleContent.isEmpty()) {
            Snackbar.make(mapView, "No article content available for AI analysis", Snackbar.LENGTH_LONG).show()
            aiSuggestionDialog.visibility = View.GONE
            return
        }

        val groqKey = sharedPrefs.getGroqKey()
        if (groqKey == null) {
            showApiKeyDialog()
            return
        }

        val customPrompt = aiPromptEt.text.toString()
        aiSuggestionDialog.visibility = View.GONE
        showAiProgress()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = callGroqForLocationSuggestions(groqKey, articleContent, customPrompt)
                hideAiProgress()

                if (result != null) {
                    val suggestions = parseAiResponse(result)
                    if (suggestions.isNotEmpty()) {
                        showLocationSuggestionsDialog(suggestions)
                    } else {
                        Snackbar.make(mapView, "No relevant locations found in the article", Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    Snackbar.make(mapView, "AI suggestion failed. Please try again.", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                hideAiProgress()
                Snackbar.make(mapView, "AI suggestion failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun callGroqForLocationSuggestions(
        apiKey: String,
        articleContent: String,
        customPrompt: String
    ): String? = suspendCoroutine { continuation ->
        val models = GroqModelProvider.getModels()
        val prompt = buildAiPrompt(articleContent, customPrompt)

        var progressJob: kotlinx.coroutines.Job? = null

        progressJob = CoroutineScope(Dispatchers.Main).launch {
            var dotCount = 0
            while (true) {
                val dots = ".".repeat(dotCount % 4)
                aiProgressText.text = "Analyzing article$dots"
                delay(500)
                dotCount++
            }
        }

        AIRequestHandlerUtil.suggest(
            apiKey,
            models.first().id,
            prompt,
            this@MapEditorActivity,
            this@MapEditorActivity.lifecycleScope,
            "",
            object : SuccessCallback<List<String>> {
                override fun onSuccess(vararg param: List<String>) {
                    progressJob.cancel()
                    continuation.resume(param.firstOrNull()?.firstOrNull())
                }

                override fun onFailure(reason: Any?) {
                    progressJob.cancel()
                    continuation.resume(null)
                }
            }
        )
    }

    private fun buildAiPrompt(articleContent: String, customPrompt: String): String {
        return """
            You are an expert cartographer and geographer. Based on the following article content, suggest relevant geographic locations, paths/routes, incidents, and points of interest that should be added to a map.
            
            Article Content:
            ${articleContent.take(4000)}
            
            Custom Instructions: $customPrompt
            
            Analyze the article and identify:
            1. SPECIFIC LOCATIONS (cities, landmarks, addresses mentioned)
            2. INCIDENTS (events that happened at specific places)
            3. PATHS/ROUTES (journeys, roads, trails, or connections between locations)
            4. POINTS OF INTEREST (other notable places mentioned)
            
            Return ONLY a valid JSON array with the following structure. Do not include any other text or explanation.
            
            [
                {
                    "type": "location",
                    "subtype": "city|incident|marker",
                    "name": "Location name",
                    "description": "Brief description and relevance to the article",
                    "latitude": 0.0,
                    "longitude": 0.0,
                    "radius_meters": 5000
                },
                {
                    "type": "path",
                    "name": "Path/Route name",
                    "description": "Description of the route and its significance",
                    "points": [
                        {"lat": 0.0, "lng": 0.0},
                        {"lat": 0.0, "lng": 0.0}
                    ]
                }
            ]
            
            For latitude/longitude, provide reasonable estimates based on the location names mentioned.
            For incidents, include an appropriate radius_meters value (default 5000).
            For paths, provide at least 2-3 points along the route.
            If the article doesn't mention specific locations, suggest relevant locations based on the topic.
        """.trimIndent()
    }

    private fun parseAiResponse(response: String): List<AiLocationSuggestion> {
        val suggestions = mutableListOf<AiLocationSuggestion>()

        try {
            var cleanResponse = response.trim()
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7)
            }
            if (cleanResponse.startsWith("```")) {
                cleanResponse = cleanResponse.substring(3)
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length - 3)
            }
            cleanResponse = cleanResponse.trim()

            val jsonArray = JSONArray(cleanResponse)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val type = obj.getString("type")

                when (type) {
                    "location" -> {
                        val subtype = obj.getString("subtype")
                        val name = obj.getString("name")
                        val description = obj.optString("description", "")
                        val latitude = obj.getDouble("latitude")
                        val longitude = obj.getDouble("longitude")
                        val radiusMeters = obj.optDouble("radius_meters", 5000.0)

                        suggestions.add(
                            AiLocationSuggestion(
                                name = name,
                                description = description,
                                latitude = latitude,
                                longitude = longitude,
                                type = subtype,
                                radiusMeters = radiusMeters,
                                pathPoints = null
                            )
                        )
                    }
                    "path" -> {
                        val name = obj.getString("name")
                        val description = obj.optString("description", "")
                        val pointsArray = obj.getJSONArray("points")
                        val pathPoints = mutableListOf<Pair<Double, Double>>()

                        for (j in 0 until pointsArray.length()) {
                            val point = pointsArray.getJSONObject(j)
                            val lat = point.getDouble("lat")
                            val lng = point.getDouble("lng")
                            pathPoints.add(lat to lng)
                        }

                        suggestions.add(
                            AiLocationSuggestion(
                                name = name,
                                description = description,
                                latitude = 0.0,
                                longitude = 0.0,
                                type = "path",
                                radiusMeters = 0.0,
                                pathPoints = pathPoints
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return suggestions
    }

    private fun showApiKeyDialog() {
        val onSuccess = object : SuccessCallback<String> {
            override fun onSuccess(vararg param: String) {
                sharedPrefs.setGroqKey(param.first())
                generateAiSuggestions()
            }
            override fun onFailure(reason: Any?) {
                Snackbar.make(mapView, "Failed to save API key", Snackbar.LENGTH_SHORT).show()
            }
        }
        AIArticleEditHelperUtil.showEnterApiKeyDialog(onSuccess, layoutInflater, this)
    }

    private fun showAiProgress() {
        aiProgressContainer.visibility = View.VISIBLE
        btnGenerateAi.isEnabled = false
        btnCancelAi.isEnabled = false
    }

    private fun hideAiProgress() {
        aiProgressContainer.visibility = View.GONE
        btnGenerateAi.isEnabled = true
        btnCancelAi.isEnabled = true
    }

    private fun showLocationSuggestionsDialog(suggestions: List<AiLocationSuggestion>) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_ai_suggestions, null)
        val container = view.findViewById<LinearLayout>(R.id.suggestionsContainer)
        val closeBtn = view.findViewById<MaterialButton>(R.id.btnClose)

        suggestions.forEach { suggestion ->
            val suggestionView = layoutInflater.inflate(R.layout.item_location_suggestion, container, false)
            val nameTv = suggestionView.findViewById<TextView>(R.id.locationName)
            val descTv = suggestionView.findViewById<TextView>(R.id.locationDescription)
            val addBtn = suggestionView.findViewById<MaterialButton>(R.id.btnAddLocation)

            nameTv.text = suggestion.name
            descTv.text = suggestion.description

            addBtn.setOnClickListener {
                when (suggestion.type.lowercase()) {
                    "incident" -> {
                        vm.addIncident(
                            suggestion.latitude,
                            suggestion.longitude,
                            suggestion.name,
                            suggestion.description,
                            suggestion.radiusMeters
                        )
                        zoomToLocation(suggestion.latitude, suggestion.longitude, suggestion.radiusMeters)
                    }
                    "city" -> {
                        vm.addCity(suggestion.latitude, suggestion.longitude, suggestion.name, suggestion.description)
                        zoomToCity(suggestion.latitude, suggestion.longitude)
                    }
                    "marker" -> {
                        vm.addOrdinary(suggestion.latitude, suggestion.longitude, suggestion.name, suggestion.description)
                        zoomToLocation(suggestion.latitude, suggestion.longitude)
                    }
                    "path" -> {
                        val points = suggestion.pathPoints ?: listOf(
                            suggestion.latitude to suggestion.longitude,
                            (suggestion.latitude + 0.01) to (suggestion.longitude + 0.01)
                        )
                        vm.addPath(points, suggestion.name)
                        zoomToPath(points)
                    }
                }
                dialog.dismiss()
                Snackbar.make(mapView, "Added: ${suggestion.name}", Snackbar.LENGTH_SHORT).show()
            }
            container.addView(suggestionView)
        }

        closeBtn.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun zoomToLocation(latitude: Double, longitude: Double, radiusMeters: Double = 5000.0) {
        val map = mapboxMap ?: return

        if (radiusMeters > 0) {
            val radiusDegrees = radiusMeters / 111000.0
            val bounds = LatLngBounds.Builder()
                .include(LatLng(latitude + radiusDegrees, longitude + radiusDegrees))
                .include(LatLng(latitude - radiusDegrees, longitude - radiusDegrees))
                .build()

            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
            map.animateCamera(cameraUpdate, 1000)
        } else {
            val position = CameraPosition.Builder()
                .target(LatLng(latitude, longitude))
                .zoom(12.0)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000)
        }
    }

    private fun zoomToCity(latitude: Double, longitude: Double) {
        val map = mapboxMap ?: return
        val position = CameraPosition.Builder()
            .target(LatLng(latitude, longitude))
            .zoom(12.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000)
    }

    private fun zoomToPath(points: List<Pair<Double, Double>>) {
        val map = mapboxMap ?: return
        if (points.isEmpty()) return

        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { (lat, lng) ->
            boundsBuilder.include(LatLng(lat, lng))
        }

        val bounds = boundsBuilder.build()
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
        map.animateCamera(cameraUpdate, 1000)
    }

    private fun setupMapView(savedInstanceState: Bundle?) {
        val inputStream = resources.openRawResource(R.raw.dark)
        val mapContent = inputStream.bufferedReader().use { it.readText() }
        // Night-mode tile URL
        val nightStyle = Style.Builder().fromJson(mapContent)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            mapboxMap = map
            map.setStyle(nightStyle) { style ->
                style.addImage(MapDrawingUtil.MARKER_ICON_ORDINARY, MapDrawingUtil.createOrdinaryMarkerBitmap())
                style.addImage(MapDrawingUtil.MARKER_ICON_INCIDENT, MapDrawingUtil.createIncidentMarkerBitmap())
                style.addImage(MapDrawingUtil.MARKER_ICON_CITY,     MapDrawingUtil.createCityMarkerBitmap())

                initAnnotationManagers(map, style)
                setupMapClickListener(map)
                setupMarkerDragListener()
                centerMap(map)
                refreshAllAnnotations()
                setupDynamicCircleUpdates()
            }
            mapView.addOnDidFinishLoadingStyleListener {
                map.getStyle { style ->
                    style.addImage(MapDrawingUtil.MARKER_ICON_ORDINARY, MapDrawingUtil.createOrdinaryMarkerBitmap())
                    style.addImage(MapDrawingUtil.MARKER_ICON_INCIDENT, MapDrawingUtil.createIncidentMarkerBitmap())
                    style.addImage(MapDrawingUtil.MARKER_ICON_CITY,     MapDrawingUtil.createCityMarkerBitmap())

                    // DEBUG — log whether each image is actually retrievable immediately after adding
                    val ordinaryCheck = style.getImage(MapDrawingUtil.MARKER_ICON_ORDINARY)
                    val incidentCheck = style.getImage(MapDrawingUtil.MARKER_ICON_INCIDENT)
                    val cityCheck     = style.getImage(MapDrawingUtil.MARKER_ICON_CITY)
                    android.util.Log.d("MapImageDebug", "ordinary: ${ordinaryCheck?.width}x${ordinaryCheck?.height}")
                    android.util.Log.d("MapImageDebug", "incident: ${incidentCheck?.width}x${incidentCheck?.height}")
                    android.util.Log.d("MapImageDebug", "city:     ${cityCheck?.width}x${cityCheck?.height}")

                    // DEBUG — log what symbol create actually returns
                    // (add this after refreshAllAnnotations)
                }
            }
        }
    }

    /**
     * Sets up dynamic circle updates when the user zooms in/out
     * This mimics Leaflet's behavior where circles maintain their real-world size
     */
    private fun setupDynamicCircleUpdates() {
        mapboxMap?.addOnCameraMoveListener {
            val currentZoom = mapboxMap?.cameraPosition?.zoom ?: 0.0
            // Update when zoom changes significantly to avoid excessive recalculations
            if (kotlin.math.abs(currentZoom - lastZoomLevel) > 0.1) {
                lastZoomLevel = currentZoom
                updateAllIncidentCircles()
            }
        }
    }

    /**
     * Updates all incident circles based on current zoom level
     * This ensures circles maintain their real-world geographic size
     */
    private fun updateAllIncidentCircles() {
        vm.elements.value?.forEach { element ->
            if (element is IncidentMarker) {
                val circle = circleById[element.id]
                if (circle != null) {
                    val newRadius = MapDrawingUtil.metersToPixels(mapboxMap, element.radiusMeters, mapboxMap?.cameraPosition?.zoom?: 10.0)
                    circle.circleRadius = newRadius
                    circleManager?.update(circle)
                }
            }
        }
    }

    private fun initAnnotationManagers(map: MapLibreMap, style: Style) {
        symbolManager = SymbolManager(mapView, map, style).apply {
            iconAllowOverlap = true
            textAllowOverlap = true
            iconIgnorePlacement = true
            textIgnorePlacement = true
            addClickListener { symbol ->
                val elementId = symbol.data?.asString ?: return@addClickListener false
                onAnnotationClicked(elementId)
                true
            }
        }
        lineManager = LineManager(mapView, map, style).apply {
            setLineDasharray(arrayOf(2f, 2f))
        }
        circleManager = CircleManager(mapView, map, style)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMapClickListener(map: MapLibreMap) {
        map.addOnMapClickListener { latLng ->
            when (vm.activeTool.value) {
                MapEditorViewModel.Tool.SELECT -> false  // let SymbolManager handle it
                else -> {
                    vm.onMapTap(latLng.latitude, latLng.longitude)
                    true
                }
            }
        }
    }

    private fun setupMarkerDragListener() {
        symbolManager?.addDragListener(object : OnSymbolDragListener {
            override fun onAnnotationDragStarted(annotation: Symbol) {}
            override fun onAnnotationDrag(annotation: Symbol) {}
            override fun onAnnotationDragFinished(annotation: Symbol) {
                val elementId = annotation.data?.asString ?: return
                vm.moveMarker(elementId, annotation.latLng.latitude, annotation.latLng.longitude)
            }
        })
    }

    private fun centerMap(map: MapLibreMap) {
        val lat = intent.getDoubleExtra(EXTRA_CENTER_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_CENTER_LNG, 0.0)
        val zoom = intent.getDoubleExtra(EXTRA_ZOOM, 4.0)
        if (lat != 0.0 || lng != 0.0) {
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(lat, lng))
                .zoom(zoom)
                .build()
        }
    }

    private fun observeViewModel() {
        vm.elements.observe(this) { refreshAllAnnotations() }
        vm.canUndo.observe(this) { fabUndo.isEnabled = it }
        vm.canRedo.observe(this) { fabRedo.isEnabled = it }
        vm.activeTool.observe(this) {
            syncToolChips(it)
            updateChipSelection(it)
        }
        vm.pathInProgress.observe(this) { pts ->
            val list = pts ?: emptyList()
            updatePathBar(list)
            drawPathPreview(list)
        }
        vm.selectedElementId.observe(this) { id ->
            if (id != null) showElementBottomSheet(id)
        }
    }

    private fun updateAnnotationLabel(elementId: String, newLabel: String) {
        symbolById[elementId]?.let { symbol ->
            symbol.textField = newLabel
            symbolManager?.update(symbol)
        }
    }

    private fun refreshAllAnnotations() {
        val sm = symbolManager ?: return
        val cm = circleManager ?: return
        val lm = lineManager ?: return

        val elements = vm.elements.value ?: emptyList()
        val currentIds = elements.map { it.id }.toSet()

        // --- Remove annotations that no longer exist ---
        val removedSymbolIds = symbolById.keys - currentIds
        val removedCircleIds = circleById.keys - currentIds
        val removedLineIds   = lineById.keys   - currentIds

        removedSymbolIds.forEach { id -> symbolById.remove(id)?.let { sm.delete(it) } }
        removedCircleIds.forEach { id -> circleById.remove(id)?.let { cm.delete(it) } }
        removedLineIds.forEach   { id -> lineById.remove(id)?.let   { lm.delete(it) } }

        // --- Add annotations that are new ---
        elements.forEach { element ->
            when (element) {
                is IncidentMarker -> if (!symbolById.containsKey(element.id)) renderIncident(element, sm, cm)
                is CityMarker     -> if (!symbolById.containsKey(element.id)) renderCity(element, sm)
                is OrdinaryMarker -> if (!symbolById.containsKey(element.id)) renderOrdinary(element, sm)
                is PathElement    -> if (!lineById.containsKey(element.id))   renderPath(element, lm)
            }
        }
    }

    private fun renderOrdinary(el: OrdinaryMarker, sm: SymbolManager) {
        val symbol = MapDrawingUtil.renderOrdinaryMarker(el, sm, mapboxMap)
        android.util.Log.d("MapImageDebug", "renderOrdinary: symbol=${symbol}, id=${symbol?.id}")
        symbol?.let { symbolById[el.id] = it }
    }

    private fun renderPath(el: PathElement, lm: LineManager) {
        val line = MapDrawingUtil.renderPath(el, lm)
        line?.let { lineById[el.id] = it }
    }

    private fun renderIncident(el: IncidentMarker, sm: SymbolManager, cm: CircleManager) {
        // Calculate radius in pixels dynamically based on current zoom level
        val radiusInPixels = MapDrawingUtil.metersToPixels(mapboxMap, el.radiusMeters, mapboxMap?.cameraPosition?.zoom?:10.0)

        // Create the circle with dynamic pixel radius
        val circle = cm.create(
            CircleOptions()
                .withLatLng(LatLng(el.latitude, el.longitude))
                .withCircleRadius(radiusInPixels)
                .withCircleColor("#ff4757")
                .withCircleOpacity(0.15f)
                .withCircleStrokeColor("#ff4757")
                .withCircleStrokeWidth(2f)
                .withCircleStrokeOpacity(0.6f)
        )
        circleById[el.id] = circle
        val symbol = sm.create(
            SymbolOptions()
                .withLatLng(LatLng(el.latitude, el.longitude))
                .withIconImage(MapDrawingUtil.MARKER_ICON_INCIDENT)
                .withIconSize(1.0f)
                .withTextField(el.label)
                .withTextFont(arrayOf("Noto Sans Regular"))
                .withTextOffset(arrayOf(0f, 1.2f))
                .withTextColor("#ffffff")
                .withTextSize(11f)
                .withDraggable(true)
                .withData(com.google.gson.JsonPrimitive(el.id))
        )
        android.util.Log.d("MapImageDebug", "renderIncident: symbol=${symbol}, id=${symbol?.id}")
        symbol?.let { symbolById[el.id] = it }
    }

    private fun renderCity(el: CityMarker, sm: SymbolManager) {
        val symbol = sm.create(
            SymbolOptions()
                .withLatLng(LatLng(el.latitude, el.longitude))
                .withIconImage(MapDrawingUtil.MARKER_ICON_CITY)
                .withIconSize(1.0f)
                .withTextField(el.label)
                .withTextFont(arrayOf("Noto Sans Regular"))
                .withTextOffset(arrayOf(0f, 1.2f))
                .withTextColor("#ffffff")
                .withTextSize(11f)
                .withDraggable(true)
                .withData(com.google.gson.JsonPrimitive(el.id))
        )
        android.util.Log.d("MapImageDebug", "renderCity: symbol=${symbol}, id=${symbol?.id}")
        symbol?.let { symbolById[el.id] = it }
    }

    private var previewLine: Line? = null

    private fun drawPathPreview(pts: List<Pair<Double, Double>>) {
        val lm = lineManager ?: return
        lm.setLineDasharray(arrayOf(2f, 2f))
        previewLine?.let { lm.delete(it) }
        previewLine = null
        if (pts.size >= 2) {
            previewLine = lm.create(
                LineOptions()
                    .withLatLngs(pts.map { (lat, lng) -> LatLng(lat, lng) })
                    .withLineColor("#FFD700")
                    .withLineWidth(3f)
                    .withLineOpacity(0.6f)
            )
        }
    }

    private fun onAnnotationClicked(elementId: String) {
        if (vm.activeTool.value == MapEditorViewModel.Tool.SELECT) {
            vm.selectElement(elementId)
        }
    }

    private fun showElementBottomSheet(elementId: String) {
        val element = vm.getElementById(elementId) ?: return
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_map_element, null)
        sheet.setContentView(view)

        val titleTv = view.findViewById<TextView>(R.id.elementTitle)
        val labelEt = view.findViewById<EditText>(R.id.labelEditText)
        val descEt = view.findViewById<EditText>(R.id.descEditText)
        val radiusGroup = view.findViewById<LinearLayout>(R.id.radiusGroup)
        val radiusSlider = view.findViewById<Slider>(R.id.radiusSlider)
        val radiusTv = view.findViewById<TextView>(R.id.radiusTv)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveElement)
        val btnDelete = view.findViewById<MaterialButton>(R.id.btnDeleteElement)

        titleTv.text = when (element) {
            is IncidentMarker -> "🚨 Incident Marker"
            is CityMarker -> "🏙 City Marker"
            is OrdinaryMarker -> "📍 Marker"
            is PathElement -> "✏ Path"
        }

        labelEt.setText(when (element) {
            is IncidentMarker -> element.label
            is CityMarker -> element.label
            is OrdinaryMarker -> element.label
            is PathElement -> element.label
        })

        descEt.setText(when (element) {
            is IncidentMarker -> element.description
            is CityMarker -> element.description
            is OrdinaryMarker -> element.description
            else -> ""
        })

        if (element is IncidentMarker) {
            radiusGroup.visibility = View.VISIBLE
            radiusSlider.value = element.radiusMeters.toFloat()
            radiusTv.text = "${element.radiusMeters.toInt()} m"
            radiusSlider.addOnChangeListener { _, value, _ ->
                radiusTv.text = "${value.toInt()} m"
            }
        } else {
            radiusGroup.visibility = View.GONE
        }

        btnSave.setOnClickListener {
            val newLabel = labelEt.text.toString()
            val newDescription = descEt.text.toString()
            // Update label
            updateAnnotationLabel(elementId,newLabel)
            vm.editLabel(elementId, newLabel)
            vm.editDescription(elementId, newDescription)
            // Update description if the element supports it
            when (element) {
                is IncidentMarker -> {
                    vm.changeRadius(elementId, radiusSlider.value.toDouble())
                    val circle = circleById[elementId]
                    if (circle != null) {
                        val newRadius = MapDrawingUtil.metersToPixels(
                            mapboxMap,
                            radiusSlider.value.toDouble(),
                            mapboxMap?.cameraPosition?.zoom ?: 10.0
                        )
                        circle.circleRadius = newRadius
                        circleManager?.update(circle)
                    }
                }

                else -> {}
            }

            sheet.dismiss()
        }

        btnDelete.setOnClickListener {
            vm.removeElement(elementId)
            vm.selectElement(null)
            sheet.dismiss()
        }

        sheet.setOnDismissListener { vm.selectElement(null) }
        sheet.show()
    }

    private fun showCommitPathDialog() {
        val et = EditText(this).apply { hint = "Path label (optional)" }
        AlertDialog.Builder(this)
            .setTitle("Save path")
            .setView(et)
            .setPositiveButton("Save") { _, _ -> vm.commitPath(et.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePathBar(pts: List<Pair<Double, Double>>) {
        val drawing = vm.activeTool.value == MapEditorViewModel.Tool.DRAW_PATH
        pathInProgressBar.visibility = if (drawing || pts.isNotEmpty()) View.VISIBLE else View.GONE
        pathPointsCountTv.text = "${pts.size} point${if (pts.size != 1) "s" else ""}"
        fabCommitPath.isEnabled = pts.size >= 2
        fabCancelPath.isEnabled = pts.isNotEmpty()
    }

    private fun syncToolChips(tool: MapEditorViewModel.Tool) {
        for (i in 0 until toolChipGroup.childCount) {
            val chip = toolChipGroup.getChildAt(i) as? Chip ?: continue
            chip.isChecked = chip.tag == tool
        }
    }

    private fun confirmDiscard() {
        if (!vm.canUndo.value!!) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Discard changes?")
            .setMessage("Unsaved map edits will be lost.")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep editing", null)
            .show()
    }

    data class AiLocationSuggestion(
        val name: String,
        val description: String,
        val latitude: Double,
        val longitude: Double,
        val type: String,
        val radiusMeters: Double = 5000.0,
        val pathPoints: List<Pair<Double, Double>>? = null
    )

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}