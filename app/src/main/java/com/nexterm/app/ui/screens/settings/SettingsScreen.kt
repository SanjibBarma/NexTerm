package com.nexterm.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexterm.app.ui.theme.TerminalColorScheme
import com.nexterm.app.ui.theme.TerminalThemes
import com.nexterm.app.ui.theme.TerminalColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val hackerGreen = TerminalColors.MonokaiGreen
    val hackerBlack = Color.Black

    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showScrollbackDialog by remember { mutableStateOf(false) }
    var showExtraKeysDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SYS_CONFIGURATION", color = hackerGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null, tint = hackerGreen) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = hackerBlack)
            )
        },
        containerColor = hackerBlack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HackerSettingsHeader("APPEARANCE_MODULE", Icons.Default.Palette, hackerGreen) }
            item {
                HackerSettingsCard(hackerGreen) {
                    HackerSettingsItem("Font Size", "${settings.fontSize}sp", Icons.Default.FormatSize, hackerGreen) { showFontSizeDialog = true }
                    HorizontalDivider(color = hackerGreen.copy(0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    Column(Modifier.padding(16.dp)) {
                        Text("COLOR_SCHEME_IDENTIFIER", color = hackerGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TerminalThemes.all) { theme ->
                                ColorSchemeChip(theme, settings.colorScheme.trim().lowercase() == theme.key) { viewModel.updateColorScheme(theme.key) }
                            }
                        }
                    }
                }
            }

            item { HackerSettingsHeader("CORE_ENGINE_PREFS", Icons.Default.Terminal, hackerGreen) }
            item {
                HackerSettingsCard(hackerGreen) {
                    HackerSettingsSwitch("Cursor Blink", "ANIM_CUR", Icons.Default.Animation, settings.cursorBlink, hackerGreen) { viewModel.updateCursorBlink(it) }
                    HorizontalDivider(color = hackerGreen.copy(0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    HackerSettingsItem("Scrollback Buffer", "${settings.scrollbackLines} lines", Icons.Default.History, hackerGreen) { showScrollbackDialog = true }
                    HorizontalDivider(color = hackerGreen.copy(0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    HackerSettingsSwitch("Keep Screen On", "PWR_MGMT", Icons.Default.Visibility, settings.keepScreenOn, hackerGreen) { viewModel.updateKeepScreenOn(it) }
                }
            }

            item { HackerSettingsHeader("INPUT_INTERFACE", Icons.Default.Keyboard, hackerGreen) }
            item {
                HackerSettingsCard(hackerGreen) {
                    HackerSettingsSwitch("Extra Keys Bar", "UI_EXT", Icons.Default.KeyboardArrowUp, settings.extraKeysEnabled, hackerGreen) { viewModel.updateExtraKeysEnabled(it) }
                    if (settings.extraKeysEnabled) {
                        HorizontalDivider(color = hackerGreen.copy(0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                        HackerSettingsItem("Configure Mapping", "KEY_MAP", Icons.Default.Settings, hackerGreen) { showExtraKeysDialog = true }
                    }
                }
            }

            item { HackerSettingsHeader("SYSTEM_INFO", Icons.Default.Info, hackerGreen) }
            item {
                HackerSettingsCard(hackerGreen) {
                    HackerSettingsItem("NexTerm Build", "1.0.0-STABLE", Icons.Default.Verified, hackerGreen) { }
                    HorizontalDivider(color = hackerGreen.copy(0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    HackerSettingsItem("Status", "ANONYMOUS_ENABLED", Icons.Default.Code, hackerGreen) { }
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showFontSizeDialog) {
        FontSizeDialog(settings.fontSize, { showFontSizeDialog = false }) { viewModel.updateFontSize(it); showFontSizeDialog = false }
    }
    if (showScrollbackDialog) {
        ScrollbackDialog(settings.scrollbackLines, { showScrollbackDialog = false }) { viewModel.updateScrollbackLines(it); showScrollbackDialog = false }
    }
    if (showExtraKeysDialog) {
        ExtraKeysDialog(settings.extraKeysRow, { showExtraKeysDialog = false }) { viewModel.updateExtraKeysRow(it); showExtraKeysDialog = false }
    }
}

@Composable
fun HackerSettingsHeader(title: String, icon: ImageVector, color: Color) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = color, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HackerSettingsCard(color: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().border(0.5.dp, color.copy(0.2f), RoundedCornerShape(12.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF080808))) {
        Column { content() }
    }
}

@Composable
fun HackerSettingsItem(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color.copy(0.7f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(subtitle, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.DarkGray)
    }
}

@Composable
fun HackerSettingsSwitch(title: String, code: String, icon: ImageVector, checked: Boolean, color: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color.copy(0.7f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text("TAG: $code", color = color.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = color, checkedTrackColor = color.copy(0.3f), uncheckedThumbColor = Color.DarkGray))
    }
}

@Composable
fun ColorSchemeChip(theme: TerminalColorScheme, isSelected: Boolean, onClick: () -> Unit) {
    val hackerGreen = TerminalColors.MonokaiGreen
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).border(if (isSelected) 1.dp else 0.dp, hackerGreen, RoundedCornerShape(8.dp)).background(if (isSelected) Color(0xFF0F0F0F) else Color.Transparent).padding(8.dp)) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(theme.background).border(0.5.dp, Color.Gray, RoundedCornerShape(4.dp)))
        Spacer(Modifier.height(4.dp))
        Text(theme.name.uppercase(), color = if (isSelected) hackerGreen else Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun FontSizeDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var size by remember { mutableFloatStateOf(current.toFloat()) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color.Black, modifier = Modifier.border(1.dp, TerminalColors.MonokaiGreen, RoundedCornerShape(28.dp)),
        title = { Text("FONT_SIZE_PX", color = TerminalColors.MonokaiGreen, fontFamily = FontFamily.Monospace) },
        text = {
            Column {
                Text("PREVIEW:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("echo 'Hello Operator'", color = Color.White, fontSize = size.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 12.dp))
                Slider(value = size, onValueChange = { size = it }, valueRange = 10f..24f, colors = SliderDefaults.colors(thumbColor = TerminalColors.MonokaiGreen, activeTrackColor = TerminalColors.MonokaiGreen))
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(size.toInt()) }) { Text("SET", color = TerminalColors.MonokaiGreen, fontFamily = FontFamily.Monospace) } }
    )
}

@Composable
fun ScrollbackDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val options = listOf(1000, 5000, 10000, 25000)
    var selected by remember { mutableIntStateOf(current) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color.Black, modifier = Modifier.border(1.dp, TerminalColors.MonokaiGreen, RoundedCornerShape(28.dp)),
        title = { Text("BUFFER_CAPACITY", color = TerminalColors.MonokaiGreen, fontFamily = FontFamily.Monospace) },
        text = {
            Column {
                options.forEach { opt ->
                    Row(Modifier.fillMaxWidth().clickable { selected = opt }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == opt, onClick = { selected = opt }, colors = RadioButtonDefaults.colors(selectedColor = TerminalColors.MonokaiGreen))
                        Text("$opt lines", color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("APPLY", color = TerminalColors.MonokaiGreen, fontFamily = FontFamily.Monospace) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtraKeysDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var keys by remember { mutableStateOf(current) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color.Black, modifier = Modifier.border(1.dp, TerminalColors.MonokaiGreen, RoundedCornerShape(28.dp)),
        title = { Text("KEY_MAPPING", color = TerminalColors.MonokaiGreen, fontFamily = FontFamily.Monospace) },
        text = {
            Column {
                OutlinedTextField(value = keys, onValueChange = { keys = it }, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TerminalColors.MonokaiGreen, unfocusedBorderColor = Color.DarkGray))
                Spacer(Modifier.height(12.dp))
                Text("SYMBOLS:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("ESC", "TAB", "CTRL", "ALT", "UP", "DOWN").forEach { k ->
                        SuggestionChip(onClick = { keys += "|$k" }, label = { Text(k, fontSize = 9.sp, fontFamily = FontFamily.Monospace) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(keys) }) { Text("COMMIT", color = TerminalColors.MonokaiGreen, fontFamily = FontFamily.Monospace) } }
    )
}
