package com.nexterm.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexterm.app.ui.theme.TerminalColorScheme
import com.nexterm.app.ui.theme.TerminalThemes
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showScrollbackDialog by remember { mutableStateOf(false) }
    var showExtraKeysDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Appearance Section
            item {
                SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Palette)
            }

            item {
                SettingsCard {
                    // Font Size
                    SettingsItem(
                        title = "Font Size",
                        subtitle = "${settings.fontSize}sp",
                        icon = Icons.Default.FormatSize,
                        onClick = { showFontSizeDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Color Scheme
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Color Scheme",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(TerminalThemes.all) { theme ->
                                ColorSchemeChip(
                                    theme = theme,
                                    isSelected = settings.colorScheme == theme.name.lowercase(),
                                    onClick = {
                                        viewModel.updateColorScheme(theme.name.lowercase())
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Terminal Section
            item {
                SettingsSectionHeader(title = "Terminal", icon = Icons.Default.Terminal)
            }

            item {
                SettingsCard {
                    // Cursor Blink
                    SettingsSwitchItem(
                        title = "Cursor Blink",
                        subtitle = "Animate the cursor",
                        icon = Icons.Default.Animation,
                        checked = settings.cursorBlink,
                        onCheckedChange = { viewModel.updateCursorBlink(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Scrollback Lines
                    SettingsItem(
                        title = "Scrollback Buffer",
                        subtitle = "${settings.scrollbackLines} lines",
                        icon = Icons.Default.History,
                        onClick = { showScrollbackDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Keep Screen On
                    SettingsSwitchItem(
                        title = "Keep Screen On",
                        subtitle = "Prevent screen from sleeping",
                        icon = Icons.Default.Visibility,
                        checked = settings.keepScreenOn,
                        onCheckedChange = { viewModel.updateKeepScreenOn(it) }
                    )
                }
            }

            // Extra Keys Section
            item {
                SettingsSectionHeader(title = "Extra Keys", icon = Icons.Default.Keyboard)
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Extra Keys Bar",
                        subtitle = "Show additional keys above keyboard",
                        icon = Icons.Default.KeyboardArrowUp,
                        checked = settings.extraKeysEnabled,
                        onCheckedChange = { viewModel.updateExtraKeysEnabled(it) }
                    )

                    if (settings.extraKeysEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            title = "Configure Keys",
                            subtitle = "Customize extra keys",
                            icon = Icons.Default.Settings,
                            onClick = { showExtraKeysDialog = true }
                        )
                    }
                }
            }

            // About Section
            item {
                SettingsSectionHeader(title = "About", icon = Icons.Default.Info)
            }

            item {
                SettingsCard {
                    SettingsItem(
                        title = "Version",
                        subtitle = "1.0.0",
                        icon = Icons.Default.Verified,
                        onClick = { }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsItem(
                        title = "NexTerm",
                        subtitle = "Professional Terminal Emulator",
                        icon = Icons.Default.Code,
                        onClick = { }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Font Size Dialog
    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = settings.fontSize,
            onDismiss = { showFontSizeDialog = false },
            onConfirm = { size ->
                viewModel.updateFontSize(size)
                showFontSizeDialog = false
            }
        )
    }

    // Scrollback Dialog
    if (showScrollbackDialog) {
        ScrollbackDialog(
            currentLines = settings.scrollbackLines,
            onDismiss = { showScrollbackDialog = false },
            onConfirm = { lines ->
                viewModel.updateScrollbackLines(lines)
                showScrollbackDialog = false
            }
        )
    }

    // Extra Keys Dialog
    if (showExtraKeysDialog) {
        ExtraKeysDialog(
            currentKeys = settings.extraKeysRow,
            onDismiss = { showExtraKeysDialog = false },
            onConfirm = { keys ->
                viewModel.updateExtraKeysRow(keys)
                showExtraKeysDialog = false
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun ColorSchemeChip(
    theme: TerminalColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(theme.background)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                theme.red,
                theme.green,
                theme.yellow,
                theme.blue
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = theme.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FontSizeDialog(
    currentSize: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var size by remember { mutableFloatStateOf(currentSize.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Font Size") },
        text = {
            Column {
                Text(
                    "Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF272822))
                        .padding(12.dp)
                ) {
                    Text(
                        "$ echo \"Hello, World!\"",
                        fontFamily = FontFamily.Monospace,
                        fontSize = size.sp,
                        color = Color(0xFFA6E22E)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "${size.toInt()}sp",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Slider(
                    value = size,
                    onValueChange = { size = it },
                    valueRange = 8f..24f,
                    steps = 15
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(size.toInt()) }) {
                Text("OK")
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
fun ScrollbackDialog(
    currentLines: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val options = listOf(1000, 5000, 10000, 25000, 50000)
    var selected by remember { mutableIntStateOf(currentLines) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scrollback Buffer") },
        text = {
            Column {
                Text(
                    "Number of lines to keep in scrollback",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                options.forEach { lines ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = lines }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == lines,
                            onClick = { selected = lines }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$lines lines")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("OK")
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
fun ExtraKeysDialog(
    currentKeys: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var keys by remember { mutableStateOf(currentKeys) }

    val availableKeys = listOf(
        "ESC", "TAB", "CTRL", "ALT", "FN",
        "HOME", "END", "PGUP", "PGDN",
        "UP", "DOWN", "LEFT", "RIGHT",
        "DEL", "INS"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Extra Keys") },
        text = {
            Column {
                Text(
                    "Enter keys separated by |",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keys,
                    onValueChange = { keys = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Available Keys:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableKeys.forEach { key ->
                        SuggestionChip(
                            onClick = {
                                keys = if (keys.isEmpty()) key else "$keys|$key"
                            },
                            label = { Text(key, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(keys) }) {
                Text("OK")
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
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}