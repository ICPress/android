package net.crowdventures.storypop.util

import android.content.Context
import android.util.Log
import android.widget.ImageView
import androidx.annotation.MainThread
import coil.ImageLoader
import coil.network.HttpException
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.OriginalSize
import coil.size.Scale
import net.crowdventures.storypop.Config

class ImageRequestListenerRetry(private val context: Context,
                                private val targetImageView: ImageView,
                                private val imageLoader:ImageLoader,
                                private val requestPath:String) : ImageRequest.Listener {
        private var performedRetry = false
        override fun onStart(request: ImageRequest) {
            Log.v(
                Config.logTag,
                "Started image request from coil for: "+request.data
            )
        }

        /**
         * Called if the request is cancelled.
         */
        @MainThread
        override fun onCancel(request: ImageRequest) {
            Log.v(
                Config.logTag,
                "Cancelled image request from coil for: "+request.data
            )
        }


        /**
         * Called if an error occurs while executing the request.
         */
        @MainThread
        override fun onError(request: ImageRequest, result: ErrorResult) {
            Log.e(
                Config.logTag,
                "Error image request from coil for: "+request.data,result.throwable
            )
            if (result.throwable is HttpException && !performedRetry) { // && throwable.response.code() == 404
                performedRetry = true
                val newRequest =  ImageRequest.Builder(context).data(requestPath)
                    .crossfade(true)
                    .size(OriginalSize) // Set the target size to load the image at.
                    .scale(Scale.FILL)
                    .target(targetImageView)
                    .diskCachePolicy(CachePolicy.WRITE_ONLY)
                    .memoryCachePolicy(CachePolicy.WRITE_ONLY).build()
                imageLoader.enqueue(newRequest)
            }
        }

        /**
         * Called if the request completes successfully.
         */
        @MainThread
        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
            Log.v(
                Config.logTag,
                "Success image request from coil for: "+request.data+ ", metadata (source):"+result.dataSource.name
            )
        }

}