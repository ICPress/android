package net.crowdventures.storypop.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import net.crowdventures.storypop.composable.ComposableUtil

class DeviceUtil {
     companion object{
         @Composable
         fun AskNotificationPermission(
             activity:Activity,
             permissionRequestLauncher: ActivityResultLauncher<String>,
             showPermissionDialog:MutableState<Boolean>
         ) {

             // This is only necessary for API level >= 33 (TIRAMISU)
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                 if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
                     PackageManager.PERMISSION_GRANTED
                 ) {
                     // FCM SDK (and your app) can post notifications.
                 } else if (shouldShowRequestPermissionRationale(activity,Manifest.permission.POST_NOTIFICATIONS)) {
                     // TODO: display an educational UI explaining to the user the features that will be enabled
                     //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                     //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                     //       If the user selects "No thanks," allow the user to continue without notifications.
                     ComposableUtil.QuestionDialog(
                         title = "Notifications",
                         question = "Allow us to send you important notifications?",
                         confirmText = "Allow" ,
                         onDismissRequest = { showPermissionDialog.value =false }) {
                         permissionRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                         showPermissionDialog.value =false
                     }
                 } else {
                     // Directly ask for the permission
                     permissionRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                 }
             }
         }

     }
}
