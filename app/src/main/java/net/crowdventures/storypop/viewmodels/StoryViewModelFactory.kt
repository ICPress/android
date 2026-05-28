package net.crowdventures.storypop.viewmodels

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner

class StoryViewModelFactory(owner: SavedStateRegistryOwner, val storySavedModel: StorySavedModel?, val mappedEnabledStyle: Array<EnabledStyle>,
                            defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = StoryViewModel(handle, storySavedModel, mappedEnabledStyle) as T


}
