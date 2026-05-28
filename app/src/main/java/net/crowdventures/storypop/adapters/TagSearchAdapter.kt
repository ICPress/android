package net.crowdventures.storypop.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.R
import net.crowdventures.storypop.util.TagCollectionChangeCallback
import net.crowdventures.storypop.viewmodels.StoryViewModel


class TagSearchAdapter : ArrayAdapter<String> {
    var vi: LayoutInflater
    private var mItems: MutableList<String> = mutableListOf()
    private var addTagViewCallback: TagCollectionChangeCallback? = null
    private var viewModel: StoryViewModel? =null
    private var latestSearchFilter : String = ""
    private var addTagBtn :CardView?= null
    private val hashTagSuggestionObserver= Observer<List<String>> {
        mItems.clear()
        mItems.addAll(it)
        nameFilter.filter(latestSearchFilter)
    }
    init {
        vi = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    constructor(lifecycleOwner: LifecycleOwner, viewModel: StoryViewModel,context: Context) : super(context, 0) {
        this.viewModel = viewModel
        viewModel.hashTagSuggestions.observe(lifecycleOwner,hashTagSuggestionObserver)
        vi = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    constructor(
        lifecycleOwner: LifecycleOwner,
        viewModel: StoryViewModel,
        context: Context,
        suggestionClickedCallback: TagCollectionChangeCallback, addTagBtn:CardView
    ) : super(context, 0, mutableListOf()) {
        this.viewModel = viewModel
        viewModel.hashTagSuggestions.observe(lifecycleOwner,hashTagSuggestionObserver)
        vi = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addTagViewCallback = suggestionClickedCallback
        this.addTagBtn = addTagBtn
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView;
        if (view == null) {
            val newView = vi.inflate(R.layout.tag_layout, null)
            newView.background = ColorDrawable(Color.TRANSPARENT)
            val typedValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            val arr = context.obtainStyledAttributes(typedValue.data, intArrayOf(android.R.attr.textColorPrimary))
            newView.findViewById<ImageView>(R.id.hashtag_iv).imageTintList = ColorStateList.valueOf(arr.getColor(0,-1))
            newView.findViewById<TextView>(R.id.hashtag_tv).setTextColor(arr.getColor(0,-1))
            arr.recycle()
            view = newView
        }
        val i: String? = mItems.getOrElse<String?>(position) { null }
        if ( view != null && i != null) {
            addTag(i, view)
        }
        return view!!
    }

    fun addTag(tag: String, inflatedView: View) {
        val inflatedText = inflatedView.findViewById<TextView>(R.id.hashtag_tv)
        if (inflatedText != null) {
            inflatedText.text = tag
            inflatedText.tag = tag
            inflatedText.setOnClickListener { addTagViewCallback?.onTagAdded(tag) }
        }
    }

    override fun getFilter(): Filter {
        return nameFilter
    }

    var nameFilter: Filter = object : Filter() {
        override fun convertResultToString(resultValue: Any): String {
            return  resultValue as String //(resultValue as TagItem).tag
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            if (constraint != null) {
                val trimmedConstraint =
                    constraint.toString().replace("#", "").replace(" ", "").replace("\n","")
                if (trimmedConstraint.length >= Config.MIN_HASHTAG_LENGTH &&
                    (constraint.toString().contains(' ') || constraint.toString()
                        .contains(',') || constraint.toString()
                .contains('\n') || constraint.length >= Config.MAX_HASHTAG_LENGTH)
                ) {
                    addTagViewCallback?.onTagAdded(trimmedConstraint.lowercase())
                    return FilterResults()
                }else if (trimmedConstraint != latestSearchFilter){
                    latestSearchFilter = constraint.toString()
                    viewModel?.findArticleTags(context,trimmedConstraint)
                }else{
                    val filterResults = FilterResults()
                    filterResults.values = mItems
                    filterResults.count = mItems.size
                    return filterResults
                }

                return FilterResults()
            } else {
               return  FilterResults()
            }
        }

        override fun publishResults(
            constraint: CharSequence?,
            results: FilterResults?
        ) {
            if (results != null && results.count > 0) {
                clear()
                for (c in results.values as List<*>) {
                    add(c as String)
                }
                notifyDataSetChanged()
            }
        }
    }
}


