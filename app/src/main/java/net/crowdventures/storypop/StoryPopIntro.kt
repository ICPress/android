package net.crowdventures.storypop

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import net.crowdventures.storypop.composable.Profile
import net.crowdventures.storypop.composable.Profile.Companion.StoryPopIcon
import net.crowdventures.storypop.util.ViewModelUtil

class StoryPopIntro {
    companion object {
        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun ShowRegisterView(
            navController: NavHostController,
            paddingValues: PaddingValues
        ) {
            val scrollState = rememberScrollState()

            Card(
                onClick = ViewModelUtil.goToRegister(navController),
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .verticalScroll(scrollState),
                shape = RoundedCornerShape(16.dp),
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Profile.StoryPopIcon(size = 80.dp)

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Independent Journalism\nStarts Here",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "An open-source contributor platform publishing verified reporting on the events that matter.",
                        fontSize = 15.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    PlatformPillar(
                        icon = Icons.Default.Verified,
                        iconTint = MaterialTheme.colors.secondary,
                        title = "Editorial Review",
                        description = "Every submission is assessed against published editorial standards before the article is published."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PlatformPillar(
                        icon = Icons.Default.Link,
                        iconTint = MaterialTheme.colors.secondary,
                        title = "Source Transparency",
                        description = "All claims must be substantiated. Sources are verifiable and attributed — no exceptions."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PlatformPillar(
                        icon = Icons.Default.Star,
                        iconTint = Color(0xFFFFC107),
                        title = "Contributor Equity",
                        description = "StoryPoints are credited for recognised contributions and are convertible to equity once the threshold is reached."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PlatformPillar(
                        icon = Icons.Default.Edit,
                        iconTint = if (isSystemInDarkTheme()) MaterialTheme.colors.onSecondary else MaterialTheme.colors.primary,
                        title = "Independent Contribution",
                        description = "Publish investigations, build a verifiable record of your work, and establish your standing as a contributor."
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = ViewModelUtil.goToRegister(navController),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = MaterialTheme.colors.onSecondary
                        ),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text(
                            text = "Register",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colors.onSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colors.onSecondary
                        )
                    }
                }
            }
        }

        @Composable
        private fun PlatformPillar(
            icon: ImageVector,
            iconTint: Color,
            title: String,
            description: String
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}