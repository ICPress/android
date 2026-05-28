package net.crowdventures.storypop

import android.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.text.ParcelableSpan
import android.util.AttributeSet
import android.view.DragEvent
import android.widget.Toast
import net.crowdventures.storypop.viewmodels.EnabledStyle
import net.crowdventures.storypop.viewmodels.ParcellableKeyValuePair
import net.crowdventures.storypop.viewmodels.SpanInfo


class MonitoringEditText : androidx.appcompat.widget.AppCompatEditText {
    private var copyParcellableKeyValuePair: ParcellableKeyValuePair? = null
    private var copyStateCharSequence: CharSequence? = null
    private var copyStateEnabledStyle = listOf<EnabledStyle>()
    private lateinit var context1: Context
    var textStyleManager: TextStyleManager? = null
    var editActivity: ArticleContentEditActivity? = null

    /*
        Just the constructors to create a new EditText...
     */
    constructor(context: Context) : super(context) {
        this.context1 = context
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        this.context1 = context
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        this.context1 = context
    }

    override fun onDragEvent(event: DragEvent?): Boolean {
        return false
    }
//    override fun performLongClick(): Boolean {
////        val clipboard = context1.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
////        val primaryClipData: ClipData? = clipboard.primaryClip
////        if (primaryClipData != null && primaryClipData.itemCount > 0){
////            val item = primaryClipData.getItemAt(0)
////            val clip: ClipData = ClipData.newPlainText("STORY_POP_COPIED_TEXT", "TEST:"+ item.text.toString())
////            clipboard.setPrimaryClip(clip)
////        }
//        val handled = super.performLongClick()
//        return handled
//    }
    override fun onTextContextMenuItem(id: Int): Boolean {
        val clipboard = context1.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        // Do your thing:
        //val consumed = super.onTextContextMenuItem(id)
        if (id == R.id.cut || id == R.id.copy) {
            val spanList = editableText.getSpans(
                selectionStart,
                selectionEnd,
                Any::class.java
            )?.filterIsInstance<ParcelableSpan>() ?: return true
            var enabledStyles =
                textStyleManager?.enabledStyles?.filter { spanList.contains(it.span) }?.toMutableList()
            val imagesToAdd = textStyleManager?.getEnabledContainedStylesSelection(selectionStart, selectionEnd, TextStyle.IMAGE)
            if (enabledStyles?.isNotEmpty() == true || imagesToAdd?.isNotEmpty() == true) {
                if (enabledStyles == null) enabledStyles = mutableListOf()
                if (imagesToAdd?.isNotEmpty() == true) enabledStyles.addAll(imagesToAdd)
                copyStateCharSequence = editableText.subSequence(selectionStart, selectionEnd)
                copyStateEnabledStyle = enabledStyles.map {
                    EnabledStyle(
                        SpanInfo(
                             Math.max(it.spanInfo.start,selectionStart)  - selectionStart,
                            Math.min( it.spanInfo.end, selectionEnd) - selectionStart , it.spanInfo.style,
                            it.spanInfo.additionalInfoFlag
                        ), false
                    )
                }.toMutableList() //map to new objects/references
                if ((spanList.isNotEmpty() == true || imagesToAdd?.isNotEmpty() == true) && copyStateCharSequence != null) {
                    copyParcellableKeyValuePair =
                        editActivity?.storyViewModel?.getParcellableKeyValuePairForSpans(
                            spanList,
                            enabledStyles.toTypedArray()
                        )
                }
            }
            return super.onTextContextMenuItem(id)
        } else if (id == R.id.paste) {
            val primaryClipData: ClipData? = clipboard.primaryClip
            if (primaryClipData != null) {
                val item = primaryClipData.getItemAt(0)
                // Get Intent from Clipboard.
                val intentPaste = item.text.toString()
                if (intentPaste ==  copyStateCharSequence.toString()) {
                    var activeSpanStylesAtLocationBeforePaste = textStyleManager?.
                    getEnabledContainedStylesSelection(selectionStart,selectionEnd)
                    if (selectionStart ==  selectionEnd)  //dont remove existing images if pasted directly after each other
                        activeSpanStylesAtLocationBeforePaste = activeSpanStylesAtLocationBeforePaste?.filter { it.spanInfo.style != TextStyle.IMAGE }
                    activeSpanStylesAtLocationBeforePaste?.forEach {
                        textStyleManager?.removeEnabledStyle(it)
                    }
                    val handled = super.onTextContextMenuItem(id)
                    val afterPasteStartPosition = selectionStart- item.text.length
                    val spanList = editableText.getSpans(
                        afterPasteStartPosition,
                        selectionEnd,
                        Any::class.java
                    ).filterIsInstance<ParcelableSpan>()
                    val currentEnabledStyles = textStyleManager?.enabledStyles
                    if (currentEnabledStyles != null && copyStateEnabledStyle.isNotEmpty() &&
                        copyParcellableKeyValuePair != null
                    ) {
                       val newMappedStyles =  copyStateEnabledStyle
                           .map {
                           EnabledStyle(
                               SpanInfo(
                                   it.spanInfo.start + afterPasteStartPosition,
                                   it.spanInfo.end + afterPasteStartPosition, it.spanInfo.style,
                                   it.spanInfo.additionalInfoFlag
                               ), false
                           )
                        }
                        editActivity?.mapParcellableKeyValuePairToStyles(
                            copyParcellableKeyValuePair!!,
                            spanList, newMappedStyles)
                        currentEnabledStyles.addAll(newMappedStyles)
                        editActivity?.storyViewModel?.setEnabledStyles(currentEnabledStyles.toMutableList())
                    }
                    return handled
                }else{
                    editableText.insert(selectionStart,intentPaste)
                    return true
                }
            }
        } else if (id == 16908838){
            Toast.makeText(context1,"Cannot paste from clipboard.", Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onTextContextMenuItem(id)
    }

}