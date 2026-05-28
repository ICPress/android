package net.crowdventures.storypop.viewmodels

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import net.crowdventures.storypop.dto.ArticlePrivateSource
import net.crowdventures.storypop.dto.StoryMap
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.UUID

open class StorySavedModel(val stylingInfo: StylingInfo, val storyTitle:String, val emptyTitle:String,
                           val contentText:String, val location:String, val langCode:String, val tags:Array<String>,
                           val authorName:String, var publicSources:Array<String>, var privateSources:Array<ArticlePrivateSource>, var isReviewed :Boolean, var storyMap: StoryMap?, @Transient @Expose(serialize = false) var updatedDateTime:DateTime,
                           @Transient @Expose(serialize = false) var storyUUID:UUID,
                           @Transient @Expose(serialize = false) var storyOriginalUUID:UUID, @Transient @Expose(serialize = false) var publishedSlugTitle:String? = null): Parcelable,java.io.Serializable {
    constructor(parcel: Parcel) :this(
        stylingInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(StylingInfo::class.java.classLoader, StylingInfo::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable<StylingInfo>(StylingInfo::class.java.classLoader)!!
        },
        storyTitle = parcel.readString().toString(),
        emptyTitle = parcel.readString().toString(),
        contentText =parcel.readString().toString(),
        location  = parcel.readString().toString(),
        langCode  = parcel.readString().toString(),
        tags = parcel.createStringArray()!!,
        authorName  = parcel.readString().toString(),
        updatedDateTime = DateTime(parcel.readString(), DateTimeZone.UTC),
        storyUUID = UUID.fromString(parcel.readString()),
        storyOriginalUUID = UUID.fromString(parcel.readString()),
        publishedSlugTitle =  parcel.readString().toString(),
        publicSources = parcel.createStringArray()?: arrayOf(),
        privateSources =  parcel.createTypedArray(ArticlePrivateSource.CREATOR) ?: arrayOf(),
        isReviewed =  parcel.readInt() == 1,
        storyMap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(StoryMap::class.java.classLoader, StoryMap::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable<StoryMap>(StoryMap::class.java.classLoader)
        }
    )
    override fun describeContents(): Int {
        return 0
    }


    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0.writeParcelable(stylingInfo,p1)
        p0.writeString(storyTitle)
        p0.writeString(emptyTitle)
        p0.writeString(contentText)
        p0.writeString(location)
        p0.writeString(langCode)
        p0.writeStringArray(tags)
        p0.writeString(authorName)
        p0.writeString(updatedDateTime.toDateTime(DateTimeZone.UTC).toString())
        p0.writeString(storyUUID.toString())
        p0.writeString(storyOriginalUUID.toString())
        p0.writeString(publishedSlugTitle?:"")
        p0.writeStringArray(publicSources)
        p0.writeTypedArray(privateSources, p1)
        p0.writeInt(if (isReviewed) 1 else 0 )
        p0.writeParcelable(storyMap,p1)
    }

    companion object CREATOR : Parcelable.Creator<StorySavedModel> {
        override fun createFromParcel(parcel: Parcel): StorySavedModel {
            return StorySavedModel(parcel)
        }

        override fun newArray(size: Int): Array<StorySavedModel?> {
            return arrayOfNulls(size)
        }

    }



}