package net.crowdventures.storypop

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class CloudMessagingService: FirebaseMessagingService() {
    private val NEW_FOLLOWING_NOTIFICATION = "newFollowingNotification"
    private val NEW_NOTIFICATION = "newNotification"
    private val NEW_MESSAGE_NOTIFICATION ="messageFromUser"
    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(Config.logTag, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        val sharedPreferenceManager = SharedPreferenceManager(this.applicationContext)
        sharedPreferenceManager.setLatestFCMToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(Config.logTag, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(Config.logTag, "Message data payload: ${remoteMessage.data}")
            val newNotificationsArray = IntArray(2)
            if (remoteMessage.data.containsKey(NEW_FOLLOWING_NOTIFICATION))
                newNotificationsArray[0]= remoteMessage.data.get(NEW_FOLLOWING_NOTIFICATION)?.toIntOrNull()?:return
            if (remoteMessage.data.containsKey(NEW_NOTIFICATION))
                newNotificationsArray[1] = remoteMessage.data.get(NEW_NOTIFICATION)?.toIntOrNull()?:return
            val intent: Intent = Intent(ArticleListActivity.LOGGED_IN_USER_INFO_CHANGED_EXTRA).putExtra(ArticleListActivity.LOGGED_IN_USER_INFO_CHANGED_EXTRA, newNotificationsArray)
            if (remoteMessage.data.containsKey(NEW_MESSAGE_NOTIFICATION))
                intent.putExtra(ArticleListActivity.NEW_MESSAGE_FROM_USER,remoteMessage.data.get(NEW_MESSAGE_NOTIFICATION))
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcastSync(intent)
            Log.v(Config.logTag,"Sent local broadcast with new notification info!")
//            if (/* Check if data needs to be processed by long running job */ true) {
//                // For long-running tasks (10 seconds or more) use WorkManager.
//                scheduleJob()
//            } else {
//                // Handle message within 10 seconds
//                handleNow()
//            }

        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(Config.logTag, "Message Notification Body: ${it.body}")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(Config.logTag, "Pending messages were deleted as they were not delivered in time to this device!")
    }
}