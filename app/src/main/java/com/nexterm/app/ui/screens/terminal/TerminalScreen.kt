package com.nexterm.app.ui.screens.terminal

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val screenContent by viewModel.screenContent.collectAsState()

    var inputText by remember(currentSession?.id) { mutableStateOf("") }
    var draftInput by remember(currentSession?.id) { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val colorScheme = remember(settings.colorScheme) {
        TerminalThemes.getByName(settings.colorScheme)
    }

    var showSessionDrawer by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var sessionToClose by remember { mutableStateOf<TerminalSession?>(null) }

    LaunchedEffect(currentSession?.id) {
        inputText = ""
        draftInput = ""
        viewModel.resetHistoryNavigation()
        focusRequester.requestFocus()
    }

    fun submitInput() {
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
        when (key.uppercase()) {
            "UP" -> {
                if (draftInput.isEmpty()) {
                    draftInput = inputText
                }
                viewModel.getPreviousCommand()?.let { inputText = it }
            }

            "DOWN" -> {
                val next = viewModel.getNextCommand()
                inputText = next ?: draftInput
                if (next == null) {
                    draftInput = ""
                }
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
                                text = currentSession?.name ?: "Terminal",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.foreground
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
                                onClick = { submitInput() }
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank()) {
                                        colorScheme.green
                                    } else {
                                        colorScheme.foreground.copy(alpha = 0.35f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            TerminalOutputView(
                lines = screenContent,
                colorScheme = colorScheme,
                fontSize = settings.fontSize,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onTap = {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            )
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

                    Spacer(modifier = Modifier.height(16.dp))

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

                    Spacer(modifier = Modifier.height(32.dp))
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
                        if (isRunning) "Running" else "Stopped",
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
                    tint = if (canClose) colorScheme.red
                    else colorScheme.foreground.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}