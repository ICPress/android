package net.crowdventures.storypop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import coil.request.ImageRequest
import com.google.gson.Gson
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.SpanInfo


class ImageClickSpan(val context:Context, val spanInfo:SpanInfo, var hitRect: Rect?=null, val isPublished:Boolean,
private val clickableImageSpanTextView: TextView, val loggedInUser:AccountInfoFull):
    ImageSpan(context,ImageUtil.getEmptyOrScaledBitmapFromImageMetadata(context,spanInfo,isPublished)) {

    var loadedDrawable :Drawable? = null

    private val loadCallback= object :coil.target.Target{
        override fun onSuccess(result: Drawable) {
            loadedDrawable = result
            loadedDrawable?.setBounds(0, 0, result.intrinsicWidth, result.intrinsicHeight)
            //hack to invalidate editText, invalidate() only works for textView
            if (clickableImageSpanTextView is EditText) clickableImageSpanTextView.text =clickableImageSpanTextView.text
            else clickableImageSpanTextView.invalidate()
        }
    }
    fun loadRemoteDrawable(){
        val imageInfoMetadata  = Gson().fromJson(spanInfo.additionalInfoFlag,ImageInfoMetadata::class.java) ?: return
        val imageName = if (imageInfoMetadata.minWidth != null && imageInfoMetadata.minHeight != null)
            Config.MINIATURE_BITMAP_PREFIX+ imageInfoMetadata.name else imageInfoMetadata.name
        val imageRequestPath = if (imageInfoMetadata.minWidth != null) loggedInUser.cdnSmallRequestPath
        else loggedInUser.cdnLargeRequestPath
        val width = imageInfoMetadata.minWidth?: imageInfoMetadata.width
        val height = imageInfoMetadata.minHeight?: imageInfoMetadata.height
        var aspect =
            ImageUtil.getMinAspectRatio(width.toFloat(), height.toFloat(),ImageUtil.convertDpToPixel(250,context).toFloat(), ImageUtil.convertDpToPixel(350,context).toFloat())
        if (aspect < 0) aspect = 1f
        val imageRequest =  ImageRequest.Builder(context).data(imageRequestPath + imageName)
            .crossfade(false)
            .size((width*aspect).toInt(),(height*aspect).toInt()) // Set the target size to load the image at.
            .target(loadCallback)
  //          .allowHardware(false)
            .build()
        val imageLoader = Config.getOrSetImageLoader(context)
        imageLoader.enqueue(imageRequest)
    }
    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        Log.v(Config.logTag,"Redrawing imageSpan with resource metadata:"+spanInfo.additionalInfoFlag)
        //loadedDrawable = drawable
        if (loadedDrawable==null) super.draw(canvas, text, start, end, x, top, y, bottom, paint)
        else drawDrawable(canvas,loadedDrawable?:return,start,end,x,top,y,bottom,paint)
        if (!isPublished || loadedDrawable != null) {
            val right = super.getSize(Paint(0), "", 0, Config.IMAGE_START_SEPARATOR.length, null)
            this.hitRect = Rect(x.toInt(), top, x.toInt() + right, bottom)
        }else  loadRemoteDrawable()
    }

    private fun drawDrawable(canvas: Canvas,b:Drawable, start: Int,
                             end: Int,
                             x: Float,
                             top: Int,
                             y: Int,
                             bottom: Int,
                             paint: Paint
    ){
        canvas.save()
        var transY: Int = bottom - b.bounds.bottom
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.fontMetricsInt.descent
        } else if (mVerticalAlignment === ALIGN_CENTER) {
            transY = top + (bottom - top) / 2 - b.bounds.height() / 2
        }
        canvas.translate(x, transY.toFloat())
        b.draw(canvas)
        canvas.restore()
    }
}