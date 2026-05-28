package net.crowdventures.storypop
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.util.UUID

class SharedPreferenceManager(context: Context) {
    private var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "story_pop_cfg"
    private val DEVICE_UUID_SETTINGS_KEY ="DEVICE_UUID"
    private val AUTHOR_UUID_SETTINGS_KEY ="AUTHOR_UUID"
    private val DEFAULT_AUTHOR_UUID_SETTINGS_KEY ="DEFAULT_AUTHOR_UUID"
    private val LATEST_BEARER_TOKEN ="LATEST_BEARER_TOKEN"
    private val LATEST_ACCOUNT_INFO ="LATEST_ACCOUNT_INFO"
    private val LATEST_NOTIFICATIONS_FIRST_PAGE ="LATEST_NOTIFICATIONS_FIRST_PAGE"
    private val LATEST_STORIES_LAST_PAGE ="LATEST_STORIES_LAST_PAGE"
    private val LATEST_STORIES_PAGE_NUMBER ="LATEST_STORIES_PAGE_NUMBER"
    private val LATEST_USERS_FOLLOWED_CACHED ="LATEST_USERS_FOLLOWED_CACHED"
    private val TMP_AUTH_PRIV_KEY ="TMP_AUTH_PRIV_KEY" //PRIVATE KEY
    private val TMP_AUTH_PUB_KEY ="TMP_AUTH_PUB_KEY" //PUB KEY
    private val HAS_PUSHED_LATEST_FCM_TOKEN ="HAS_PUSHED_LATEST_FCM_TOKEN"
    private val LATEST_FCM_TOKEN ="LATEST_FCM_TOKEN"
    private val LATEST_CAMERA_URI ="LATEST_CAMERA_URI"
    private val LATEST_SPIN_CLAIMED_DATE_CACHED ="LATEST_SPIN_CLAIMED_DATE"
    private val NOTIFICATION_PERMISSION_REQUESTED ="NOTIFICATION_PERMISSION_REQUEST"
    private val LATEST_MESSAGES_FIRST_PAGE_PREFIX ="LATEST_MESSAGES_FIRST_PAGE_PREFIX"
    private val LATEST_MESSAGE_CACHED_KEY_PREFIX ="LATEST_MESSAGE_CACHED_KEY_PREFIX"
    private val LATEST_NOTIFICATION_CACHED_KEY_PREFIX ="LATEST_NOTIFICATION_CACHED_KEY_PREFIX"
    private val LATEST_MESSAGE_ID_REPLIED_KEY_PREFIX ="LATEST_MESSAGE_ID_REPLIED_KEY_PREFIX"
    private val CONTACT_BLOCKED_KEY_PREFIX ="CONTACT_BLOCKED_KEY_PREFIX"
    private val LATEST_STORY_PUBLISHED_SLUG_TITLE ="LATEST_STORY_PUBLISHED"
    private val CHECK_SUB_IS_ACKNOWLEDGED ="CHECK_SUB_IS_ACKNOWLEDGED"
    private val GROQ_API_KEY ="GROQ_API_KEY"
    private val API_ENDPOINT_KEY ="API_ENDPOINT_KEY"
    init {
//        @Deprecated("https://github.com/google/tink/issues/504")
//        val keyAlias = Config.getEncryptionKeyAlias()
//        sharedPreferences = EncryptedSharedPreferences.create(PREFS_NAME,keyAlias,context,
//            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM) //context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences =  context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getApiEndpoint():String?{
        return sharedPreferences.getString(API_ENDPOINT_KEY,null)
    }
    fun setApiEndpoint(apiEndpoint:String){
        val editor = sharedPreferences.edit()
        editor.putString(API_ENDPOINT_KEY, apiEndpoint)
        editor.apply()
    }
    fun latestFCMToken():String?{
        return sharedPreferences.getString(LATEST_FCM_TOKEN,null)
    }
    fun hasPushedLatestFCMToken():Boolean{
        return sharedPreferences.getBoolean(HAS_PUSHED_LATEST_FCM_TOKEN,false)
    }
    fun setPushedLatestFCMToken(){
        val editor = sharedPreferences.edit()
        editor.putBoolean(HAS_PUSHED_LATEST_FCM_TOKEN,true)
        editor.apply()
    }
    fun getLatestCameraURI():Uri?{
        val uriString = sharedPreferences.getString(LATEST_CAMERA_URI,null)
        return if (uriString != null) Uri.parse(uriString) else null
    }
    fun setLatestCameraURI(uri: Uri?) {
        val editor = sharedPreferences.edit()
        editor.putString(LATEST_CAMERA_URI, uri?.toString())
        editor.apply()
    }

    fun getCheckSubscriptionIsAcknowledged():Boolean{
        val isAcknowledged = sharedPreferences.getBoolean(CHECK_SUB_IS_ACKNOWLEDGED,false)
        return isAcknowledged
    }
    fun setCheckSubscriptionIsAcknowledged(check: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(CHECK_SUB_IS_ACKNOWLEDGED,check)
        editor.apply()
    }
    fun setLatestFCMToken(token: String) {
        val editor = sharedPreferences.edit()
        editor.putString(LATEST_FCM_TOKEN,token)
        editor.putBoolean(HAS_PUSHED_LATEST_FCM_TOKEN,false)
        editor.apply()
    }
    fun notificationPermissionRequested():Boolean{
        return sharedPreferences.getBoolean(NOTIFICATION_PERMISSION_REQUESTED,false)
    }
    fun setNotificationPermissionRequested() {
        val editor = sharedPreferences.edit()
        editor.putBoolean(NOTIFICATION_PERMISSION_REQUESTED,true)
        editor.apply()
    }
    fun hasDeviceUUID():Boolean{
        return sharedPreferences.contains(DEVICE_UUID_SETTINGS_KEY)
    }
    fun getDeviceUUID(): UUID {
        if (!hasDeviceUUID()) setDeviceUUID(UUID.randomUUID())
        return UUID.fromString(sharedPreferences.getString(DEVICE_UUID_SETTINGS_KEY,"")!!)
    }
    fun setDeviceUUID(deviceUUID: UUID) {
        val editor = sharedPreferences.edit()
        editor.putString(DEVICE_UUID_SETTINGS_KEY,deviceUUID.toString())
        editor.apply()
    }

    fun setLatestStoryPublishedSlugTitle(string: String) {
        val editor = sharedPreferences.edit()
        editor.putString(LATEST_STORY_PUBLISHED_SLUG_TITLE, string)
        editor.apply()
    }

    fun setGroqKey(string: String) {
        val editor = sharedPreferences.edit()
        editor.putString(GROQ_API_KEY, string)
        editor.apply()
    }
    fun getGroqKey():String? {
        return sharedPreferences.getString(GROQ_API_KEY,null)
    }

    fun getLatestStoryPublishedSlugTitle():String? {
        return sharedPreferences.getString(LATEST_STORY_PUBLISHED_SLUG_TITLE,null)
    }


    fun getLatestAccountInfoCached():String? {
        return sharedPreferences.getString(LATEST_ACCOUNT_INFO,null)
    }

    fun hasUsersFollowedCached():Boolean{
        return sharedPreferences.contains(LATEST_USERS_FOLLOWED_CACHED)
    }
    fun getLatestUsersFollowedCached():String? {
        return sharedPreferences.getString(LATEST_USERS_FOLLOWED_CACHED,null)
    }
    fun getLatestSpinClaimedDateCached():String? {
        return sharedPreferences.getString(LATEST_SPIN_CLAIMED_DATE_CACHED,null)
    }
    fun setLatestSpinClaimedDateCached(newSpinClaimedDate:String) {
        val editor = sharedPreferences.edit()
        editor.putString(LATEST_SPIN_CLAIMED_DATE_CACHED,newSpinClaimedDate)
        editor.apply()
    }

    fun clearAll(){
        clearLatestUserFollowedCached()
        clearLatestNotifications()
        clearLatestAccountInfo()
    }

    fun clearLatestUserFollowedCached() {
        val editor = sharedPreferences.edit()
        editor.remove(LATEST_USERS_FOLLOWED_CACHED)
        editor.apply()
    }

    fun setLatestUserFollowedCached(latestUsersFollowed:String) {
        val editor = sharedPreferences.edit()
        editor.putString(LATEST_USERS_FOLLOWED_CACHED, latestUsersFollowed)
        editor.apply()
    }

    fun getTempAuthPrivKey():String? {
        return sharedPreferences.getString(TMP_AUTH_PRIV_KEY,null)
    }
    fun setTempAuthPrivKey(key:String) {
        val editor = sharedPreferences.edit()
        editor.putString(TMP_AUTH_PRIV_KEY, key)
        editor.apply()
    }

    fun getTempAuthPubKey():String? {
        return sharedPreferences.getString(TMP_AUTH_PUB_KEY,null)
    }
    fun setTempAuthPubKey(key:String) {
        val editor = sharedPreferences.edit()
        editor.putString(TMP_AUTH_PUB_KEY, key)
        editor.apply()
    }
    fun getLatestTopMessagesCachedForUser(username: String):String? {
        return sharedPreferences.getString("${LATEST_MESSAGES_FIRST_PAGE_PREFIX}/$username",null)
    }

    fun getLatestCachedMessageKeyForUser(username: String):UInt? {
        val key =  sharedPreferences.getLong("${LATEST_MESSAGE_CACHED_KEY_PREFIX}/$username",-1)
        return if (key<0)  null else key.toUInt()
    }

    fun setLatestMessagesForUser(username: String, latestMessagesString: String) {
        val editor = sharedPreferences.edit()
        editor.putString("${LATEST_MESSAGES_FIRST_PAGE_PREFIX}/$username", latestMessagesString)
        editor.apply()
    }

    fun setLatestCachedMessageKeyForUser(username: String, latestKey: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong("${LATEST_MESSAGE_CACHED_KEY_PREFIX}/$username", latestKey)
        editor.apply()
    }

    fun getLatestTopNotificationsCached():String? {
        return sharedPreferences.getString(LATEST_NOTIFICATIONS_FIRST_PAGE,null)
    }

    fun setLatestMessageIdRepliedForUser(username: String, latestKey: UInt) {
        val editor = sharedPreferences.edit()
        editor.putLong("${LATEST_MESSAGE_ID_REPLIED_KEY_PREFIX}/$username", latestKey.toLong())
        editor.apply()
    }
    fun getLatestLatestMessageIdRepliedForUser(username: String):UInt? {
        val latestReplyMessageId =  sharedPreferences.getLong("${LATEST_MESSAGE_ID_REPLIED_KEY_PREFIX}/$username",-1)
        return if (latestReplyMessageId>0) latestReplyMessageId.toUInt() else null
    }

    fun getUserContactBlocked(username: String):Boolean? {
        if (!sharedPreferences.contains("${CONTACT_BLOCKED_KEY_PREFIX}/$username")) return null
        return sharedPreferences.getBoolean("${CONTACT_BLOCKED_KEY_PREFIX}/$username",false)
    }
    fun setUserContactBlocked(username: String,value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("${CONTACT_BLOCKED_KEY_PREFIX}/$username", value)
        editor.apply()
    }

    fun clearLatestNotifications() {
        val editor = sharedPreferences.edit()
        editor.remove(LATEST_NOTIFICATIONS_FIRST_PAGE)
        editor.apply()
    }
    fun setLatestNotifications(latestNotifications:String) {
        val editor = sharedPreferences.edit()
        editor.putString(LATEST_NOTIFICATIONS_FIRST_PAGE, latestNotifications)
        editor.apply()
    }

    fun setLatestCachedNotificationKey(latestKey: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(LATEST_NOTIFICATION_CACHED_KEY_PREFIX, latestKey)
        editor.apply()
    }

    fun getLatestCachedNotificationKey():UInt? {
        val key = sharedPreferences.getLong(LATEST_NOTIFICATION_CACHED_KEY_PREFIX,-1)
        return if (key<0) null else key.toUInt()
    }

    fun getLatestStoriesCached():String? {
        return sharedPreferences.getString(LATEST_STORIES_LAST_PAGE,null)
    }
    fun getLatestStoriesCachedPageNumber():Int? {
        val latestPage = sharedPreferences.getInt(LATEST_STORIES_PAGE_NUMBER,-1)
        if (latestPage == -1) return null else return latestPage
    }
    fun setLatestStories(latestNotifications:String,page:Int) {
        val editor = sharedPreferences.edit()
        editor.putString(LATEST_STORIES_LAST_PAGE, latestNotifications)
        editor.putInt(LATEST_STORIES_PAGE_NUMBER, page)
        editor.apply()
    }


    fun setLatestAccountInfo(latestAccountInfoJson:String) {
        val editor = sharedPreferences.edit()
        editor.putString(LATEST_ACCOUNT_INFO, latestAccountInfoJson)
        editor.apply()
    }
    fun clearLatestAccountInfo() {
        val editor = sharedPreferences.edit()
        editor.remove(LATEST_ACCOUNT_INFO )
        editor.apply()
    }
    fun getDefaultAuthorUUID(): UUID {
        if (!sharedPreferences.contains(DEFAULT_AUTHOR_UUID_SETTINGS_KEY)) setDefaultAuthorUUID(UUID.randomUUID())
        return UUID.fromString(sharedPreferences.getString(DEFAULT_AUTHOR_UUID_SETTINGS_KEY,"")!!)
    }

    fun setDefaultAuthorUUID(authorUUID: UUID) {
        val editor = sharedPreferences.edit()
        editor.putString(DEFAULT_AUTHOR_UUID_SETTINGS_KEY,authorUUID.toString())
        editor.apply()
    }


}