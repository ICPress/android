package net.crowdventures.storypop.viewmodels

import android.os.Parcel
import android.os.Parcelable

class ParcellableKeyValuePair(val keyValuePair: Map<Int,Int>) :Parcelable {

        companion object CREATOR : Parcelable.Creator<ParcellableKeyValuePair> {
            override fun createFromParcel(parcel: Parcel): ParcellableKeyValuePair {
                return ParcellableKeyValuePair(parcel)
            }

            override fun newArray(size: Int): Array<ParcellableKeyValuePair?> {
                return arrayOfNulls(size)
            }

            private fun parcelToMap(parcel: Parcel):Map<Int,Int>{
                val keys = parcel.readArray(Int::class.java.classLoader)
                val values = parcel.readArray(Int::class.java.classLoader)
                val mutableMap = LinkedHashMap<Int,Int>()
                if (keys != null && values != null){

                    for (i in keys.indices){
                        mutableMap[keys[i] as Int] = values[i] as Int
                    }
                }
                return mutableMap
            }
        }


    constructor(parcel: Parcel) : this(parcelToMap(parcel))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeArray(keyValuePair.keys.toTypedArray())
        parcel.writeArray(keyValuePair.values.toTypedArray())
    }

    override fun describeContents(): Int {
        return 0
    }


}