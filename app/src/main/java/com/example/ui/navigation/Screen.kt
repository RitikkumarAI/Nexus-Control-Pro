package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object RoleSelection : Screen("role_selection")
    object Pairing : Screen("pairing")
    object AdminDashboard : Screen("admin_dashboard")
    object ManagedDashboard : Screen("managed_dashboard")
    object DeviceDetails : Screen("device_details")
    object ScreenShare : Screen("screen_share")
    object AppManager : Screen("app_manager")
    object FileManager : Screen("file_manager")
    object Gallery : Screen("gallery")
    object Location : Screen("location")
    object CommLog : Screen("comm_log")
    object PermissionDashboard : Screen("permission_dashboard")
    object SecuritySettings : Screen("security_settings")
    object EnterpriseAdmin : Screen("enterprise_admin")
    object AuditLog : Screen("audit_log")
    object RemoteTerminal : Screen("remote_terminal")
    object ManagedDevices : Screen("managed_devices")
    object RemoteLogcat : Screen("remote_logcat")
}
