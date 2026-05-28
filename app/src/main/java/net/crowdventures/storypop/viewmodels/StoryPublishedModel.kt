package net.crowdventures.storypop.viewmodels

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import coil.request.ImageRequest
import com.google.gson.annotations.Expose
import net.crowdventures.storypop.dto.ArticlePrivateSource
import net.crowdventures.storypop.dto.ArticlePublicSource
import net.crowdventures.storypop.dto.StoryMap
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.UUID

class StoryPublishedModel(
     stylingInfo: StylingInfo,
     storyTitle: String,
    emptyTitle: String, contentText: String,
    location: String, langCode: String, tags: Array<String>, authorName: String, publicSources: Array<String>, privateSources:Array<ArticlePrivateSource>, isReviewed:Boolean, storyMap: StoryMap?,
    var timestamp: String,
    var slugTitle: String,
    var hearts: Int, var comments: Int,
     var authorBadge :String?,
    var rejectionReason: String?,
     var category: String?
) : StorySavedModel(
    stylingInfo,
    storyTitle,
    emptyTitle,
    contentText,
    location,
    langCode,
    tags,
    authorName,
    publicSources,
    privateSources,
    isReviewed,
    storyMap,
    DateTime.now(DateTimeZone.UTC),
    UUID.randomUUID(),
    UUID.randomUUID(),
), Parcelable {

    constructor(parcel: Parcel) : this(
        stylingInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(StylingInfo::class.java.classLoader, StylingInfo::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable<StylingInfo>(StylingInfo::class.java.classLoader)!!
        },
        storyTitle = parcel.readString().toString(),
        emptyTitle = parcel.readString().toString(),
        contentText = parcel.readString().toString(),
        location = parcel.readString().toString(),
        langCode = parcel.readString().toString(),
        tags = parcel.createStringArray()!!,
        authorName = parcel.readString().toString(),
        timestamp = parcel.readString()!!,
        slugTitle = parcel.readString()!!,
        hearts = parcel.readInt(),
        comments = parcel.readInt(),
        authorBadge = parcel.readString(),
        publicSources = parcel.createStringArray()?: arrayOf(),
        privateSources = parcel.createTypedArray(ArticlePrivateSource.CREATOR) ?: arrayOf(),
        isReviewed =  parcel.readInt() == 1,
        storyMap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(StoryMap::class.java.classLoader, StoryMap::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable<StoryMap>(StoryMap::class.java.classLoader)
        },
        rejectionReason = parcel.readString(),
        category = parcel.readString()
    ) {
        //fix for base class properties that need to be defined
        updatedDateTime = DateTime.now(DateTimeZone.UTC)
        storyUUID = UUID.randomUUID()
        storyOriginalUUID = storyUUID
    }

    @Transient @Expose(serialize = false)
    var imageRequest:ImageRequest? = null
    @Transient @Expose(serialize = false)
    var badgeImageRequest:ImageRequest? = null

    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0.writeParcelable(stylingInfo,p1)
        p0.writeString(storyTitle)
        p0.writeString(emptyTitle)
        p0.writeString(contentText)
        p0.writeString(location)
        p0.writeString(langCode)
        p0.writeStringArray(tags)
        p0.writeString(authorName)
        p0.writeString(timestamp)
        p0.writeString(slugTitle)
        p0.writeInt(hearts)
        p0.writeInt(comments)
        p0.writeString(authorBadge)
        p0.writeStringArray(publicSources)
        p0.writeTypedArray(privateSources, p1)
        p0.writeInt(if (isReviewed) 1 else 0)
        p0.writeParcelable(storyMap, p1)
        p0.writeString(rejectionReason)
        p0.writeString(category)
    }

    companion object CREATOR : Parcelable.Creator<StoryPublishedModel> {
        override fun createFromParcel(p0: Parcel): StoryPublishedModel {
            return StoryPublishedModel(p0)
        }

        override fun newArray(p0: Int): Array<StoryPublishedModel?> {
            return arrayOfNulls(p0)
        }
    }

}