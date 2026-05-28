package net.crowdventures.storypop.util

interface SuccessCallback<T> {
    fun onSuccess(vararg param: T)
    fun onFailure(reason:Any?)
}