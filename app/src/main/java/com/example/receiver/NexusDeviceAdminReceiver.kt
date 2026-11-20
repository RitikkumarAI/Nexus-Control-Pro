package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NexusDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("NexusDeviceAdminReceiver", "Android Enterprise Device Admin privileges ENABLED")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("NexusDeviceAdminReceiver", "Android Enterprise Device Admin privileges DISABLED")
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.d("NexusDeviceAdminReceiver", "Android Enterprise Profile Provisioning Complete")
    }
}
