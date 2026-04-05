package com.nexterm.app.ui.screens.main

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import java.io.File

import com.nexterm.app.package_manager.PackageManager
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    packageManager: PackageManager = koinInject()
) {
    var showPackagesDialog by remember { mutableStateOf(false) }
    var showFeaturesDialog by remember { mutableStateOf(false) }

    val onAction: (String, Boolean) -> Unit = { toolOrPkg, isComplexTool ->
        val command = if (isComplexTool) getToolCommand(toolOrPkg) else toolOrPkg
        val binaryName = command.split(" ")[0].lowercase()
        val isAvailable = packageManager.isInstalled(binaryName) || File(navController.context.filesDir, "usr/bin/$binaryName").exists() || listOf("ping", "help", "ls", "sh").contains(binaryName)
        val finalCommand = if (isAvailable) command else "pkg install $binaryName"
        navController.navigate(Screen.Terminal.createRoute(URLEncoder.encode(finalCommand, StandardCharsets.UTF_8.toString()))) { launchSingleTop = true }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).background(Color.Black, RoundedCornerShape(4.dp)).border(1.dp, TerminalColors.MonokaiGreen, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text("NX", color = TerminalColors.MonokaiGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("NEXTERM OS", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TerminalColors.MonokaiGreen, fontFamily = FontFamily.Monospace)
                            Text("STATUS: ANONYMOUS", style = MaterialTheme.typography.labelSmall, color = TerminalColors.MonokaiYellow, letterSpacing = 2.sp)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFeaturesDialog = true }) { Icon(Icons.Default.Security, null, tint = TerminalColors.MonokaiGreen) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth().border(1.dp, TerminalColors.MonokaiGreen.copy(0.3f), RoundedCornerShape(12.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnLock, null, tint = TerminalColors.MonokaiGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CORE_DECRYPT_READY", color = TerminalColors.MonokaiGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(12.dp).border(0.5.dp, TerminalColors.MonokaiGreen.copy(0.2f))) {
                            Column {
                                TerminalPreviewLine("root@nexterm:", " decrypt --all", TerminalColors.MonokaiGreen)
                                TerminalPreviewLine("[*]", " System state: Operational", TerminalColors.MonokaiYellow)
                                TerminalPreviewLine("[+]", " Anonymous level: High", TerminalColors.MonokaiCyan)
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HackerActionCard(Modifier.weight(1f), Icons.Default.Terminal, "EXECUTE", "Terminal", TerminalColors.MonokaiGreen) { navController.navigate(Screen.Terminal.createRoute()) }
                    HackerActionCard(Modifier.weight(1f), Icons.Default.FolderOpen, "EXPLORE", "Files", TerminalColors.MonokaiBlue) { navController.navigate(Screen.Files.route) }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HackerActionCard(Modifier.weight(1f), Icons.Default.AddModerator, "PACKAGES", "Repo 1.5", TerminalColors.MonokaiMagenta) { showPackagesDialog = true }
                    HackerActionCard(Modifier.weight(1f), Icons.Default.SettingsInputComponent, "CONFIG", "System", TerminalColors.MonokaiYellow) { navController.navigate(Screen.Settings.route) }
                }
            }

            item {
                Text("C A P A B I L I T I E S", color = TerminalColors.MonokaiGreen.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }

            item { CapabilityCard(Icons.Default.Code, "DEVELOPMENT", "Compilers, runtimes and SDKs", listOf("Python 3.11", "Node.js 18", "PHP 8.2", "Ruby 3.2", "Go 1.20", "GCC 12", "Rust 1.70"), TerminalColors.MonokaiGreen) { onAction(it, true) } }
            item { CapabilityCard(Icons.Default.Cloud, "SERVERS & DB", "Web servers and database engines", listOf("Nginx", "Apache", "MySQL", "PostgreSQL", "Redis", "MongoDB", "SQLite"), TerminalColors.MonokaiBlue) { onAction(it, true) } }
            item { CapabilityCard(Icons.Default.Security, "EXPLOITATION", "Vulnerability and pentest tools", listOf("Nmap", "Metasploit", "Hydra", "SQLMap", "Aircrack-ng", "Wireshark"), TerminalColors.MonokaiRed) { onAction(it, true) } }
            item { CapabilityCard(Icons.Default.Wifi, "NETWORKING", "Diagnostics and packet analysis", listOf("Ping", "Netcat", "TCPDump", "Netstat", "SSH", "Curl", "Wget"), TerminalColors.MonokaiCyan) { onAction(it, true) } }
            item { CapabilityCard(Icons.Default.CloudSync, "DEVOPS", "Automation and management", listOf("Docker", "Git", "Make", "Tmux", "Bash Scripts", "Cron Jobs"), TerminalColors.MonokaiMagenta) { onAction(it, true) } }
            item { CapabilityCard(Icons.Default.School, "LEARNING", "Linux and admin skills", listOf("Linux Commands", "Shell Scripting", "Vim", "Nano", "File System"), TerminalColors.MonokaiYellow) { onAction(it, true) } }

            // --- RESTORED SYSTEM INFO SECTION ---
            item {
                Text("S Y S T E M  I N F O", color = TerminalColors.MonokaiGreen.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, Color.DarkGray, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF050505))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SystemInfoRow("DEVICE_ID", android.os.Build.MODEL)
                        SystemInfoRow("ANDROID_OS", "API ${android.os.Build.VERSION.SDK_INT}")
                        SystemInfoRow("ARCH", android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "UNKNOWN")
                        SystemInfoRow("BASE_SHELL", "/system/bin/sh")
                        SystemInfoRow("NEXTERM_VER", "1.0.0-STABLE")
                        SystemInfoRow("ACCESS", "ANONYMOUS_SESSION")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusChip("ENC: AES-256", TerminalColors.MonokaiGreen)
                    StatusChip("ANON: ACTIVE", TerminalColors.MonokaiYellow)
                    StatusChip("PROXY: TOR", TerminalColors.MonokaiCyan)
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showPackagesDialog) PackagesDialog(onDismiss = { showPackagesDialog = false }, onPackageClick = { pkg -> onAction(pkg, false) })
    if (showFeaturesDialog) FeaturesDialog { showFeaturesDialog = false }
}

@Composable
fun SystemInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = "$label:", color = TerminalColors.MonokaiGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Text(text = value, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HackerActionCard(modifier: Modifier, icon: ImageVector, label: String, title: String, color: Color, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(90.dp).border(0.5.dp, color.copy(0.4f), RoundedCornerShape(12.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Column {
                Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color) {
    Surface(color = Color.Transparent, border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(0.5f)), shape = RoundedCornerShape(4.dp)) {
        Text(text, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

private fun getToolCommand(tool: String): String = when (tool.lowercase().trim()) {
    "ping" -> "ping -c 4 8.8.8.8"
    "python 3.11", "python" -> "python"
    "node.js 18", "node" -> "node"
    "php 8.2" -> "php"
    "ruby 3.2" -> "ruby"
    "go 1.20" -> "go"
    "gcc 12" -> "gcc"
    "rust 1.70" -> "rust"
    "nginx" -> "nginx"
    "apache" -> "apache"
    "mysql" -> "mysql"
    "postgresql" -> "postgresql"
    "redis" -> "redis"
    "mongodb" -> "mongodb"
    "sqlite" -> "sqlite"
    "nmap" -> "nmap"
    "metasploit" -> "metasploit"
    "hydra" -> "hydra"
    "sqlmap" -> "sqlmap"
    "aircrack-ng" -> "aircrack-ng"
    "wireshark" -> "wireshark"
    "netcat" -> "netcat"
    "tcpdump" -> "tcpdump"
    "netstat" -> "netstat"
    "ssh" -> "ssh"
    "curl" -> "curl"
    "wget" -> "wget"
    "docker" -> "docker"
    "git" -> "git"
    "make" -> "make"
    "tmux" -> "tmux"
    "bash scripts" -> "sh"
    "cron jobs" -> "help"
    "linux commands" -> "help"
    "shell scripting" -> "sh"
    "vim" -> "vim"
    "nano" -> "nano"
    "file system" -> "ls -la"
    "networking" -> "ifconfig"
    else -> tool.lowercase().split(" ")[0]
}

@Composable
fun TerminalPreviewLine(prompt: String, text: String, color: Color) {
    Row {
        Text(prompt, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TerminalColors.MonokaiRed)
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = color)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CapabilityCard(icon: ImageVector, title: String, description: String, tools: List<String>, color: Color, onToolClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().animateContentSize().border(0.5.dp, color.copy(0.2f), RoundedCornerShape(12.dp)), onClick = { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = Color(0xFF080808))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text(description, color = Color.Gray, fontSize = 11.sp)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.DarkGray)
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tools.forEach { tool ->
                        AssistChip(onClick = { onToolClick(tool) }, label = { Text(tool, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }, colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(0.1f), labelColor = color), border = null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesDialog(onDismiss: () -> Unit, onPackageClick: (String) -> Unit) {
    val packages = listOf(
        PackageItem("python", "PYTHON_3.11", "pkg install python"),
        PackageItem("node", "NODEJS_20", "pkg install node"),
        PackageItem("git", "GIT_VERSION", "pkg install git"),
        PackageItem("vim", "VIM_EDITOR", "pkg install vim"),
        PackageItem("nano", "NANO_EDITOR", "pkg install nano"),
        PackageItem("nginx", "NGINX_SERVER", "pkg install nginx"),
        PackageItem("mysql", "MYSQL_DB", "pkg install mysql"),
        PackageItem("postgresql", "POSTGRES_DB", "pkg install postgresql"),
        PackageItem("redis", "REDIS_CACHE", "pkg install redis"),
        PackageItem("mongodb", "MONGODB_NOSQL", "pkg install mongodb"),
        PackageItem("openssh", "OPENSSH_TOOL", "pkg install openssh"),
        PackageItem("nmap", "NMAP_SCANNER", "pkg install nmap"),
        PackageItem("curl", "CURL_TOOL", "pkg install curl"),
        PackageItem("wget", "WGET_TOOL", "pkg install wget"),
        PackageItem("php", "PHP_WEB", "pkg install php"),
        PackageItem("ruby", "RUBY_WEB", "pkg install ruby"),
        PackageItem("go", "GO_LANG", "pkg install go"),
        PackageItem("rust", "RUST_LANG", "pkg install rust"),
        PackageItem("gcc", "GCC_COMPILER", "pkg install gcc"),
        PackageItem("docker", "DOCKER_CONT", "pkg install docker"),
        PackageItem("tmux", "TMUX_MUX", "pkg install tmux"),
        PackageItem("htop", "HTOP_VIEW", "pkg install htop"),
        PackageItem("ffmpeg", "FFMPEG_MEDIA", "pkg install ffmpeg"),
        PackageItem("metasploit", "METASPLOIT", "pkg install metasploit"),
        PackageItem("hydra", "HYDRA_CRACK", "pkg install hydra"),
        PackageItem("sqlmap", "SQL_INJECT", "pkg install sqlmap"),
        PackageItem("aircrack-ng", "AIRCRACK_WIFI", "pkg install aircrack-ng")
    )
    AlertDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false), modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f)) {
        Card(modifier = Modifier.fillMaxSize().border(1.dp, TerminalColors.MonokaiGreen, RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = Color.Black)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SYSTEM_REPOSITORIES", color = TerminalColors.MonokaiGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(packages) { pkg ->
                        Surface(modifier = Modifier.fillMaxWidth().clickable { onDismiss(); onPackageClick(pkg.name) }, color = Color(0xFF0F0F0F), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.DarkGray)) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(pkg.displayName, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                Text("INSTALL", color = TerminalColors.MonokaiGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false), modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.6f)) {
        Card(modifier = Modifier.fillMaxSize().border(1.dp, TerminalColors.MonokaiGreen, RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = Color.Black)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("SYSTEM_CAPABILITIES", color = TerminalColors.MonokaiGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))
                Text("• Full Package Manager (27+ Tools)\n• Native Development Runtimes\n• Security & Pentest Suite\n• High-Level Encryption Emulation\n• Anonymous Session Routing", color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = TerminalColors.MonokaiGreen)) { Text("ACKNOWLEDGE", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

data class PackageItem(val name: String, val displayName: String, val installCommand: String)

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() { MaterialTheme { MainScreen(navController = rememberNavController()) } }
