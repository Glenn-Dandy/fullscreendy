package de.kewl.fullscreendy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DASHBOARD_URL = stringPreferencesKey("dashboard_url")
        val MQTT_HOST = stringPreferencesKey("mqtt_host")
        val MQTT_PORT = intPreferencesKey("mqtt_port")
        val MQTT_TLS = booleanPreferencesKey("mqtt_tls")
        val MQTT_USER = stringPreferencesKey("mqtt_user")
        val MQTT_PASS = stringPreferencesKey("mqtt_pass")
        val BASE_TOPIC = stringPreferencesKey("base_topic")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val MOTION_ENABLED = booleanPreferencesKey("motion_enabled")
        val MOTION_WAKES = booleanPreferencesKey("motion_wakes_screen")
        val MOTION_SENS = intPreferencesKey("motion_sensitivity")
        val SOUND_WAKE = booleanPreferencesKey("sound_wake_enabled")
        val SOUND_SENS = intPreferencesKey("sound_sensitivity")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DIM_TIMEOUT = intPreferencesKey("dim_timeout_secs")
        val SCREEN_OFF = intPreferencesKey("screen_off_secs")
        val IGNORE_FONT_SCALE = booleanPreferencesKey("ignore_font_scale")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val MEDIA_ENABLED = booleanPreferencesKey("media_enabled")
        val ZOOM_ENABLED = booleanPreferencesKey("zoom_enabled")
        val PULL_TO_REFRESH = booleanPreferencesKey("pull_to_refresh")
        val RELOAD_INTERVAL = intPreferencesKey("reload_interval_mins")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val LANGUAGE = stringPreferencesKey("language")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val defaults = Settings()
        Settings(
            dashboardUrl = p[Keys.DASHBOARD_URL] ?: defaults.dashboardUrl,
            mqttHost = p[Keys.MQTT_HOST] ?: defaults.mqttHost,
            mqttPort = p[Keys.MQTT_PORT] ?: defaults.mqttPort,
            mqttTls = p[Keys.MQTT_TLS] ?: defaults.mqttTls,
            mqttUser = p[Keys.MQTT_USER] ?: defaults.mqttUser,
            mqttPass = p[Keys.MQTT_PASS] ?: defaults.mqttPass,
            baseTopic = p[Keys.BASE_TOPIC] ?: defaults.baseTopic,
            deviceId = p[Keys.DEVICE_ID] ?: defaults.deviceId,
            motionEnabled = p[Keys.MOTION_ENABLED] ?: defaults.motionEnabled,
            motionWakesScreen = p[Keys.MOTION_WAKES] ?: defaults.motionWakesScreen,
            motionSensitivity = p[Keys.MOTION_SENS] ?: defaults.motionSensitivity,
            soundWakeEnabled = p[Keys.SOUND_WAKE] ?: defaults.soundWakeEnabled,
            soundSensitivity = p[Keys.SOUND_SENS] ?: defaults.soundSensitivity,
            keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
            dimTimeoutSecs = p[Keys.DIM_TIMEOUT] ?: defaults.dimTimeoutSecs,
            screenOffSecs = p[Keys.SCREEN_OFF] ?: defaults.screenOffSecs,
            ignoreSystemFontScale = p[Keys.IGNORE_FONT_SCALE] ?: defaults.ignoreSystemFontScale,
            ttsEnabled = p[Keys.TTS_ENABLED] ?: defaults.ttsEnabled,
            mediaEnabled = p[Keys.MEDIA_ENABLED] ?: defaults.mediaEnabled,
            zoomEnabled = p[Keys.ZOOM_ENABLED] ?: defaults.zoomEnabled,
            pullToRefresh = p[Keys.PULL_TO_REFRESH] ?: defaults.pullToRefresh,
            reloadIntervalMins = p[Keys.RELOAD_INTERVAL] ?: defaults.reloadIntervalMins,
            startOnBoot = p[Keys.START_ON_BOOT] ?: defaults.startOnBoot,
            language = p[Keys.LANGUAGE] ?: defaults.language,
            adminPin = p[Keys.ADMIN_PIN] ?: defaults.adminPin,
        )
    }

    suspend fun save(s: Settings) {
        context.dataStore.edit { p ->
            p[Keys.DASHBOARD_URL] = s.dashboardUrl
            p[Keys.MQTT_HOST] = s.mqttHost
            p[Keys.MQTT_PORT] = s.mqttPort
            p[Keys.MQTT_TLS] = s.mqttTls
            p[Keys.MQTT_USER] = s.mqttUser
            p[Keys.MQTT_PASS] = s.mqttPass
            p[Keys.BASE_TOPIC] = s.baseTopic
            p[Keys.DEVICE_ID] = s.deviceId
            p[Keys.MOTION_ENABLED] = s.motionEnabled
            p[Keys.MOTION_WAKES] = s.motionWakesScreen
            p[Keys.MOTION_SENS] = s.motionSensitivity
            p[Keys.SOUND_WAKE] = s.soundWakeEnabled
            p[Keys.SOUND_SENS] = s.soundSensitivity
            p[Keys.KEEP_SCREEN_ON] = s.keepScreenOn
            p[Keys.DIM_TIMEOUT] = s.dimTimeoutSecs
            p[Keys.SCREEN_OFF] = s.screenOffSecs
            p[Keys.IGNORE_FONT_SCALE] = s.ignoreSystemFontScale
            p[Keys.TTS_ENABLED] = s.ttsEnabled
            p[Keys.MEDIA_ENABLED] = s.mediaEnabled
            p[Keys.ZOOM_ENABLED] = s.zoomEnabled
            p[Keys.PULL_TO_REFRESH] = s.pullToRefresh
            p[Keys.RELOAD_INTERVAL] = s.reloadIntervalMins
            p[Keys.START_ON_BOOT] = s.startOnBoot
            p[Keys.LANGUAGE] = s.language
            p[Keys.ADMIN_PIN] = s.adminPin
        }
    }
}
