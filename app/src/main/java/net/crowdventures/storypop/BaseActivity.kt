package net.crowdventures.storypop

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

open class BaseActivity: AppCompatActivity(){
    private var keyboardListenersAttached = false
    private var rootLayout: ViewGroup? = null

    protected open fun onShowKeyboard(keyboardHeight: Int) {}
    protected open fun onHideKeyboard() {}

    protected fun attachKeyboardListeners() {
        if (keyboardListenersAttached) {
            return
        }
        rootLayout = findViewById<View>(R.id.rootLayout) as ViewGroup
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout!!.rootView) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            if (imeVisible){
                onShowKeyboard(imeHeight)
            }else
            {
                onHideKeyboard()
            }
            insets
        }
        //rootLayout!!.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)
        keyboardListenersAttached = true
    }

}