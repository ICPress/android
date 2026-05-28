package net.crowdventures.storypop.dto

data class Transaction(val transactionId:UInt, val timestamp:String, val descriptionType: TransactionDescriptionType, val amount :Int,val additionalData: String)