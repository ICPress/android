package net.crowdventures.storypop.dto

import android.os.Parcel
import android.os.Parcelable
import net.crowdventures.storypop.viewmodels.StoryPublishedModel

class UsersFollowingInfo(val username:String, val profileIcon:String?,
                              val newStories: List<StoryPublishedModel>):Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString(),
        parcel.createTypedArrayList(StoryPublishedModel)!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(username)
        parcel.writeString(profileIcon)
        parcel.writeTypedList(newStories)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UsersFollowingInfo> {
        override fun createFromParcel(parcel: Parcel): UsersFollowingInfo {
            return UsersFollowingInfo(parcel)
        }

        override fun newArray(size: Int): Array<UsersFollowingInfo?> {
            return arrayOfNulls(size)
        }
    }
}

