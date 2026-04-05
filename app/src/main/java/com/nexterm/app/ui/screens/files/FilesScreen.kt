package com.nexterm.app.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val hackerGreen = TerminalColors.MonokaiGreen
    val hackerBlack = Color.Black

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FILE_EXPLORER", style = MaterialTheme.typography.titleMedium, color = hackerGreen, fontFamily = FontFamily.Monospace)
                        Text(currentPath, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontFamily = FontFamily.Monospace, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null, tint = hackerGreen) }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Default.Refresh, null, tint = hackerGreen) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = hackerBlack)
            )
        },
        containerColor = hackerBlack
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = hackerGreen)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    if (currentPath != "/" && currentPath != "/data/user/0/com.nexterm.app.debug/files") {
                        item {
                            FileListItem(name = "..", isDirectory = true, isParent = true, hackerGreen = hackerGreen) {
                                viewModel.navigateUp()
                            }
                        }
                    }
                    items(files) { file ->
                        FileListItem(name = file.name, isDirectory = file.isDirectory, isParent = false, hackerGreen = hackerGreen) {
                            if (file.isDirectory) viewModel.navigateTo(file.path)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileListItem(name: String, isDirectory: Boolean, isParent: Boolean, hackerGreen: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        color = Color(0xFF0A0A0A),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDirectory) hackerGreen.copy(0.4f) else Color.DarkGray)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    isParent -> Icons.Default.DriveFileMove
                    isDirectory -> Icons.Default.Folder
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                tint = if (isDirectory) hackerGreen else Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = name,
                color = if (isDirectory) hackerGreen else Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = if (isDirectory) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
