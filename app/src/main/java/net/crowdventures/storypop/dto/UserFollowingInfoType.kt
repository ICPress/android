package net.crowdventures.storypop.dto

import android.os.Build
import android.os.Bundle
import androidx.navigation.NavType
import com.google.gson.Gson

class UserFollowingInfoType  : NavType<UsersFollowingInfo>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): UsersFollowingInfo? {
        return  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)  bundle.getParcelable(key,UsersFollowingInfo::class.java) else bundle.getParcelable(key)
    }
    override fun parseValue(value: String): UsersFollowingInfo {
        return Gson().fromJson(value, UsersFollowingInfo::class.java)
    }
    override fun put(bundle: Bundle, key: String, value: UsersFollowingInfo) {
        bundle.putParcelable(key, value)
    }
}