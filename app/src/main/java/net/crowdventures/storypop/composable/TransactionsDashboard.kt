package net.crowdventures.storypop.composable

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.dto.Transaction
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

class TransactionsDashboard {
    companion object {
        @Composable
        fun ShowTransactions(
            context: Context,
            storySavedViewModel: StorySavedViewModel,
            paddingValues: PaddingValues
        ) {
            val dtf: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
            val transactionItems: LazyPagingItems<Transaction> =
                storySavedViewModel.transactionsSource.collectAsLazyPagingItems()
            val loggedInUser = Constants.loggedInUser ?: return

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Balance Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = MaterialTheme.shapes.medium,
                    elevation = 2.dp,
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Balance",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = loggedInUser.accountBalance.toString(),
                                    fontSize = 32.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.offset(2.dp, 2.dp)
                                )
                                Text(
                                    text = loggedInUser.accountBalance.toString(),
                                    fontSize = 32.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.secondary
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            RewardUtil.DrawStoryPointsIcon(24)
                        }
                    }
                }

                // Transactions Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transaction History",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onSurface
                    )
                }

                Divider(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(0.25f),
                        text = "Date",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        modifier = Modifier.weight(0.45f),
                        text = "Description",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        modifier = Modifier.weight(0.3f),
                        text = "Amount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.End
                    )
                }

                Divider(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                // Transactions List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding()
                    )
                ) {
                    items(
                        count = transactionItems.itemCount,
                        key = { index -> "transaction_${transactionItems[index]?.timestamp ?: index}" }
                    ) { index ->
                        val item = transactionItems[index]
                        if (item != null) {
                            val localPublishedDateTime = DateTime.parse(item.timestamp).toLocalDateTime()
                            val isPositive = item.amount > 0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = MaterialTheme.shapes.small,
                                elevation = 0.dp,
                                backgroundColor = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Date
                                    Text(
                                        modifier = Modifier.weight(0.25f),
                                        text = localPublishedDateTime.toString(dtf),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Description
                                    Text(
                                        modifier = Modifier.weight(0.45f),
                                        text = StoryUtil.getLocalTransactionDescription(context, item),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Amount
                                    Text(
                                        modifier = Modifier.weight(0.3f),
                                        text = "${if (isPositive) "+" else ""}${item.amount} SP",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isPositive)
                                            Color(0xFF28a745) // Green for positive
                                        else
                                            MaterialTheme.colors.error, // Red for negative
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}