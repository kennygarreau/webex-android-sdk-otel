package com.ciscowebex.androidsdk.kitchensink

import android.app.Application
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.ciscowebex.androidsdk.kitchensink.auth.LoginActivity
import com.ciscowebex.androidsdk.kitchensink.auth.loginModule
import com.ciscowebex.androidsdk.kitchensink.calling.calendarMeeting.calendarMeetingsModule
import com.ciscowebex.androidsdk.kitchensink.calling.callModule
import com.ciscowebex.androidsdk.kitchensink.extras.extrasModule
import com.ciscowebex.androidsdk.kitchensink.messaging.messagingModule
import com.ciscowebex.androidsdk.kitchensink.messaging.search.searchPeopleModule
import com.ciscowebex.androidsdk.kitchensink.person.personModule
import com.ciscowebex.androidsdk.kitchensink.search.searchModule
import com.ciscowebex.androidsdk.kitchensink.utils.SharedPrefUtils
import com.ciscowebex.androidsdk.kitchensink.webhooks.webhooksModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.unloadKoinModules
import com.splunk.rum.SplunkRum
import com.splunk.rum.SplunkRumBuilder
import com.splunk.rum.StandardAttributes
import io.opentelemetry.api.common.Attributes
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager


class KitchenSinkApp : Application(), LifecycleObserver {

    companion object {
        lateinit var instance: KitchenSinkApp
            private set

        fun applicationContext(): Context {
            return instance.applicationContext
        }

        fun get(): KitchenSinkApp {
            return instance
        }

        var inForeground: Boolean = false


        // App level boolean to keep track of if the CUCM login is of type SSO Login
        var isUCSSOLogin = false

        var isKoinModulesLoaded : Boolean = false

    }

    override fun onCreate() {
        super.onCreate()

        SplunkRum.builder()
            .setApplicationName("webex-sdk")
            .setDeploymentEnvironment("test")
            .setRealm(realm)
            .setRumAccessToken(rumAccessToken)
            .setGlobalAttributes(
                    Attributes.builder() // Add the application version. Alternatively, you
                        // can pass BuildConfig.VERSION_NAME as the value.
                        .put(StandardAttributes.APP_VERSION, "3.11.2")
                        .build()
            )
            // Turn off instrumentation of background processes
            //.disableBackgroundTaskReporting
            // Enables debug logging if needed
            //.enableDebug()
            .build(this);

        startKoin {
            androidLogger()
            androidContext(this@KitchenSinkApp)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this);
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // app moved to foreground
        inForeground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        // app moved to background
        inForeground = false
    }

    fun closeApplication() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }


    fun loadModules(): Boolean {
        val type = SharedPrefUtils.getLoginTypePref(this@KitchenSinkApp)
        if(type != null) {
            loadKoinModules(LoginActivity.LoginType.valueOf(type))
            return true
        }
        return false
    }


    fun loadKoinModules(type: LoginActivity.LoginType) {
        when (type) {
            LoginActivity.LoginType.JWT -> {
                loadKoinModules(listOf(mainAppModule, webexModule, loginModule, JWTWebexModule, searchModule, callModule, messagingModule, personModule, searchPeopleModule, webhooksModule, extrasModule, calendarMeetingsModule))
            }
            LoginActivity.LoginType.AccessToken -> {
                loadKoinModules(listOf(mainAppModule, webexModule, loginModule, AccessTokenWebexModule, searchModule, callModule, messagingModule, personModule, searchPeopleModule, webhooksModule, extrasModule, calendarMeetingsModule))
            }
            else -> {
                loadKoinModules(listOf(mainAppModule, webexModule, loginModule, OAuthWebexModule, searchModule, callModule, messagingModule, personModule, searchPeopleModule, webhooksModule, extrasModule, calendarMeetingsModule))
            }
        }
        isKoinModulesLoaded = true
    }

    fun unloadKoinModules() {
        unloadKoinModules(listOf(mainAppModule, webexModule, loginModule, JWTWebexModule, AccessTokenWebexModule, OAuthWebexModule, searchModule, callModule, messagingModule, personModule, searchPeopleModule, webhooksModule, extrasModule, calendarMeetingsModule))
        isKoinModulesLoaded = false
    }

    // Helper Functions
    fun getWifiDetail(reqParam: String): String? {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo
        //return wifiInfo?.bssid
        return when (reqParam) {
            "BSSID" -> wifiInfo?.bssid
            "RSSI" -> wifiInfo?.rssi?.toString()
            "LinkSpeed" -> wifiInfo?.linkSpeed?.toString()
            "IP" -> wifiInfo?.ipAddress?.toString() // deprecated API 31+
            //"TXLinkSpeed" -> wifiInfo?.txLinkSpeedMbps?.toString() // API 29+
            //"RXLinkSpeed" -> wifiInfo?.rxLinkSpeedMbps?.toString() // API 29+
            else -> null
        }
    }
}