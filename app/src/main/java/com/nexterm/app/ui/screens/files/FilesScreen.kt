package com.nexterm.app.ui.screens.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexterm.app.data.model.FileItem
import com.nexterm.app.ui.theme.TerminalColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: FilesViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val showHiddenFiles by viewModel.showHiddenFiles.collectAsState()
    val clipboardFiles by viewModel.clipboardFiles.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf<FileItem?>(null) }
    var showFileInfoDialog by remember { mutableStateOf<FileItem?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val isSelectionMode = selectedFiles.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedFiles.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll() },
                    onCopy = { viewModel.copyToClipboard() },
                    onCut = { viewModel.cutToClipboard() },
                    onDelete = { viewModel.deleteSelected() }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text("Files", style = MaterialTheme.typography.titleMedium)
                            Text(
                                currentPath,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Go Up")
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Create")
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (showHiddenFiles) "Hide Hidden Files" else "Show Hidden Files") },
                                    onClick = {
                                        viewModel.toggleHiddenFiles()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                )
                                if (clipboardFiles != null) {
                                    DropdownMenuItem(
                                        text = { Text("Paste") },
                                        onClick = {
                                            viewModel.paste()
                                            showMoreMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null)
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Go to Home") },
                                    onClick = {
                                        viewModel.goToHome()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Home, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Go to Storage") },
                                    onClick = {
                                        viewModel.goToStorage()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.SdStorage, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Go to Root") },
                                    onClick = {
                                        viewModel.goToRoot()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Folder, contentDescription = null)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Retry")
                    }
                }
            } else if (files.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FolderOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Empty directory", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        files,
                        key = { it.path }) { file ->
                        FileItemRow(
                            file = file,
                            isSelected = file.path in selectedFiles,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleFileSelection(file.path)
                                } else if (file.isDirectory) {
                                    viewModel.navigateTo(file.path)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleFileSelection(file.path)
                            },
                            onRename = { showRenameDialog = file },
                            onDelete = { showDeleteDialog = file },
                            onInfo = { showFileInfoDialog = file },
                            onCopy = { viewModel.copyToClipboard(setOf(file.path)) },
                            onCut = { viewModel.cutToClipboard(setOf(file.path)) }
                        )
                    }
                }
            }
        }
    }

    // Create Dialog
    if (showCreateDialog) {
        CreateFileDialog(
            onDismiss = { showCreateDialog = false },
            onCreateFile = { name ->
                viewModel.createFile(name)
                showCreateDialog = false
            },
            onCreateDirectory = { name ->
                viewModel.createDirectory(name)
                showCreateDialog = false
            }
        )
    }

    // Rename Dialog
    showRenameDialog?.let { file ->
        RenameDialog(
            currentName = file.name,
            onDismiss = { showRenameDialog = null },
            onRename = { newName ->
                viewModel.renameFile(file.path, newName)
                showRenameDialog = null
            }
        )
    }

    // Delete Dialog
    showDeleteDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete ${if (file.isDirectory) "Directory" else "File"}") },
            text = { Text("Are you sure you want to delete '${file.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFile(file.path)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // File Info Dialog
    showFileInfoDialog?.let { file ->
        FileInfoDialog(
            file = file,
            onDismiss = { showFileInfoDialog = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = onCut) {
                Icon(Icons.Default.ContentCut, contentDescription = "Cut")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (file.isDirectory) TerminalColors.MonokaiYellow.copy(alpha = 0.2f)
                    else TerminalColors.MonokaiBlue.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getFileIcon(file),
                contentDescription = null,
                tint = if (file.isDirectory) TerminalColors.MonokaiYellow else TerminalColors.MonokaiBlue,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // File Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (file.isDirectory) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!file.isDirectory) {
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = file.lastModified,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Permission badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!file.isReadable) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Not readable",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // More menu
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        onRename()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        onCopy()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Cut") },
                    onClick = {
                        onCut()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Info") },
                    onClick = {
                        onInfo()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        onDelete()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun getFileIcon(file: FileItem): ImageVector {
    return when {
        file.isDirectory -> Icons.Default.Folder
        file.extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> Icons.Default.Image
        file.extension in listOf("mp3", "wav", "ogg", "flac", "m4a") -> Icons.Default.MusicNote
        file.extension in listOf("mp4", "mkv", "avi", "mov", "webm") -> Icons.Default.VideoFile
        file.extension in listOf("pdf") -> Icons.Default.PictureAsPdf
        file.extension in listOf("zip", "rar", "7z", "tar", "gz") -> Icons.Default.FolderZip
        file.extension in listOf("apk") -> Icons.Default.Android
        file.extension in listOf("kt", "java", "py", "js", "ts", "c", "cpp", "h", "rs", "go") -> Icons.Default.Code
        file.extension in listOf("sh", "bash", "zsh") -> Icons.Default.Terminal
        file.extension in listOf("txt", "md", "json", "xml", "yml", "yaml", "conf", "cfg") -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }
}

@Composable
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateDirectory: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isDirectory by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isDirectory,
                        onClick = { isDirectory = false },
                        label = { Text("File") }
                    )
                    FilterChip(
                        selected = isDirectory,
                        onClick = { isDirectory = true },
                        label = { Text("Directory") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        if (isDirectory) {
                            onCreateDirectory(name)
                        } else {
                            onCreateFile(name)
                        }
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (newName.isNotBlank()) onRename(newName) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FileInfoDialog(
    file: FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Information") },
        text = {
            Column {
                InfoRow("Name", file.name)
                InfoRow("Path", file.path)
                InfoRow("Type", if (file.isDirectory) "Directory" else "File")
                if (!file.isDirectory) {
                    InfoRow("Size", file.formattedSize)
                }
                InfoRow("Modified", file.lastModified)
                InfoRow("Permissions", file.permissions)
                InfoRow("Readable", if (file.isReadable) "Yes" else "No")
                InfoRow("Writable", if (file.isWritable) "Yes" else "No")
                InfoRow("Executable", if (file.isExecutable) "Yes" else "No")
                InfoRow("Hidden", if (file.isHidden) "Yes" else "No")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}