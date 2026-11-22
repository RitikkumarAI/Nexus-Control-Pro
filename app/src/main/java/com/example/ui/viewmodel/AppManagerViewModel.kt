package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppsRepository
import com.example.data.FirestoreRepository
import com.example.model.AppItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val appsRepository = AppsRepository(application)
    private val firestoreRepository = FirestoreRepository(application)

    private val _appList = MutableStateFlow<List<AppItem>>(emptyList())
    val appList: StateFlow<List<AppItem>> = _appList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    init {
        loadApplications()
    }

    fun loadApplications(targetDeviceId: String? = null, targetIp: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            if (!targetIp.isNullOrBlank()) {
                val p2p = com.example.data.NexusP2PManager.getInstance(getApplication())
                val result = p2p.fetchRemoteApps(targetIp)
                if (result.isSuccess && result.getOrThrow().isNotEmpty()) {
                    _appList.value = result.getOrThrow()
                    _isLoading.value = false
                    return@launch
                }
            }

            _hasUsageAccess.value = appsRepository.hasUsageAccessPermission()
            val apps = appsRepository.getInstalledApplications()
            _appList.value = apps
            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredApps(): List<AppItem> {
        val query = _searchQuery.value.trim().lowercase()
        if (query.isEmpty()) return _appList.value
        return _appList.value.filter {
            it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query)
        }
    }

    fun toggleRemoteAppPermission(targetDeviceId: String, packageName: String, permission: String, grant: Boolean) {
        viewModelScope.launch {
            firestoreRepository.sendPermissionCommand(targetDeviceId, packageName, permission, grant)
            // Optimistically update local list state
            _appList.value = _appList.value.map { app ->
                if (app.packageName == packageName) {
                    val updatedPerms = app.sensitivePermissions.map { perm ->
                        if (perm.permissionKey == permission) {
                            perm.copy(isGranted = grant)
                        } else perm
                    }
                    app.copy(sensitivePermissions = updatedPerms)
                } else app
            }
        }
    }
}
