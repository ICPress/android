package net.crowdventures.storypop.dto

open class Reward(val rewardId :UInt, val rewardName:String, val rewardPrice:UInt,
                  val rewardType : RewardType, var rewardRarity : RewardRarity, val imagePath: String?,
                  val availableUntil: String, val rewardMetadata: String, val description:String)