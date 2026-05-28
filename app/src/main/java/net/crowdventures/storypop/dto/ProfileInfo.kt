package net.crowdventures.storypop.dto

data class ProfileInfo(val username:String, val profileIcon: String?,
                       val profileBackgroundImage:String?,val profileText:String?,
                       val followerSpan:String?,val memberSince: String?,
                        val articlesPublished:Long?, var deleted:Boolean = false, var contactBlocked:Boolean = false)
