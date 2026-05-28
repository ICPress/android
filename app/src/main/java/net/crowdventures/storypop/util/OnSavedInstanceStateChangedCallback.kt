package net.crowdventures.storypop.util

import net.crowdventures.storypop.viewmodels.EnabledStyle
import net.crowdventures.storypop.viewmodels.ParcellableKeyValuePair

interface OnSavedInstanceStateChangedCallback {
    fun generateSpanIndexKeyValuePair(enabledStyles: List<EnabledStyle>): ParcellableKeyValuePair


}