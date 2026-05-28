package net.crowdventures.storypop.util

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import net.crowdventures.storypop.map.models.LineStringGeometry
import net.crowdventures.storypop.map.models.MapGeometry
import net.crowdventures.storypop.map.models.PointGeometry
import java.lang.reflect.Type

class MapGeometryDeserializer : JsonDeserializer<MapGeometry> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): MapGeometry {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type").asString

        return when (type) {
            "Point" -> context.deserialize(json, PointGeometry::class.java)
            "LineString" -> context.deserialize(json, LineStringGeometry::class.java)
            else -> throw JsonParseException("Unknown geometry type: $type")
        }
    }
}