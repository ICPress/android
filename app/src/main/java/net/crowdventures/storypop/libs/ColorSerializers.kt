package net.crowdventures.storypop.libs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.toArgb

class ColorSerializers {
    companion object{
        fun androidx.compose.ui.graphics.Color?.serialize(): String {
            return if (this == null || isUnspecified) "null" else "${toArgb()}"
        }

        fun String.deserializeToColor(): androidx.compose.ui.graphics.Color? {
            return if (this == "null") null else Color(this.toInt())
        }
    }

}