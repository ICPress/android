package net.crowdventures.storypop.map.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.gson.JsonPrimitive
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.*
import net.crowdventures.storypop.map.models.OrdinaryMarker
import net.crowdventures.storypop.map.models.PathElement
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

/**
 * Utility class for map drawing operations
 */
object MapDrawingUtil {

    // Marker types
    const val MARKER_ICON_ORDINARY = "ordinary_icon"
    const val MARKER_ICON_INCIDENT = "incident_icon"
    const val MARKER_ICON_CITY = "city_icon"

    // Path styling
    const val PATH_COLOR = "#FFD700"
    const val PATH_WIDTH = 4f
    const val PATH_OPACITY = 0.9f
    const val PATH_DASH_PATTERN = "2,2"
    const val MAP_FONT = "Noto Sans Regular"

    // Earth's circumference at the equator in metres (WGS-84)
    private const val EARTH_CIRCUMFERENCE_METRES = 40_075_016.686

    // MapLibre tile size in pixels (same as Leaflet default)
    private const val TILE_SIZE = 256.0

    /**
     * Registers all marker bitmaps into a Style that is fully loaded.
     * Call this inside addOnDidFinishLoadingStyleListener, not inside setStyle { }.
     * Uses style.addImage directly (no null guard) so bitmaps are always current.
     */
    fun addMarkerImages(style: Style) {
        style.addImage(MARKER_ICON_ORDINARY, createOrdinaryMarkerBitmap())
        style.addImage(MARKER_ICON_INCIDENT, createIncidentMarkerBitmap())
        style.addImage(MARKER_ICON_CITY, createCityMarkerBitmap())
    }

    /**
     * Legacy overload kept for call sites that only have MapLibreMap available.
     * Prefer the Style overload above wherever possible.
     */
    fun addMarkerImages(mapboxMap: MapLibreMap?) {
        mapboxMap?.style?.let { addMarkerImages(it) }
    }

    /**
     * Renders an ordinary marker on the map
     */
    fun renderOrdinaryMarker(
        el: OrdinaryMarker,
        symbolManager: SymbolManager?,
        mapboxMap: MapLibreMap?
    ): Symbol? {
        return symbolManager?.create(
            SymbolOptions()
                .withLatLng(LatLng(el.latitude, el.longitude))
                .withIconImage(MARKER_ICON_ORDINARY)
                .withIconSize(1.0f)
                .withTextField(el.label)
                .withTextFont(arrayOf(MAP_FONT))
                .withTextOffset(arrayOf(0f, 1.2f))
                .withTextColor("#ffffff")
                .withTextSize(11f)
                .withDraggable(true)
                .withData(JsonPrimitive(el.id))
        )
    }

    /**
     * Renders a path on the map
     */
    fun renderPath(
        el: PathElement,
        lineManager: LineManager?
    ): Line? {
        lineManager?.setLineDasharray(arrayOf(2f, 2f))
        return lineManager?.create(
            LineOptions()
                .withLatLngs(el.points.map { (lat, lng) -> LatLng(lat, lng) })
                .withLineColor(PATH_COLOR)
                .withLineWidth(PATH_WIDTH)
                .withLineOpacity(PATH_OPACITY)
        )
    }

    /**
     * Creates an incident marker bitmap
     */
    fun createIncidentMarkerBitmap(): Bitmap {
        return createCircleBitmap(
            fill = Color.parseColor("#FF4757"),
            diameter = 20,
            strokeColor = Color.WHITE,
            strokeWidth = 3,
            withExclamation = true
        )
    }

    /**
     * Creates a city marker bitmap
     */
    fun createCityMarkerBitmap(): Bitmap {
        return createCircleBitmap(
            fill = Color.parseColor("#1E90FF"),
            diameter = 20,
            strokeColor = Color.WHITE,
            strokeWidth = 3,
            withExclamation = false
        )
    }

    /**
     * Creates an ordinary marker bitmap (outlined circle)
     */
    fun createOrdinaryMarkerBitmap(): Bitmap {
        return createCircleBitmap(
            fill = Color.GRAY,
            diameter = 30,
            strokeColor = Color.WHITE,
            strokeWidth = 5,
            withExclamation = false
        )
    }

    /**
     * Creates a filled circle bitmap.*/
    fun createCircleBitmap(
        fill: Int,
        diameter: Int,
        strokeColor: Int,
        strokeWidth: Int,
        withExclamation: Boolean = false
    ): Bitmap {
        val padding = 2  // prevents anti-alias clipping at bitmap boundary
        val size = diameter + strokeWidth * 2 + padding * 2
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        val r = diameter / 2f

        // Stroke (outer ring)
        val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r + strokeWidth, paintStroke)

        // Fill (inner circle)
        val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fill
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r, paintFill)

        // Add exclamation mark for incident markers
        if (withExclamation) {
            val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                textSize = diameter * 0.8f  // scale with diameter, not hardcoded
                textAlign = Paint.Align.CENTER
            }
            // fontMetrics-based vertical centering
            val metrics = paintText.fontMetrics
            val textY = cy - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText("!", cx, textY, paintText)
        }

        return bmp
    }

    /**
     * Creates an outlined circle bitmap
     */
    fun createOutlinedCircleBitmap(
        fill: Int,
        diameter: Int,
        strokeColor: Int,
        strkWidth: Int
    ): Bitmap {
        val padding = 2
        val size = diameter + strkWidth * 2 + padding * 2
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        val r = diameter / 2f

        // Stroke (outer ring)
        val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = strkWidth.toFloat()  // fixed: was strokeWidth (Paint property = 0)
        }
        canvas.drawCircle(cx, cy, r + strkWidth / 2f, paintStroke)

        // Fill (inner circle)
        val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fill
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r - strkWidth / 2f, paintFill)

        return bmp
    }

    /**
     * Converts a real-world radius in metres to the pixel radius expected by
     * MapLibre/Mapbox GL's circleRadius paint property.
     */
    fun metersToPixels(
        map: MapLibreMap?,
        radiusMetres: Double,
        zoom: Double,
        latitudeDegrees: Double? = null
    ): Float {
        val lat = latitudeDegrees
            ?: map?.cameraPosition?.target?.latitude
            ?: 0.0

        val latRadians = lat * PI / 180.0

        val metresPerPixel =
            (EARTH_CIRCUMFERENCE_METRES * cos(latRadians)) / (TILE_SIZE * 2.0.pow(zoom))

        return if (metresPerPixel > 0) {
            (radiusMetres / metresPerPixel).toFloat()
        } else {
            0f
        }
    }

    /**
     * Creates a symbol layer for markers.*/
    fun createSymbolLayer(
        sourceId: String,
        layerId: String
    ): org.maplibre.android.style.layers.SymbolLayer {
        return org.maplibre.android.style.layers.SymbolLayer(layerId, sourceId).apply {
            setProperties(
                org.maplibre.android.style.layers.PropertyFactory.iconImage(
                    org.maplibre.android.style.expressions.Expression.get("icon")
                ),
                org.maplibre.android.style.layers.PropertyFactory.iconSize(1.2f),
                org.maplibre.android.style.layers.PropertyFactory.textField(
                    org.maplibre.android.style.expressions.Expression.get("label")
                ),
                org.maplibre.android.style.layers.PropertyFactory.textFont(arrayOf(MAP_FONT)),
                org.maplibre.android.style.layers.PropertyFactory.textOffset(arrayOf(0f, 1.5f)),
                org.maplibre.android.style.layers.PropertyFactory.textColor("#ffffff"),
                org.maplibre.android.style.layers.PropertyFactory.textSize(12f),
                org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true),
                org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap(true)
            )
        }
    }

    /**
     * Creates a line layer for paths
     */
    fun createLineLayer(
        sourceId: String,
        layerId: String,
        color: String = PATH_COLOR,
        width: Float = PATH_WIDTH,
        opacity: Float = PATH_OPACITY,
        dashPattern: Array<Float> = arrayOf(2f, 2f)
    ): org.maplibre.android.style.layers.LineLayer {
        return org.maplibre.android.style.layers.LineLayer(layerId, sourceId).apply {
            setProperties(
                org.maplibre.android.style.layers.PropertyFactory.lineColor(color),
                org.maplibre.android.style.layers.PropertyFactory.lineWidth(width),
                org.maplibre.android.style.layers.PropertyFactory.lineOpacity(opacity),
                org.maplibre.android.style.layers.PropertyFactory.lineDasharray(dashPattern)
            )
        }
    }
}