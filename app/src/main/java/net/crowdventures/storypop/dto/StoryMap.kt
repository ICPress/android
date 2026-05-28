package net.crowdventures.storypop.dto

import android.os.Parcel
import android.os.Parcelable

data class StoryMap(
    var centerLat: Double = 0.0,
    var centerLng: Double = 0.0,
    var zoom: Double = 8.0,
    var geoJson: String = ""
) : Parcelable {

    // 1. Read from parcel in the same order you write to it
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString() ?: ""
    )

    // 2. Write your properties to the parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(centerLat)
        parcel.writeDouble(centerLng)
        parcel.writeDouble(zoom)
        parcel.writeString(geoJson)
    }

    override fun describeContents(): Int = 0

    // 3. The Creator object that Android uses to generate instances
    companion object CREATOR : Parcelable.Creator<StoryMap> {
        override fun createFromParcel(parcel: Parcel): StoryMap {
            return StoryMap(parcel)
        }

        override fun newArray(size: Int): Array<StoryMap?> {
            return arrayOfNulls(size)
        }
    }
}