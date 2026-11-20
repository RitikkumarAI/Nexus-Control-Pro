package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.NexusDevice
import com.example.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nexus Control Pro", appName)
    }

    @Test
    fun `verify nexus device model defaults`() {
        val device = NexusDevice(
            deviceName = "Phone A",
            role = UserRole.MANAGED.name
        )
        assertNotNull(device)
        assertEquals("Phone A", device.deviceName)
        assertEquals("MANAGED", device.role)
    }
}
