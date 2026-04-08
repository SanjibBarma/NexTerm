package com.nexterm.app.ui.screens.terminal

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexterm.app.terminal.TerminalSession
import com.nexterm.app.ui.components.ExtraKeysBar
import com.nexterm.app.ui.components.TerminalOutputView
import com.nexterm.app.ui.theme.LocalTerminalColorScheme
import com.nexterm.app.ui.theme.TerminalColorScheme
import com.nexterm.app.ui.theme.TerminalThemes
import com.nexterm.app.ui.theme.TerminalColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.compose.koinViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    initialCommand: String? = null,
    viewModel: TerminalViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val screenContent by viewModel.screenContent.collectAsState()

    val hackerGreen = TerminalColors.MonokaiGreen
    val hackerBlack = Color.Black

    val spinnerFrames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
    var spinnerIndex by remember { mutableIntStateOf(0) }

    val colorScheme = remember(settings.colorScheme) {
        TerminalThemes.getByName(settings.colorScheme)
    }

    val decodedInitialCommand = remember(initialCommand) {
        initialCommand?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
    }

    var executedInitialCommandForSession by remember { mutableStateOf<Pair<Long?, String?>?>(null) }

    val isCurrentSessionRunning by remember(currentSession) {
        currentSession?.isRunning ?: MutableStateFlow(false)
    }.collectAsState(initial = false)

    val showStopButton = isCurrentSessionRunning

    var inputText by remember(currentSession?.id) { mutableStateOf("") }
    var draftInput by remember(currentSession?.id) { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var showSessionDrawer by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var sessionToClose by remember { mutableStateOf<TerminalSession?>(null) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    LaunchedEffect(configuration.screenWidthDp, configuration.screenHeightDp, settings.fontSize, currentSession?.id) {
        val cols = max(20, (configuration.screenWidthDp * density.density / (settings.fontSize * 0.62f)).toInt())
        val rows = max(10, (configuration.screenHeightDp * density.density / (settings.fontSize * 1.45f)).toInt())
        viewModel.resize(rows, cols)
    }

    LaunchedEffect(currentSession?.id) {
        inputText = ""
        draftInput = ""
        viewModel.resetHistoryNavigation()
        focusRequester.requestFocus()
    }

    fun submitInput() {
        if (inputText.isBlank()) { viewModel.sendKey("ENTER") } 
        else {
            viewModel.onCommand(inputText)
            inputText = ""
            draftInput = ""
            viewModel.resetHistoryNavigation()
        }
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun handleExtraKey(key: String) {
        when (key.uppercase()) {
            "UP" -> {
                if (draftInput.isEmpty()) draftInput = inputText
                viewModel.getPreviousCommand()?.let { inputText = it }
            }
            "DOWN" -> {
                val next = viewModel.getNextCommand()
                inputText = next ?: draftInput
                if (next == null) draftInput = ""
            }
            else -> {
                viewModel.sendKey(key)
                focusRequester.requestFocus()
            }
        }
    }

    CompositionLocalProvider(LocalTerminalColorScheme provides colorScheme) {
        Scaffold(
            containerColor = hackerBlack,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, null, tint = hackerGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("TERMINAL", style = MaterialTheme.typography.titleMedium, color = hackerGreen, fontFamily = FontFamily.Monospace)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null, tint = hackerGreen) }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.createSession() }) { Icon(Icons.Default.Add, null, tint = hackerGreen) }
                        IconButton(onClick = { showSessionDrawer = true }) {
                            BadgedBox(badge = { if (sessions.size > 1) { Badge(containerColor = hackerGreen) { Text(sessions.size.toString(), color = Color.Black) } } }) {
                                Icon(Icons.Default.Layers, null, tint = hackerGreen)
                            }
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = hackerGreen) }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }, modifier = Modifier.background(hackerBlack).border(0.5.dp, hackerGreen)) {
                                DropdownMenuItem(text = { Text("Clear Buffer", color = hackerGreen, fontFamily = FontFamily.Monospace) }, onClick = { viewModel.sendKey("CTRL+L"); showMoreMenu = false })
                                DropdownMenuItem(text = { Text("File Explorer", color = hackerGreen, fontFamily = FontFamily.Monospace) }, onClick = { onNavigateToFiles(); showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Sys Config", color = hackerGreen, fontFamily = FontFamily.Monospace) }, onClick = { onNavigateToSettings(); showMoreMenu = false })
                                HorizontalDivider(color = hackerGreen.copy(0.2f))
                                DropdownMenuItem(text = { Text("Send SIGINT (C)", color = Color.Red, fontFamily = FontFamily.Monospace) }, onClick = { viewModel.sendKey("CTRL+C"); showMoreMenu = false })
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = hackerBlack, titleContentColor = hackerGreen)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(hackerBlack)
            ) {
                // Terminal Output Area - using weight(1f, fill = false) to "wrap" content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(horizontal = 8.dp)
                        .border(0.5.dp, hackerGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                ) {
                    TerminalOutputView(
                        lines = screenContent,
                        colorScheme = colorScheme,
                        fontSize = settings.fontSize,
                        modifier = Modifier.fillMaxWidth(),
                        onTap = { focusRequester.requestFocus(); keyboardController?.show() }
                    )
                }

                // Input Section - moved here to follow the terminal view
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(hackerBlack)
                        .navigationBarsPadding()
                ) {
                    if (settings.extraKeysEnabled) {
                        ExtraKeysBar(keysRow = settings.extraKeysRow, onKeyClick = ::handleExtraKey, colorScheme = colorScheme)
                    }
                    Surface(
                        color = hackerBlack,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, hackerGreen.copy(0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("❯", color = hackerGreen, fontFamily = FontFamily.Monospace, fontSize = settings.fontSize.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f)) {
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it; draftInput = it; viewModel.resetHistoryNavigation() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .padding(vertical = 10.dp),
                                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = settings.fontSize.sp),
                                    cursorBrush = SolidColor(hackerGreen),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { submitInput() }),
                                    singleLine = true
                                )
                                if (inputText.isEmpty()) Text("waiting...", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = settings.fontSize.sp, modifier = Modifier.padding(vertical = 10.dp))
                            }
                            IconButton(
                                onClick = { if (showStopButton) viewModel.sendKey("CTRL+C") else submitInput() },
                                enabled = showStopButton || inputText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = if (showStopButton) Icons.Default.Close else Icons.Default.Send,
                                    contentDescription = null,
                                    tint = if (showStopButton) Color.Red else if (inputText.isNotBlank()) hackerGreen else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSessionDrawer) {
            ModalBottomSheet(onDismissRequest = { showSessionDrawer = false }, containerColor = hackerBlack) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ACTIVE_SESSIONS", color = hackerGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.createSession() }) { Text("+ NEW_NODE", color = hackerGreen, fontFamily = FontFamily.Monospace) }
                    }
                    Spacer(Modifier.height(16.dp))
                    sessions.forEach { session ->
                        SessionItem(session, session.id == currentSession?.id, colorScheme, sessions.size > 1, 
                            onClick = { viewModel.switchSession(session); showSessionDrawer = false },
                            onClose = { if (sessions.size > 1) sessionToClose = session })
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        if (sessionToClose != null) {
            AlertDialog(
                onDismissRequest = { sessionToClose = null },
                containerColor = hackerBlack,
                modifier = Modifier.border(1.dp, hackerGreen, RoundedCornerShape(28.dp)),
                title = { Text("TERMINATE_SESSION", color = hackerGreen, fontFamily = FontFamily.Monospace) },
                text = { Text("Confirm termination of ${sessionToClose?.name}?", color = Color.White, fontFamily = FontFamily.Monospace) },
                confirmButton = { TextButton(onClick = { sessionToClose?.let { viewModel.closeSession(it) }; sessionToClose = null }) { Text("TERMINATE", color = Color.Red, fontFamily = FontFamily.Monospace) } },
                dismissButton = { TextButton(onClick = { sessionToClose = null }) { Text("ABORT", color = hackerGreen, fontFamily = FontFamily.Monospace) } }
            )
        }
    }
}

@Composable
fun SessionItem(session: TerminalSession, isActive: Boolean, colorScheme: TerminalColorScheme, canClose: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    val isRunning by session.isRunning.collectAsState()
    val hackerGreen = TerminalColors.MonokaiGreen
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(if (isActive) 1.dp else 0.dp, hackerGreen, RoundedCornerShape(12.dp)), colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFF0F0F0F) else Color.Transparent)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(if (isRunning) hackerGreen else Color.Red, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(session.name.uppercase(), color = if (isActive) hackerGreen else Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(if (isRunning) "STATUS: RUNNING" else "STATUS: IDLE", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
            if (canClose) IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(18.dp)) }
        }
    }
}
