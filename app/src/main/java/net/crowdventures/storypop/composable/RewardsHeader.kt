package net.crowdventures.storypop.composable

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.Task
import com.google.gson.GsonBuilder
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.KeyExchange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.BuildConfig
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.Reward
import net.crowdventures.storypop.dto.RewardClaimed
import net.crowdventures.storypop.dto.TransferRequest
import net.crowdventures.storypop.dto.TransferRequestItem
import net.crowdventures.storypop.libs.Base58
import net.crowdventures.storypop.util.AccountUtil
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.endpoints.NullableUintJson
import net.crowdventures.storypop.util.endpoints.RewardEndpoint
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import retrofit2.Retrofit
import java.nio.charset.StandardCharsets

class RewardsHeader {
    companion object {
        enum class RewardsHeaderItem {
            Available, Claimed
        }

        val items = listOf(
            RewardsHeaderItem.Available,
            RewardsHeaderItem.Claimed
        )

        @Composable
        fun DrawHeaderRewardsToggleButtonGroup(
            storySavedViewModel: StorySavedViewModel,
            context: Context,
            lifecycleOwner: LifecycleOwner,
            sharedPreferenceManager: SharedPreferenceManager,
            registerViewModel: RegisterViewModel,
            paddingValues: PaddingValues
        ) {
            var selectedIndex by remember { mutableStateOf(0) }
            val cornerRadius = 8.dp
            val loggedInUser = Constants.loggedInUser ?: return
            var walletAddress by remember { mutableStateOf(loggedInUser.walletAddress) }
            var showConnectDialog by remember { mutableStateOf(false) }
            var showRemoveWalletDialog by remember { mutableStateOf(false) }

            registerViewModel.loggedInUser.observe(lifecycleOwner) { x ->
                if (x != null) {
                    walletAddress = x.walletAddress
                }
            }

            val walletAddressShort = walletAddress?.let { Config.getShortWalletString(it) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header Row with Title and Wallet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title with Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colors.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rewards",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colors.onSurface
                        )
                    }

                    // Wallet Connection Button
                    WalletButton(
                        walletAddressShort = walletAddressShort,
                        isConnected = walletAddress != null,
                        onConnectClick = { showConnectDialog = true },
                        onDisconnectClick = { showRemoveWalletDialog = true }
                    )
                }

                // Wallet Dialogs
                if (showRemoveWalletDialog && walletAddressShort != null && walletAddress != null) {
                    ComposableUtil.QuestionDialog(
                        title = "Remove Wallet",
                        question = "Are you sure you want to remove your wallet: $walletAddressShort?",
                        confirmText = "Remove",
                        onDismissRequest = { showRemoveWalletDialog = false }
                    ) {
                        showRemoveWalletDialog = false
                        registerViewModel.isVerifyingToken.value = true
                        AccountUtil.removeWallet(
                            registerViewModel = registerViewModel,
                            loggedInUser = loggedInUser,
                            context = context,
                            walletAddress = walletAddress!!,
                            walletAddressShort = walletAddressShort
                        )
                    }
                }

                if (showConnectDialog) {
                    ConnectWalletDialog(
                        onDismissRequest = { showConnectDialog = false },
                        onConnectRequest = {
                            showConnectDialog = false
                            connectWallet(context, sharedPreferenceManager)
                        }
                    )
                }

                // Toggle Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items.forEachIndexed { index, item ->
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            onClick = { selectedIndex = index },
                            shape = when (index) {
                                0 -> RoundedCornerShape(
                                    topStart = cornerRadius,
                                    topEnd = 0.dp,
                                    bottomStart = cornerRadius,
                                    bottomEnd = 0.dp
                                )
                                items.size - 1 -> RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = cornerRadius,
                                    bottomStart = 0.dp,
                                    bottomEnd = cornerRadius
                                )
                                else -> RoundedCornerShape(0.dp)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selectedIndex == index) {
                                    MaterialTheme.colors.secondary
                                } else {
                                    MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                }
                            ),
                            colors = if (selectedIndex == index) {
                                ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colors.secondary
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = Color.Transparent,
                                    contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Reorder
                                        1 -> Icons.Default.Savings
                                        else -> Icons.Default.Favorite
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedIndex == index)
                                        MaterialTheme.colors.secondary
                                    else
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedIndex == index) FontWeight.Medium else FontWeight.Normal,
                                    color = if (selectedIndex == index)
                                        MaterialTheme.colors.secondary
                                    else
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content Area
                when (selectedIndex) {
                    RewardsHeaderItem.Available.ordinal -> {
                        RewardsDashboard.RewardsDashboard(
                            storySavedViewModel = storySavedViewModel,
                            context = context,
                            sharedPreferenceManager = sharedPreferenceManager,
                            registerViewModel = registerViewModel,
                            lifecycleOwner = lifecycleOwner,
                            paddingValues = paddingValues
                        )
                    }
                    RewardsHeaderItem.Claimed.ordinal -> {
                        RewardsDashboard.ClaimedRewardsDashboard(
                            storySavedViewModel = storySavedViewModel,
                            context = context,
                            sharedPreferenceManager = sharedPreferenceManager,
                            registerViewModel = registerViewModel,
                            lifecycleOwner = lifecycleOwner,
                            paddingValues = paddingValues
                        )
                    }
                }
            }
        }

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        private fun WalletButton(
            walletAddressShort: String?,
            isConnected: Boolean,
            onConnectClick: () -> Unit,
            onDisconnectClick: () -> Unit
        ) {
            Card(
                onClick = if (isConnected) onDisconnectClick else onConnectClick,
                shape = RoundedCornerShape(20.dp),
                backgroundColor = MaterialTheme.colors.surface,
                border = BorderStroke(
                    1.dp,
                    if (isConnected)
                        MaterialTheme.colors.secondary
                    else
                        MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallet,
                        contentDescription = null,
                        tint = if (isConnected)
                            MaterialTheme.colors.secondary
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = walletAddressShort ?: "Connect",
                        color = if (isConnected)
                            MaterialTheme.colors.secondary
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = if (isConnected) FontWeight.Medium else FontWeight.Normal
                    )
                    if (isConnected) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Disconnect",
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Connect",
                            tint = MaterialTheme.colors.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        @Composable
        private fun ConnectWalletDialog(
            onDismissRequest: () -> Unit,
            onConnectRequest: () -> Unit
        ) {
            Dialog(
                onDismissRequest = onDismissRequest,
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp,
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = "Connect Wallet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Phantom Icon
                        Icon(
                            painter = painterResource(id = R.drawable.phantom_logo_purple),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Description
                        Text(
                            text = "Connect your Phantom wallet to transfer your rewards",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Button
                            OutlinedButton(
                                onClick = onDismissRequest,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colors.onSurface
                                )
                            ) {
                                Text(
                                    text = "CANCEL",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Connect Button
                            Button(
                                onClick = onConnectRequest,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.secondary,
                                    contentColor = MaterialTheme.colors.onSecondary
                                ),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text(
                                    text = "CONNECT",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        private fun connectWallet(
            context: Context,
            sharedPreferenceManager: SharedPreferenceManager
        ) {
            val phantomPackageName = "app.phantom"
            if (AccountUtil.isAppInstalled(phantomPackageName, context)) {
                val sodium = SodiumAndroid()
                val publicKey = ByteArray(KeyExchange.PUBLICKEYBYTES)
                val privateKey = ByteArray(KeyExchange.SECRETKEYBYTES)
                val lazySodium = LazySodiumAndroid(sodium, StandardCharsets.UTF_8)
                lazySodium.sodium.crypto_box_keypair(publicKey, privateKey)
                val publicKeyStr = Base58.encode(publicKey)
                sharedPreferenceManager.setTempAuthPrivKey(Base58.encode(privateKey))
                sharedPreferenceManager.setTempAuthPubKey(publicKeyStr)
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://phantom.app/ul/v1/connect?app_url=https%3A%2F%2Ficpress.org%2Fwallet%2Fverify%3F&cluster=" + BuildConfig.WALLET_CLUSTER +
                                "&dapp_encryption_public_key=${publicKeyStr}&redirect_link=https%3A%2F%2Ficpress.org%2Fwallet%2Fverify%3F&cluster=" + BuildConfig.WALLET_CLUSTER
                    )
                )
                context.startActivity(browserIntent)
            } else {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$phantomPackageName")))
                } catch (e: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$phantomPackageName")))
                }
            }
        }
    }
}