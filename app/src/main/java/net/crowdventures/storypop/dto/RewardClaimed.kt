package net.crowdventures.storypop.dto

class RewardClaimed( rewardId :UInt, rewardName:String,
                     rewardPrice:UInt, rewardType : RewardType,
                     rewardRarity : RewardRarity, imagePath: String?,
                     availableUntil: String, rewardMetadata: String,description:String, val walletTransferable:Boolean,val transferedDate:String?, val claimId:UInt, val transferRequestId:UInt?)
    : Reward(rewardId,rewardName,rewardPrice,rewardType, rewardRarity, imagePath,availableUntil,rewardMetadata, description)