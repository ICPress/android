package net.crowdventures.storypop.dto

import android.os.Parcel
import android.os.Parcelable

data class ArticleGuidelines(
    val version: String = "1.0",
    val lastUpdated: String = "",
    val categories: List<GuidelineCategory> = emptyList()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        version = parcel.readString() ?: "1.0",
        lastUpdated = parcel.readString() ?: "",
        categories = parcel.createTypedArrayList(GuidelineCategory.CREATOR) ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(version)
        parcel.writeString(lastUpdated)
        parcel.writeTypedList(categories)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ArticleGuidelines> {
        override fun createFromParcel(parcel: Parcel): ArticleGuidelines = ArticleGuidelines(parcel)
        override fun newArray(size: Int): Array<ArticleGuidelines?> = arrayOfNulls(size)
    }
}

data class GuidelineCategory(
    val id: String,
    val name: String,
    val description: String,
    val weight: Float = 1.0f,
    val subRules: List<GuidelineRule> = emptyList()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        name = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        weight = parcel.readFloat(),
        subRules = parcel.createTypedArrayList(GuidelineRule.CREATOR) ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeFloat(weight)
        parcel.writeTypedList(subRules)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<GuidelineCategory> {
        override fun createFromParcel(parcel: Parcel): GuidelineCategory = GuidelineCategory(parcel)
        override fun newArray(size: Int): Array<GuidelineCategory?> = arrayOfNulls(size)
    }
}

data class GuidelineRule(
    val id: String,
    val text: String,
    val isRequired: Boolean = true,
    val minScore: Float = 0.7f
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        text = parcel.readString() ?: "",
        isRequired = parcel.readByte() != 0.toByte(),
        minScore = parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(text)
        parcel.writeByte(if (isRequired) 1 else 0)
        parcel.writeFloat(minScore)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<GuidelineRule> {
        override fun createFromParcel(parcel: Parcel): GuidelineRule = GuidelineRule(parcel)
        override fun newArray(size: Int): Array<GuidelineRule?> = arrayOfNulls(size)
    }
}