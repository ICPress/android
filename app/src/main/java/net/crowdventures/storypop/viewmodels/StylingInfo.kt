package net.crowdventures.storypop.viewmodels

import android.os.Parcel
import android.os.Parcelable

data class StylingInfo(    var titleBackgroundColor:Int,
                      var titleHighlightColor : Int,
                      var titleBackgroundImage: String?,
                      var spans:MutableList<SpanInfo>): Parcelable,java.io.Serializable {


    override fun describeContents(): Int {
        return 0
    }

    constructor(parcel: Parcel) :this(
            titleBackgroundColor = parcel.readInt(),
                titleHighlightColor = parcel.readInt(),
                titleBackgroundImage = parcel.readString(),
                spans = getSpanList(parcel).toMutableList()

    )



    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0.writeInt(titleBackgroundColor)
        p0.writeInt(titleHighlightColor)
        p0.writeString(titleBackgroundImage)
        p0.writeTypedList(spans)
    }

    companion object CREATOR : Parcelable.Creator<StylingInfo> {
        override fun createFromParcel(parcel: Parcel): StylingInfo {
            return StylingInfo(parcel)
        }

        override fun newArray(size: Int): Array<StylingInfo?> {
            return arrayOfNulls(size)
        }
        fun getSpanList(parcel: Parcel):List<SpanInfo>{
            val list = ArrayList<SpanInfo>()
            parcel.readTypedList<SpanInfo>(list, SpanInfo)
            return list
        }
    }


}

