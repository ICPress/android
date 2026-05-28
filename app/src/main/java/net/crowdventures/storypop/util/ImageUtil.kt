package net.crowdventures.storypop.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.widget.ImageView
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.OriginalSize
import coil.size.Scale
import com.google.gson.Gson
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.crowdventures.storypop.BuildConfig
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Config.Companion.MAX_IMAGE_SCALED_HEIGHT
import net.crowdventures.storypop.Config.Companion.MAX_IMAGE_SCALED_WIDTH
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.SpanInfo
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID


class ImageUtil {
    companion object {
        /**
         * This method converts dp unit to equivalent pixels, depending on device density.
         *
         * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
         * @param context Context to get resources and device specific display metrics
         * @return A float value to represent px equivalent to dp depending on device density
         */
        fun convertDpToPixel(dp: Int, context: Context): Int {
            return (dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
        }

        /**
         * This method converts device specific pixels to density independent pixels.
         *
         * @param px A value in px (pixels) unit. Which we need to convert into db
         * @param context Context to get resources and device specific display metrics
         * @return A float value to represent dp equivalent to px value
         */
        fun convertPixelsToDp(px: Int, context: Context): Int {
            return (px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
        }

        fun makeTintedBitmap(src: Bitmap, color: Long): Bitmap? {
            val c = Canvas(src)
            val paint = Paint()
            paint.colorFilter = PorterDuffColorFilter(color.toInt(), PorterDuff.Mode.SRC_IN)
            c.drawBitmap(src, Matrix(), paint);
            return src
        }

        public fun getMinAspectRatio(
            currentWidth: Float,
            currentHeight: Float,
            maxWidth: Float,
            maxHeight: Float
        ): Float {
            val aspect = maxWidth / currentWidth
            return Math.min(
                aspect,
                maxHeight / currentHeight
            )
        }

        fun getSuitableAspectRatio(
            currentWidth: Float,
            currentHeight: Float,
            maxWidth: Float,
            maxHeight: Float
        ): Float {
            if (currentWidth > maxWidth && currentHeight > maxHeight) return getMinAspectRatio(
                currentWidth,
                currentHeight,
                maxWidth,
                maxHeight
            )
            if (currentWidth > maxWidth) {
                return maxWidth / currentWidth
            }
            return maxHeight / currentHeight
        }

        fun BITMAP_RESIZER(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
            val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
            val ratioX = newWidth / bitmap.width.toFloat()
            val ratioY = newHeight / bitmap.height.toFloat()
            val middleX = newWidth / 2.0f
            val middleY = newHeight / 2.0f
            val scaleMatrix = Matrix()
            scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
            val canvas = Canvas(scaledBitmap)
            canvas.setMatrix(scaleMatrix)
            canvas.drawBitmap(
                bitmap,
                middleX - bitmap.width / 2,
                middleY - bitmap.height / 2,
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            return scaledBitmap
        }

        fun getScaledBitmap(drawable: Bitmap, maxWidth: Float, maxHeight: Float): Bitmap {
            val currentWidth = drawable.width.toFloat()
            val currentHeight = drawable.height.toFloat()
            val aspect = getSuitableAspectRatio(currentWidth, currentHeight, maxWidth, maxHeight)
            if (aspect <= 1) {
                val newHeight = drawable.height.toFloat() * aspect
                val newWidth = drawable.width.toFloat() * aspect
                return BITMAP_RESIZER(drawable, newWidth.toInt(), newHeight.toInt())
                    ?: return drawable
//                return Bitmap.createScaledBitmap(
//                    drawable,
//                    newWidth.toInt(),
//                    newHeight.toInt(),
//                    false
//                )
            } else return drawable
        }

        fun requestImageCroppedVersionBuilder(
            imageUri: Uri,
            isTitleBackgroundImage: Boolean,
            setFixAspectRatio: Boolean
        ): CropImage.ActivityBuilder {
            val builder = CropImage.activity(imageUri)
                .setIsTitleBackgroundImage(isTitleBackgroundImage)
                .setGuidelines(CropImageView.Guidelines.ON)//.setAutoZoomEnabled(false)
            if (setFixAspectRatio) {
                builder.setAspectRatio(16, 9)
                    .setFixAspectRatio(true)
            }
            return builder
        }

        fun requestImageCroppedVersion(
            callbackActivity: Activity,
            imageUri: Uri,
            isTitleBackgroundImage: Boolean,
            setFixAspectRatio: Boolean
        ) {
            requestImageCroppedVersionBuilder(imageUri, isTitleBackgroundImage, setFixAspectRatio)
                .start(callbackActivity)
        }

        fun requestImage(callbackActivity: Activity, imageUri: Uri) {
            CropImage.activity(imageUri)
                .setFixAspectRatio(false)
                .setGuidelines(CropImageView.Guidelines.ON) //.setAutoZoomEnabled(false)
                .start(callbackActivity)
        }

        // Returns the File for a photo stored on disk given the fileName
        fun getPhotoFileUri(context: Context): File {
            // Get safe storage directory for photos
            // Use `getExternalFilesDir` on Context to access package-specific directories.
            // This way, we don't need to request external read/write runtime permissions.
            val mediaStorageDir =
                File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), Config.logTag)

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                Log.d(Config.logTag, "failed to create directory")
            }

            // Return the file target for the photo based on filename
            return File(mediaStorageDir.path + File.separator + Config.defaultPhotoFile)
        }

        fun requestImageIntent(context: Context): Intent {
            val getContentIntent = Intent(Intent.ACTION_GET_CONTENT)
            getContentIntent.type = "image/*"
            val allIntents: ArrayList<Intent> = ArrayList()
            val listGallery: List<ResolveInfo> =
                context.packageManager.queryIntentActivities(getContentIntent, 0)
            for (res in listGallery) {
                val intent: Intent = Intent(getContentIntent)
                intent.component =
                    ComponentName(res.activityInfo.packageName, res.activityInfo.name)
                intent.setPackage(res.activityInfo.packageName)
                allIntents.add(intent)
            }
            val galleryIntent: Intent? =
                allIntents.firstOrNull { x -> x.component?.className?.contains("gallery") == true }
            val takePhotoIntent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val ph = getPhotoFileUri(context)
            val mPhotoUri: Uri =
                FileProvider.getUriForFile(context, "net.crowdventures.storypop.Fileprovider", ph)
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri)
            val pickTitle = "Choose a Picture"
            val chooserIntent = Intent.createChooser(getContentIntent, pickTitle)
            chooserIntent.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                if (galleryIntent != null) arrayOf(takePhotoIntent, galleryIntent) else arrayOf(
                    takePhotoIntent
                )
            )
            return chooserIntent
        }

        fun getMiniatureBitmapFromDeviceWidth(
            fullBitmap: Bitmap,
            deviceWidth: Float,
            context: Context
        ): Bitmap {
            val miniatureBitmap: Bitmap
            if (fullBitmap.width > MAX_IMAGE_SCALED_WIDTH || fullBitmap.height > MAX_IMAGE_SCALED_HEIGHT || (deviceWidth > 1f && deviceWidth < MAX_IMAGE_SCALED_WIDTH)) {
                val maxWidth =
                    if (deviceWidth > 1f && deviceWidth < MAX_IMAGE_SCALED_WIDTH) deviceWidth else MAX_IMAGE_SCALED_WIDTH
                val maxHeight =
                    if (fullBitmap.height <= MAX_IMAGE_SCALED_WIDTH) fullBitmap.height.toFloat() else MAX_IMAGE_SCALED_WIDTH
                miniatureBitmap = getScaledBitmap(fullBitmap, maxWidth, maxHeight)
                //DISABLED BELOW-> UGLY 20220513 ->>
                //addExpandImageIcon(miniatureBitmap,context.resources)
            } else miniatureBitmap = fullBitmap
            return miniatureBitmap
        }

        fun getCreateImageRootDir(context: Context): File {
            val root = context.getDir("tmp", Context.MODE_PRIVATE).absolutePath
            val myDir = File("$root/img")
            if (!myDir.exists()) {
                myDir.mkdirs();
            }
            return myDir
        }

        fun getEmptyOrScaledBitmapFromImageMetadata(
            context: Context,
            spanInfo: SpanInfo,
            isPublished: Boolean
        ): Bitmap {
            val imageInfoMetadata =
                Gson().fromJson(spanInfo.additionalInfoFlag, ImageInfoMetadata::class.java)
            val width = imageInfoMetadata.minWidth ?: imageInfoMetadata.width
            val height = imageInfoMetadata.minHeight ?: imageInfoMetadata.height
            var aspect =
                ImageUtil.getMinAspectRatio(
                    width.toFloat(),
                    height.toFloat(),
                    ImageUtil.convertDpToPixel(250, context).toFloat(),
                    ImageUtil.convertDpToPixel(350, context).toFloat()
                )
            if (aspect < 0) aspect = 1f
            val outWidth = (width * aspect).toInt()
            val outHeight = (height * aspect).toInt()
            if (!isPublished && spanInfo.additionalInfoFlag != null) {
                val scaledBitmap = ImageUtil.getLocalImageBitmapFromMetaData(
                    context,
                    false,
                    spanInfo.additionalInfoFlag,
                    false
                )
                if (scaledBitmap != null) return Bitmap.createScaledBitmap(
                    scaledBitmap,
                    outWidth,
                    outHeight,
                    false
                )
            }
            val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.GRAY)
            return bitmap
        }

        fun createImageRequestWithRetryOnce(
            context: Context,
            targetImageView: ImageView,
            imageLoader: ImageLoader,
            requestPath: String
        ): ImageRequest {
            return createImageRequest(context, requestPath)
                .target(targetImageView)
                .listener(
                    ImageRequestListenerRetry(
                        context,
                        targetImageView,
                        imageLoader,
                        requestPath
                    )
                )
                .build()
        }

        fun createImageRequest(
            context: Context,
            requestPath: String
        ): ImageRequest.Builder {
            return ImageRequest.Builder(context).data(requestPath)
                .crossfade(true)
                .size(OriginalSize) // Set the target size to load the image at.
                .scale(Scale.FILL)
        }

        fun getLocalImageBitmapFromMetaData(
            context: Context,
            inMutable: Boolean,
            imageJsonMetadata: String,
            fullSize: Boolean
        ): Bitmap? {
            val imageInfoMetadata = Gson().fromJson(
                imageJsonMetadata,
                ImageInfoMetadata::class.java
            ) ?: return null
            val imageName = if (!fullSize && imageInfoMetadata.minWidth != null)
                Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name else imageInfoMetadata.name
            val imagePath = getCreateImageRootDir(context).absolutePath + "/" + imageName
            val imageFile = File(imagePath)
            if (!imageFile.exists()) return null
            val imageUri = Uri.fromFile(imageFile)
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bitmapOptions = BitmapFactory.Options()
            bitmapOptions.inMutable = inMutable
            bitmapOptions.inScaled = false
            if (inputStream != null)
                return  inputStream.use { stream-> BitmapFactory.decodeStream(stream, null, bitmapOptions)}
            return null
        }

        fun loadMinBitmapImageViewRemote(
            imageJsonMetadata: String,
            context: Context,
            imageView: ImageView,
            loggedInUser: AccountInfoFull
        ) {
            val imageInfoMetadata =
                Gson().fromJson(imageJsonMetadata, ImageInfoMetadata::class.java) ?: return
            //test with low resolution currently
            val imageName =
                if (imageInfoMetadata.minWidth != null && imageInfoMetadata.minHeight != null)
                    Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name else imageInfoMetadata.name
            val imageRequestPath = if (imageInfoMetadata.minWidth != null) loggedInUser.cdnSmallRequestPath
            else loggedInUser.cdnLargeRequestPath
            val imageLoader = Config.getOrSetImageLoader(context)
            val imageRequest = createImageRequestWithRetryOnce(
                context,
                imageView, imageLoader, imageRequestPath + imageName
            )
            imageLoader.enqueue(imageRequest)
        }

        fun getImageRequestFromMetadata(context: Context, imageJsonMetadata: String,loggedInUser: AccountInfoFull): ImageRequest {
            val imageInfoMetadata = Gson().fromJson(
                imageJsonMetadata,
                ImageInfoMetadata::class.java
            )
            val imageName = if (imageInfoMetadata.minWidth != null)
                Config.MINIATURE_BITMAP_PREFIX + imageInfoMetadata.name else imageInfoMetadata.name
            val imageRequestPath = if (imageInfoMetadata.minWidth != null) loggedInUser.cdnSmallRequestPath
            else loggedInUser.cdnLargeRequestPath
            val reqBuilder = ImageUtil.createImageRequest(context, imageRequestPath + imageName)
            val imgRequest = reqBuilder.build()
            return imgRequest
        }

        fun checkImageExists(imageMetadata: ImageInfoMetadata, context: Context): Boolean {
            val myDir = getCreateImageRootDir(context)
            val ordinaryImgFile = File(myDir.absoluteFile, imageMetadata.name)
            return ordinaryImgFile.exists()
        }

        fun compressAndReturnBitmapUri(
            scaledBitmap: Bitmap,
            context: Context,
            originalFilename: String?
        ): Uri? {
            val compressFormat: Bitmap.CompressFormat
            val fileType: String = "jpg"
            compressFormat = Bitmap.CompressFormat.JPEG
            val uniqueFilename =
                if (originalFilename != null) "${Config.MINIATURE_BITMAP_PREFIX}${originalFilename}" else UUID.randomUUID()
                    .toString().replace("-", "") + ".$fileType"
            var mFileTemp: File? = null
            val myDir = getCreateImageRootDir(context)
            try {
                mFileTemp = File(
                    myDir.absoluteFile,
                    uniqueFilename
                );
            } catch (e1: IOException) {
                //show blank image,failure
                Log.e(Config.logTag, "Exception ocurred when creating a file " + e1.message, e1)
                return null
            }
            val fout = FileOutputStream(mFileTemp)
            val compressSuccess: Boolean = scaledBitmap.compress(compressFormat, 90, fout)
            if (compressSuccess) return Uri.fromFile(mFileTemp!!)
            else {
                Log.e(Config.logTag, "compressFailed for scaled bitmap")
                return null
            }
        }

        @Composable
        fun getImageForResult(
            storyViewModel: StorySavedViewModel,
            profileIcon: Boolean,
            context: Context,
            useBigMiniatureBitmap: Boolean,
            fixedAspect: Boolean,
            callback: SuccessCallback<ImageInfoMetadata>
        ): ManagedActivityResultLauncher<Intent, ActivityResult> {
            val requestImageCroppedForResult =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data = result.data ?: return@rememberLauncherForActivityResult
                        val cropImageActivityResult = CropImage.getActivityResult(data)
                        if (cropImageActivityResult?.uri != null) {
                            storyViewModel.viewModelScope.launch(Dispatchers.IO + StoryUtil.coroutineExceptionHandler) {
                                val inputStream: InputStream =
                                    context.contentResolver.openInputStream(cropImageActivityResult.uri)
                                        ?: return@launch
                                val bitmapOptions = BitmapFactory.Options()
                                bitmapOptions.inMutable = true
                                bitmapOptions.inScaled = false
                                val bitmap = inputStream.use { stream ->  BitmapFactory.decodeStream(stream, null, bitmapOptions) }
                                        ?: return@launch

                                if (!useBigMiniatureBitmap) {
                                    val compressedUri = compressAndReturnBitmapUri(
                                        bitmap,
                                        context,
                                        null
                                    )

                                    if (compressedUri != null) {
                                        val orgFile = File(compressedUri.toString())
                                        val miniatureBitmap: Bitmap =
                                            getMiniatureBitmapFromDeviceWidth(
                                                bitmap,
                                                MAX_IMAGE_SCALED_WIDTH, context
                                            )
                                        if (miniatureBitmap.width < bitmap.width || miniatureBitmap.height < bitmap.height) {
                                            val scaledUri = compressAndReturnBitmapUri(
                                                miniatureBitmap,
                                                context, orgFile.name
                                            )
                                            if (scaledUri != null) callback.onSuccess(
                                                ImageInfoMetadata(
                                                    orgFile.name,
                                                    bitmap.width,
                                                    bitmap.height,
                                                    miniatureBitmap.width,
                                                    miniatureBitmap.height
                                                )
                                            )
                                            else callback.onFailure("Did not receive miniature compressed uri!")
                                        } else callback.onSuccess(
                                            ImageInfoMetadata(
                                                orgFile.name,
                                                bitmap.width,
                                                bitmap.height,
                                                null,
                                                null
                                            )
                                        )
                                    } else {
                                        callback.onFailure("Did not receive compressed uri!")
                                    }
                                } else {
                                    val bigMiniatureBitmap: Bitmap = ImageUtil.getScaledBitmap(
                                        bitmap,
                                        Config.MAX_IMAGE_FULL_WIDTH,
                                        Config.MAX_IMAGE_FULL_HEIGHT
                                    )
                                    val compressedUri = ImageUtil.compressAndReturnBitmapUri(
                                        bigMiniatureBitmap,
                                        context,
                                        null
                                    )
                                    if (compressedUri != null) {
                                        val orgFile = File(compressedUri.toString())
                                        callback.onSuccess(
                                            ImageInfoMetadata(
                                                orgFile.name,
                                                bitmap.width,
                                                bitmap.height,
                                                bigMiniatureBitmap.width,
                                                bigMiniatureBitmap.height
                                            )
                                        )
                                    }else callback.onFailure("Did not receive miniature compressed uri!")
                                }
                            }
                        } else {
                            callback.onFailure("Result was not OK")
                        }
                    } else {
                        callback.onFailure("Result was not OK")
                    }
                }
            return rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val ph = getPhotoFileUri(context)
                    val mPhotoUri: Uri? = if (ph.exists())
                        FileProvider.getUriForFile(
                            context,
                            "net.crowdventures.storypop.Fileprovider",
                            ph
                        ) else null
                    val imageUri = if (result.data == null) mPhotoUri else result.data?.data
                    if (imageUri == null) return@rememberLauncherForActivityResult
                    val intent = if (profileIcon) CropImage.activity(imageUri)
                        .setAspectRatio(1, 1)
                        .setCropShape(CropImageView.CropShape.OVAL)
                        .setFixAspectRatio(true)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .getIntent(context) // .setAutoZoomEnabled(false)
                    else requestImageCroppedVersionBuilder(imageUri, fixedAspect, fixedAspect).getIntent(context)
                    requestImageCroppedForResult.launch(intent)
                } else {
                    callback.onFailure("Result was not OK")
                }
            }
        }

    }
}