package net.crowdventures.storypop.util.endpoints

import com.google.gson.*
import java.lang.reflect.Type

class NullableUintJson : JsonSerializer<UInt?>, JsonDeserializer<UInt?>
{
    override fun serialize(src: UInt?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement
    {
        if (src == null)
            return JsonNull.INSTANCE

        return JsonPrimitive(src.toLong())
    }

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): UInt?
    {
        if (json.isJsonNull)
            return null

        return json.asLong.toUInt()
    }
}