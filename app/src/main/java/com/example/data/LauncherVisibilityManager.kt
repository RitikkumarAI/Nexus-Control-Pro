package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object LauncherVisibilityManager {

    /**
     * Programmatically toggles the launcher icon visibility using PackageManager.setComponentEnabledSetting
     */
    fun setLauncherIconVisible(context: Context, visible: Boolean) {
        val pm = context.packageManager
        val aliasComponent = ComponentName(context, "com.example.MainActivityAlias")
        val mainComponent = ComponentName(context, "com.example.MainActivity")

        val newState = if (visible) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        try {
            pm.setComponentEnabledSetting(
                aliasComponent,
                newState,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for main component if alias fails
            try {
                pm.setComponentEnabledSetting(
                    mainComponent,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /**
     * Checks if the launcher component is currently enabled
     */
    fun isLauncherIconVisible(context: Context): Boolean {
        val pm = context.packageManager
        val aliasComponent = ComponentName(context, "com.example.MainActivityAlias")
        val state = pm.getComponentEnabledSetting(aliasComponent)
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
}
