package net.crowdventures.storypop.util

import net.crowdventures.storypop.viewmodels.LinkType

interface LinkClickCallback {
    fun onClick(uriContent:String,linkType: LinkType)
}