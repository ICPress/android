package net.crowdventures.storypop.dto

import net.crowdventures.storypop.viewmodels.ImageInfoMetadata

data class UpdateProfileInfo (val profileBadgeImageInfo: ImageInfoMetadata?,
                              val profileBackgroundImageInfo: ImageInfoMetadata?,
                              val profileDescription: String?)