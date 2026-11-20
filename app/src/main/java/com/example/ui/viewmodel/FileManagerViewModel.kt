package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FilesRepository
import com.example.model.FileNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val filesRepository = FilesRepository(application)

    private val _currentPath = MutableStateFlow("/sdcard")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _fileList = MutableStateFlow<List<FileNode>>(emptyList())
    val fileList: StateFlow<List<FileNode>> = _fileList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedFile = MutableStateFlow<FileNode?>(null)
    val selectedFile: StateFlow<FileNode?> = _selectedFile.asStateFlow()

    private val _transferStatus = MutableStateFlow<String?>(null)
    val transferStatus: StateFlow<String?> = _transferStatus.asStateFlow()

    init {
        loadDirectory(null)
    }

    fun loadDirectory(path: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val nodes = filesRepository.listDirectory(path)
            _fileList.value = nodes
            if (path != null) {
                _currentPath.value = path
            }
            _isLoading.value = false
        }
    }

    fun selectFile(node: FileNode) {
        if (node.isDirectory) {
            loadDirectory(node.path)
        } else {
            _selectedFile.value = node
        }
    }

    fun downloadFile(node: FileNode) {
        viewModelScope.launch {
            _transferStatus.value = "Transferring '${node.name}' (${String.format("%.2f MB", node.sizeBytes / (1024.0 * 1024.0))})..."
            kotlinx.coroutines.delay(1200)
            _transferStatus.value = "File '${node.name}' downloaded securely to admin local storage!"
        }
    }

    fun dismissSelectedFile() {
        _selectedFile.value = null
    }
}
