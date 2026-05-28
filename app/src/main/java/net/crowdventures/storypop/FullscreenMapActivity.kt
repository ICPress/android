package net.crowdventures.storypop

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.sources.GeoJsonSource
import net.crowdventures.storypop.map.models.LineStringGeometry
import net.crowdventures.storypop.map.models.MarkerType
import net.crowdventures.storypop.map.models.PointGeometry
import net.crowdventures.storypop.map.models.StoryMapGeoJson
import net.crowdventures.storypop.map.util.MapDrawingUtil

class FullscreenMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var btnClose: ImageButton
    private var mapboxMap: MapLibreMap? = null

    // Held so the finish-loading listener can access them
    private var pendingMapData: String = ""
    private var pendingCenterLat: Double = 0.0
    private var pendingCenterLng: Double = 0.0
    private var pendingZoom: Double = 4.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContentView(R.layout.activity_fullscreen_map)

        mapView = findViewById(R.id.fullscreen_map)
        btnClose = findViewById(R.id.btn_close_map)

        pendingMapData   = intent.getStringExtra("map_data") ?: ""
        pendingCenterLat = intent.getDoubleExtra("center_lat", 0.0)
        pendingCenterLng = intent.getDoubleExtra("center_lng", 0.0)
        pendingZoom      = intent.getDoubleExtra("zoom", 4.0)

        btnClose.setOnClickListener { finish() }

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            mapboxMap = map

            val inputStream = resources.openRawResource(R.raw.dark)
            val mapContent = inputStream.bufferedReader().use { it.readText() }
            val nightStyle = Style.Builder().fromJson(mapContent)

            mapView.addOnDidFinishLoadingStyleListener {
                map.getStyle { style ->
                    // Register bitmaps now that the GL texture atlas is ready.
                    MapDrawingUtil.addMarkerImages(style)

                    try {
                        setupMap(style, map, pendingMapData, pendingCenterLat, pendingCenterLng, pendingZoom)
                    } catch (e: Exception) {
                        Log.e("FullscreenMap", "Setup error", e)
                        Toast.makeText(this, "Failed to load map: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            map.setStyle(nightStyle)
        }
    }

    private fun setupMap(
        style: Style,
        map: MapLibreMap,
        mapData: String,
        centerLat: Double,
        centerLng: Double,
        zoom: Double
    ) {
        val geoJson = StoryMapGeoJson.fromJson(mapData)

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
                        val pixelRadius  = MapDrawingUtil.metersToPixels(mapboxMap, radiusMeters, zoom)
                        mapboxFeature.addNumberProperty("radius_pixels", pixelRadius.toDouble())
                        circleFeatures.add(mapboxFeature)
                    }

                    pointFeatures.add(mapboxFeature)
                }
                is LineStringGeometry -> {
                    val points = geom.latLngPairs().map {
                        org.maplibre.geojson.Point.fromLngLat(it.second, it.first)
                    }
                    val lineString   = org.maplibre.geojson.LineString.fromLngLats(points)
                    val mapboxFeature = org.maplibre.geojson.Feature.fromGeometry(lineString)
                    mapboxFeature.addStringProperty("label", feature.properties.label ?: "Path")
                    lineFeatures.add(mapboxFeature)
                }
                else -> {
                    Log.d("FullscreenMap", "Unknown geometry type: ${geom?.javaClass?.simpleName}")
                }
            }
        }

        Log.d("FullscreenMap", "Points: ${pointFeatures.size}, Lines: ${lineFeatures.size}, Circles: ${circleFeatures.size}")

        if (pointFeatures.isNotEmpty()) {
            val pointCollection = org.maplibre.geojson.FeatureCollection.fromFeatures(pointFeatures)
            style.addSource(GeoJsonSource("story-map-points", pointCollection))
            style.addLayer(MapDrawingUtil.createSymbolLayer("story-map-points", "markers"))
        }

        if (lineFeatures.isNotEmpty()) {
            val lineCollection = org.maplibre.geojson.FeatureCollection.fromFeatures(lineFeatures)
            style.addSource(GeoJsonSource("story-map-lines", lineCollection))
            style.addLayer(MapDrawingUtil.createLineLayer("story-map-lines", "paths"))
            Log.d("FullscreenMap", "Line layer added with ${lineFeatures.size} features")
        }

        if (circleFeatures.isNotEmpty()) {
            val circleCollection = org.maplibre.geojson.FeatureCollection.fromFeatures(circleFeatures)
            style.addSource(GeoJsonSource("story-map-circles", circleCollection))

            val circleLayer = org.maplibre.android.style.layers.CircleLayer("incident-circles", "story-map-circles")
            circleLayer.setProperties(
                PropertyFactory.circleColor("#ff4757"),
                PropertyFactory.circleOpacity(0.15f),
                PropertyFactory.circleStrokeColor("#ff4757"),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeOpacity(0.6f),
                PropertyFactory.circleRadius(Expression.get("radius_pixels"))
            )
            style.addLayer(circleLayer)
            Log.d("FullscreenMap", "Circle layer added with ${circleFeatures.size} incident circles")
        }

        val target = if (centerLat != 0.0) LatLng(centerLat, centerLng) else LatLng(20.0, 0.0)
        map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(target, zoom))

        map.addOnMapClickListener {
            finish()
            true
        }
    }

    override fun onStart()    { super.onStart();    mapView.onStart()    }
    override fun onResume()   { super.onResume();   mapView.onResume()   }
    override fun onPause()    { super.onPause();    mapView.onPause()    }
    override fun onStop()     { super.onStop();     mapView.onStop()     }
    override fun onLowMemory(){ super.onLowMemory();mapView.onLowMemory()}
    override fun onDestroy()  { super.onDestroy();  mapView.onDestroy()  }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}