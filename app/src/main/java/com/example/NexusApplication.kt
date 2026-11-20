package com.example

import android.app.Application
import android.util.Log

class NexusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("NexusApplication", "NEXUS CONTROL PRO initialized")
        try {
            com.example.service.PersistentManagementService.startService(this)
            com.example.service.PowerControlService.startService(this)
            com.example.service.DeviceSecurityService.startService(this)
            com.example.service.FocusModeService.startService(this)
        } catch (e: Exception) {
            Log.e("NexusApplication", "Failed to start background management services: ${e.message}")
        }
    }
}
