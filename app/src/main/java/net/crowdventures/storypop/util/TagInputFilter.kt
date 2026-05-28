package net.crowdventures.storypop.util

import android.text.InputFilter
import android.text.Spanned

class TagInputFilter : InputFilter {

   override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val specialChars = "/*!@$%^&*()\"{}_[]|\\?/<>,.:-'';§£¥..."
        for (i in start until end) {
            val type = Character.getType(source[i])
            if (type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt() || type == Character.MATH_SYMBOL.toInt() || specialChars.contains(
                    "" + source
                ) || Character.isWhitespace(0)
            ) {
                return ""
            }
        }
        return null
    }
}