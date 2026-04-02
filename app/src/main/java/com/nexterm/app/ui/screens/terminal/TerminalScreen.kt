package com.nexterm.app.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexterm.app.ui.components.TerminalOutputView
import com.nexterm.app.terminal.TerminalSession
import com.nexterm.app.ui.components.ExtraKeysBar
import com.nexterm.app.ui.screens.terminal.TerminalViewModel
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

    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val colorScheme = remember(settings.colorScheme) {
        TerminalThemes.getByName(settings.colorScheme)
    }

    var showSessionDrawer by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Initialize session if none exists
    LaunchedEffect(Unit) {
        if (currentSession == null) {
            viewModel.createSession()
        }
    }

    // Request focus when session changes
    LaunchedEffect(currentSession) {
        focusRequester.requestFocus()
    }

    Scaffold(
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
                            currentSession?.name ?: "Terminal",
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
                    // New session button
                    IconButton(
                        onClick = { viewModel.createSession("Terminal ${sessions.size + 1}") }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Session",
                            tint = colorScheme.foreground
                        )
                    }

                    // Sessions button with badge
                    IconButton(onClick = { showSessionDrawer = true }) {
                        BadgedBox(
                            badge = {
                                if (sessions.size > 1) {
                                    Badge(
                                        containerColor = colorScheme.green
                                    ) {
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

                    // More menu
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
                modifier = Modifier.background(colorScheme.background)
            ) {
                // Extra keys bar
                if (settings.extraKeysEnabled) {
                    ExtraKeysBar(
                        keysRow = settings.extraKeysRow,
                        onKeyClick = { key -> viewModel.sendKey(key) },
                        colorScheme = colorScheme
                    )
                }

                // Input field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prompt
                    Text(
                        "❯ ",
                        color = colorScheme.green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = settings.fontSize.sp
                    )

                    // Input field
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
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
                            onSend = {
                                if (inputText.isNotEmpty()) {
                                    viewModel.executeCommand(inputText)
                                    inputText = ""
                                    focusRequester.requestFocus()
                                } else {
                                    // Just send Enter
                                    viewModel.sendKey("ENTER")
                                }
                            }
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box {
                                if (inputText.isEmpty()) {
                                    Text(
                                        "Enter command...",
                                        color = colorScheme.foreground.copy(alpha = 0.3f),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = settings.fontSize.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // Send button
                    IconButton(
                        onClick = {
                            if (inputText.isNotEmpty()) {
                                viewModel.executeCommand(inputText)
                                inputText = ""
                                focusRequester.requestFocus()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotEmpty()) colorScheme.green
                            else colorScheme.foreground.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // Terminal output
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

    // Session drawer
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
                    TextButton(
                        onClick = {
                            viewModel.createSession("Terminal ${sessions.size + 1}")
                        }
                    ) {
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
                        onClick = {
                            viewModel.switchSession(session)
                            showSessionDrawer = false
                        },
                        onClose = {
                            viewModel.closeSession(session)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SessionItem(
    session: TerminalSession,
    isActive: Boolean,
    colorScheme: TerminalColorScheme,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val isRunning by session.isRunning.collectAsState()

    Card(
        onClick = onClick,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isRunning) colorScheme.green else colorScheme.red,
                            shape = androidx.compose.foundation.shape.CircleShape
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

            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colorScheme.red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}