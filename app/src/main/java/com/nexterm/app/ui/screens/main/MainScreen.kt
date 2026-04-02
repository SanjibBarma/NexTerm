package com.nexterm.app.presentation.screens.main

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nexterm.app.ui.navigation.Screen
import com.nexterm.app.ui.theme.TerminalColors
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFeaturesDialog by remember { mutableStateOf(false) }
    var showPackagesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            TerminalColors.MonokaiGreen,
                                            TerminalColors.MonokaiBlue
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "NexTerm",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                "Professional Terminal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFeaturesDialog = true }) {
                        Icon(Icons.Default.Stars, contentDescription = "Features")
                    }
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = TerminalColors.MonokaiBackground
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Welcome to NexTerm",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TerminalColors.MonokaiGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "The most powerful terminal emulator for Android. Develop, Learn, Deploy - All from your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TerminalColors.MonokaiForeground.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Column {
                                TerminalPreviewLine("$ ", "pkg install python nodejs git", TerminalColors.MonokaiGreen)
                                TerminalPreviewLine("", "Installing packages...", TerminalColors.MonokaiYellow)
                                TerminalPreviewLine("$ ", "python --version", TerminalColors.MonokaiGreen)
                                TerminalPreviewLine("", "Python 3.11.0", TerminalColors.MonokaiForeground)
                                TerminalPreviewLine("$ ", "nginx start", TerminalColors.MonokaiGreen)
                                TerminalPreviewLine("", "Server running on port 8080", TerminalColors.MonokaiCyan)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MainActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Terminal,
                        title = "Terminal",
                        subtitle = "Start coding",
                        gradient = listOf(TerminalColors.MonokaiGreen, Color(0xFF2E7D32)),
                        onClick = { navController.navigate(Screen.Terminal.createRoute()) }
                    )
                    MainActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Folder,
                        title = "Files",
                        subtitle = "Manage files",
                        gradient = listOf(TerminalColors.MonokaiBlue, Color(0xFF1565C0)),
                        onClick = { navController.navigate(Screen.Files.route) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MainActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Extension,
                        title = "Packages",
                        subtitle = "Install tools",
                        gradient = listOf(TerminalColors.MonokaiMagenta, Color(0xFF7B1FA2)),
                        onClick = { showPackagesDialog = true }
                    )
                    MainActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        subtitle = "Customize",
                        gradient = listOf(TerminalColors.MonokaiYellow, Color(0xFFF57C00)),
                        onClick = { navController.navigate(Screen.Settings.route) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "What You Can Do",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                CapabilityCard(
                    icon = Icons.Default.Code,
                    title = "Development",
                    description = "Full-stack development with Python, Node.js, PHP, Ruby, Go, Rust, C/C++",
                    tools = listOf("Python 3.11", "Node.js 18", "PHP 8.2", "Ruby 3.2", "Go 1.20", "GCC 12"),
                    color = TerminalColors.MonokaiGreen,
                    onToolClick = { tool -> navigateToToolCommand(navController, tool) }
                )
            }

            item {
                CapabilityCard(
                    icon = Icons.Default.Cloud,
                    title = "Web Servers & Databases",
                    description = "Run production-ready servers and databases locally",
                    tools = listOf("Nginx", "Apache", "MySQL", "PostgreSQL", "Redis", "MongoDB"),
                    color = TerminalColors.MonokaiBlue,
                    onToolClick = { tool -> navigateToToolCommand(navController, tool) }
                )
            }

            item {
                CapabilityCard(
                    icon = Icons.Default.Security,
                    title = "Security & Pentesting",
                    description = "Ethical hacking and security analysis tools",
                    tools = listOf("Nmap", "Metasploit", "Hydra", "SQLMap", "Aircrack-ng", "Wireshark"),
                    color = TerminalColors.MonokaiRed,
                    onToolClick = { tool -> navigateToToolCommand(navController, tool) }
                )
            }

            item {
                CapabilityCard(
                    icon = Icons.Default.Wifi,
                    title = "Network Analysis",
                    description = "Network scanning, monitoring and diagnostics",
                    tools = listOf("Ping", "Netcat", "TCPDump", "Netstat", "SSH", "Curl/Wget"),
                    color = TerminalColors.MonokaiCyan,
                    onToolClick = { tool -> navigateToToolCommand(navController, tool) }
                )
            }

            item {
                CapabilityCard(
                    icon = Icons.Default.CloudSync,
                    title = "DevOps & Automation",
                    description = "Container management, CI/CD and scripting",
                    tools = listOf("Docker", "Git", "Make", "Tmux", "Bash Scripts", "Cron Jobs"),
                    color = TerminalColors.MonokaiMagenta,
                    onToolClick = { tool -> navigateToToolCommand(navController, tool) }
                )
            }

            item {
                CapabilityCard(
                    icon = Icons.Default.School,
                    title = "Learning & Education",
                    description = "Learn Linux, programming, and system administration",
                    tools = listOf("Linux Commands", "Shell Scripting", "Vim/Nano", "File System", "Networking", "Security"),
                    color = TerminalColors.MonokaiYellow,
                    onToolClick = { tool -> navigateToToolCommand(navController, tool) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "System Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        SystemInfoRow("Device", android.os.Build.MODEL)
                        SystemInfoRow("Android", "API ${android.os.Build.VERSION.SDK_INT}")
                        SystemInfoRow("Architecture", android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown")
                        SystemInfoRow("Shell", "/system/bin/sh")
                        SystemInfoRow("Terminal", "NexTerm v1.0.0")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showFeaturesDialog) {
        FeaturesDialog(onDismiss = { showFeaturesDialog = false })
    }

    if (showPackagesDialog) {
        PackagesDialog(onDismiss = { showPackagesDialog = false })
    }
}

private fun getToolCommand(tool: String): String {
    return when (tool) {
        "Python 3.11" -> "python --version"
        "Node.js 18" -> "node --version"
        "PHP 8.2" -> "php --version"
        "Ruby 3.2" -> "ruby --version"
        "Go 1.20" -> "go version"
        "GCC 12" -> "gcc --version"

        "Nginx" -> "nginx -v"
        "Apache" -> "apachectl -v"
        "MySQL" -> "mysql --version"
        "PostgreSQL" -> "psql --version"
        "Redis" -> "redis-server --help"
        "MongoDB" -> "mongod --help"

        "Nmap" -> "nmap --version"
        "Metasploit" -> "msfconsole --version"
        "Hydra" -> "hydra -h"
        "SQLMap" -> "sqlmap --help"
        "Aircrack-ng" -> "aircrack-ng --help"
        "Wireshark" -> "wireshark --version"

        "Ping" -> "ping -c 4 8.8.8.8"
        "Netcat" -> "nc -h"
        "TCPDump" -> "tcpdump --help"
        "Netstat" -> "netstat"
        "SSH" -> "ssh -V"
        "Curl/Wget" -> "curl --help"

        "Docker" -> "docker --version"
        "Git" -> "git --version"
        "Make" -> "make --version"
        "Tmux" -> "tmux -V"
        "Bash Scripts" -> "bash --version"
        "Cron Jobs" -> "crontab -l"

        "Linux Commands" -> "help"
        "Shell Scripting" -> "sh --help"
        "Vim/Nano" -> "vim --version"
        "File System" -> "pwd"
        "Networking" -> "ifconfig"
        "Security" -> "help"

        else -> tool.lowercase()
    }
}

private fun navigateToToolCommand(
    navController: NavController,
    tool: String
) {
    val command = getToolCommand(tool)
    val encodedCommand = URLEncoder.encode(command, StandardCharsets.UTF_8.toString())
    navController.navigate(Screen.Terminal.createRoute(encodedCommand)) {
        launchSingleTop = true
    }
}

@Composable
fun TerminalPreviewLine(prompt: String, text: String, color: Color) {
    Row {
        if (prompt.isNotEmpty()) {
            Text(
                prompt,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TerminalColors.MonokaiGreen
            )
        }
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CapabilityCard(
    icon: ImageVector,
    title: String,
    description: String,
    tools: List<String>,
    color: Color,
    onToolClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tools.forEach { tool ->
                        AssistChip(
                            onClick = { onToolClick(tool) },
                            label = {
                                Text(tool, fontSize = 11.sp)
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = color.copy(alpha = 0.1f),
                                labelColor = color
                            ),
                            border = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    TerminalColors.MonokaiBackground,
                                    Color(0xFF1a1a2e)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            TerminalColors.MonokaiGreen,
                                            TerminalColors.MonokaiCyan
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "NexTerm",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Professional Terminal Emulator",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = TerminalColors.MonokaiGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Version 1.0.0",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TerminalColors.MonokaiGreen
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        "NexTerm হলো Android এর জন্য সবচেয়ে powerful ও professional terminal emulator। এটি দিয়ে আপনি আপনার phone থেকেই full-scale development, server management, penetration testing এবং আরও অনেক কিছু করতে পারবেন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    AboutSection(
                        icon = Icons.Default.Code,
                        title = "🚀 Development",
                        color = TerminalColors.MonokaiGreen,
                        features = listOf(
                            "Python 3.11 - Web, AI/ML, Automation",
                            "Node.js 18 - Full-stack JavaScript",
                            "PHP 8.2 - Web Development",
                            "Ruby 3.2 - Rails Development",
                            "Go 1.20 - System Programming",
                            "Rust 1.70 - Safe Systems Programming",
                            "GCC/Clang - C/C++ Development",
                            "pip, npm, composer - Package Managers"
                        )
                    )

                    AboutSection(
                        icon = Icons.Default.Storage,
                        title = "🗄️ Servers & Databases",
                        color = TerminalColors.MonokaiBlue,
                        features = listOf(
                            "Nginx - High-performance HTTP Server",
                            "Apache - Popular Web Server",
                            "MySQL 8.0 - Relational Database",
                            "PostgreSQL 15 - Advanced Database",
                            "Redis 7.0 - In-memory Database",
                            "MongoDB 6.0 - NoSQL Database",
                            "SQLite - Embedded Database",
                            "SSH Server - Remote Access"
                        )
                    )

                    AboutSection(
                        icon = Icons.Default.Security,
                        title = "🔐 Security & Pentesting",
                        color = TerminalColors.MonokaiRed,
                        features = listOf(
                            "Nmap - Network Scanner",
                            "Metasploit - Penetration Testing",
                            "Hydra - Password Cracker",
                            "SQLMap - SQL Injection Tool",
                            "Aircrack-ng - WiFi Security",
                            "Wireshark - Protocol Analyzer",
                            "Netcat - Network Utility",
                            "TCPDump - Packet Analyzer"
                        )
                    )

                    AboutSection(
                        icon = Icons.Default.Wifi,
                        title = "🌐 Networking",
                        color = TerminalColors.MonokaiCyan,
                        features = listOf(
                            "SSH Client - Secure Shell",
                            "Curl/Wget - HTTP Clients",
                            "Ping/Traceroute - Diagnostics",
                            "Netstat - Network Statistics",
                            "IP/Ifconfig - Network Config",
                            "DNS Tools - Name Resolution",
                            "Port Scanning - Service Discovery",
                            "VPN Support - Secure Connections"
                        )
                    )

                    AboutSection(
                        icon = Icons.Default.BuildCircle,
                        title = "🛠️ DevOps & Tools",
                        color = TerminalColors.MonokaiMagenta,
                        features = listOf(
                            "Git - Version Control",
                            "Docker - Containerization",
                            "Tmux - Terminal Multiplexer",
                            "Vim/Nano - Text Editors",
                            "Make - Build Automation",
                            "Htop - Process Viewer",
                            "FFmpeg - Media Processing",
                            "ImageMagick - Image Processing"
                        )
                    )

                    AboutSection(
                        icon = Icons.Default.Terminal,
                        title = "💻 Terminal Features",
                        color = TerminalColors.MonokaiYellow,
                        features = listOf(
                            "Full ANSI Color Support (256 colors)",
                            "Multiple Sessions with Tabs",
                            "Session Persistence",
                            "Command History",
                            "Auto-complete Support",
                            "Extra Keys Bar (CTRL, ALT, etc.)",
                            "5+ Color Themes",
                            "Customizable Font Size"
                        )
                    )

                    AboutSection(
                        icon = Icons.Default.FolderOpen,
                        title = "📁 File Management",
                        color = TerminalColors.MonokaiForeground,
                        features = listOf(
                            "Full File Browser",
                            "Create/Delete Files & Folders",
                            "Copy/Cut/Paste Operations",
                            "Rename Files",
                            "File Properties & Permissions",
                            "Hidden Files Toggle",
                            "Multi-select Support",
                            "Navigate to Any Directory"
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Built With",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TechBadge("Kotlin")
                                TechBadge("Jetpack Compose")
                                TechBadge("Material 3")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TechBadge("Koin DI")
                                TechBadge("Room DB")
                                TechBadge("Coroutines")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalColors.MonokaiGreen
                        )
                    ) {
                        Text("Got it!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutSection(
    icon: ImageVector,
    title: String,
    color: Color,
    features: List<String>
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
        }
        features.forEach { feature ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
            ) {
                Text(
                    "•",
                    color = color,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    feature,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun TechBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = TerminalColors.MonokaiBackground
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 11.sp,
            color = TerminalColors.MonokaiForeground,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.8f)
    ) {
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                TopAppBar(
                    title = { Text("সব Features") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TerminalColors.MonokaiBackground,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )

                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FeatureCategory(
                            title = "✅ Development করতে পারবেন",
                            items = listOf(
                                "Web Development (HTML, CSS, JS, PHP)",
                                "Backend Development (Python, Node.js, Go)",
                                "Mobile App Backend APIs",
                                "REST API Development",
                                "Database Management",
                                "Full-stack Applications"
                            )
                        )
                    }

                    item {
                        FeatureCategory(
                            title = "✅ Server চালাতে পারবেন",
                            items = listOf(
                                "Nginx/Apache HTTP Server",
                                "MySQL/PostgreSQL Database",
                                "Redis Cache Server",
                                "MongoDB NoSQL Database",
                                "SSH Server (sshd)",
                                "FTP Server"
                            )
                        )
                    }

                    item {
                        FeatureCategory(
                            title = "✅ Programming শিখতে পারবেন",
                            items = listOf(
                                "Python Programming",
                                "JavaScript/Node.js",
                                "C/C++ Programming",
                                "Shell Scripting",
                                "Linux Commands",
                                "Git Version Control"
                            )
                        )
                    }

                    item {
                        FeatureCategory(
                            title = "✅ Penetration Testing করতে পারবেন",
                            items = listOf(
                                "Network Scanning (Nmap)",
                                "Vulnerability Assessment",
                                "Password Cracking (Hydra)",
                                "SQL Injection (SQLMap)",
                                "WiFi Security Testing",
                                "Packet Analysis"
                            )
                        )
                    }

                    item {
                        FeatureCategory(
                            title = "✅ Automation করতে পারবেন",
                            items = listOf(
                                "Bash/Shell Scripts",
                                "Python Automation",
                                "Cron Jobs",
                                "Task Scheduling",
                                "File Operations",
                                "API Automation"
                            )
                        )
                    }

                    item {
                        FeatureCategory(
                            title = "✅ Network Analysis করতে পারবেন",
                            items = listOf(
                                "Port Scanning",
                                "Network Monitoring",
                                "DNS Lookup",
                                "HTTP/HTTPS Testing",
                                "SSH Connections",
                                "Network Diagnostics"
                            )
                        )
                    }

                    item {
                        FeatureCategory(
                            title = "✅ File Management করতে পারবেন",
                            items = listOf(
                                "Browse All Files",
                                "Create/Edit/Delete",
                                "Copy/Move/Rename",
                                "Archive (zip, tar, gz)",
                                "Permissions Management",
                                "Search Files"
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureCategory(
    title: String,
    items: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TerminalColors.MonokaiGreen
            )
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        "→ ",
                        color = TerminalColors.MonokaiYellow,
                        fontSize = 13.sp
                    )
                    Text(
                        item,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesDialog(onDismiss: () -> Unit) {
    val packages = listOf(
        PackageItem("python", "Python 3.11", "Programming language", "pkg install python"),
        PackageItem("nodejs", "Node.js 18", "JavaScript runtime", "pkg install nodejs"),
        PackageItem("git", "Git 2.40", "Version control", "pkg install git"),
        PackageItem("vim", "Vim 9.0", "Text editor", "pkg install vim"),
        PackageItem("nano", "Nano 7.0", "Simple editor", "pkg install nano"),
        PackageItem("nginx", "Nginx 1.24", "HTTP server", "pkg install nginx"),
        PackageItem("mysql", "MySQL 8.0", "Database", "pkg install mysql"),
        PackageItem("postgresql", "PostgreSQL 15", "Database", "pkg install postgresql"),
        PackageItem("redis", "Redis 7.0", "Cache server", "pkg install redis"),
        PackageItem("mongodb", "MongoDB 6.0", "NoSQL database", "pkg install mongodb"),
        PackageItem("openssh", "OpenSSH 9.3", "SSH client", "pkg install openssh"),
        PackageItem("nmap", "Nmap 7.93", "Network scanner", "pkg install nmap"),
        PackageItem("curl", "Curl 8.0", "HTTP client", "pkg install curl"),
        PackageItem("wget", "Wget 1.21", "Downloader", "pkg install wget"),
        PackageItem("php", "PHP 8.2", "Web language", "pkg install php"),
        PackageItem("ruby", "Ruby 3.2", "Programming language", "pkg install ruby"),
        PackageItem("go", "Go 1.20", "Go language", "pkg install go"),
        PackageItem("rust", "Rust 1.70", "Rust language", "pkg install rust"),
        PackageItem("gcc", "GCC 12", "C/C++ compiler", "pkg install gcc"),
        PackageItem("docker", "Docker 24", "Containers", "pkg install docker"),
        PackageItem("tmux", "Tmux 3.3", "Terminal multiplexer", "pkg install tmux"),
        PackageItem("htop", "Htop 3.2", "Process viewer", "pkg install htop"),
        PackageItem("ffmpeg", "FFmpeg 6.0", "Media tool", "pkg install ffmpeg"),
        PackageItem("metasploit", "Metasploit 6.3", "Pentest framework", "pkg install metasploit"),
        PackageItem("hydra", "Hydra 9.5", "Password cracker", "pkg install hydra"),
        PackageItem("sqlmap", "SQLMap 1.7", "SQL injection", "pkg install sqlmap"),
        PackageItem("aircrack-ng", "Aircrack-ng 1.7", "WiFi security", "pkg install aircrack-ng")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f)
    ) {
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Available Packages")
                            Text(
                                "${packages.size} packages available",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TerminalColors.MonokaiBackground,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )

                LazyColumn(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(packages) { pkg ->
                        PackageItemCard(pkg)
                    }
                }
            }
        }
    }
}

data class PackageItem(
    val name: String,
    val displayName: String,
    val description: String,
    val installCommand: String
)

@Composable
fun PackageItemCard(pkg: PackageItem) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalColors.MonokaiGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    pkg.name.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = TerminalColors.MonokaiGreen,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pkg.displayName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    pkg.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    pkg.installCommand,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalColors.MonokaiCyan
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        MainScreen(navController = rememberNavController())
    }
}