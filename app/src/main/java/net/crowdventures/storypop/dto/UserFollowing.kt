package net.crowdventures.storypop.dto

data class UserFollowing (val latestFetchTimestamp:String, val userFollowings:List<UsersFollowingInfo>)