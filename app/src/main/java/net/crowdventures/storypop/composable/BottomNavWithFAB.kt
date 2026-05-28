package net.crowdventures.storypop.composable

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import net.crowdventures.storypop.ArticleContentEditActivity
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.composable.BottomNavComposable.Companion.BottomNav
import net.crowdventures.storypop.composable.MainNavigation.Companion.MainScreenNavigation
import net.crowdventures.storypop.util.BottomBarManagerCallback
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import kotlin.math.roundToInt

class BottomNavWithFAB {
    companion object{
        @Composable
        fun BottomBarWithFabDem(activity: Activity, viewModel: StorySavedViewModel,
                                sharedPreferenceManager: SharedPreferenceManager,
                                registerViewModel: RegisterViewModel,lifecycleOwner: LifecycleOwner) {
            val navController = rememberNavController()
            //för scroll
            val bottomBarOffsetHeightPx = remember { mutableStateOf(0f) }
            val bottomBarVisible = remember { mutableStateOf(true) }

              Scaffold( //för scroll Modifier.nestedScroll(nestedScrollConnection)
                  modifier = Modifier.fillMaxSize(), // Ensure full width
                bottomBar = {
                    if (bottomBarVisible.value)   BottomAppBarWithOffset(navController,bottomBarOffsetHeightPx,registerViewModel,lifecycleOwner)
                },
                floatingActionButtonPosition = FabPosition.Center,
                isFloatingActionButtonDocked = true,
                floatingActionButton = {
                    if (bottomBarVisible.value)    FloatingActionButtonWithOffset(navController, bottomBarOffsetHeightPx)
                }
            ) {
                   MainScreenNavigation(activity,navController, viewModel, sharedPreferenceManager,registerViewModel,lifecycleOwner,bottomBarVisible,it)
            }
        }

        @Composable
        fun BottomAppBarWithOffset(navController:NavHostController, bottomBarOffsetHeightPx:MutableState<Float>,registerViewModel: RegisterViewModel,lifecycleOwner: LifecycleOwner){
            BottomAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(5.dp, 5.dp, 0.dp, 0.dp))
                    .offset {
                        IntOffset(
                            x = 0,
                            y = -bottomBarOffsetHeightPx.value.roundToInt() +5 // +5 for shadow offset
                        )
                    }.graphicsLayer {
                    shadowElevation = 30f
                } ,
                contentPadding = PaddingValues(0.dp),
                elevation = 22.dp
            ) {
                BottomNav(navController = navController, registerViewModel,lifecycleOwner)
            }
        }
        @Composable
        fun FloatingActionButtonWithOffset(navController:NavHostController, bottomBarOffsetHeightPx:MutableState<Float>){
            val editActivityIntent =  rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    navController.navigate(Screen.Profile.route!!)
                }
            }
            val loggedInUser = Constants.loggedInUser
            FloatingActionButton(
                shape = RoundedCornerShape(
                    topStart = 15.0f,
                    topEnd = 15.0f,
                    bottomEnd = 0.0f,
                    bottomStart = 0.0f
                ),
                modifier = Modifier
                    .absoluteOffset(
                        0.dp,
                        (if (bottomBarOffsetHeightPx.value < 0) 28 else 18).dp
                    )
                    .offset {
                        IntOffset(
                            x = 0,
                            y = -bottomBarOffsetHeightPx.value.roundToInt()
                        )
                    }, //för scroll
                onClick = {
                    if (loggedInUser != null) {
                        val intent =
                            Intent(navController.context, ArticleContentEditActivity::class.java)
                        editActivityIntent.launch(intent)
                    }else{ //show notification intro-register view
                        Screen.Notifications.route?.let {
                            navController.navigate(it) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    }
                    //MainNavigation.startEditActivity(navController.context)
//                            Screen.Camera.route?.let {
//                                navController.navigate(it) {
//                                    popUpTo(navController.graph.findStartDestination().id) {
//                                        saveState = true
//                                    }
//                                    launchSingleTop = true
//                                    restoreState = true
//                                }
//                            }
//                            Screen.Camera.route?.let { navController.navigate(it) }
                },
                contentColor = MaterialTheme.colors.secondary
            ) {
//                        val image = rememberImagePainter( // rememberImagePainter
//                            data = Uri.parse(Config.APP_RESOURCE_PATH + R.drawable.ic_add),
//                            builder = {
//                                crossfade(true)
//                            }
//                        )
                Icon(imageVector = Icons.Default.Edit, modifier = Modifier.size(25.dp),
                    tint = MaterialTheme.colors.primary, contentDescription = "Add story") //painter = image,
            }

        }
    }
}
