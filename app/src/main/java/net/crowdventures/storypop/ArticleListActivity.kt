package net.crowdventures.storypop

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdClient
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.SecretBox
import com.instacart.library.truetime.TrueTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.composable.BottomNavWithFAB
import net.crowdventures.storypop.composable.ComposableUtil
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.PhantomData
import net.crowdventures.storypop.libs.Base58
import net.crowdventures.storypop.paging.NotificationSource
import net.crowdventures.storypop.util.AccountUtil
import net.crowdventures.storypop.util.DeviceUtil
import net.crowdventures.storypop.util.EndpointAccountHandler
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.util.endpoints.AccountEndpoint
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModelFactory
import net.crowdventures.storypop.viewmodels.StorySavedViewModelRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.nio.charset.StandardCharsets


class ArticleListActivity : ComponentActivity() {
    lateinit var sharedPreferenceManager: SharedPreferenceManager
    lateinit var registerViewModel: RegisterViewModel
    @Volatile var loadSlugTitle: String? = null


    companion object {
        val HASHTAG_FILTER_INTENT_NAME = "HASHTAG_FILTER"
        val LOGGED_IN_USER_INFO_CHANGED_EXTRA = "LOGGED_IN_USER_INFO_CHANGED_EXTRA"
        val NEW_MESSAGE_FROM_USER = "NEW_MESSAGE_FROM_USER"

        @Volatile
        var storySavedViewModel: StorySavedViewModel? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(mReceiver, IntentFilter(LOGGED_IN_USER_INFO_CHANGED_EXTRA))
        sharedPreferenceManager = SharedPreferenceManager(this)
        registerViewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val areNotificationsEnabled: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
        if (areNotificationsEnabled) {
            notificationManager.cancelAll() //clear any notifications
        }
        //val registerViewModel : RegisterViewModel by viewModels()
        val storySavedViewModelInit = ViewModelProvider(
            this, StorySavedViewModelFactory(
                this,
                StorySavedViewModelRepository(
                    this.applicationContext,
                    sharedPreferenceManager,
                    this,
                    registerViewModel
                )
            )
        ).get(StorySavedViewModel::class.java) // StorySavedViewModel by viewModels() //EmployeeViewModel
        storySavedViewModel = storySavedViewModelInit
        if (intent.hasExtra(HASHTAG_FILTER_INTENT_NAME))
            storySavedViewModelInit.performArticleFiltering(
                intent.getStringExtra(
                    HASHTAG_FILTER_INTENT_NAME
                )!!
            )
        lifecycleScope.launch(Dispatchers.IO + StoryUtil.coroutineExceptionHandler) {
            try {
                TrueTime.build().initialize(Config.NTP_SERVER)
            } catch (ex: Exception) {
                Log.e("spop", "Exception when initializing TrueTime")
            }

        }

        // Declare the launcher at the top of your Activity/Fragment:
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            sharedPreferenceManager.setNotificationPermissionRequested()
            if (isGranted) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // TODO: Inform user that that your app will not show notifications.
                registerViewModel.errorMessage.value =
                    "We are sorry that you disabled notifications, we won't be able to keep you up to date."
            }
        }

        val latestAccountInfoJson = sharedPreferenceManager.getLatestAccountInfoCached()

        if (latestAccountInfoJson != null) {
            val latestAccountInfo =
                Gson().fromJson(latestAccountInfoJson, AccountInfoFull::class.java)
            registerViewModel.loggedInUser.value = latestAccountInfo
            Constants.loggedInUser = latestAccountInfo
            registerViewModel.isVerifyingToken.value = true
            registerViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                signInUpdateState(
                    this@ArticleListActivity.applicationContext,
                    latestAccountInfo.refreshToken,
                    this
                )
                withContext(Dispatchers.Main) {
                    registerViewModel.isVerifyingToken.value = false
                }
                val slugTitleNotNull = loadSlugTitle
                if (slugTitleNotNull != null) {
                    ViewModelUtil.fetchArticle(
                        this@ArticleListActivity.applicationContext,
                        registerViewModel.viewModelScope,
                        slugTitleNotNull
                    ) { story ->
                        registerViewModel.showProgressbar.value = false
                        if (story != null) {
                            StoryUtil.startContentActivity(
                                story,
                                this@ArticleListActivity
                            )
                            loadSlugTitle = null
                        }
                    }
                }
            }
            registerViewModel.viewModelScope.launch(Dispatchers.IO + StoryUtil.coroutineExceptionHandler) {
                ViewModelUtil.startResumeUploads(this, this@ArticleListActivity.applicationContext)
            }
        }

        setContent {
            var showProgressbar by remember {
                mutableStateOf(registerViewModel.showProgressbar.value?:false)
            }
            registerViewModel.showProgressbar.observe(this){
                x-> showProgressbar = x
            }
            var verifyingToken by remember { mutableStateOf(false) }
            var showErrorDialog by remember { mutableStateOf<String?>(null) }
            val showRequestNotificationPermission = remember {
                mutableStateOf(!areNotificationsEnabled && !sharedPreferenceManager.notificationPermissionRequested())
            }
            if (showRequestNotificationPermission.value) {
                DeviceUtil.AskNotificationPermission(
                    this,
                    requestPermissionLauncher,
                    showRequestNotificationPermission
                )
            }
            registerViewModel.errorMessage.observe(this) { x ->
                showErrorDialog = x
            }
            Box(Modifier.safeDrawingPadding()) {
                //val navController = rememberNavController()
                //MainNavigation.MainScreenNavigation(navController)
                registerViewModel.isVerifyingToken.observe(this@ArticleListActivity) { x -> verifyingToken = x }
                MaterialTheme(
                    colors = if (isSystemInDarkTheme()) ComposableUtil.GetDarkColors(this@ArticleListActivity) else ComposableUtil.GetLightColors(
                        this@ArticleListActivity
                    ) //,
                    //typography = Typography(),
                    //shapes = …
                ) {
                    if (showProgressbar) ComposableUtil.ShowProgressDialog()
                    BottomNavWithFAB.BottomBarWithFabDem(
                        this@ArticleListActivity,
                        storySavedViewModelInit,
                        sharedPreferenceManager,
                        registerViewModel,
                        this@ArticleListActivity
                    )
                    if (verifyingToken && intent.data?.query != null) ComposableUtil.ShowProgressDialog()
                    if (showErrorDialog != null) ComposableUtil.ShowErrorDialog(showErrorDialog!!) {
                        registerViewModel.errorMessage.value = null
                        registerViewModel.isVerifyingToken.value = false
                    }
                }
            }
            //UserList(viewModel = employeeViewModel, context = this)
        }
        if (savedInstanceState == null) handleIntent(intent) //otherwise this will run on layout change/orientation change when verified already
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val appLinkAction = intent.action
        val appLinkData: Uri? = intent.data
        if (Intent.ACTION_VIEW == appLinkAction || appLinkAction == "VERIFY") {
            if (appLinkData?.path?.contains("/account/verify") == true) {
                appLinkData.query?.also { query ->
                    Log.v(Config.logTag, "Received query:$query")
                    registerViewModel.isVerifyingToken.value = true
                    // Declare the launcher at the top of your Activity/Fragment:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val permissionRequest = registerForActivityResult(
                            ActivityResultContracts.RequestPermission(),
                        ) { isGranted: Boolean -> //request notification permission
                            sharedPreferenceManager.setNotificationPermissionRequested()
                        }
                        permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    registerViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                        Config.firebaseAnalytics.logEvent("register_verify_token", null)
                        val accountSignInToken: String? =
                            EndpointAccountHandler.verifyAccountGetToken(
                                this@ArticleListActivity.applicationContext,
                                query
                            )
                        if (accountSignInToken == null) {
                            Config.firebaseAnalytics.logEvent("register_verify_token_failed", null)
                            Log.e(Config.logTag, "Received null-token when verifying account!")
                            runOnUiThread {
                                registerViewModel.errorMessage.value =
                                    "The verification link has expired."
                            }
                            //finish()
                            //return@launch //something went wrong
                        } else {
                            Config.firebaseAnalytics.logEvent("register_verify_success", null)
                            signInUpdateState(
                                this@ArticleListActivity.applicationContext,
                                accountSignInToken,
                                this
                            )
                        }

                    }
                }
            } else if (appLinkData?.path?.contains("/wallet/verify") == true && appLinkData.query?.contains(
                    "phantom_encryption_public_key"
                ) == true
            ) {
                if (Constants.loggedInUser == null) {
                    Config.firebaseAnalytics.logEvent("link_wallet_failed_not_logged_in", null)
                    registerViewModel.errorMessage.value =
                        "You need to be logged in to verify your new wallet address"
                    return
                }
                if (sharedPreferenceManager.getTempAuthPrivKey() == null) {
                    Config.firebaseAnalytics.logEvent("link_wallet_failed_session_expired", null)
                    registerViewModel.errorMessage.value =
                        "Your wallet verification session expired, please try again"
                    return
                }
                registerViewModel.isVerifyingToken.value = true
                val privateStringKey = sharedPreferenceManager.getTempAuthPrivKey() ?: return
                val loggedInUser = Constants.loggedInUser ?: return
                val sodium = SodiumAndroid()
                val lazySodium = LazySodiumAndroid(sodium, StandardCharsets.UTF_8)
                appLinkData.query?.also { query ->
                    var pubKeyServerIndex = query.indexOf("phantom_encryption_public_key=")
                    var dataIndex = query.indexOf("data=")
                    var nonceIndex = query.indexOf("nonce=")
                    if (pubKeyServerIndex < 0 || nonceIndex < 0 || dataIndex < 0) return
                    pubKeyServerIndex += "phantom_encryption_public_key=".length
                    dataIndex += "data=".length
                    nonceIndex += "nonce=".length
                    var pubKeyServerIndexEnd = query.indexOf("&", pubKeyServerIndex)
                    if (pubKeyServerIndexEnd < 0) pubKeyServerIndexEnd = query.length
                    var nonceIndexEnd = query.indexOf("&", nonceIndex)
                    if (nonceIndexEnd < 0) nonceIndexEnd = query.length
                    var dataIndexEnd = query.indexOf("&", dataIndex)
                    if (dataIndexEnd < 0) dataIndexEnd = query.length
                    val privateKeyBytes = Base58.decode(privateStringKey)
                    val serverPublicKeyStr =
                        query.subSequence(pubKeyServerIndex, pubKeyServerIndexEnd)
                    val serverPublicKeyBytes = Base58.decode(serverPublicKeyStr.toString())
                    val nonceStr = query.subSequence(nonceIndex, nonceIndexEnd).toString()
                    val dataStr = query.subSequence(dataIndex, dataIndexEnd).toString()
                    val dataBytes = Base58.decode(dataStr)
                    val nonceBase58Decoded = Base58.decode(nonceStr)
                    val messageBytes = ByteArray(dataBytes.size - SecretBox.MACBYTES) //
                    if (nonceBase58Decoded.size != SecretBox.NONCEBYTES) {
                        Log.e(Config.logTag, "Wrong size of nonce, quit!")
                        return
                    }
                    val success = lazySodium.cryptoBoxOpenEasy(
                        messageBytes,
                        dataBytes,
                        dataBytes.size.toLong(),
                        nonceBase58Decoded,
                        serverPublicKeyBytes,
                        privateKeyBytes
                    )
                    if (success) {
                        val result = String(messageBytes, StandardCharsets.UTF_8)
                        val phantomData = Gson().fromJson(result, PhantomData::class.java)
                        val client = AppSet.getClient(applicationContext) as AppSetIdClient
                        val task: Task<AppSetIdInfo> = client.appSetIdInfo as Task<AppSetIdInfo>

                        task.addOnSuccessListener {
                            // Determine current scope of app set ID.
                            //val scope: Int = it.scope
                            // Read app set ID value, which uses version 4 of the
                            // universally unique identifier (UUID) format.
                            val appSetId: String = it.id
                            registerViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                                try {
                                    val restAdapter = Retrofit.Builder()
                                        .baseUrl(Config.APP_ENDPOINT)
                                        .addConverterFactory(
                                            GsonConverterFactory.create(
                                                GsonBuilder().registerTypeAdapter(
                                                    UInt::class.java,
                                                    NullableUintJson()
                                                ).create()
                                            )
                                        )
                                        .client(RetrofitUtil.generateSecureOkHttpClient(this@ArticleListActivity))
                                        .build()

                                    val service: AccountEndpoint =
                                        restAdapter.create(AccountEndpoint::class.java)
                                    val response = service.updateWallet(
                                        "Bearer ${loggedInUser.refreshToken}",
                                        phantomData.public_key,
                                        appSetId
                                    )

                                    if (response.isSuccessful) {
                                        withContext(Dispatchers.Main) {
                                            registerViewModel.isVerifyingToken.value = false
                                            val newAccountInfo = loggedInUser.copy(walletAddress = phantomData.public_key)
                                            registerViewModel.loggedInUser.value = newAccountInfo
                                            Constants.loggedInUser = newAccountInfo
                                            val walletAddressShort =
                                                Config.getShortWalletString(phantomData.public_key)

                                            registerViewModel.errorMessage.value =
                                                "Your wallet $walletAddressShort is now connected!"
                                            sharedPreferenceManager.setTempAuthPubKey("") //clear keys
                                            sharedPreferenceManager.setTempAuthPrivKey("")
                                            Config.firebaseAnalytics.logEvent(
                                                "link_wallet_success",
                                                null
                                            )
                                        }
                                    } else {
                                        Config.firebaseAnalytics.logEvent(
                                            "link_wallet_failed_server_down",
                                            null
                                        )
                                        Log.e(
                                            Config.logTag,
                                            "Could send request to update wallet, response status-code:" + response.code()
                                        )
                                        withContext(Dispatchers.Main) {
                                            registerViewModel.isVerifyingToken.value = false
                                            registerViewModel.errorMessage.value =
                                                "Could not update wallet, we are currently experiencing issues, try again later"

                                        }
                                    }
                                } catch (ex: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Config.firebaseAnalytics.logEvent(
                                            "link_wallet_connection_issues",
                                            null
                                        )
                                        registerViewModel.errorMessage.value =
                                            "Could not update wallet, please check your connection"
                                        Log.e(
                                            Config.logTag,
                                            "Could not update wallet, ex: " + ex.message
                                        )
                                    }
                                }
                            }
                        }

                    } else {
                        registerViewModel.errorMessage.value =
                            "Could not verify wallet connection, please try again"
                        Log.e(Config.logTag, "Could not decrypt phantom payload!")
                    }
                }
            } else if (appLinkData?.path?.contains("/s/") == true) {
                val pathNotNull = appLinkData.path ?: return
                var slugTitle = if (pathNotNull.indexOf("?") > 0)
                    pathNotNull.substring(pathNotNull.lastIndexOf("/")+1, pathNotNull.indexOf("?"))
                else pathNotNull.substring(pathNotNull.lastIndexOf("/")+1)
                if (slugTitle.indexOf("#") > 0) slugTitle= pathNotNull.substring(0,pathNotNull.indexOf("#"))
                registerViewModel.showProgressbar.value = true
                loadSlugTitle = slugTitle
            }

        }
    }

    override fun onResume() {
        super.onResume()
        registerViewModel.loggedInUser.value = Constants.loggedInUser
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(mReceiver)
        } catch (ex: Exception) { // should not occur!
            Log.e(Config.logTag, "Could not unregister receiver!, exception:" + ex.message, ex)
        }

    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val changedParams = intent.getIntArrayExtra(LOGGED_IN_USER_INFO_CHANGED_EXTRA)
            val loggedInUser = Constants.loggedInUser ?: return
            if (changedParams != null && changedParams.isNotEmpty()) {
                val newFollowingNotification = changedParams[0]
                val newNotification = changedParams[1]
                val messageTargetUsername = storySavedViewModel?.messagesTargetUsername?.value
                val contextNotNull = context
                if (contextNotNull != null && messageTargetUsername != null &&
                    intent.hasExtra(NEW_MESSAGE_FROM_USER) && messageTargetUsername.username == intent.getStringExtra(
                        NEW_MESSAGE_FROM_USER
                    )
                ) {
                    storySavedViewModel?.setUserMessagesFromDatabase(
                        contextNotNull,
                        messageTargetUsername,
                        loggedInUser
                    )
                }
                var unreadNotifications = loggedInUser.unreadNotifications
                var unreadFollowingNotifications = loggedInUser.unreadFollowedStories
                if (registerViewModel.userNotificationsConsumed.value == true)
                    unreadNotifications = 0u
                if (registerViewModel.userFollowedNotificationsPending.value == false)
                    unreadFollowingNotifications = 0u
                val totalNewNotificationItems = unreadNotifications + newNotification.toUInt()
                val totalFollowedNotificationItems =
                    unreadFollowingNotifications + newFollowingNotification.toUInt()
                val newAccountInfo = loggedInUser.copy(
                    unreadNotifications=totalNewNotificationItems,
                    unreadFollowedStories = totalFollowedNotificationItems)
                Constants.loggedInUser = newAccountInfo
                runOnUiThread {
                    if (totalFollowedNotificationItems > 0u) registerViewModel.userFollowedNotificationsPending.value =
                        true
                    if (totalNewNotificationItems > 0u) {
                        registerViewModel.userNotificationsConsumed.value = false
                        val storySavedViewModel = storySavedViewModel
                        if (storySavedViewModel != null) { //refresh notification source always
                            storySavedViewModel.notificationSource = Pager(
                                PagingConfig(pageSize = 6),
                                initialKey = null
                            ) {
                                NotificationSource(
                                    this@ArticleListActivity.applicationContext,
                                    registerViewModel,
                                    storySavedViewModel.storySavedViewModelRepository.sharedPreferenceManager
                                )
                            }.flow.cachedIn(storySavedViewModel.viewModelScope)
                        }
                    }
                    registerViewModel.loggedInUser.value = newAccountInfo
                }
            }
        }
    }

    private suspend fun signInUpdateState(
        applicationContext: Context,
        accountSignInToken: String,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.apply {
            AccountUtil.signInRefreshState(
                applicationContext,
                accountSignInToken,
                sharedPreferenceManager,
                registerViewModel
            )
        }

    }

}

