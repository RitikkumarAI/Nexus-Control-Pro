package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

class LauncherVisibilityHelper(private val context: Context) {

    private val aliasComponent = ComponentName(context, "com.example.MainActivityAlias")
    private val mainComponent = ComponentName(context, "com.example.MainActivity")
    private val packageManager: PackageManager = context.packageManager

    /**
     * Helper to toggle the component state (ENABLED / DISABLED) of the launcher activity.
     * Removes or restores the app icon from the system launcher without stopping background services.
     */
    fun setLauncherIconHidden(hideIcon: Boolean) {
        val newState = if (hideIcon) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

        try {
            packageManager.setComponentEnabledSetting(
                aliasComponent,
                newState,
                PackageManager.DONT_KILL_APP
            )
            Log.d("LauncherVisibilityHelper", "Launcher alias state changed to hideIcon=$hideIcon")
        } catch (e: Exception) {
            Log.e("LauncherVisibilityHelper", "Failed to update alias component: ${e.message}")
            try {
                packageManager.setComponentEnabledSetting(
                    mainComponent,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            } catch (ex: Exception) {
                Log.e("LauncherVisibilityHelper", "Failed to update main component: ${ex.message}")
            }
        }
    }

    /**
     * Checks if the app icon is currently hidden in the launcher.
     */
    fun isLauncherIconHidden(): Boolean {
        val state = packageManager.getComponentEnabledSetting(aliasComponent)
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
}
