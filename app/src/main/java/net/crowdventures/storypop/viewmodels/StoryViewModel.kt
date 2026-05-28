package net.crowdventures.storypop.viewmodels

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.ParcelableSpan
import android.util.Log
import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import net.crowdventures.storypop.ArticleContentEditActivity
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.dto.ArticlePrivateSource
import net.crowdventures.storypop.dto.StoryMap
import net.crowdventures.storypop.libs.RoundedBackgroundSpan
import net.crowdventures.storypop.util.ViewModelUtil
import java.io.File
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.arrayListOf
import kotlin.collections.filter
import kotlin.collections.filterIsInstance
import kotlin.collections.firstOrNull
import kotlin.collections.indices
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.set
import kotlin.collections.toMutableList
import kotlin.collections.union

class StoryViewModel(
    val state_: SavedStateHandle,
    val storySavedModel: StorySavedModel?,
    val mappedEnabledStyle: Array<EnabledStyle>
) : ViewModel() {
    var hashTagSuggestions =  MutableLiveData<List<String>>()
    val highlightColorSpanSaved = storySavedModel?.stylingInfo?.titleHighlightColor
    val titleHighlightColor: MutableLiveData<RoundedBackgroundSpan?> =
        if (highlightColorSpanSaved != null) MutableLiveData( RoundedBackgroundSpan(highlightColorSpanSaved))
        else MutableLiveData(null)
    var titleBackgroundColor: MutableLiveData<Int?> = MutableLiveData(storySavedModel?.stylingInfo?.titleBackgroundColor)
    var titleImageUri: MutableLiveData<String?> = MutableLiveData(storySavedModel?.stylingInfo?.titleBackgroundImage)
    var enabledStyles : MutableLiveData<Array<EnabledStyle>> = MutableLiveData(mappedEnabledStyle)
    val savedTags = storySavedModel?.tags
    var selectedTags : MutableLiveData<ArrayList<String>> = if (savedTags != null) MutableLiveData(arrayListOf(*savedTags)) else  MutableLiveData()
    val publicSources:MutableLiveData<Array<String>> = MutableLiveData(storySavedModel?.publicSources?: arrayOf())
    val privateSources: MutableLiveData<Array<ArticlePrivateSource>> = MutableLiveData(storySavedModel?.privateSources?: arrayOf())
    val alternativeTitle: MutableLiveData<String> =MutableLiveData("")
    val mapData: MutableLiveData<StoryMap?> =MutableLiveData(storySavedModel?.storyMap)
    private val stylingInfoKey = "stylingInfo"
    private val editTextMetaDataKey = "editTextMeta"
    private val savedStateFlag ="savedStateFlag"
    private val selectedTagsKey = "selectedTagsKey"
    private val publicSourcesKey = "publicSourcesKey"
    private val privateSourcesKey = "privateSourcesKey"


    init {
        if (!state_.contains(savedStateFlag)) {
            Log.v(Config.logTag,"No saved state in viewModel, init new state...")
        }else  Log.v(Config.logTag,"Saved state exists, trying to restore state...")
    }
    fun restoreState(savedInstanceState: Bundle):ParcellableKeyValuePair? {
        Log.v(Config.logTag,"Found saved state, restoring..")
        if (!savedInstanceState.containsKey(stylingInfoKey) || !savedInstanceState.containsKey(editTextMetaDataKey) )
            return null
        val stylingInfo :StylingInfo? = savedInstanceState.getParcelable(stylingInfoKey)
        val spanKeyValuePair = savedInstanceState.getParcelable<ParcellableKeyValuePair>(editTextMetaDataKey)
        if (stylingInfo != null && spanKeyValuePair != null){
            titleBackgroundColor.value = stylingInfo.titleBackgroundColor
            titleHighlightColor.value = RoundedBackgroundSpan( stylingInfo.titleHighlightColor)
            titleImageUri.value = stylingInfo.titleBackgroundImage
            enabledStyles.value = stylingInfo.spans.map { EnabledStyle(it,false,null) }.toTypedArray()
            selectedTags.value = savedInstanceState.getStringArrayList(selectedTagsKey)
            publicSources.value = savedInstanceState.getStringArray(publicSourcesKey)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelableArray(privateSourcesKey, ArticlePrivateSource::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelableArray(privateSourcesKey)
                    ?.filterIsInstance<ArticlePrivateSource>()
                    ?.toTypedArray()
            }
            Log.v(Config.logTag,"Found following saved state items:" +
                    " ${enabledStyles.value?.size} enablesStyles, ${spanKeyValuePair.keyValuePair.size} spanKeyValuePair")
            Log.v(Config.logTag,"Restore state done!")
            return spanKeyValuePair


        }else{
            Log.e(Config.logTag,"Inconsistent state info, one of state variables null!")
            return null
        }
    }
    fun sanitizeInvalidStyles(enabledStyles: Array<EnabledStyle>,editText: EditText, updateEditTextTrimNulls:Boolean):List<EnabledStyle>{ // android text-watcher can loose track of current index selected due to old bugg!?!?
        val editTextLimit = if (editText.text.length > Constants.MAX_CONTENT_LENGTH) Constants.MAX_CONTENT_LENGTH else editText.text.length
        if (updateEditTextTrimNulls) editText.setText(editText.text.trimEnd('\u0000')) //do not do this if editing text, focus is reset to start of input
        return enabledStyles.filter {  it.spanInfo.start >= 0 && it.spanInfo.start <= editTextLimit && it.spanInfo.end >= 0 && it.spanInfo.end <= editTextLimit  }
    }

    fun generateStylingInfo(editText: EditText, updateEditTextTrimNulls:Boolean):StylingInfo{
        val backgroundColor = if (titleBackgroundColor.value == null)  0 else titleBackgroundColor.value!!
        val highlightColor  = if (titleHighlightColor.value == null)  0 else titleHighlightColor.value!!.backgroundColor
        val spanInfoList:MutableList<SpanInfo>
        if (enabledStyles.value== null)
            spanInfoList = mutableListOf<SpanInfo>()
        else
            spanInfoList = sanitizeInvalidStyles(enabledStyles.value!!,editText, updateEditTextTrimNulls).map {x-> x.spanInfo  }.toMutableList()
        val stylingInfo = StylingInfo(backgroundColor,highlightColor,titleImageUri.value, spanInfoList )
        return stylingInfo
    }
   // fun getStory() = stylingInfo

//    fun getCurrentStylingInfo():StylingInfo{
//        return StylingInfo(if (titleBackgroundColor.value == null) 0 else titleBackgroundColor.value!! ,// selectedTitleBackgroundColor
//            if (titleHighlightColor.value?.backgroundColor == null) 0 else titleHighlightColor.value?.backgroundColor!!,titleImageUri.value, // selectedTitleHighlightColor, selectedTitleImageUri
//            enabledStyles.value?.list?.map {
//                when (it.spanInfo.style){
//                    TextStyle.TEXT_ALIGNMENT -> SpanInfo(it.spanInfo.start,it.spanInfo.end, it.spanInfo.style, (it.span as AlignmentSpan).alignment.ordinal)
//                    TextStyle.IMAGE -> SpanInfo(it.spanInfo.start,it.spanInfo.end, it.spanInfo.style, it.span.toString())
//                    else -> SpanInfo(it.spanInfo.start,it.spanInfo.end, it.spanInfo.style,-1)
//                }
//            } as MutableList<SpanInfo>)
//    }
fun generateSpanIndexKeyValuePair(editText: EditText,enabledStyles: Array<EnabledStyle>): ParcellableKeyValuePair {
    val spanList = editText.text.getSpans(
        0,
        editText.text.length - 1,
        Any::class.java
    ).filterIsInstance<ParcelableSpan>()
    return getParcellableKeyValuePairForSpans(spanList,enabledStyles)
}
    fun getParcellableKeyValuePairForSpans(spanList: List<ParcelableSpan>,enabledStyles: Array<EnabledStyle>): ParcellableKeyValuePair{
        val mutableList = LinkedHashMap<Int,Int>()
        for (i in spanList.indices){
            val firstMatchingSpan = enabledStyles.firstOrNull { it.span === spanList[i]}
            if (firstMatchingSpan != null){
                val matchingIndex = enabledStyles.indexOf( firstMatchingSpan )
                Log.v(Config.logTag,"mapping at key $i to enabledStyle index $matchingIndex" +
                        ", matching style is ${firstMatchingSpan.spanInfo.style.name} with spanClass ${spanList[i].javaClass.name} ")
                mutableList[i] = matchingIndex
            }
        }
        return ParcellableKeyValuePair(mutableList)
    }
    fun saveUserStory(editText: EditText, fullAppState: Bundle){
        Log.v(Config.logTag,"App paused, trying to save state..")
        if (enabledStyles.value != null) {
            val stylingInfo = generateStylingInfo(editText,false)
            val spanIndexKeyValuePair =generateSpanIndexKeyValuePair(editText,enabledStyles.value!!)
            Log.v(Config.logTag,"Found ${stylingInfo.spans.size} spansInfo to save" +
                    " with ${spanIndexKeyValuePair.keyValuePair.size} spans that can be retained")
            //fullAppState.putParcelable(editTextContentKey,CharSequenceWrapper(editText.text))
            fullAppState.putParcelable(stylingInfoKey, stylingInfo)
            fullAppState.putParcelable(editTextMetaDataKey, spanIndexKeyValuePair)
            fullAppState.putStringArrayList(selectedTagsKey,selectedTags.value)
            fullAppState.putStringArray(publicSourcesKey,publicSources.value)
            fullAppState.putParcelableArray(privateSourcesKey, privateSources.value)
            state_.set(savedStateFlag,true)
            Log.v(Config.logTag,"Done saving state!")
        }
        //TODO save text
        //https://developer.android.com/reference/android/text/TextUtils#writeToParcel(java.lang.CharSequence,%20android.os.Parcel,%20int)
        //TextUtils.writeToParcel()
    }

    fun setEnabledStyles(newEnabledStyles: MutableList<EnabledStyle>){
        enabledStyles.value = newEnabledStyles.toTypedArray()
    }

    fun addSelectedTag(tag: String){
        val all:ArrayList<String>
        if (selectedTags.value == null) all = arrayListOf(tag)
        else{
            all = ArrayList(selectedTags.value!!.union(arrayListOf(tag)))
        }
        selectedTags.value = ArrayList(all)
    }
    fun removeSelectedTag(tag: String){
        val all = selectedTags.value?.filter{ it != tag}
        if (all != null) selectedTags.value = ArrayList(all)
    }
    fun setTitleBackgroundColor(color: Int?){
        titleBackgroundColor.value = color
    }
    fun setTitleHighlightColor(color: Int) {
        titleHighlightColor.value = RoundedBackgroundSpan(color)
    }
    fun clearTitleHighlightColor() {
        titleHighlightColor.value = null
    }

    fun setTitleImageUri(uri: Uri?,originalBitmap: Bitmap, miniatureBitmap: Bitmap?,activity: Activity) {
        if (uri != null) {
            val file = File(uri.toString())
            val imageInfoMetadata = ImageInfoMetadata(file.name,originalBitmap.width, originalBitmap.height,miniatureBitmap?.width,miniatureBitmap?.height)
            val jsonMetadata = Gson().toJson(imageInfoMetadata)
            activity.runOnUiThread { titleImageUri.value = jsonMetadata}
        }else activity.runOnUiThread { titleImageUri.value = null}
    }
    fun clearTitleImageUri() {
        titleImageUri.value = null
    }

    fun findArticleTags(applicationContext: Context,searchTerm:String){
        ViewModelUtil.fetchArticleTagSuggestions(applicationContext,viewModelScope,hashTagSuggestions,searchTerm)
    }

}