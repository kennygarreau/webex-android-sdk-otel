package com.ciscowebex.androidsdk.kitchensink

import android.app.ActivityManager
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
//import com.splunk.rum.SplunkRum
//import com.splunk.rum.SplunkRumBuilder
//import com.splunk.rum.StandardAttributes
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.fragment.app.Fragment
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.OpenTelemetryRumBuilder
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

class KitchenSinkApp : Application(), LifecycleObserver {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    //private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val realm = "us1"
    private val rumAccessToken = "set_to_your_access_token"

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

        lateinit var tracer: Tracer
        lateinit var meter: Meter

        var rum: OpenTelemetryRum? = null

        fun rumTracer(name: String): Tracer? {
            return rum?.openTelemetry?.tracerProvider?.get(name)
        }
    }

    override fun onCreate() {
        super.onCreate()

/*
    SplunkRum.builder()
        .setApplicationName("webex-sdk")
        .setDeploymentEnvironment("webex-dev")
        .setRealm(realm)
        .setRumAccessToken(rumAccessToken)
        .setGlobalAttributes(
                Attributes.builder() //
                    .put(StandardAttributes.APP_VERSION, BuildConfig.VERSION_NAME)
                    .build()
        )
        .build(this);

        */
        val TAG = "otel-sdk"
        Log.i(TAG, "Initializing OpenTelemetry SDK")
        val diskBufferingConfig =
            DiskBufferingConfiguration.builder()
                .setEnabled(true)
                .setMaxCacheSize(50_000_000)
                .build()
        val config =
            OtelRumConfig()
                //.setGlobalAttributes(Attributes.of(stringKey("toolkit"), "jetpack compose"))
                .setDiskBufferingConfiguration(diskBufferingConfig)

        // val ingestUrl = "http://pi-truck:4318/v1/traces"
        val ingestUrl = "http://192.168.29.117/v1/traces"
        //val otlpIngestUrl = "http://192.168.29.117:4317"
        val otlpIngestUrl = "http://pi-truck:4317"

        val resource = Resource.getDefault()
            .merge(Resource.builder()
                .put("service.name", "webex-sdk")
                .put("deployment.environment", "webex-dev")
                .build())

        val spanExporter: SpanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(otlpIngestUrl)
            .build()

        val metricExporter: MetricExporter = OtlpGrpcMetricExporter.builder()
            .setEndpoint(otlpIngestUrl)
            .build()

        val metricReader = PeriodicMetricReader.builder(metricExporter)
            .setInterval(60, TimeUnit.SECONDS)
            .build()

        val meterProvider = SdkMeterProvider.builder()
            .setResource(Resource.getDefault()
            .toBuilder()
            .put(ResourceAttributes.SERVICE_NAME, "webex-sdk")
            .build())
            .registerMetricReader(metricReader)
            .build()

        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setResource(resource)
            .build()

        OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(meterProvider)
            .buildAndRegisterGlobal()

        tracer = GlobalOpenTelemetry.getTracer("Webex-SDK-Tracer")
        meter = GlobalOpenTelemetry.getMeter("Webex-SDK-Meter")
        //val span = tracer.spanBuilder("Webex-Start").startSpan()

        val otelRumBuilder: OpenTelemetryRumBuilder =
            OpenTelemetryRum.builder(this, config)
                .addSpanExporterCustomizer {
                    OtlpHttpSpanExporter.builder()
                        .setEndpoint(ingestUrl)
                        .build()
                }
        try {
            rum = otelRumBuilder.build()
            Log.i(TAG, "RUM session started: " + rum!!.rumSessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Oh no!", e)
        }

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
            "BSSID" -> wifiInfo?.bssid // Attribute?
            "RSSI" -> wifiInfo?.rssi?.toString() // Metric
            "LinkSpeed" -> wifiInfo?.linkSpeed?.toString() // Metric
            "IP" -> wifiInfo?.ipAddress?.toString() // deprecated API 31+
            //"TXLinkSpeed" -> wifiInfo?.txLinkSpeedMbps?.toString() // API 29+
            //"RXLinkSpeed" -> wifiInfo?.rxLinkSpeedMbps?.toString() // API 29+
            else -> null
        }
    }

    data class MemoryInfo(
        val totalMemory: Long,
        val freeMemory: Long,
        val usedMemoryPercentage: String,
        val lowMemory: Boolean,
        val cpuModel: String
    )
    fun getMemoryInfo(): MemoryInfo {
        val activityManager = applicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val cpuModel = Build.MODEL
        val usedMemoryPercentage = (1.0 - memoryInfo.availMem.toDouble() / memoryInfo.totalMem) * 100
        val formattedPercentage = String.format("%.2f", usedMemoryPercentage)

        return MemoryInfo(memoryInfo.totalMem / (1024 * 1024), memoryInfo.availMem / (1024 * 1024), formattedPercentage, memoryInfo.lowMemory, cpuModel)
    }

}