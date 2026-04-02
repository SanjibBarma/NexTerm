package com.nexterm.app.ui.screens.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexterm.app.data.model.FileItem
import com.nexterm.app.data.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FilesViewModel(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _currentPath = MutableStateFlow(fileRepository.getHomeDirectory())
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(false)
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _clipboardFiles = MutableStateFlow<ClipboardData?>(null)
    val clipboardFiles: StateFlow<ClipboardData?> = _clipboardFiles.asStateFlow()

    private val pathHistory = mutableListOf<String>()
    private var historyIndex = -1

    init {
        loadFiles()
    }

    fun loadFiles(path: String = _currentPath.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fileRepository.listFiles(path).fold(
                onSuccess = { fileList ->
                    val filteredFiles = if (_showHiddenFiles.value) {
                        fileList
                    } else {
                        fileList.filter { !it.isHidden }
                    }
                    _files.value = filteredFiles
                    _currentPath.value = path

                    // Update history
                    if (historyIndex == -1 || pathHistory.getOrNull(historyIndex) != path) {
                        if (historyIndex < pathHistory.lastIndex) {
                            pathHistory.subList(historyIndex + 1, pathHistory.size).clear()
                        }
                        pathHistory.add(path)
                        historyIndex = pathHistory.lastIndex
                    }
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to load files"
                }
            )

            _isLoading.value = false
        }
    }

    fun navigateTo(path: String) {
        _selectedFiles.value = emptySet()
        loadFiles(path)
    }

    fun navigateUp() {
        val parentPath = File(_currentPath.value).parent
        if (parentPath != null) {
            navigateTo(parentPath)
        }
    }

    fun navigateBack(): Boolean {
        if (historyIndex > 0) {
            historyIndex--
            val previousPath = pathHistory[historyIndex]
            _selectedFiles.value = emptySet()
            loadFiles(previousPath)
            return true
        }
        return false
    }

    fun navigateForward(): Boolean {
        if (historyIndex < pathHistory.lastIndex) {
            historyIndex++
            val nextPath = pathHistory[historyIndex]
            _selectedFiles.value = emptySet()
            loadFiles(nextPath)
            return true
        }
        return false
    }

    fun refresh() {
        loadFiles()
    }

    fun toggleHiddenFiles() {
        _showHiddenFiles.value = !_showHiddenFiles.value
        loadFiles()
    }

    fun selectFile(path: String) {
        _selectedFiles.value = _selectedFiles.value + path
    }

    fun deselectFile(path: String) {
        _selectedFiles.value = _selectedFiles.value - path
    }

    fun toggleFileSelection(path: String) {
        if (path in _selectedFiles.value) {
            deselectFile(path)
        } else {
            selectFile(path)
        }
    }

    fun selectAll() {
        _selectedFiles.value = _files.value.map { it.path }.toSet()
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            fileRepository.createFile(_currentPath.value, name).fold(
                onSuccess = { loadFiles() },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun createDirectory(name: String) {
        viewModelScope.launch {
            fileRepository.createDirectory(_currentPath.value, name).fold(
                onSuccess = { loadFiles() },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _selectedFiles.value.forEach { path ->
                fileRepository.deleteFile(path)
            }
            clearSelection()
            loadFiles()
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            fileRepository.deleteFile(path).fold(
                onSuccess = { loadFiles() },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun renameFile(oldPath: String, newName: String) {
        viewModelScope.launch {
            fileRepository.renameFile(oldPath, newName).fold(
                onSuccess = { loadFiles() },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun copyToClipboard(paths: Set<String> = _selectedFiles.value) {
        _clipboardFiles.value = ClipboardData(paths.toList(), ClipboardOperation.COPY)
        clearSelection()
    }

    fun cutToClipboard(paths: Set<String> = _selectedFiles.value) {
        _clipboardFiles.value = ClipboardData(paths.toList(), ClipboardOperation.CUT)
        clearSelection()
    }

    fun paste() {
        viewModelScope.launch {
            _clipboardFiles.value?.let { clipboard ->
                clipboard.paths.forEach { sourcePath ->
                    val fileName = File(sourcePath).name
                    val destPath = "${_currentPath.value}/$fileName"

                    when (clipboard.operation) {
                        ClipboardOperation.COPY -> {
                            fileRepository.copyFile(sourcePath, destPath)
                        }
                        ClipboardOperation.CUT -> {
                            fileRepository.moveFile(sourcePath, destPath)
                        }
                    }
                }

                if (clipboard.operation == ClipboardOperation.CUT) {
                    _clipboardFiles.value = null
                }

                loadFiles()
            }
        }
    }

    fun readFile(path: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = fileRepository.readFile(path)
            onResult(result)
        }
    }

    fun writeFile(path: String, content: String) {
        viewModelScope.launch {
            fileRepository.writeFile(path, content).fold(
                onSuccess = { loadFiles() },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun getFileInfo(path: String, onResult: (Result<FileItem>) -> Unit) {
        viewModelScope.launch {
            val result = fileRepository.getFileInfo(path)
            onResult(result)
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun goToHome() {
        navigateTo(fileRepository.getHomeDirectory())
    }

    fun goToStorage() {
        navigateTo(fileRepository.getStorageDirectory())
    }

    fun goToRoot() {
        navigateTo("/")
    }
}

data class ClipboardData(
    val paths: List<String>,
    val operation: ClipboardOperation
)

enum class ClipboardOperation {
    COPY, CUT
}