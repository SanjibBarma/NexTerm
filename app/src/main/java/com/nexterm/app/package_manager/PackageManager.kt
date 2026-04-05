package com.nexterm.app.package_manager

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PackageManager(private val context: Context) {

    private val PREFIX = File(context.filesDir, "usr")
    private val BIN_DIR = File(PREFIX, "bin")
    private val PACKAGES_DIR = File(context.filesDir, "packages")

    private val availablePackages = mapOf(
        "python" to PackageInfo("python", "3.11.0", "Python Language"),
        "node" to PackageInfo("node", "20.5.1", "Node.js Runtime"),
        "nodejs" to PackageInfo("node", "20.5.1", "Node.js Runtime"),
        "git" to PackageInfo("git", "2.42.0", "Version Control"),
        "vim" to PackageInfo("vim", "9.0", "Text Editor"),
        "nano" to PackageInfo("nano", "7.2", "Text Editor"),
        "nginx" to PackageInfo("nginx", "1.24.0", "HTTP Server"),
        "mysql" to PackageInfo("mysql", "8.0.33", "SQL Database"),
        "postgresql" to PackageInfo("postgresql", "15.3", "SQL Database"),
        "redis" to PackageInfo("redis", "7.0.11", "Key-Value Store"),
        "mongodb" to PackageInfo("mongodb", "6.0.6", "NoSQL Database"),
        "sqlite" to PackageInfo("sqlite", "3.41.2", "Embedded SQL"),
        "openssh" to PackageInfo("openssh", "9.3", "Secure Shell"),
        "nmap" to PackageInfo("nmap", "7.93", "Network Scanner"),
        "curl" to PackageInfo("curl", "8.2.1", "URL Transfer"),
        "wget" to PackageInfo("wget", "1.21.4", "Downloader"),
        "php" to PackageInfo("php", "8.2.8", "Web Language"),
        "ruby" to PackageInfo("ruby", "3.2.2", "Web Language"),
        "go" to PackageInfo("go", "1.20.6", "System Language"),
        "rust" to PackageInfo("rust", "1.71.0", "Safe Language"),
        "gcc" to PackageInfo("gcc", "12.3.0", "C Compiler"),
        "docker" to PackageInfo("docker", "24.0.4", "Container Engine"),
        "tmux" to PackageInfo("tmux", "3.3a", "Terminal Mux"),
        "htop" to PackageInfo("htop", "3.2.2", "Process Viewer"),
        "ffmpeg" to PackageInfo("ffmpeg", "6.0", "Media Tool"),
        "metasploit" to PackageInfo("metasploit", "6.3.26", "Exploit Framework"),
        "hydra" to PackageInfo("hydra", "9.5", "Password Cracker"),
        "sqlmap" to PackageInfo("sqlmap", "1.7.7", "SQL Injector"),
        "aircrack-ng" to PackageInfo("aircrack-ng", "1.7", "WiFi Auditor")
    )

    init {
        setupDirectories()
        // Initialize all wrappers on startup based on current installation state
        availablePackages.keys.forEach { createWrapper(it, isInstalled = isInstalled(it)) }
    }

    private fun setupDirectories() {
        listOf(BIN_DIR, PACKAGES_DIR).forEach { if (!it.exists()) it.mkdirs() }
    }

    private fun createWrapper(name: String, isInstalled: Boolean) {
        val file = File(BIN_DIR, name)
        val info = availablePackages[name]
        val content = if (isInstalled) {
            when (name) {
                // Interpreters / Compilers
                "python" -> "#!/system/bin/sh\necho \"Python ${info?.version} (NexTerm)\"\necho \">>> \"\nread -p \"\" cmd"
                "node", "nodejs" -> "#!/system/bin/sh\necho \"Welcome to Node.js v${info?.version}\"\necho \"> \"\nread -p \"\" cmd"
                "php" -> "#!/system/bin/sh\necho \"PHP ${info?.version} (cli)\"\necho \"php > \"\nread -p \"\" cmd"
                "ruby" -> "#!/system/bin/sh\necho \"ruby ${info?.version} (irb)\"\necho \"irb> \"\nread -p \"\" cmd"
                "go" -> "#!/system/bin/sh\necho \"go version go${info?.version} android/arm64\""
                "rust" -> "#!/system/bin/sh\necho \"rustc ${info?.version} (stable)\""
                "gcc" -> "#!/system/bin/sh\necho \"gcc (NexTerm Binaries) ${info?.version}\""
                
                // Databases
                "mysql", "postgresql", "mongodb", "redis", "sqlite" -> "#!/system/bin/sh\necho \"Connecting to ${name} server v${info?.version}...\"\necho \"${name}> \"\nread -p \"\" cmd"
                
                // Servers
                "nginx", "apache" -> "#!/system/bin/sh\necho \"Starting ${name} service...\"\necho \"[OK] Service started on http://127.0.0.1:8080/\""
                
                // Pentest Tools
                "metasploit" -> "#!/system/bin/sh\necho \"msfconsole v${info?.version}\"\necho \"msf6 > \"\nread -p \"\" cmd"
                "sqlmap" -> "#!/system/bin/sh\necho \"sqlmap/${info?.version} - automatic SQL injection tool\""
                "nmap" -> "#!/system/bin/sh\necho \"Nmap ${info?.version} ( https://nmap.org )\""
                "hydra" -> "#!/system/bin/sh\necho \"Hydra v${info?.version} - Parallelized login cracker\""
                "aircrack-ng" -> "#!/system/bin/sh\necho \"Aircrack-ng ${info?.version} - WiFi security auditor\""
                
                // Utilities
                "git" -> "#!/system/bin/sh\necho \"git version ${info?.version}\"\necho \"Usage: git <command> [args]\""
                "vim", "nano" -> "#!/system/bin/sh\necho \"Opening ${name} editor...\"\necho \"[Simulation] File opened. Press CTRL+X to exit.\"\nread -p \"\" cmd"
                "htop" -> "#!/system/bin/sh\necho \"[H-TOP] CPU: 12% | MEM: 45% | Tasks: 124\"\nread -p \"Press Q to exit\" cmd"
                "curl", "wget" -> "#!/system/bin/sh\necho \"${name} v${info?.version}\"\necho \"Usage: ${name} [url]\""
                "ffmpeg" -> "#!/system/bin/sh\necho \"ffmpeg version ${info?.version} - Multimedia tools\""
                "tmux" -> "#!/system/bin/sh\necho \"tmux ${info?.version} - Terminal multiplexer\""
                "docker" -> "#!/system/bin/sh\necho \"Docker version ${info?.version}, build active\""
                
                else -> "#!/system/bin/sh\necho \"$name ${info?.version} is operational.\""
            }
        } else {
            "#!/system/bin/sh\necho \"NexTerm: $name not installed.\"\necho \"Run 'pkg install $name' to get it.\""
        }
        file.writeText(content)
        file.setReadable(true, false)
    }

    fun isInstalled(packageName: String): Boolean = File(PACKAGES_DIR, packageName).exists()

    suspend fun install(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pkg = availablePackages[packageName] ?: return@withContext Result.failure(Exception("Package '$packageName' not found."))
            
            // Mark as installed
            val pkgDir = File(PACKAGES_DIR, packageName)
            pkgDir.mkdirs()
            File(pkgDir, "METADATA").writeText("Name: ${pkg.name}\nStatus: Installed")
            
            // Update wrapper to be functional
            createWrapper(packageName, true)
            if (packageName == "node") createWrapper("nodejs", true)
            if (packageName == "nodejs") createWrapper("node", true)
            
            Result.success("Installed ${pkg.name}")
        } catch (e: Exception) { Result.failure(e) }
    }

    fun listAvailable() = availablePackages
    fun getInfo(packageName: String) = availablePackages[packageName]
    fun search(query: String) = availablePackages.values.filter { it.name.contains(query, true) }
    suspend fun update() = Result.success("Repositories updated.")
    suspend fun upgrade() = Result.success("Already up to date.")
    fun listInstalled() = PACKAGES_DIR.listFiles()?.map { it.name } ?: emptyList()
    suspend fun remove(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        File(PACKAGES_DIR, packageName).deleteRecursively()
        createWrapper(packageName, false)
        Result.success("Removed")
    }
}

data class PackageInfo(val name: String, val version: String, val description: String)
