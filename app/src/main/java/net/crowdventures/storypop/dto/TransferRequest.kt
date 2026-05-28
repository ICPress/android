package net.crowdventures.storypop.dto

data class TransferRequest(val deviceUUID:String, val walletAddress:String, val items : List<TransferRequestItem> )