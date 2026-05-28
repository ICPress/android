package net.crowdventures.storypop.composable

import android.app.Activity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import coil.compose.rememberImagePainter
import coil.size.OriginalSize
import coil.size.Scale
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.StoryPopIntro.Companion.ShowRegisterView
import net.crowdventures.storypop.composable.Profile.Companion.ProfileView
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel


class MainNavigation {
    companion object {

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun MainScreenNavigation(
            activity: Activity,
            navController: NavHostController,
            viewModel: StorySavedViewModel,
            sharedPreferenceManager: SharedPreferenceManager,
            registerViewModel: RegisterViewModel,
            lifecycleOwner: LifecycleOwner,
            bottomBarVisibility:MutableState<Boolean>,
            paddingValues: PaddingValues
        ) {
            var loggedInUser by remember {
                mutableStateOf(registerViewModel.loggedInUser.value)
            }
            registerViewModel.loggedInUser.observe(lifecycleOwner) { user ->
                loggedInUser = user
            }
            val currentLoggedInUser = loggedInUser
            //bottomBarOffsetEnabled.value = false TO DISABLE POP-UP BOTTOM BAR
            NavHost(navController, startDestination = Screen.Stories.route!!,
                enterTransition =  {
                    EnterTransition.None
                }, exitTransition = {
                    ExitTransition.None
                }, popEnterTransition = {
                    EnterTransition.None
                }, popExitTransition = {
                    ExitTransition.None
                }, modifier = Modifier.padding(0.dp)) { //modifier = Modifier.padding(paddingValues) creates blank space when navbar is hidden
                composable(Screen.Messages.route!!){ //NOT WORKING -> arguments = listOf(navArgument("userinfo") { type = UserFollowingInfoType() })) {
//                    val userFollowing =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)  it.arguments?.getParcelable("userinfo",
//                        UsersFollowingInfo::class.java) else it.arguments?.getParcelable<UsersFollowingInfo>("userinfo")
//                    if (navController.currentDestination?.route != it.destination.route) {
//                        return@composable
//                    }
//                    if (userFollowing == null ) return@composable
                    Messages.ShowMessages(lifecycleOwner,viewModel,bottomBarVisibility,loggedInUser?:return@composable, paddingValues,sharedPreferenceManager, navController)
                }
                composable(Screen.Profile.route!!) {
                    if (navController.currentDestination?.route != it.destination.route) {
                        return@composable
                    }
                    //Text("Test", modifier = Modifier, color = Color.Red)
                    //resetBottomBarOffsetCallback.onClick(null)
                    var registerEmailSent by remember { mutableStateOf(false) }
                    registerViewModel.sentRegisterRequest.observe(lifecycleOwner) { x ->
                        registerEmailSent = x
                    }
                    if (currentLoggedInUser == null) {
                        val defaultAppTextSelectionColors = ComposableUtil.GetDefaultTextSelectionColors()
                        CompositionLocalProvider(LocalTextSelectionColors provides defaultAppTextSelectionColors ) {
                            if (!registerEmailSent)
                                Register.RegisterView(
                                    activity, registerViewModel, bottomBarVisibility
                                )
                            else Register.VerifyEmail()
                        }
                    } else {
                        ProfileView(activity,
                            viewModel,
                            navController.context,
                            sharedPreferenceManager,
                            registerViewModel, lifecycleOwner,
                            bottomBarVisibility ,
                            paddingValues
                        )
                    }
                }
                composable(Screen.Following.route!!) {
                    if (navController.currentDestination?.route != it.destination.route) {
                        return@composable
                    }
                    if (currentLoggedInUser == null) {
                         ShowRegisterView(navController,paddingValues)
                    }
                    else Following.FollowingView(lifecycleOwner,navController.context,viewModel,registerViewModel ,paddingValues,navController)
                }
                composable(Screen.Notifications.route!!) {
                    if (navController.currentDestination?.route != it.destination.route) {
                        return@composable
                    }
                    if (currentLoggedInUser == null) {
                        ShowRegisterView(navController,paddingValues)
                    }
                    else NotificationsDashboard.ShowNotifications(navController.context,lifecycleOwner,viewModel,registerViewModel, sharedPreferenceManager,paddingValues,bottomBarVisibility,navController)
                }
                composable(Screen.Stories.route) {
                    if (navController.currentDestination?.route != it.destination.route) {
                        return@composable
                    }
                    // PickupScreen()
                   // bottomBarOffsetEnabled.value = true
                   // resetBottomBarOffsetCallback.onClick(null)
                    if (currentLoggedInUser == null) {
                        ShowRegisterView(navController,paddingValues)
                    }
                    else ArticleList.ShowArticles(viewModel = viewModel, context = activity,lifecycleOwner,bottomBarVisibility.value,sharedPreferenceManager, paddingValues, currentLoggedInUser, navController, registerViewModel)
                }

            }


        }

    }

}