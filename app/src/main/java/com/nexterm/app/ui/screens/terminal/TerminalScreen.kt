package com.nexterm.app.ui.screens.terminal

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    Log.e("TerminalScreen", "screenContent size=${screenContent.size}")
    screenContent.forEachIndexed { index, line ->
        Log.e("TerminalScreen", "screen line[$index]=${line.getText()}")
    }

    val spinnerFrames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
    var spinnerIndex by remember { mutableIntStateOf(0) }

    val colorScheme = remember(settings.colorScheme) {
        TerminalThemes.getByName(settings.colorScheme)
    }

    val decodedInitialCommand = remember(initialCommand) {
        initialCommand?.let {
            URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
        }
    }

    var executedInitialCommandForSession by remember {
        mutableStateOf<Pair<Long?, String?>?>(null)
    }

    val isCurrentSessionRunning by remember(currentSession) {
        currentSession?.isRunning ?: MutableStateFlow(false)
    }.collectAsState(initial = false)

    Log.e("TerminalScreen", "isCurrentSessionRunning=$isCurrentSessionRunning")

    val showRunningOverlay = isCurrentSessionRunning
    val showStopButton = isCurrentSessionRunning

    Log.e(
        "TerminalScreen",
        "UI flags -> running=$isCurrentSessionRunning overlay=$showRunningOverlay stopButton=$showStopButton"
    )

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
        Log.e("TerminalScreen", "resize effect rows=$rows cols=$cols")
        viewModel.resize(rows, cols)
    }

    LaunchedEffect(currentSession?.id) {
        Log.e("TerminalScreen", "currentSession changed id=${currentSession?.id}")
        inputText = ""
        draftInput = ""
        viewModel.resetHistoryNavigation()
        focusRequester.requestFocus()
    }

    LaunchedEffect(isCurrentSessionRunning) {
        Log.e("TerminalScreen", "spinner effect running=$isCurrentSessionRunning")

        if (!isCurrentSessionRunning) {
            spinnerIndex = 0
            return@LaunchedEffect
        }

        while (isCurrentSessionRunning) {
            delay(100)
            spinnerIndex = (spinnerIndex + 1) % spinnerFrames.size
        }
    }

    LaunchedEffect(currentSession?.id, decodedInitialCommand) {
        val sessionId = currentSession?.id
        val command = decodedInitialCommand?.trim()

        Log.e(
            "TerminalScreen",
            "initial command effect sessionId=$sessionId command=[$command] executed=$executedInitialCommandForSession"
        )

        if (sessionId == null || command.isNullOrBlank()) return@LaunchedEffect

        val currentKey = sessionId to command
        if (executedInitialCommandForSession == currentKey) return@LaunchedEffect

        delay(500)

        Log.e("TerminalScreen", "executing initial command after delay=[$command]")
        viewModel.executeCommand(command)
        executedInitialCommandForSession = currentKey
    }

    fun submitInput() {
        Log.e("TerminalScreen", "submitInput inputText=[$inputText]")
        if (inputText.isBlank()) {
            viewModel.sendKey("ENTER")
        } else {
            viewModel.executeCommand(inputText)
            inputText = ""
            draftInput = ""
            viewModel.resetHistoryNavigation()
        }
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun handleExtraKey(key: String) {
        Log.e("TerminalScreen", "handleExtraKey key=$key")
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
            containerColor = colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = null,
                                tint = colorScheme.green,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Terminal",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.foreground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (showRunningOverlay) colorScheme.green
                                        else colorScheme.foreground.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = colorScheme.foreground
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.createSession() }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "New Session",
                                tint = colorScheme.foreground
                            )
                        }

                        IconButton(onClick = { showSessionDrawer = true }) {
                            BadgedBox(
                                badge = {
                                    if (sessions.size > 1) {
                                        Badge(containerColor = colorScheme.green) {
                                            Text(
                                                sessions.size.toString(),
                                                color = colorScheme.background
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = "Sessions",
                                    tint = colorScheme.foreground
                                )
                            }
                        }

                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = colorScheme.foreground
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Clear Screen") },
                                    onClick = {
                                        viewModel.sendKey("CTRL+L")
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ClearAll, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("File Manager") },
                                    onClick = {
                                        onNavigateToFiles()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Folder, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        onNavigateToSettings()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, contentDescription = null)
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Send CTRL+C") },
                                    onClick = {
                                        viewModel.sendKey("CTRL+C")
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Cancel, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Send CTRL+D") },
                                    onClick = {
                                        viewModel.sendKey("CTRL+D")
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.background
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.background)
                        .navigationBarsPadding()
                ) {
                    if (settings.extraKeysEnabled) {
                        ExtraKeysBar(
                            keysRow = settings.extraKeysRow,
                            onKeyClick = ::handleExtraKey,
                            colorScheme = colorScheme
                        )
                    }

                    Surface(
                        color = colorScheme.background,
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "❯",
                                color = colorScheme.green,
                                fontFamily = FontFamily.Monospace,
                                fontSize = settings.fontSize.sp
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.black.copy(alpha = 0.22f)
                            ) {
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = {
                                        inputText = it
                                        draftInput = it
                                        viewModel.resetHistoryNavigation()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    textStyle = TextStyle(
                                        color = colorScheme.foreground,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = settings.fontSize.sp
                                    ),
                                    cursorBrush = SolidColor(colorScheme.cursor),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Ascii,
                                        imeAction = ImeAction.Send
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = { submitInput() }
                                    ),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (inputText.isEmpty()) {
                                                Text(
                                                    "Enter command...",
                                                    color = colorScheme.foreground.copy(alpha = 0.32f),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = settings.fontSize.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    Log.e(
                                        "TerminalScreen",
                                        "action button clicked, running=$showStopButton input=[$inputText]"
                                    )
                                    if (showStopButton) {
                                        viewModel.sendKey("CTRL+C")
                                    } else {
                                        submitInput()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (showStopButton) Icons.Default.Close else Icons.Default.Send,
                                    contentDescription = if (showStopButton) "Stop" else "Send",
                                    tint = when {
                                        showStopButton -> colorScheme.red
                                        inputText.isNotBlank() -> colorScheme.green
                                        else -> colorScheme.foreground.copy(alpha = 0.35f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Log.e("TerminalScreen", "Before TerminalOutputView, size=${screenContent.size}")

                TerminalOutputView(
                    lines = screenContent,
                    colorScheme = colorScheme,
                    fontSize = settings.fontSize,
                    modifier = Modifier.fillMaxSize(),
                    onTap = {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                )

                if (showRunningOverlay) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .background(
                                color = colorScheme.black.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[${spinnerFrames[spinnerIndex]}]",
                            color = colorScheme.green,
                            fontFamily = FontFamily.Monospace,
                            fontSize = settings.fontSize.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Running command...",
                            color = colorScheme.foreground.copy(alpha = 0.9f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = settings.fontSize.sp
                        )
                    }
                }
            }
        }

        if (showSessionDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showSessionDrawer = false },
                containerColor = colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Sessions",
                            style = MaterialTheme.typography.titleLarge,
                            color = colorScheme.foreground
                        )

                        TextButton(onClick = { viewModel.createSession() }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = colorScheme.green
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", color = colorScheme.green)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    sessions.forEach { session ->
                        SessionItem(
                            session = session,
                            isActive = session.id == currentSession?.id,
                            colorScheme = colorScheme,
                            canClose = sessions.size > 1,
                            onClick = {
                                viewModel.switchSession(session)
                                showSessionDrawer = false
                            },
                            onClose = {
                                if (sessions.size > 1) {
                                    sessionToClose = session
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.padding(bottom = 32.dp))
                }
            }
        }

        if (sessionToClose != null) {
            AlertDialog(
                onDismissRequest = { sessionToClose = null },
                containerColor = colorScheme.background,
                title = {
                    Text(
                        text = "Close Session",
                        color = colorScheme.foreground
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to close \"${sessionToClose?.name}\"?",
                        color = colorScheme.foreground.copy(alpha = 0.82f)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            sessionToClose?.let { viewModel.closeSession(it) }
                            sessionToClose = null
                        }
                    ) {
                        Text("Close", color = colorScheme.red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToClose = null }) {
                        Text("Cancel", color = colorScheme.foreground)
                    }
                }
            )
        }
    }
}

@Composable
fun SessionItem(
    session: TerminalSession,
    isActive: Boolean,
    colorScheme: TerminalColorScheme,
    canClose: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val isRunning by session.isRunning.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) colorScheme.selection else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isRunning) colorScheme.green else colorScheme.red,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    tint = if (isActive) colorScheme.green else colorScheme.foreground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        session.name,
                        color = colorScheme.foreground,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        if (isRunning) "Running" else "Idle",
                        color = colorScheme.foreground.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            IconButton(
                onClick = onClose,
                enabled = canClose,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = if (canClose) "Close" else "Cannot close last session",
                    tint = if (canClose) colorScheme.red else colorScheme.foreground.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}