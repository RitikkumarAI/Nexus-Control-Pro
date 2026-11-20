package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FilesRepository
import com.example.model.MediaItemData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val filesRepository = FilesRepository(application)

    private val _mediaList = MutableStateFlow<List<MediaItemData>>(emptyList())
    val mediaList: StateFlow<List<MediaItemData>> = _mediaList.asStateFlow()

    private val _filterType = MutableStateFlow("ALL") // ALL, IMAGES, VIDEOS
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedMedia = MutableStateFlow<MediaItemData?>(null)
    val selectedMedia: StateFlow<MediaItemData?> = _selectedMedia.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            val media = filesRepository.getMediaItems()
            _mediaList.value = media
            _isLoading.value = false
        }
    }

    fun setFilter(filter: String) {
        _filterType.value = filter
    }

    fun selectMedia(item: MediaItemData?) {
        _selectedMedia.value = item
    }

    fun getFilteredMedia(): List<MediaItemData> {
        return when (_filterType.value) {
            "IMAGES" -> _mediaList.value.filter { !it.isVideo }
            "VIDEOS" -> _mediaList.value.filter { it.isVideo }
            else -> _mediaList.value
        }
    }
}
