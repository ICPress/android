package net.crowdventures.storypop.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.crowdventures.storypop.dto.ProfileInfo

public class ProfileViewModel (var profileInfo: MutableLiveData<ProfileInfo?> = MutableLiveData(null)) : ViewModel() //var username:String? = null, var profileIcon: String? = null
 //   , var profileBackgroundImage:String? = null,var profileText:String? = null