package net.crowdventures.storypop

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.util.ImageUtil

class ClickableImageSpanTextView: androidx.appcompat.widget.AppCompatTextView ,
    ImageLoader<ImageClickSpan> {
    var imageSpans :List<ImageClickSpan> = listOf()
    var loggedInUser:AccountInfoFull? = null
    constructor(context: Context) : super(context) {

    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {

    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val consumed = super.onTouchEvent(event)
        if (event != null && event.action == MotionEvent.ACTION_UP) {
            val clickedImages = imageSpans.filter {
                it.hitRect != null && it.hitRect?.contains(
                    event.x.toInt(),
                    event.y.toInt()
                ) == true
            }
            StfalconImageViewer.Builder<ImageClickSpan>(context,
                clickedImages, this ).show()
            return true
        }
        return consumed
    }

    override fun loadImage(imageView: ImageView?, image: ImageClickSpan?) {
        val currentLoggedInUser = loggedInUser
        if (image == null || currentLoggedInUser == null) return
        if (image.isPublished){
            if (image.spanInfo.additionalInfoFlag != null && imageView != null)
                ImageUtil.loadMinBitmapImageViewRemote(image.spanInfo.additionalInfoFlag, context, imageView, currentLoggedInUser)
        }else {
            val contentExpandImage = ImageUtil.getLocalImageBitmapFromMetaData(context,false,image.spanInfo.additionalInfoFlag?:return,true)
            if (imageView == null || contentExpandImage == null) return
            imageView.setImageBitmap(contentExpandImage)
        }
    }
}