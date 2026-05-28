package net.crowdventures.storypop.viewmodels

import android.os.Parcel
import android.os.Parcelable
import net.crowdventures.storypop.TextStyle

class SpanInfo(
    var start: Int,
    var end: Int,
    val style: TextStyle,
    val additionalInfoFlag : String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        start = parcel.readInt(),
                end = parcel.readInt(),
                style = TextStyle.values()[parcel.readInt()],
        additionalInfoFlag = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(start)
        parcel.writeInt(end)
        parcel.writeInt(style.ordinal)
        parcel.writeString(additionalInfoFlag.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SpanInfo> {
        override fun createFromParcel(parcel: Parcel): SpanInfo {
            return SpanInfo(parcel)
        }

        override fun newArray(size: Int): Array<SpanInfo?> {
            return arrayOfNulls(size)
        }
    }
}