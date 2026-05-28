package net.crowdventures.storypop.util

import android.text.Editable
import android.text.TextWatcher

class MultilineRemover {
    companion object{
        fun getMultilineTextRemover(maxLines:Int, onTextChanged:(String)->Unit) : TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun afterTextChanged(p0: Editable?) {
                    if (p0 == null) return
                    onTextChanged(p0.toString().replace("\n",""))
                    val indexLineBr = p0.indexOf("\n")
                    if (indexLineBr >= 0){
                        p0.delete(indexLineBr, p0.length)
                    }
                }
            }
        }
    }

}