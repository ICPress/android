package net.crowdventures.storypop

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.text.Editable
import android.text.Layout
import android.text.ParcelableSpan
import android.text.Selection
import android.text.SpanWatcher
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.QuoteSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.content.ContextCompat.getColor
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config.Companion.IMAGE_START_SEPARATOR
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.viewmodels.EnabledStyle
import net.crowdventures.storypop.viewmodels.ImageInfoMetadata
import net.crowdventures.storypop.viewmodels.LinkType
import net.crowdventures.storypop.viewmodels.SpanInfo
import net.crowdventures.storypop.viewmodels.StoryViewModel
import java.io.File


class TextStyleManager(
    val editTextAttached: EditText,
    val imageButtonOnClickListener: View.OnClickListener,
    val storyViewModel: StoryViewModel,
    val activity: ArticleContentEditActivity
) {
    private var lastModifiedItem: EnabledStyle? = null

    init {
        storyViewModel.enabledStyles.observe(activity) { x ->
            val lastEnabledStyleItem = x.lastOrNull()
            if (x != null && lastModifiedItem !== lastEnabledStyleItem) {
                lastModifiedItem = lastEnabledStyleItem
                enabledStyles.clear()
                enabledStyles.addAll(x)
                handler.post { setTextWatcher() }
            }
        }
    }

    fun disableTextWatcher() {
        editTextAttached.removeTextChangedListener(textWatcher)
        watcherSet = false
    }

    fun addEnabledStyles(enabledStylesNew: List<EnabledStyle>) {
        enabledStyles.addAll(enabledStylesNew)
        storyViewModel.setEnabledStyles(enabledStyles.toMutableList())
    }
    private fun addEnabledStyle(enabledStyle: EnabledStyle) {
        enabledStyles.add(enabledStyle)
        storyViewModel.setEnabledStyles(enabledStyles.toMutableList())
    }

    val enabledStyles: java.util.concurrent.ConcurrentLinkedQueue<EnabledStyle> =
        java.util.concurrent.ConcurrentLinkedQueue()

    fun getEnabledStylesSelection(start: Int, end: Int, filterContinueSpan:Boolean = true): List<EnabledStyle> {
        return enabledStyles.filter { x -> ((!filterContinueSpan || x.continueSpan) && start >= x.spanInfo.start -1 && start <= x.spanInfo.end ) ||  (start >= x.spanInfo.start && end <= x.spanInfo.end || x.spanInfo.start in start .. end || x.spanInfo.end in start .. end  ) }
    }

    fun getEnabledContainedStylesSelection(start: Int, end: Int): List<EnabledStyle> {
        return enabledStyles.filter { x -> x.spanInfo.start >= start && x.spanInfo.end <= end }
    }

    fun getEnabledContainedStylesSelection(
        start: Int,
        end: Int,
        style: TextStyle
    ): List<EnabledStyle> {
        return enabledStyles.filter { x -> x.spanInfo.start >= start && x.spanInfo.end <= end && x.spanInfo.style == style }
    }

    fun styleIsRowSpan(it:EnabledStyle):Boolean{
        return it.spanInfo.style == TextStyle.BULLET_LIST || it.spanInfo.style == TextStyle.QUOTE ||
                it.spanInfo.style == TextStyle.TEXT_ALIGNMENT
    }
    @Volatile private var watcherSet = false
    private var handler: android.os.Handler = android.os.Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + NonCancellable) //IO dispatcher scope for intensive operations
    private var oldEditTextSize = 0
    var styleButtons: Array<ImageButton> = arrayOf()
    @Volatile private var existingJobs : Job? = null

    private suspend fun adjustSpans(editTextDiff:Int, start: Int, before:Int) = withContext(Dispatchers.IO + NonCancellable) {
        //below to clear styles when deleting a longer marked text, concurrent execution issue
        //start-1 for textalignment and row-styles
        if (editTextDiff > 0) {
            getEnabledContainedStylesSelection(
                start,
                start + editTextDiff
            ).forEach {
                if (it.spanInfo.style != TextStyle.IMAGE || (it.spanInfo.style == TextStyle.IMAGE && it.spanInfo.end == start))  //20220315 senad added if for image
                    withContext(Dispatchers.Main) { removeEnabledStyle(it) }
            }
            //adjust spans ahead when letters are deleted
            enabledStyles.filter { it.spanInfo.end >= start + editTextDiff && it.spanInfo.start < start }
                .forEach { it.spanInfo.end -= editTextDiff } //inside span
            enabledStyles.filter { it.spanInfo.end >= start + editTextDiff && it.spanInfo.start >= start && it.spanInfo.start <= start + editTextDiff }
                .forEach {
                    it.spanInfo.start = start; it.spanInfo.end -= editTextDiff
                } //inside span and outside
            enabledStyles.filter { it.spanInfo.end >= start && it.spanInfo.start > start + editTextDiff }
                .forEach {
                    it.spanInfo.start -= editTextDiff; it.spanInfo.end -= editTextDiff
                } //removing characters before span
            val editTextLength = editTextAttached.text.length
            //remove any trailing styles outside text length
            enabledStyles.filter { it.spanInfo.start >= editTextLength }.forEach {
                withContext(Dispatchers.Main) { removeEnabledStyle(it) }
            }
        }
        enabledStyles.filter {
            it.spanInfo.style != TextStyle.IMAGE && //senad 20220313 lade till special för bilder som har tidig start utan innebörd så att dessa inte raderas
                    (it.spanInfo.start == start || it.spanInfo.start == start - 1 ||
                            it.spanInfo.start >= editTextAttached.text.length)
        }.forEach { withContext(Dispatchers.Main) { removeEnabledStyle(it) } }
        //extend reach of any rowstyle span
        val newLineInfo = editTextAttached.text.indexOf('\n', start)
        val nextEndingLine =
            if (newLineInfo == -1) editTextAttached.text.length - 1 else newLineInfo
        enabledStyles.filter {
            it.spanInfo.end >= start && it.spanInfo.start < start && (styleIsRowSpan(it))
        }.forEach { it.spanInfo.end = nextEndingLine +1 }

        oldEditTextSize = editTextAttached.text.length
        if (before >= start) {
            withContext(Dispatchers.Main) { highlightButtonEnabledStyles(listOf()) }
        } //refresh soft input extra buttons
    }
    private val textWatcher = object : TextWatcher,ITextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            oldEditTextSize = editTextAttached.text.length
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (before == 0 && count == editTextAttached.text.length) return //restore of state
            if (count == 0 && before > 0) {
                val editTextDiff = oldEditTextSize - editTextAttached.text.length
                if (existingJobs == null  || existingJobs?.isActive ==false) {
                    existingJobs = scope.launch {adjustSpans(editTextDiff,start,before)}
                }else{
                    runBlocking {
                        existingJobs!!.join()
                        existingJobs = scope.launch {adjustSpans(editTextDiff,start,before)}
                    }
                }
                return
            }
            var spannableWithin =
                getEnabledStylesSelection(start, start + count - 1).filter { it.spanInfo.style != TextStyle.IMAGE } // senad 20220314 added it.spanInfo.style != TextStyle.IMAGE //.filter { it.continueSpan }
            if (s!= null && s.length > start && s[start] == '\n') { // count == 1 && before == 0 &&
                //terminate line spans on new row
                val lineSpans = spannableWithin.filter { styleIsRowSpan(it) }
                lineSpans.forEach { removeEnabledStyle(it) }
                spannableWithin =
                    spannableWithin.filter { it.spanInfo.end != start || !lineSpans.contains(it) } //
                lineSpans.filter { it.spanInfo.end == start }.forEach {
                    enableDisableSpan(
                        it.spanInfo.start,
                        it.spanInfo.end,
                        it.spanInfo.style,
                        false,
                        it.span
                    )
                }
            }
            val diff = count - before
            if (before == 0) {
                spannableWithin = getEnabledStylesSelection(
                    start,
                    start
                ).filter {
                    it.spanInfo.style != TextStyle.IMAGE
                            && (!styleIsRowSpan(it) || s?.indexOf('\n', start) != start)
                } // senad 20220314 added it.spanInfo.style != TextStyle.IMAGE // get spannable at current position, as no prev position exists
            }else{ //make sure old span limits are taken into account before padding/updating span start/end
                val oldSpans = getEnabledStylesSelection(
                    start, start+before)
                spannableWithin = spannableWithin + oldSpans.filter { !spannableWithin.contains(it) }
            }
            if (diff > 0 || count > 0) { //check so that we are not blocking enabledStyles
                //pad current and upcoming spans
                padSpans(spannableWithin, diff, start)

                val selectionEnd = start + before
                enabledStyles.filter { x -> x.spanInfo.start == start && x.spanInfo.end == selectionEnd && x.spanInfo.style != TextStyle.IMAGE }
                    .forEach { x ->
                        x.spanInfo.end = start + count
                    } //update existing span when text is replaced
            }

        }

        private fun padSpans(spannableWithin: List<EnabledStyle>, count: Int, start: Int) {
            spannableWithin.filter { x->  x.spanInfo.end > start || x.continueSpan }.forEach { it.spanInfo.end += count }
            val spannableAhead = enabledStyles.filter { it.spanInfo.start > start }
            spannableAhead.forEach { it.spanInfo.start += count; it.spanInfo.end += count }
        }

        override fun afterTextChanged(s: Editable?) {
        }
    }

    private fun highlightButtonEnabledStyles(enabledStyles: List<EnabledStyle>) {
        val enabledTextStyles = enabledStyles.map { x -> x.spanInfo.style }
        for (it: ImageButton in styleButtons) {
            when (it.id) {
                R.id.bold_text_btn -> if (enabledTextStyles.contains(TextStyle.BOLD)) enableButton(
                    it
                ) else disableButton(it)
                R.id.italic_text_btn -> if (enabledTextStyles.contains(TextStyle.ITALIC)) enableButton(
                    it
                ) else disableButton(it)
                R.id.underline_text_btn -> if (enabledTextStyles.contains(TextStyle.UNDERLINE)) enableButton(
                    it
                ) else disableButton(it)
                R.id.text_size_increase_btn -> if (enabledTextStyles.contains(TextStyle.TEXT_SIZE_LARGE)) enableButton(
                    it
                ) else disableButton(it)
                R.id.refer_link_text_btn -> if (enabledTextStyles.contains(TextStyle.REFER_LINK)) enableButton(
                    it
                ) else disableButton(it)
                R.id.image_text_btn -> if (enabledTextStyles.contains(TextStyle.IMAGE)) disableButton(
                    it
                ) else disableButton(it)
            }
        }
    }

    private val spanWatchAction = object : SpanWatcher {
        var lastSelectionStart = 0
        var lastSelectionEnd = 0
        override fun onSpanAdded(text: Spannable?, what: Any?, start: Int, end: Int) {
        }

        override fun onSpanRemoved(text: Spannable?, what: Any?, start: Int, end: Int) {
        }

        override fun onSpanChanged(
            text: Spannable?,
            what: Any?,
            ostart: Int,
            oend: Int,
            nstart: Int,
            nend: Int
        ) {
            if (what == Selection.SELECTION_START) {
                //selection start changed from ostart to nstart
                lastSelectionStart = nstart;
            } else if (what == Selection.SELECTION_END) {
                //selection end changed from ostart to nstart
                lastSelectionEnd = nend
            }
            if (lastSelectionStart == lastSelectionEnd) {
                lastSelectionEnd = editTextAttached.selectionEnd
                lastSelectionStart = lastSelectionEnd
            }
            val foundEnablesStyles: List<EnabledStyle> = getEnabledStylesSelection(
                lastSelectionStart,
                lastSelectionEnd
            )
            highlightButtonEnabledStyles(foundEnablesStyles)

        }
    }

    fun removeEnabledStyle(enabledStyle: EnabledStyle) {
        editTextAttached.text.removeSpan(enabledStyle.span)
        enabledStyles.remove(enabledStyle)
        lastModifiedItem = enabledStyle
        storyViewModel.setEnabledStyles(enabledStyles.toMutableList())
    }

    private fun updateTextAlignmentSpan(
        textStyle: TextStyle,
        alignment: Layout.Alignment
    ) {
        val flag = Spannable.SPAN_INCLUSIVE_INCLUSIVE
        val span = AlignmentSpan.Standard(alignment)
        val textSelection = editTextAttached.text
        var firstNewline =
            textSelection.lastIndexOf('\n', Math.max(editTextAttached.selectionStart - 1, 1))
        var lastNewLine = textSelection.indexOf('\n', editTextAttached.selectionEnd)
        if (firstNewline == -1) firstNewline = 0
        if (lastNewLine == -1) lastNewLine =
            Math.max(textSelection.length - 1, editTextAttached.selectionEnd)
        editTextAttached.text.setSpan(span, firstNewline, lastNewLine, flag)
        addEnabledStyle(
            EnabledStyle(
                SpanInfo(
                    firstNewline,
                    lastNewLine,
                    textStyle,
                    alignment.ordinal.toString()
                ), true, span
            )
        )

    }

    private fun enableDisableSpan(
        start: Int,
        end: Int,
        textStyle: TextStyle,
        continueSpan: Boolean = true,
        oldSpan: ParcelableSpan? = null
    ) {
        var flag = Spannable.SPAN_INCLUSIVE_INCLUSIVE
        var finalEnd = end
        var finalStart = start
        if (!continueSpan) flag = Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        when (textStyle) {
            TextStyle.BOLD -> {
                if (start != end)
                    editTextAttached.text.replace(
                        end,
                        end,
                        Config.STYLE_TERMINATOR_OPERATOR //null/termination char
                    )
                if (continueSpan)  {
                    editTextAttached.text.replace(
                        start,
                        start,
                        Config.STYLE_TERMINATOR_OPERATOR //null/termination char
                    )
                    finalEnd = end + 1
                }
                val span = StyleSpan(Typeface.BOLD)
                editTextAttached.text.setSpan(span, start, finalEnd, flag)
                addEnabledStyle(
                    EnabledStyle(
                        SpanInfo(start, finalEnd, textStyle),
                        continueSpan,
                        span
                    )
                )
            }
            TextStyle.ITALIC -> {
                if (start != end)
                    editTextAttached.text.replace(
                        end,
                        end,
                        Config.STYLE_TERMINATOR_OPERATOR //null/termination char
                    )
                if (continueSpan)  {
                    editTextAttached.text.replace(
                        start,
                        start,
                        Config.STYLE_TERMINATOR_OPERATOR //null/termination char
                    )
                    finalEnd = end + 1
                }
                val span = StyleSpan(Typeface.ITALIC)
                editTextAttached.text.setSpan(span, start, finalEnd, flag)
                addEnabledStyle(
                    EnabledStyle(
                        SpanInfo(start, finalEnd, textStyle),
                        continueSpan,
                        span
                    )
                )
            }
            TextStyle.UNDERLINE -> {
                if (start != end)
                    editTextAttached.text.replace(
                        end,
                        end,
                        Config.STYLE_TERMINATOR_OPERATOR //null/termination char
                    )
                if (continueSpan)  {
                    editTextAttached.text.replace(
                        start,
                        start,
                        Config.STYLE_TERMINATOR_OPERATOR //null/termination char
                    )
                    finalEnd = end + 1
                }
                val span = UnderlineSpan()
                editTextAttached.text.setSpan(span, start, finalEnd, flag)
                addEnabledStyle(
                    EnabledStyle(
                        SpanInfo(start, finalEnd, textStyle),
                        continueSpan,
                        span
                    )
                )
            }
            TextStyle.TEXT_ALIGNMENT -> {
                if (continueSpan) {
                    val firstAndLastNewline = getFirstAndListNewline(start, end)
                    if (firstAndLastNewline.first == firstAndLastNewline.second) {
                        editTextAttached.text.append(
                            "\n", firstAndLastNewline.first, firstAndLastNewline.first
                        )
                        finalEnd = end
                    } else
                        finalEnd = firstAndLastNewline.second
                    finalStart = firstAndLastNewline.first
                    if (finalStart > finalEnd) finalStart = finalEnd
                }

                val alignment =
                    if (oldSpan != null) (oldSpan as AlignmentSpan).alignment else Layout.Alignment.ALIGN_CENTER
                val span = AlignmentSpan.Standard(alignment)


                editTextAttached.text.setSpan(
                    span,
                    finalStart,
                    finalEnd,
                    flag
                )
                addEnabledStyle(
                    EnabledStyle(
                        SpanInfo(
                            finalStart,
                            finalEnd,
                            textStyle,
                            alignment.ordinal.toString()
                        ), continueSpan, span
                    )
                )

            }
            TextStyle.TEXT_SIZE_LARGE -> {
                if (start != end)
                    editTextAttached.text.replace(
                        end,
                        end,
                        Config.STYLE_TERMINATOR_OPERATOR //null/termination char
                    )
                if (continueSpan)  {
                    editTextAttached.text.replace(
                        start,
                        start,
                        Config.STYLE_TERMINATOR_OPERATOR //null/termination char
                    )
                    finalEnd = end + 1
                }
                val span = AbsoluteSizeSpan(Config.LARGE_FONT_SIZE,true)
                editTextAttached.text.setSpan(span, start, finalEnd, flag)
                addEnabledStyle(
                    EnabledStyle(
                        SpanInfo(start, finalEnd, textStyle,Config.LARGE_FONT_SIZE.toString()),
                        continueSpan,
                        span
                    )
                )
            }
            TextStyle.BULLET_LIST -> {

                    val firstAndLastNewline = getFirstAndListNewline(start, end)
                    if (firstAndLastNewline.first == firstAndLastNewline.second) {
                        editTextAttached.text.append(
                            "\n", firstAndLastNewline.first, firstAndLastNewline.first
                        )
                        finalEnd = end
                    } else
                        finalEnd = firstAndLastNewline.second
                    finalStart = firstAndLastNewline.first
                    if (finalStart > finalEnd) finalStart = finalEnd

                val span:BulletSpan
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val typedArray = activity.obtainStyledAttributes(intArrayOf(R.attr.editTextColor))
                    val textColor = typedArray.getColor(0,0)
                    typedArray.recycle()
                    span = BulletSpan(15,textColor,8)
                }else{
                    span = BulletSpan()
                }
                editTextAttached.text.setSpan(span, finalStart, finalEnd, flag)
                addEnabledStyle(
                    EnabledStyle(
                        SpanInfo(finalStart, finalEnd, textStyle),
                        true,
                        span
                    )
                )
            }
            TextStyle.QUOTE -> {
                if (continueSpan) {
                    val firstAndLastNewline = getFirstAndListNewline(start, end)
                    if (firstAndLastNewline.first == firstAndLastNewline.second) {
                        editTextAttached.text.append(
                            "\n", firstAndLastNewline.first, firstAndLastNewline.first
                        )
                        finalEnd = end
                    } else
                        finalEnd = firstAndLastNewline.second
                    finalStart = firstAndLastNewline.first
                    if (finalStart > finalEnd) finalStart = finalEnd
                }
                val span = QuoteSpan(getColor(editTextAttached.context, R.color.primaryColor))
                editTextAttached.text.setSpan(
                    span,
                    finalStart,
                    finalEnd,
                    flag
                )
                addEnabledStyle(
                    EnabledStyle(
                        SpanInfo(finalStart, finalEnd, textStyle),
                        continueSpan,
                        span
                    )
                )
            }
            TextStyle.IMAGE -> {
                imageButtonOnClickListener.onClick(editTextAttached)
            }
            TextStyle.REFER_LINK -> {
                if (oldSpan == null) return
                if (start != end)
                    editTextAttached.text.replace(
                        end,
                        end,
                        Config.STYLE_TERMINATOR_OPERATOR //null/termination char
                    )
                if (continueSpan)  {
                    editTextAttached.text.replace(
                        start,
                        start,
                        Config.STYLE_TERMINATOR_OPERATOR //null/termination char
                    )
                    finalEnd = end + 1
                }
                val span = URLSpan((oldSpan as URLSpan).url)
                //TODO IMPLEMENTERA PARCELABLE, hur skall man kunna ta bort länkar?? ->>
                editTextAttached.text.setSpan(span, start, finalEnd, flag)
                //enabledStyles.add(EnabledStyle(SpanInfo(start, finalEnd, textStyle, span),continueSpan, span))
                //storyViewModel.setEnabledStyles(enabledStyles.toMutableList())
            }
            else -> {}
        }
    }

    private val customClickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
        }

    }

    fun getFirstAndListNewline(start: Int, end: Int): Pair<Int, Int> {
        val textSelection = editTextAttached.text
        var firstNewline = textSelection.lastIndexOf('\n', Math.max(start - 1, 1))
        if (firstNewline>=0) firstNewline++
        var lastNewLine = textSelection.indexOf('\n', end)
        if (firstNewline == -1) firstNewline = 0
        if (lastNewLine == -1 && firstNewline > 0) lastNewLine = firstNewline
        if (lastNewLine == -1) lastNewLine = 0
        return Pair(firstNewline, lastNewLine)
    }

    fun splitOrUpdateDisabledSpan(enabledStyle: EnabledStyle, editText: EditText) {
        if (enabledStyle.spanInfo.start < editText.selectionStart && enabledStyle.spanInfo.end > editText.selectionEnd) {
            //split span
            removeEnabledStyle(enabledStyle)
            enableDisableSpan(
                enabledStyle.spanInfo.start,
                editText.selectionStart,
                enabledStyle.spanInfo.style,
                continueSpan = false
            )
            enableDisableSpan(
                editText.selectionEnd,
                enabledStyle.spanInfo.end,
                enabledStyle.spanInfo.style,
                continueSpan = enabledStyle.continueSpan
            )
        } else if (enabledStyle.spanInfo.start < editText.selectionStart) {
            removeEnabledStyle(enabledStyle)
            enableDisableSpan(
                enabledStyle.spanInfo.start,
                editText.selectionStart,
                enabledStyle.spanInfo.style,
                enabledStyle.continueSpan
            )
        } else if (enabledStyle.spanInfo.end > editText.selectionEnd) {
            removeEnabledStyle(enabledStyle)
            enableDisableSpan(
                editText.selectionEnd,
                enabledStyle.spanInfo.end,
                enabledStyle.spanInfo.style,
                enabledStyle.continueSpan
            )
        }
    }

    fun setTextWatcher() {
        val watcherSpan = editTextAttached.text.getSpans(0,
            editTextAttached.text.length - 1, Any::class.java ).filterIsInstance<ITextWatcher>()
        if (watcherSpan.isEmpty()) {
            editTextAttached.text.setSpan(
                spanWatchAction,
                0,
                editTextAttached.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }
        if (!watcherSet){
            watcherSet = true
            editTextAttached.addTextChangedListener(textWatcher)
        }
    }

    fun enableDisableStyle(
        textStyle: TextStyle,
        imageButton: ImageButton,
    ) {
        setTextWatcher()
        val enabledStylesSelection = getEnabledStylesSelection(
            editTextAttached.selectionStart,
            editTextAttached.selectionEnd
        )
        //never remove images by selection as users usually add more images next to each other ->>
        val currentStyleSpan :List<EnabledStyle> = if (textStyle == TextStyle.IMAGE) listOf() else enabledStylesSelection.filter { x -> x.spanInfo.style == textStyle };
        if (currentStyleSpan.count() > 0) {
            val foundEnabledStyle = currentStyleSpan[0]
            if (foundEnabledStyle.spanInfo.style == TextStyle.TEXT_ALIGNMENT && (foundEnabledStyle.span as AlignmentSpan).alignment == Layout.Alignment.ALIGN_CENTER) {
                currentStyleSpan.forEach { removeEnabledStyle(it) }
                updateTextAlignmentSpan(
                    TextStyle.TEXT_ALIGNMENT,
                    Layout.Alignment.ALIGN_OPPOSITE
                )
                imageButton.setImageResource(R.drawable.ic_text_align_right)
            } else if (foundEnabledStyle.spanInfo.style == TextStyle.QUOTE || foundEnabledStyle.spanInfo.style == TextStyle.BULLET_LIST ||
                (foundEnabledStyle.spanInfo.style == TextStyle.TEXT_ALIGNMENT && (foundEnabledStyle.span as AlignmentSpan).alignment == Layout.Alignment.ALIGN_OPPOSITE)
            ) {
                currentStyleSpan.forEach { removeEnabledStyle(it) }
                if (foundEnabledStyle.spanInfo.style == TextStyle.TEXT_ALIGNMENT) imageButton.setImageResource(
                    R.drawable.ic_text_align_left
                )
                disableButton(imageButton)
            } else {
                if (editTextAttached.selectionStart == editTextAttached.selectionEnd &&
                    foundEnabledStyle.spanInfo.end == editTextAttached.selectionEnd && foundEnabledStyle.spanInfo.end != foundEnabledStyle.spanInfo.start
                ) {
                    currentStyleSpan.forEach { removeEnabledStyle(it) }
                    enableDisableSpan(
                        foundEnabledStyle.spanInfo.start,
                        foundEnabledStyle.spanInfo.end,
                        foundEnabledStyle.spanInfo.style,
                        continueSpan = false,
                        foundEnabledStyle.span
                    )
                } else if (editTextAttached.selectionStart == editTextAttached.selectionEnd)
                    currentStyleSpan.forEach { removeEnabledStyle(it) }
                else if (foundEnabledStyle.spanInfo.start < editTextAttached.selectionStart || foundEnabledStyle.spanInfo.end > editTextAttached.selectionEnd)
                    splitOrUpdateDisabledSpan(foundEnabledStyle, editTextAttached)
                else currentStyleSpan.forEach { removeEnabledStyle(it) }
                disableButton(imageButton)
            }

        } else {
            if (textStyle == TextStyle.REFER_LINK){
                StoryUtil.showAddLinkDialog(editTextAttached,this)
                return
            }
            if (editTextAttached.selectionStart != editTextAttached.selectionEnd) {
                val stylesToRemove = getEnabledContainedStylesSelection(
                    editTextAttached.selectionStart,
                    editTextAttached.selectionEnd,
                    textStyle
                )
                stylesToRemove.forEach { x -> removeEnabledStyle(x) }
            }
            enableDisableSpan(
                editTextAttached.selectionStart,
                editTextAttached.selectionEnd,
                textStyle,
                editTextAttached.selectionStart == editTextAttached.selectionEnd
            )
            enableButton(imageButton)
            if (textStyle == TextStyle.TEXT_ALIGNMENT)
                imageButton.setImageResource(R.drawable.ic_text_align_center)
        }

    }

    private fun disableButton(imageButton: ImageButton) {
        imageButton.imageTintList = null
        imageButton.background = null
    }

    private fun enableButton(imageButton: ImageButton) {
        imageButton.imageTintList = ColorStateList.valueOf(Color.WHITE)
        imageButton.setBackgroundColor(getColor(imageButton.context, R.color.primaryColor))
    }

    fun addImageStyleFromBitmap(scaledBitmap: Bitmap, bitmapUri: Uri, miniatureBitmap: Bitmap?, loggedInUser:AccountInfoFull) {
        val file = File(bitmapUri.toString())
        editTextAttached.text.replace(
            editTextAttached.selectionStart,
            editTextAttached.selectionEnd,
            IMAGE_START_SEPARATOR //"          \n"
        )
        //val start = (editTextAttached.selectionStart - 5).coerceAtLeast(0)
        //val end = editTextAttached.selectionEnd.coerceAtLeast(5)
        val start = (editTextAttached.selectionStart - IMAGE_START_SEPARATOR.length).coerceAtLeast(0)
        val end = (start+IMAGE_START_SEPARATOR.length)
        val imageInfoMetadata = ImageInfoMetadata(file.name,scaledBitmap.width, scaledBitmap.height,miniatureBitmap?.width,miniatureBitmap?.height)
        val imgMetadataJson = Gson().toJson(imageInfoMetadata)
        val spanInfo = SpanInfo(
            end, //(end-IMAGE_START_SEPARATOR.length).coerceAtLeast(0)// 20220313 testar minska spannet på bilder,gammal -> // start
            end,
            TextStyle.IMAGE,
            imgMetadataJson
        )
        val span = ImageClickSpan(editTextAttached.context,spanInfo,null,false,editTextAttached, loggedInUser)
        editTextAttached.text.setSpan(
            span, start,
            end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        addEnabledStyle(
            EnabledStyle(
                spanInfo, false
            )
        )

    }

    fun addLinkStyle(linkSpan: URLSpan, linkText:String, linkType: LinkType) {
        val start = editTextAttached.selectionStart
        val end =  start + linkText.length
        editTextAttached.text.replace(editTextAttached.selectionStart,editTextAttached.selectionEnd,linkText + Config.STYLE_TERMINATOR_OPERATOR)
        editTextAttached.text.setSpan(
            linkSpan, start,
            end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        addEnabledStyle(
            EnabledStyle(
                SpanInfo(
                    start,
                    end,
                    TextStyle.REFER_LINK,
                    linkSpan.url  //linkType.name
                ), false,linkSpan
            )
        )

    }


    suspend fun insertImage(
        drawable: Bitmap,
        context: Activity,
        loggedInUser: AccountInfoFull
    ) {
        val scaledBitmap = ImageUtil.getScaledBitmap(drawable, Config.MAX_IMAGE_FULL_WIDTH,Config.MAX_IMAGE_FULL_HEIGHT)
        val originalUri =  ImageUtil.compressAndReturnBitmapUri(
            scaledBitmap,
            context,null)
        if (originalUri != null){
            val miniatureBitmap = ImageUtil.getMiniatureBitmapFromDeviceWidth(scaledBitmap,editTextAttached.measuredWidth.toFloat(), context)
            val orgFile = File( originalUri.toString())
            if (miniatureBitmap.width < scaledBitmap.width || miniatureBitmap.height < scaledBitmap.height) {
                val miniatureUri = ImageUtil.compressAndReturnBitmapUri(
                    miniatureBitmap,
                    context, orgFile.name
                )
                if (miniatureUri != null) {
                    context.runOnUiThread{ addImageStyleFromBitmap(miniatureBitmap, originalUri, miniatureBitmap,loggedInUser)}
                }
            }else{
                context.runOnUiThread{ addImageStyleFromBitmap(scaledBitmap, originalUri,null, loggedInUser)}
            }
        }

    }
}