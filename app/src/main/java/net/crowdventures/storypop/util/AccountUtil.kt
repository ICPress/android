package net.crowdventures.storypop.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.AcknowledgeStatus
import net.crowdventures.storypop.util.endpoints.AccountEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.endpoints.PlaystoreEndpont
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class AccountUtil {
    companion object {
        private fun getPackageInfoCompat(
            pm: PackageManager,
            packageName: String,
            flags: Int = 0
        ): PackageInfo {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION") return pm.getPackageInfo(packageName, flags)
            }
        }

        private fun getApplicationInfoCompact(
            pm: PackageManager,
            packageName: String,
            flags: Int = 0
        ): ApplicationInfo {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return pm.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION") return pm.getApplicationInfo(packageName, flags)
            }
        }

        fun isAppInstalled(packageName: String, context: Context): Boolean {
            return try {
                val pm: PackageManager = context.packageManager
                //val pack = getPackageInfoCompat(pm,packageName, PackageManager.GET_ACTIVITIES)
                return getApplicationInfoCompact(pm, packageName, 0).enabled
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                false
            }
        }

        suspend fun signInRefreshState(applicationContext: Context,
                                       refreshToken: String,
                                       sharedPreferenceManager: SharedPreferenceManager,
                                       registerViewModel: RegisterViewModel
        ) {
            try {
                val accountInfoFull: AccountInfoFull? =
                    EndpointAccountHandler.signIn(applicationContext,refreshToken, sharedPreferenceManager)
                if (accountInfoFull == null) {
                    Log.e(Config.logTag, "Received empty account info when verifying account!")
                    withContext(Dispatchers.Main) {
                        registerViewModel.errorMessage.value = applicationContext.getString(R.string.verification_link_has_expired)
                    }
                    //finish()
                    //return@launch //something went wrong
                } else {
                    val accountInfoJsonString = Gson().toJson(accountInfoFull)
                    sharedPreferenceManager.setLatestAccountInfo(accountInfoJsonString)
                    Constants.loggedInUser = accountInfoFull
                    withContext(Dispatchers.Main) {
                        //storySavedViewModel.onLoggedInUserChanged() <-- SHOULD NOT MATTER!
                        registerViewModel.loggedInUser.value = accountInfoFull
                        registerViewModel.isVerifyingToken.value = false
                        if (accountInfoFull.unreadFollowedStories > 0u)
                            registerViewModel.userFollowedNotificationsPending.value = true
                        if (accountInfoFull.unreadNotifications > 0u){
                            registerViewModel.userNotificationsConsumed.value = false
                        }
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    if (ex is retrofit2.HttpException && ex.code() == 401) {
                        registerViewModel.errorMessage.value = applicationContext.getString(R.string.login_session_has_expired_sign_in_email)
                        registerViewModel.loggedInUser.value = null
                        Constants.loggedInUser = null
                        return@withContext
                    }
                    registerViewModel.errorMessage.value = applicationContext.getString(R.string.there_was_an_connection_issue_check_connection)
                }
            }
        }


        fun removeWallet(
            registerViewModel: RegisterViewModel,
            loggedInUser: AccountInfoFull,
            context: Context,
            walletAddress: String,
            walletAddressShort: String
        ) {
            val client = AppSet.getClient(context)
            val task: Task<AppSetIdInfo> = client.appSetIdInfo

            task.addOnSuccessListener {
                // Determine current scope of app set ID.
                //val scope: Int = it.scope
                // Read app set ID value, which uses version 4 of the
                // universally unique identifier (UUID) format.
                val appSetId: String = it.id

                registerViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                    try {
                        val restAdapter = Retrofit.Builder()
                            .baseUrl(net.crowdventures.storypop.Config.APP_ENDPOINT)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(RetrofitUtil.generateSecureOkHttpClient(context))
                            .build()

                        val service: AccountEndpoint =
                            restAdapter.create(AccountEndpoint::class.java)
                        val response = service.deleteWallet(
                            "Bearer ${loggedInUser.refreshToken}",
                            walletAddress,
                            appSetId
                        )

                        if (response.isSuccessful) {
                            //TODO: update local account info cache and memory
                            withContext(Dispatchers.Main) {
                                registerViewModel.isVerifyingToken.value = false
                                val newAccountInfo = loggedInUser.copy(walletAddress = null)
                                registerViewModel.loggedInUser.value = newAccountInfo
                                Constants.loggedInUser = newAccountInfo
                                registerViewModel.errorMessage.value =
                                    "Your wallet $walletAddressShort has now been removed"
                            }
                        } else {
                            Log.e(
                                net.crowdventures.storypop.Config.logTag,
                                "Could send request to update wallet, response status-code:" + response.code()
                            )
                            withContext(Dispatchers.Main) {
                                registerViewModel.isVerifyingToken.value = false
                                registerViewModel.errorMessage.value =
                                    "Could not remove wallet, we are currently experiencing issues, try again later"
                            }
                        }
                    } catch (ex: Exception) {
                        withContext(Dispatchers.Main) {
                            registerViewModel.isVerifyingToken.value = false
                            registerViewModel.errorMessage.value =
                                "Could not remove wallet, please check your connection"
                            Log.e(
                                Config.logTag,
                                "Could not update wallet, ex: " + ex.message
                            )
                        }
                    }
                }
            }
        }

    }
}