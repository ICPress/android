package net.crowdventures.storypop.viewmodels

import android.os.Parcel
import android.os.Parcelable

interface ParcellableListType<T >:Parcelable.Creator<T> {
     fun getList(parcel: Parcel):ArrayList<T>

}