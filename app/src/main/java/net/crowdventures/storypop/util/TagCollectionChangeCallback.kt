package net.crowdventures.storypop.util

interface TagCollectionChangeCallback {
    fun onTagAdded(tag:String)
    fun onTagRemoved(tag: String)
}