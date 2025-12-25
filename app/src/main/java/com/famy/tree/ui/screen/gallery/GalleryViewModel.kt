package com.famy.tree.ui.screen.gallery

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Media
import com.famy.tree.domain.model.MediaKind
import com.famy.tree.domain.model.MediaWithMember
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryUiState(
    val isLoading: Boolean = true,
    val mediaItems: List<MediaWithMember> = emptyList(),
    val filteredMedia: List<MediaWithMember> = emptyList(),
    val selectedFilter: MediaKind? = null,
    val searchQuery: String = "",
    val selectedMemberId: Long? = null,
    val members: List<FamilyMember> = emptyList(),
    val totalSize: String = "0 B",
    val totalCount: Int = 0,
    val selectedMedia: Media? = null,
    val showMediaViewer: Boolean = false,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val viewMode: GalleryViewMode = GalleryViewMode.GRID
)

enum class GalleryViewMode {
    GRID,
    LIST
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val memberRepository: FamilyMemberRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val treeId: Long = savedStateHandle.get<Long>("treeId") ?: 0L

    // Filter state - separated to avoid unnecessary recomputations
    private val _selectedFilter = MutableStateFlow<MediaKind?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedMemberId = MutableStateFlow<Long?>(null)
    private val _viewMode = MutableStateFlow(GalleryViewMode.GRID)
    private val _selectedMedia = MutableStateFlow<Media?>(null)
    private val _isDeleting = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Reactive uiState using Flow.combine - no blocking calls
    val uiState: StateFlow<GalleryUiState> = combine(
        mediaRepository.observeMediaByTree(treeId),
        memberRepository.observeMembersByTree(treeId),
        _selectedFilter,
        _searchQuery,
        _selectedMemberId,
        _viewMode
    ) { mediaList, members, filter, searchQuery, memberId, viewMode ->
        val membersMap = members.associateBy { it.id }
        val mediaWithMembers = mediaList.mapNotNull { media ->
            val member = membersMap[media.memberId]
            if (member != null) MediaWithMember(media, member) else null
        }.sortedByDescending { it.media.createdAt }

        val filtered = filterMediaInternal(mediaWithMembers, filter, searchQuery, memberId)
        val totalSize = formatSize(mediaList.sumOf { it.fileSize })

        GalleryUiState(
            isLoading = false,
            mediaItems = mediaWithMembers,
            filteredMedia = filtered,
            selectedFilter = filter,
            searchQuery = searchQuery,
            selectedMemberId = memberId,
            members = members,
            totalSize = totalSize,
            totalCount = mediaList.size,
            selectedMedia = _selectedMedia.value,
            showMediaViewer = _selectedMedia.value != null,
            error = _error.value,
            isDeleting = _isDeleting.value,
            viewMode = viewMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState()
    )

    fun setFilter(filter: MediaKind?) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMemberFilter(memberId: Long?) {
        _selectedMemberId.value = memberId
    }

    fun setViewMode(mode: GalleryViewMode) {
        _viewMode.value = mode
    }

    fun selectMedia(media: Media?) {
        _selectedMedia.value = media
    }

    fun dismissMediaViewer() {
        _selectedMedia.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearFilters() {
        _selectedFilter.value = null
        _searchQuery.value = ""
        _selectedMemberId.value = null
    }

    val hasActiveFilters: Boolean
        get() = _selectedFilter.value != null ||
                _searchQuery.value.isNotBlank() ||
                _selectedMemberId.value != null

    fun deleteMedia(mediaId: Long) {
        viewModelScope.launch {
            try {
                _isDeleting.value = true
                mediaRepository.deleteMedia(mediaId)
                _isDeleting.value = false
                _selectedMedia.value = null
            } catch (e: Exception) {
                _isDeleting.value = false
                _error.value = e.message ?: "Failed to delete media"
            }
        }
    }

    fun addMedia(
        memberId: Long,
        uri: Uri,
        title: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fileName = getFileName(uri) ?: "file_${System.currentTimeMillis()}"
                    val mimeType = context.contentResolver.getType(uri)

                    mediaRepository.addMedia(
                        memberId = memberId,
                        inputStream = inputStream,
                        fileName = fileName,
                        mimeType = mimeType,
                        title = title,
                        description = description
                    )
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add media"
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (nameIndex >= 0) cursor.getString(nameIndex) else null
        }
    }

    private fun filterMediaInternal(
        items: List<MediaWithMember>,
        filter: MediaKind?,
        searchQuery: String,
        memberId: Long?
    ): List<MediaWithMember> {
        return items.filter { item ->
            val matchesFilter = filter == null || item.media.type == filter
            val matchesMember = memberId == null || item.media.memberId == memberId
            val matchesSearch = searchQuery.isBlank() ||
                    item.media.title?.contains(searchQuery, ignoreCase = true) == true ||
                    item.media.description?.contains(searchQuery, ignoreCase = true) == true ||
                    item.member.fullName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesMember && matchesSearch
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes.toDouble() / (1024 * 1024 * 1024))
        }
    }

    fun getMediaTypeStats(): Map<MediaKind, Int> {
        return uiState.value.mediaItems.groupBy { it.media.type }
            .mapValues { it.value.size }
    }
}
