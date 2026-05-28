package net.crowdventures.storypop.composable

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.Reward
import net.crowdventures.storypop.dto.RewardClaimed
import net.crowdventures.storypop.dto.RewardRarity
import net.crowdventures.storypop.dto.RewardType
import net.crowdventures.storypop.dto.TransferRequestItem
import net.crowdventures.storypop.util.AccountUtil
import net.crowdventures.storypop.util.ClaimRewardResult
import net.crowdventures.storypop.util.ImageUtil
import net.crowdventures.storypop.util.RetrofitUtil
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.endpoints.RewardEndpoint
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import retrofit2.Retrofit

class RewardUtil {
    companion object {
        val RewardCardWidth = 160.dp
        val RewardStandardColor = Color.Gray

        @Composable
        fun DrawStoryPointsIcon(size: Int) {
            val density = LocalDensity.current
            val textSize = remember(size) {
                with(density) { (size * 0.4f).sp.value }
            }

            Box(
                modifier = Modifier
                    .size(size.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colors.primary,
                                MaterialTheme.colors.primaryVariant
                            )
                        ),
                        shape = CircleShape
                    )
                    .shadow(2.dp, CircleShape)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                // Border
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            BorderStroke((size / 20).dp, Color.White),
                            CircleShape
                        )
                )

                // Text with proper centering
                Text(
                    text = "SP",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(0.dp),  // Remove any default padding
                    lineHeight = textSize.sp,  // Match line height to font size
                    softWrap = false,
                    maxLines = 1
                )
            }
        }
        @Composable
        fun getRareRasterTintColor(rewardRarity: RewardRarity): Color {
            return when (rewardRarity) {
                RewardRarity.LEGENDARY -> Color(0xFFFFD700)
                RewardRarity.EPIC -> Color(0xFF9500FF)
                RewardRarity.RARE -> Color(0xFF3E92CC)
                else -> MaterialTheme.colors.onSurface
            }
        }

        @Composable
        private fun getRareRasterGradient(rewardRarity: RewardRarity): List<Color> {
            return when (rewardRarity) {
                RewardRarity.LEGENDARY -> listOf(
                    Color(0xFFC5A707),
                    Color(0xFFFFD700),
                    Color(0xFFFFF1A4),
                    Color(0xFFFFD700),
                    Color(0xFF9B8301)
                )
                RewardRarity.EPIC -> listOf(
                    Color(0xFF6C07C5),
                    Color(0xFF9500FF),
                    Color(0xFFDFA4FF),
                    Color(0xFF9500FF),
                    Color(0xFF60019B)
                )
                RewardRarity.RARE -> listOf(
                    Color(0xFF3E92CC),
                    Color(0xFF58A6FF),
                    Color(0xFFA4D5FF),
                    Color(0xFF58A6FF),
                    Color(0xFF2A7CB0)
                )
                else -> listOf(MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
            }
        }

        private fun getRewardTypeStringFromType(context: Context, rewardType: RewardType): String {
            return when (rewardType) {
                RewardType.VOUCHER -> context.getString(R.string.VOUCHER)
                RewardType.TOKEN -> context.getString(R.string.token)
                RewardType.COIN -> context.getString(R.string.COIN)
                RewardType.CREDITS -> context.getString(R.string.credits)
                else -> ""
            }
        }

        @Composable
        fun DrawRarityDescription(rarity: RewardRarity) {
            val colors = when (rarity) {
                RewardRarity.LEGENDARY -> Pair(Color(0xFFFFD700), Color(0xFFD5A200))
                RewardRarity.EPIC -> Pair(
                    if (isSystemInDarkTheme()) Color(0xFFC880FC) else Color(0xFF9500FF),
                    if (isSystemInDarkTheme()) Color(0xFFAE4BC0) else Color(0xFF710485)
                )
                RewardRarity.RARE -> Pair(
                    if (isSystemInDarkTheme()) Color(0xFF58A6FF) else Color(0xFF3E92CC),
                    if (isSystemInDarkTheme()) Color(0xFF2A7CB0) else Color(0xFF0A2463)
                )
                else -> Pair(MaterialTheme.colors.onSurface, MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
            }

            Box(
                modifier = Modifier
                    .background(
                        colors.first.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = rarity.name,
                    color = colors.first,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun DrawReward(
            context: Context,
            reward: Reward,
            showPrice: Boolean,
            isDialog: Boolean,
            isGiftBox: Boolean = false,
            loggedInUser: AccountInfoFull
        ) {
            Card(
                modifier = Modifier
                    .size(RewardCardWidth)
                    .shadow(
                        elevation = if (reward.rewardRarity != RewardRarity.STANDARD) 8.dp else 2.dp,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = MaterialTheme.colors.surface,
                border = if (reward.rewardRarity != RewardRarity.STANDARD) {
                    BorderStroke(
                        2.dp,
                        Brush.linearGradient(getRareRasterGradient(reward.rewardRarity))
                    )
                } else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isSystemInDarkTheme())
                                MaterialTheme.colors.surface
                            else
                                MaterialTheme.colors.background
                        )
                ) {
                    // Reward Type Badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = getRewardTypeStringFromType(context, reward.rewardType),
                            fontSize = 11.sp,
                            color = MaterialTheme.colors.secondary,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Reward Image
                    Card(
                        onClick = {
                            if (reward.imagePath != null) {
                                val inflater = LayoutInflater.from(context)
                                val v: View = inflater.inflate(R.layout.image_description_overlay, null, false)
                                val tvTitle = v.findViewById<TextView>(R.id.title)
                                val tvText = v.findViewById<TextView>(R.id.text)
                                tvTitle.text = reward.rewardName
                                tvText.text = reward.description
                                StfalconImageViewer.Builder<String>(
                                    context,
                                    listOf(reward.imagePath),
                                    object : ImageLoader<String> {
                                        override fun loadImage(
                                            imageView: ImageView?,
                                            image: String?
                                        ) {
                                            if (imageView == null || image == null) return
                                            val imageLoader = Config.getOrSetImageLoader(context)
                                            val imageRequest = ImageUtil.createImageRequestWithRetryOnce(
                                                context,
                                                imageView,
                                                imageLoader,
                                                loggedInUser.imageStaticPath + image
                                            )
                                            imageLoader.enqueue(imageRequest)
                                        }
                                    }
                                ).withOverlayView(v).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 0.dp,
                        backgroundColor = Color.Transparent
                    ) {
                        if (reward.imagePath != null) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = loggedInUser.imageStaticPath + reward.imagePath,
                                    contentScale = ContentScale.Fit
                                ),
                                contentDescription = reward.rewardName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "?",
                                    fontSize = 32.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    // Reward Name
                    Text(
                        text = reward.rewardName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = if (isGiftBox) 16.sp else 13.sp,
                        fontWeight = if (isGiftBox) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Rarity Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        DrawRarityDescription(reward.rewardRarity)
                    }

                    // Price
                    if (showPrice) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = reward.rewardPrice.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            DrawStoryPointsIcon(16)
                        }
                    }
                }
            }
        }

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun DrawRewardCard(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            loggedInUser: AccountInfoFull,
            reward: Reward,
            registerViewModel: RegisterViewModel,
            sharedPreferenceManager: SharedPreferenceManager,
            isClaimed: Boolean,
            isSelected: Boolean,
            transferRequestItems: List<TransferRequestItem>
        ) {
            var showLoading by remember { mutableStateOf(false) }
            var showErrorDialog by remember { mutableStateOf<String?>(null) }
            var showRewardClaimedDialog by remember { mutableStateOf(false) }

            if (showErrorDialog != null) {
                ComposableUtil.ShowErrorDialog(showErrorDialog!!) {
                    showErrorDialog = null
                }
            }

            var checkedState by remember {
                mutableStateOf(
                    reward is RewardClaimed && reward.walletTransferable &&
                            !transferRequestItems.any { x -> x.claimId == reward.claimId && x.rewardId == reward.rewardId } &&
                            (registerViewModel.claimedRewardsSelectedForTransfer.value?.any { x ->
                                x.claimId == reward.claimId && x.rewardId == reward.rewardId
                            } == true)
                )
            }

            registerViewModel.claimedRewardsSelectedForTransfer.observe(lifecycleOwner) { x ->
                if (x != null) {
                    checkedState = reward is RewardClaimed && reward.walletTransferable &&
                            !transferRequestItems.any { it.claimId == reward.claimId && it.rewardId == reward.rewardId } &&
                            x.any { it.claimId == reward.claimId && it.rewardId == reward.rewardId }
                } else {
                    checkedState = false
                }
            }

            var rewardClaimed by remember { mutableStateOf(false) }
            var showConfirmClaimReward by remember { mutableStateOf<Reward?>(null) }

            if (showLoading) ComposableUtil.ShowProgressDialog()

            if (showRewardClaimedDialog) {
                ShowRewardClaimedDialog(
                    context = context,
                    reward = reward,
                    loggedInUser = loggedInUser
                ) {
                    showRewardClaimedDialog = false
                    registerViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                        AccountUtil.signInRefreshState(
                            applicationContext = context,
                            refreshToken = loggedInUser.refreshToken,
                            sharedPreferenceManager = sharedPreferenceManager,
                            registerViewModel = registerViewModel
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(8.dp)
            ) {
                Box {
                    DrawReward(
                        context = context,
                        reward = reward,
                        showPrice = !isClaimed,
                        isDialog = false,
                        loggedInUser =  loggedInUser
                    )

                    // Checkbox for transfer selection
                    if (isClaimed && isSelected &&
                        (reward as? RewardClaimed)?.walletTransferable == true &&
                        !transferRequestItems.any { it.claimId == reward.claimId && it.rewardId == reward.rewardId }
                    ) {
                        Checkbox(
                            checked = checkedState,
                            onCheckedChange = {
                                if (it) {
                                    registerViewModel.claimedRewardsSelectedForTransfer.value?.add(
                                        TransferRequestItem(reward.claimId, reward.rewardId)
                                    )
                                } else {
                                    registerViewModel.claimedRewardsSelectedForTransfer.value?.removeAll {
                                            x -> x.claimId == reward.claimId && x.rewardId == reward.rewardId
                                    }
                                }
                                checkedState = it
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(24.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colors.secondary,
                                uncheckedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Status indicators for claimed rewards
                if (isClaimed && reward is RewardClaimed) {
                    when {
                        reward.transferedDate != null -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Transferred",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = StoryUtil.noTimeFormatDate(
                                            DateTime(reward.transferedDate, DateTimeZone.UTC)
                                                .withZone(DateTimeZone.getDefault()),
                                            context
                                        ),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                        reward.transferRequestId != null ||
                                transferRequestItems.any { it.claimId == reward.claimId && it.rewardId == reward.rewardId } -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "Processing",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.secondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Claim confirmation dialog
                if (showConfirmClaimReward != null) {
                    val rewardToConfirm = showConfirmClaimReward
                    if (rewardToConfirm != null) {
                        ComposableUtil.QuestionDialog(
                            title = "Claim Reward",
                            question = "Spend ${rewardToConfirm.rewardPrice} StoryPoints for \"${rewardToConfirm.rewardName}\"?",
                            confirmText = "CLAIM",
                            onDismissRequest = { showConfirmClaimReward = null }
                        ) {
                            showConfirmClaimReward = null
                            showLoading = true
                            registerViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                                val claimRewardResult = claimReward(
                                    context = context,
                                    loggedInUser = loggedInUser,
                                    reward = rewardToConfirm
                                )
                                withContext(Dispatchers.Main) {
                                    showLoading = false
                                    when (claimRewardResult) {
                                        ClaimRewardResult.SUCCESS -> {
                                            rewardClaimed = true
                                            showRewardClaimedDialog = true
                                        }
                                        ClaimRewardResult.NOT_ENOUGH_FUNDS -> {
                                            showErrorDialog = context.getString(R.string.claim_reward_failed_not_enough_funds)
                                        }
                                        ClaimRewardResult.COMMUNICATION_FAILURE -> {
                                            showErrorDialog = context.getString(R.string.connection_issue_check_connection)
                                        }
                                        else -> {
                                            showErrorDialog = context.getString(R.string.we_are_experiencing_problems)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Claim button
                if (!rewardClaimed && !isClaimed) {
                    val availableBalance = loggedInUser.accountBalance >= reward.rewardPrice
                    Button(
                        onClick = { showConfirmClaimReward = reward },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(36.dp),
                        enabled = availableBalance,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (availableBalance)
                                MaterialTheme.colors.secondary
                            else
                                MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                            contentColor = if (availableBalance)
                                MaterialTheme.colors.onSecondary
                            else
                                MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "CLAIM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (!isClaimed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(36.dp)
                            .background(
                                MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CLAIMED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        private suspend fun claimReward(
            context: Context,
            loggedInUser: AccountInfoFull,
            reward: Reward
        ): ClaimRewardResult {
            val restAdapter = Retrofit.Builder()
                .baseUrl(Config.APP_ENDPOINT)
                .client(RetrofitUtil.generateSecureOkHttpClient(context))
                .build()
            val service: RewardEndpoint = restAdapter.create(RewardEndpoint::class.java)

            return try {
                val authToken = loggedInUser.refreshToken
                val response = service.claimReward(
                    "Bearer $authToken",
                    reward.rewardId,
                    reward.rewardPrice,
                    reward.rewardType.ordinal.toUShort()
                )

                if (response.isSuccessful) {
                    ClaimRewardResult.SUCCESS
                } else {
                    val responseCode = response.code()
                    Log.e(Config.logTag, "Claim failed, code: $responseCode")
                    if (responseCode == 406) {
                        ClaimRewardResult.NOT_ENOUGH_FUNDS
                    } else {
                        ClaimRewardResult.GENERAL_FAILURE
                    }
                }
            } catch (ex: Exception) {
                Log.e(Config.logTag, "Claim exception: ${ex.message}", ex)
                ClaimRewardResult.COMMUNICATION_FAILURE
            }
        }

        @Composable
        fun ShowRewardClaimedDialog(
            context: Context,
            reward: Reward,
            loggedInUser: AccountInfoFull,
            onDismissRequest: () -> Unit
        ) {
            Dialog(
                onDismissRequest = onDismissRequest,
                DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp,
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tick_symbol),
                            contentDescription = null,
                            tint = MaterialTheme.colors.secondary,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Reward Claimed!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "You have successfully claimed:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reward preview
                        DrawReward(
                            context = context,
                            reward = reward,
                            showPrice = false,
                            isDialog = true,
                            loggedInUser = loggedInUser
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = MaterialTheme.colors.onSecondary
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text(
                                text = "OK",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}