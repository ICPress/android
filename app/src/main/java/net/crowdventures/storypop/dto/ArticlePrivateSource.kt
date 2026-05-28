package net.crowdventures.storypop.dto

import android.os.Parcel
import android.os.Parcelable

data class ArticlePrivateSource(
    val name: String,
    val address: String = "",
    val phone: String = "",
    val notes: String = ""
) : Parcelable, java.io.Serializable {

    override fun describeContents(): Int {
        return 0
    }

    constructor(parcel: Parcel) : this(
        name = parcel.readString() ?: "",
        address = parcel.readString() ?: "",
        phone = parcel.readString() ?: "",
        notes = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(address)
        parcel.writeString(phone)
        parcel.writeString(notes)
    }

    companion object CREATOR : Parcelable.Creator<ArticlePrivateSource> {
        override fun createFromParcel(parcel: Parcel): ArticlePrivateSource {
            return ArticlePrivateSource(parcel)
        }

        override fun newArray(size: Int): Array<ArticlePrivateSource?> {
            return arrayOfNulls(size)
        }
    }
}