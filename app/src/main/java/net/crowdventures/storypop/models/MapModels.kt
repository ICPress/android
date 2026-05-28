package net.crowdventures.storypop.map.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import net.crowdventures.storypop.util.MapGeometryDeserializer

// ─────────────────────────────────────────────────────────────────
// GEOJSON SCHEMA  (shared with Leaflet / web)
// ─────────────────────────────────────────────────────────────────

/** Top-level GeoJSON FeatureCollection – serialised for storage / API */
data class StoryMapGeoJson(
    @SerializedName("type")       val type: String = "FeatureCollection",
    @SerializedName("features")   val features: MutableList<MapFeature> = mutableListOf()
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): StoryMapGeoJson {
            val gson = GsonBuilder()
                .registerTypeAdapter(MapGeometry::class.java, MapGeometryDeserializer())
                .create()

            return gson.fromJson(json, StoryMapGeoJson::class.java)
        }

    }
}

/** One GeoJSON Feature – can represent a marker, path, or incident */
data class MapFeature(
    @SerializedName("type")       val type: String = "Feature",
    @SerializedName("geometry")   val geometry: MapGeometry,
    @SerializedName("properties") val properties: MapProperties
)

// ── Geometry ─────────────────────────────────────────────────────

sealed class MapGeometry {
    abstract val type: String
}

/**
 * Point geometry – used for Incident marker, City marker, Ordinary marker.
 * coordinates = [longitude, latitude]  (GeoJSON order)
 */
data class PointGeometry(
    @SerializedName("type")        override val type: String = "Point",
    @SerializedName("coordinates") val coordinates: List<Double>  // [lng, lat]
) : MapGeometry() {
    val latitude:  Double get() = coordinates[1]
    val longitude: Double get() = coordinates[0]
}

/**
 * LineString geometry – used for flight paths / movement paths.
 * coordinates = [[lng, lat], [lng, lat], ...]
 */
data class LineStringGeometry(
    @SerializedName("type")        override val type: String = "LineString",
    @SerializedName("coordinates") val coordinates: List<List<Double>>
) : MapGeometry() {
    fun latLngPairs(): List<Pair<Double, Double>> =
        coordinates.map { it[1] to it[0] }   // lat, lng
}

// ── Properties ───────────────────────────────────────────────────

/**
 * Properties block – mirrors every field used by the Leaflet popup and legend.
 *
 * markerType values:
 *   "incident"  – red pulsing crash/incident marker + optional radius circle
 *   "city"      – white/blue city dot with label
 *   "ordinary"  – plain white dot with editable label
 *   "path"      – yellow dashed polyline (flight route / movement)
 */
data class MapProperties(
    @SerializedName("markerType")   val markerType: MarkerType,
    @SerializedName("label")        val label: String           = "",
    @SerializedName("description")  val description: String     = "",
    // Incident-only
    @SerializedName("radiusMeters") val radiusMeters: Double    = 5000.0,
    // Path-only
    @SerializedName("pathLabel")    val pathLabel: String       = "",
    @SerializedName("dashPattern")  val dashPattern: String     = "10,10",
    // Display hints preserved from Leaflet
    @SerializedName("color")        val color: String           = "",
    @SerializedName("weight")       val weight: Float           = 3f,
    @SerializedName("opacity")      val opacity: Float          = 0.7f
)

enum class MarkerType {
    @SerializedName("incident")  INCIDENT,
    @SerializedName("city")      CITY,
    @SerializedName("ordinary")  ORDINARY,
    @SerializedName("path")      PATH
}

// ─────────────────────────────────────────────────────────────────
// DOMAIN MODEL  (used inside the editor activity / ViewModel)
// ─────────────────────────────────────────────────────────────────

/** Lightweight domain wrapper used by the editor ViewModel */
sealed class MapElement {
    abstract val id: String         // stable UUID for undo/redo identity
    abstract fun toFeature(): MapFeature
}

data class IncidentMarker(
    override val id: String,
    val latitude: Double,
    val longitude: Double,
    val label: String       = "Incident",
    val description: String = "",
    val radiusMeters: Double = 5000.0
) : MapElement() {
    override fun toFeature() = MapFeature(
        geometry   = PointGeometry(coordinates = listOf(longitude, latitude)),
        properties = MapProperties(
            markerType   = MarkerType.INCIDENT,
            label        = label,
            description  = description,
            radiusMeters = radiusMeters,
            color        = "#ff4757"
        )
    )
}

data class CityMarker(
    override val id: String,
    val latitude: Double,
    val longitude: Double,
    val label: String       = "",
    val description: String = ""
) : MapElement() {
    override fun toFeature() = MapFeature(
        geometry   = PointGeometry(coordinates = listOf(longitude, latitude)),
        properties = MapProperties(
            markerType  = MarkerType.CITY,
            label       = label,
            description = description,
            color       = "#ffffff"
        )
    )
}

data class OrdinaryMarker(
    override val id: String,
    val latitude: Double,
    val longitude: Double,
    val label: String       = "",
    val description: String = ""
) : MapElement() {
    override fun toFeature() = MapFeature(
        geometry   = PointGeometry(coordinates = listOf(longitude, latitude)),
        properties = MapProperties(
            markerType  = MarkerType.ORDINARY,
            label       = label,
            description = description,
            color       = "#ffffff"
        )
    )
}

data class PathElement(
    override val id: String,
    /** List of (lat, lng) waypoints */
    val points: List<Pair<Double, Double>>,
    val label: String   = "",
    val description: String = "",
    val color: String   = "#ffa502",
    val weight: Float   = 3f,
    val opacity: Float  = 0.7f,
    val dashPattern: String = "10,10"
) : MapElement() {
    override fun toFeature() = MapFeature(
        geometry   = LineStringGeometry(
            coordinates = points.map { (lat, lng) -> listOf(lng, lat) }
        ),
        properties = MapProperties(
            markerType  = MarkerType.PATH,
            label   = label,
            description = description,
            color       = color,
            weight      = weight,
            opacity     = opacity,
            dashPattern = dashPattern
        )
    )
}

// ─────────────────────────────────────────────────────────────────
// EDITOR STATE MODEL  (Parcelable so it survives rotation)
// ─────────────────────────────────────────────────────────────────

/** Everything needed to save / restore a map session */
data class MapEditorState(
    val title: String           = "",
    val centerLat: Double       = 0.0,
    val centerLng: Double       = 0.0,
    val zoom: Double            = 8.0,
    val geoJsonString: String   = "{\"type\":\"FeatureCollection\",\"features\":[]}"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        title         = parcel.readString() ?: "",
        centerLat     = parcel.readDouble(),
        centerLng     = parcel.readDouble(),
        zoom          = parcel.readDouble(),
        geoJsonString = parcel.readString() ?: "{\"type\":\"FeatureCollection\",\"features\":[]}"
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeDouble(centerLat)
        parcel.writeDouble(centerLng)
        parcel.writeDouble(zoom)
        parcel.writeString(geoJsonString)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<MapEditorState> {
        override fun createFromParcel(parcel: Parcel) = MapEditorState(parcel)
        override fun newArray(size: Int): Array<MapEditorState?> = arrayOfNulls(size)
    }
}

// ─────────────────────────────────────────────────────────────────
// UNDO / REDO  command stack
// ─────────────────────────────────────────────────────────────────

sealed class EditorCommand {
    data class AddElement(val element: MapElement) : EditorCommand()
    data class RemoveElement(val element: MapElement) : EditorCommand()
    data class MoveMarker(val id: String, val oldLat: Double, val oldLng: Double, val newLat: Double, val newLng: Double) : EditorCommand()
    data class EditLabel(val id: String, val oldLabel: String, val newLabel: String) : EditorCommand()
    data class EditDescription(val id: String, val oldDescription: String, val newDescription: String) : EditorCommand()
    data class ChangeRadius(val id: String, val oldRadius: Double, val newRadius: Double) : EditorCommand()
    data class AddPathPoint(val id: String, val point: Pair<Double, Double>) : EditorCommand()
}