package net.crowdventures.storypop.composable

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.nio.charset.StandardCharsets

class RewardsDashboard {
    companion object {
        @Composable
        private fun AvailableRewardsHeader(
            loggedInUser: AccountInfoFull
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reorder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colors.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Available Rewards",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colors.onSurface
                        )
                    }

                    // Balance
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Balance:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = loggedInUser.accountBalance.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.offset(1.dp, 1.dp)
                            )
                            Text(
                                text = loggedInUser.accountBalance.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.secondary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        RewardUtil.DrawStoryPointsIcon(18)
                    }
                }
            }
        }

        @Composable
        private fun ClaimedRewardsHeader(
            registerViewModel: RegisterViewModel,
            chooseItemsForTransfer: MutableState<Boolean>
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title
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
                            text = "Claimed Rewards",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colors.onSurface
                        )
                    }

                    // Transfer Button or Selection Mode Text
                    if (!chooseItemsForTransfer.value) {
                        Button(
                            onClick = {
                                registerViewModel.claimedRewardsSelectedForTransfer.value = arrayListOf()
                                chooseItemsForTransfer.value = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = MaterialTheme.colors.onSecondary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallet,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TRANSFER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = "Select items to transfer",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun RewardsDashboard(
            storySavedViewModel: StorySavedViewModel,
            context: Context,
            sharedPreferenceManager: SharedPreferenceManager,
            registerViewModel: RegisterViewModel,
            lifecycleOwner: LifecycleOwner,
            paddingValues: PaddingValues
        ) {
            val user = Constants.loggedInUser ?: return
            var currentLoggedInUser by remember { mutableStateOf<AccountInfoFull>(user) }
            registerViewModel.loggedInUser.observe(lifecycleOwner) { x ->
                if (x != null) currentLoggedInUser = x
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                AvailableRewardsHeader(currentLoggedInUser)

                val availableRewardItems: LazyPagingItems<Reward> =
                    storySavedViewModel.rewardsStorySource.collectAsLazyPagingItems()

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = RewardUtil.RewardCardWidth),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = 8.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Loading State
                    if (availableRewardItems.loadState.refresh is LoadState.Loading && availableRewardItems.itemCount == 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colors.secondary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    // Empty State
                    else if (availableRewardItems.loadState.refresh is LoadState.NotLoading &&
                        availableRewardItems.loadState.append.endOfPaginationReached &&
                        availableRewardItems.itemCount == 0) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No rewards available",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Check back soon for new rewards!",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Reward Items
                    items(
                        count = availableRewardItems.itemCount,
                        key = { index -> "available_reward_${availableRewardItems[index]?.rewardId ?: index}" }
                    ) { index ->
                        val it = availableRewardItems[index]
                        if (it != null) {
                            RewardUtil.DrawRewardCard(
                                context = context,
                                lifecycleOwner = lifecycleOwner,
                                loggedInUser = currentLoggedInUser,
                                reward = it,
                                registerViewModel = registerViewModel,
                                sharedPreferenceManager = sharedPreferenceManager,
                                isClaimed = false,
                                isSelected = false,
                                transferRequestItems = listOf()
                            )
                        }
                    }
                }
            }
        }

        @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
        @Composable
        fun ClaimedRewardsDashboard(
            storySavedViewModel: StorySavedViewModel,
            context: Context,
            sharedPreferenceManager: SharedPreferenceManager,
            registerViewModel: RegisterViewModel,
            lifecycleOwner: LifecycleOwner,
            paddingValues: PaddingValues
        ) {
            var showErrorDialog by remember { mutableStateOf<String?>(null) }
            if (showErrorDialog != null) {
                ComposableUtil.ShowErrorDialog(showErrorDialog!!) {
                    showErrorDialog = null
                }
            }

            var showLoading by remember { mutableStateOf(false) }
            registerViewModel.isVerifyingToken.observe(lifecycleOwner) { x ->
                showLoading = x
            }

            if (showLoading) ComposableUtil.ShowProgressDialog()

            val chooseItemsForTransfer = remember { mutableStateOf(false) }
            var loggedInUserChanged = Constants.loggedInUser ?: return
            var walletAddress by remember { mutableStateOf(loggedInUserChanged.walletAddress) }

            registerViewModel.loggedInUser.observe(lifecycleOwner) { x ->
                if (x != null) {
                    loggedInUserChanged = x
                    walletAddress = x.walletAddress
                }
            }

            var transferRequestItems by remember { mutableStateOf(listOf<TransferRequestItem>()) }
            var confirmTransfer by remember { mutableStateOf(false) }

            val walletAddressShort = walletAddress?.let { Config.getShortWalletString(it) }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ClaimedRewardsHeader(registerViewModel, chooseItemsForTransfer)

                val availableRewardItems: LazyPagingItems<RewardClaimed> =
                    storySavedViewModel.claimedRewardsStorySource.collectAsLazyPagingItems()

                // Transfer Selection Button
                if (chooseItemsForTransfer.value) {
                    Button(
                        onClick = { confirmTransfer = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = MaterialTheme.colors.onSecondary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallet,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TRANSFER SELECTED",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Transfer Confirmation Dialog
                if (confirmTransfer) {
                    val walletAddressCurrent = loggedInUserChanged.walletAddress
                    val selectedItems = registerViewModel.claimedRewardsSelectedForTransfer.value

                    if (selectedItems == null || selectedItems.isEmpty()) {
                        showErrorDialog = "Select at least one item to transfer."
                        confirmTransfer = false
                    } else if (walletAddressCurrent == null) {
                        showErrorDialog = "Please connect a wallet to continue with the transfer"
                        confirmTransfer = false
                    } else {
                        ComposableUtil.QuestionDialog(
                            title = "Transfer Items",
                            question = "Are you sure you want to transfer ${selectedItems.size} item(s) to wallet $walletAddressShort?",
                            confirmText = "Confirm",
                            onDismissRequest = { confirmTransfer = false }
                        ) {
                            confirmTransfer = false
                            registerViewModel.isVerifyingToken.value = true
                            val client = AppSet.getClient(context)
                            val task: Task<AppSetIdInfo> = client.appSetIdInfo
                            task.addOnSuccessListener {
                                val appSetId: String = it.id
                                registerViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                                    val transferRequest = TransferRequest(
                                        appSetId,
                                        walletAddressCurrent,
                                        selectedItems.toList()
                                    )
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
                                            .client(RetrofitUtil.generateSecureOkHttpClient(context))
                                            .build()

                                        val service: RewardEndpoint = restAdapter.create(RewardEndpoint::class.java)
                                        val response = service.transferReward(
                                            "Bearer ${loggedInUserChanged.refreshToken}",
                                            transferRequest
                                        )

                                        if (response.isSuccessful) {
                                            withContext(Dispatchers.Main) {
                                                registerViewModel.isVerifyingToken.value = false
                                                transferRequestItems = selectedItems + transferRequestItems
                                                registerViewModel.claimedRewardsSelectedForTransfer.value = arrayListOf()
                                                chooseItemsForTransfer.value = false
                                                showErrorDialog = "We have received your transfer request. You will receive an email once the transfer is completed."
                                            }
                                        } else {
                                            Log.e(Config.logTag, "Error transferring items: ${response.code()}")
                                            withContext(Dispatchers.Main) {
                                                registerViewModel.isVerifyingToken.value = false
                                                showErrorDialog = "Could not process your request at this time. Please try again later."
                                            }
                                        }
                                    } catch (ex: Exception) {
                                        Log.e(Config.logTag, "Exception transferring items: ${ex.message}", ex)
                                        withContext(Dispatchers.Main) {
                                            registerViewModel.isVerifyingToken.value = false
                                            showErrorDialog = "Connection issue. Please check your connection and try again."
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = RewardUtil.RewardCardWidth),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = 8.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Loading State
                    if (availableRewardItems.loadState.refresh is LoadState.Loading && availableRewardItems.itemCount == 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colors.secondary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    // Empty State
                    else if (availableRewardItems.loadState.refresh is LoadState.NotLoading &&
                        availableRewardItems.loadState.append.endOfPaginationReached &&
                        availableRewardItems.itemCount == 0) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No claimed rewards",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Claimed Reward Items
                    items(
                        count = availableRewardItems.itemCount,
                        key = { index -> "claimed_reward_${availableRewardItems[index]?.claimId ?: index}" }
                    ) { index ->
                        val it = availableRewardItems[index]
                        if (it != null) {
                            val isSelected =it.walletTransferable && chooseItemsForTransfer.value
                            RewardUtil.DrawRewardCard(
                                context = context,
                                lifecycleOwner = lifecycleOwner,
                                loggedInUser = loggedInUserChanged,
                                reward = it,
                                registerViewModel = registerViewModel,
                                sharedPreferenceManager = sharedPreferenceManager,
                                isClaimed = true,
                                isSelected = isSelected && chooseItemsForTransfer.value,
                                transferRequestItems = transferRequestItems
                            )
                        }
                    }
                }
            }
        }
    }
}