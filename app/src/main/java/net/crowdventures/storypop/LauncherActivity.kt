package net.crowdventures.storypop

import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.Task
import java.util.UUID


class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        //android.os.Debug.waitForDebugger()
        //FirebaseMessaging.getInstance().deleteToken() <- FOR TESTING
        val settingsPreferenceManager = SharedPreferenceManager(this)
        if (!settingsPreferenceManager.hasDeviceUUID()){
            val client = AppSet.getClient(this)
            val task: Task<AppSetIdInfo> = client.appSetIdInfo

            task.addOnSuccessListener {
                // Determine current scope of app set ID.
                //val scope: Int = it.scope
                // Read app set ID value, which uses version 4 of the
                // universally unique identifier (UUID) format.
                val appSetId: String = it.id
                settingsPreferenceManager.setDeviceUUID(UUID.fromString(appSetId))
            }
        }
        val apiEndpoint = settingsPreferenceManager.getApiEndpoint()
        if (!apiEndpoint.isNullOrEmpty()){
            Config.APP_ENDPOINT =apiEndpoint
        }
        val newIntent = Intent(intent) //ArticleContentEditActivity::class.java //ArticleListActivity::class.java
        newIntent.flags =  FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_NEW_TASK
        newIntent.component = ComponentName(this, ArticleListActivity::class.java)
        startActivity(newIntent)
        finish()
    }
}
