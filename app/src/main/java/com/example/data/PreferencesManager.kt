package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "nexus_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_ROLE = stringPreferencesKey("user_role")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        private val KEY_PAIRED_DEVICE_ID = stringPreferencesKey("paired_device_id")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "DARK"
    }

    val roleFlow: Flow<UserRole> = context.dataStore.data.map { prefs ->
        val roleStr = prefs[KEY_ROLE] ?: UserRole.UNSET.name
        runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.UNSET)
    }

    val deviceIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVICE_ID] ?: UUID.randomUUID().toString()
    }

    val deviceNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVICE_NAME] ?: "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}"
    }

    val pairedDeviceIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PAIRED_DEVICE_ID]
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun saveRole(role: UserRole) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ROLE] = role.name
        }
    }

    suspend fun saveDeviceId(deviceId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_ID] = deviceId
        }
    }

    suspend fun saveDeviceName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_NAME] = name
        }
    }

    suspend fun savePairedDeviceId(deviceId: String?) {
        context.dataStore.edit { prefs ->
            if (deviceId == null) {
                prefs.remove(KEY_PAIRED_DEVICE_ID)
            } else {
                prefs[KEY_PAIRED_DEVICE_ID] = deviceId
            }
        }
    }
}
