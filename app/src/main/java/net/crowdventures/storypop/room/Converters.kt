package net.crowdventures.storypop.room

import androidx.room.TypeConverter
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromUUID(uuid: UUID?):String?{
        if (uuid == null) return null
        return uuid.toString()
    }
    @TypeConverter
    fun toUUID(string: String?):UUID?{
        if (string == null) return null
        return UUID.fromString(string)
    }
    @TypeConverter
    fun fromPendingUploadType(type: PendingUploadType):String{
        return type.name.toString()
    }
    @TypeConverter
    fun toPendingUploadType(string: String):PendingUploadType{
        return PendingUploadType.valueOf(string)
    }

    @TypeConverter
    fun fromMessageType(type: MessageType):String{
        return type.name.toString()
    }
    @TypeConverter
    fun toMessageType(string: String):MessageType{
        return MessageType.valueOf(string)
    }
}