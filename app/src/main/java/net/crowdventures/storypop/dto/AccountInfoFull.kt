package net.crowdventures.storypop.dto

import android.os.Parcel
import android.os.Parcelable

class AccountInfoFull(
    user_uuid: String,
    email: String,
    username: String,
    val refreshToken: String,
    val profileIcon: String?,
    val profileBackgroundImage: String?,
    val profileText: String?,
    val followingLatestCheck: String,
    val accountBalance: UInt,
    val unreadNotifications: UInt,
    val unreadFollowedStories: UInt,
    val walletAddress: String?,
    val useTempAuthSwiftStorage: Boolean,
    val cdnLargePublishPath: String,
    val cdnSmallPublishPath: String,
    val cdnMessagePublishPath: String,
    val cdnLargeRequestPath: String,
    val cdnSmallRequestPath: String,
    val cdnMessageRequestPath: String,
    val imageStaticPath: String,
    val requireArticleSources:Boolean,
    val requireArticleReview:Boolean
) : AccountInfo(user_uuid, email, username), Parcelable {

    constructor(parcel: Parcel) : this(
        user_uuid = parcel.readString()!!,
        email = parcel.readString()!!,
        username = parcel.readString()!!,
        refreshToken = parcel.readString()!!,
        profileIcon = parcel.readString(),
        profileBackgroundImage = parcel.readString(),
        profileText = parcel.readString(),
        followingLatestCheck = parcel.readString()!!,
        accountBalance = parcel.readInt().toUInt(),
        unreadNotifications = parcel.readInt().toUInt(),
        unreadFollowedStories = parcel.readInt().toUInt(),
        walletAddress = parcel.readString(),
        useTempAuthSwiftStorage = parcel.readInt() == 1,
        cdnLargePublishPath = parcel.readString()!!,
        cdnSmallPublishPath = parcel.readString()!!,
        cdnMessagePublishPath = parcel.readString()!!,
        cdnLargeRequestPath = parcel.readString()!!,
        cdnSmallRequestPath = parcel.readString()!!,
        cdnMessageRequestPath = parcel.readString()!!,
        imageStaticPath = parcel.readString()!!,
        requireArticleSources= parcel.readInt() == 1,
        requireArticleReview = parcel.readInt() == 1,
    )

    /**
     * Creates a copy of this AccountInfoFull with optional modified fields
     */
    fun copy(
        user_uuid: String = this.user_uuid,
        email: String = this.email,
        username: String = this.username,
        refreshToken: String = this.refreshToken,
        profileIcon: String? = this.profileIcon,
        profileBackgroundImage: String? = this.profileBackgroundImage,
        profileText: String? = this.profileText,
        followingLatestCheck: String = this.followingLatestCheck,
        accountBalance: UInt = this.accountBalance,
        unreadNotifications: UInt = this.unreadNotifications,
        unreadFollowedStories: UInt = this.unreadFollowedStories,
        walletAddress: String? = this.walletAddress,
        useTempAuthSwiftStorage: Boolean = this.useTempAuthSwiftStorage,
        cdnLargePublishPath: String = this.cdnLargePublishPath,
        cdnSmallPublishPath: String = this.cdnSmallPublishPath,
        cdnMessagePublishPath: String = this.cdnMessagePublishPath,
        cdnLargeRequestPath: String = this.cdnLargeRequestPath,
        cdnSmallRequestPath: String = this.cdnSmallRequestPath,
        cdnMessageRequestPath: String = this.cdnMessageRequestPath,
        imageStaticPath: String = this.imageStaticPath,
         requireArticleSources :Boolean= this.requireArticleSources,
         requireArticleReview :Boolean = this.requireArticleReview
    ): AccountInfoFull {
        return AccountInfoFull(
            user_uuid = user_uuid,
            email = email,
            username = username,
            refreshToken = refreshToken,
            profileIcon = profileIcon,
            profileBackgroundImage = profileBackgroundImage,
            profileText = profileText,
            followingLatestCheck = followingLatestCheck,
            accountBalance = accountBalance,
            unreadNotifications = unreadNotifications,
            unreadFollowedStories = unreadFollowedStories,
            walletAddress = walletAddress,
            useTempAuthSwiftStorage = useTempAuthSwiftStorage,
            cdnLargePublishPath = cdnLargePublishPath,
            cdnSmallPublishPath = cdnSmallPublishPath,
            cdnMessagePublishPath = cdnMessagePublishPath,
            cdnLargeRequestPath = cdnLargeRequestPath,
            cdnSmallRequestPath = cdnSmallRequestPath,
            cdnMessageRequestPath = cdnMessageRequestPath,
            imageStaticPath = imageStaticPath,
            requireArticleSources = requireArticleSources,
            requireArticleReview = requireArticleReview
        )
    }

    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0.writeString(user_uuid)
        p0.writeString(email)
        p0.writeString(username)
        p0.writeString(refreshToken)
        p0.writeString(profileIcon)
        p0.writeString(profileBackgroundImage)
        p0.writeString(profileText)
        p0.writeString(followingLatestCheck)
        p0.writeInt(accountBalance.toInt())
        p0.writeInt(unreadNotifications.toInt())
        p0.writeInt(unreadFollowedStories.toInt())
        p0.writeString(walletAddress)
        p0.writeInt(if (useTempAuthSwiftStorage) 1 else 0)
        p0.writeString(cdnLargePublishPath)
        p0.writeString(cdnSmallPublishPath)
        p0.writeString(cdnMessagePublishPath)
        p0.writeString(cdnLargeRequestPath)
        p0.writeString(cdnSmallRequestPath)
        p0.writeString(cdnMessageRequestPath)
        p0.writeString(imageStaticPath)
        p0.writeInt(if (requireArticleSources) 1 else 0)
        p0.writeInt(if (requireArticleReview) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<AccountInfoFull> {
        override fun createFromParcel(p0: Parcel): AccountInfoFull {
            return AccountInfoFull(p0)
        }

        override fun newArray(p0: Int): Array<AccountInfoFull?> {
            return arrayOfNulls(p0)
        }
    }

    @Transient
    private var cachedRole: String? = null
    /**
     * Parses the JWT refresh token and returns the role claim.
     * Result is cached — subsequent calls with the same token skip parsing.
     * Returns null if the token is malformed or has no role claim.
     */
    fun getRoleFromToken(): String? {
        if (cachedRole != null)  return cachedRole

        return try {
            // JWT is three Base64url segments separated by dots: header.payload.signature
            val parts = refreshToken.split(".")
            if (parts.size != 3) return null

            // Base64url decode the payload (middle segment)
            // Android's Base64 needs NO_WRAP | NO_PADDING and URL_SAFE flags
            val payloadJson = String(
                android.util.Base64.decode(
                    parts[1],
                    android.util.Base64.NO_WRAP or
                            android.util.Base64.URL_SAFE or
                            android.util.Base64.NO_PADDING
                ),
                Charsets.UTF_8
            )

            // Parse role from JSON without a full JSON library
            // Looks for "role":"value" or "role": "value"
            val role = extractJsonStringValue(payloadJson, "JWT_ROLE")

            // Cache result so next call is instant
            cachedRole = role
            role
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts a string value for a given key from a flat JSON string.
     * Handles both "key":"value" and "key": "value" formats.
     * For nested JSON or arrays use a proper JSON parser instead.
     */
    private fun extractJsonStringValue(json: String, key: String): String? {
        // Regex: "key"\s*:\s*"captured_value"
        val pattern = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]+)\"")
        return pattern.find(json)?.groupValues?.get(1)
    }

    override fun describeContents(): Int {
        return 0
    }
}