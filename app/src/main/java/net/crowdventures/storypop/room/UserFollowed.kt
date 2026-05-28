package net.crowdventures.storypop.room

import androidx.room.Entity
import java.util.UUID

@Entity(primaryKeys = ["username","authorUUID"])
data class UserFollowed (val username: String,  val authorUUID: UUID)