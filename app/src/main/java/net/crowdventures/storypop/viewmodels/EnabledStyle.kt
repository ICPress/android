package net.crowdventures.storypop.viewmodels

import android.os.Parcel
import android.os.Parcelable
import android.text.ParcelableSpan
import android.text.style.AlignmentSpan
import android.text.style.BulletSpan
import android.text.style.QuoteSpan
import android.text.style.StyleSpan
import net.crowdventures.storypop.TextStyle

class EnabledStyle(
    var spanInfo :SpanInfo,
    var continueSpan: Boolean,
    var  span: ParcelableSpan? = null,
) : Parcelable{
    constructor(parcel: Parcel) : this(
        spanInfo = parcel.readParcelable<SpanInfo>(SpanInfo::class.java.classLoader)!!,
        continueSpan = parcel.readInt() == 1
    ){
        when (spanInfo.style){
            TextStyle.ITALIC, TextStyle.BOLD, TextStyle.UNDERLINE -> {
                span = parcel.readParcelable<ParcelableSpan>(StyleSpan::class.java.classLoader)!!
            }
            TextStyle.TEXT_ALIGNMENT -> {
                span = parcel.readParcelable<ParcelableSpan>(AlignmentSpan.Standard::class.java.classLoader)!!
            }
            TextStyle.BULLET_LIST -> {
                span = parcel.readParcelable<ParcelableSpan>(BulletSpan::class.java.classLoader)!!
            }
            TextStyle.QUOTE -> {
                span = parcel.readParcelable<ParcelableSpan>(QuoteSpan::class.java.classLoader)!!
            }
            else  -> {
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(spanInfo,flags)
        parcel.writeInt(if (continueSpan) 1 else 0 )
        parcel.writeParcelable(span,flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EnabledStyle> {
        override fun createFromParcel(parcel: Parcel): EnabledStyle {
            return EnabledStyle(parcel)
        }

        override fun newArray(size: Int): Array<EnabledStyle?> {
            return arrayOfNulls(size)
        }
    }
}