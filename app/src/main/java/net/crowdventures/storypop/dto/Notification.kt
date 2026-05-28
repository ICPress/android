package net.crowdventures.storypop.dto

data class Notification (val notificationId:UInt, val notificationType : NotificationType?, val  additionalData :String,
                         val transactionDescriptionType: TransactionDescriptionType?, val timestamp: String, val profileIcon:String?,val storyTitle:String?, val triggerAuthor : String?)