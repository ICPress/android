package net.crowdventures.storypop.behaviour

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.crowdventures.storypop.R


class CustomFABBehaviour :
    com.google.android.material.floatingactionbutton.FloatingActionButton.Behavior{

    constructor():super()
    constructor(context: Context?, attrs: AttributeSet?):super(context, attrs)

  override fun onDependentViewChanged(
      parent: CoordinatorLayout,
      child: FloatingActionButton,
      dependency: View
  ): Boolean {
      val lp = child.layoutParams as CoordinatorLayout.LayoutParams
      val oldAnchor = lp.anchorId
      val baseAnchorView = parent.findViewById<AppBarLayout>(R.id.app_bar_edit)
      lp.anchorId = R.id.app_bar_edit
      val opValue = super.onDependentViewChanged(parent, child, baseAnchorView)
      lp.anchorId = oldAnchor
      child.layoutParams = lp
      return opValue
  }

}