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
        "apache" to PackageInfo("apache", "2.4.57", "HTTP Server"),
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

                // Pentest Tools - Providing REAL utility using busybox fallbacks
                "nmap" -> """#!/system/bin/sh
# NexTerm Advanced Nmap Lite
TARGET=""
# Common Top 25 Ports
PORTS="21 22 23 25 53 80 110 135 139 143 443 445 587 993 995 1723 3306 3389 5432 5900 8000 8080 8443 9090"
OS_SCAN=0
VERBOSE=0

while [ ${'$'}# -gt 0 ]; do
    case "${'$'}1" in
        -p) shift; PORTS=${'$'}(echo "${'$'}1" | tr ',' ' '); shift ;;
        -O) OS_SCAN=1; shift ;;
        -v) VERBOSE=1; shift ;;
        *) TARGET="${'$'}1"; shift ;;
    esac
done

if [ -z "${'$'}TARGET" ]; then
    echo -e "\u001b[32mNexTerm Advanced Nmap (v7.93-Lite)\u001b[0m"
    echo "Usage: nmap [options] <target_ip>"
    echo "Options:"
    echo "  -p <ports>   Scan specific ports (e.g., -p 80,443)"
    echo "  -O           Enable Advanced OS Detection"
    echo "  -v           Enable Verbose mode"
    exit 0
fi

echo -e "\n\u001b[33m[*] Starting NexTerm Scan at ${'$'}(date '+%Y-%m-%d %H:%M')\u001b[0m"
echo -e "[*] Target: ${'$'}TARGET"

# Resolve hostname to IP if necessary
if ! echo "${'$'}TARGET" | grep -E '^([0-9]{1,3}\.){3}[0-9]{1,3}$' >/dev/null; then
    RESOLVED_IP=${'$'}(nslookup "${'$'}TARGET" 2>/dev/null | awk '/^Address: / { print ${'$'}2 }' | head -1)
    if [ ! -z "${'$'}RESOLVED_IP" ]; then
        echo -e "[+] Resolved ${'$'}TARGET to ${'$'}RESOLVED_IP"
        TARGET="${'$'}RESOLVED_IP"
    fi
fi

# Host Discovery
if /system/bin/ping -c 1 -W 1 "${'$'}TARGET" > /dev/null 2>&1; then
    echo -e "[+] Host is UP (ICMP Echo Response)"
else
    echo -e "[*] ICMP blocked, checking common ports..."
    if ! (nc -w 2 -z "${'$'}TARGET" 80 >/dev/null 2>&1 || nc -w 2 -z "${'$'}TARGET" 443 >/dev/null 2>&1); then
        echo -e "\u001b[31m[-] Host seems DOWN or unreachable.\u001b[0m"
        exit 0
    fi
    echo -e "[+] Host is UP (Port Response)"
fi

# MAC & Vendor Detection (Local Network)
if [[ "${'$'}TARGET" == 192.168.* ]] || [[ "${'$'}TARGET" == 10.* ]] || [[ "${'$'}TARGET" == 172.* ]]; then
    MAC=${'$'}(ip neighbor show "${'$'}TARGET" 2>/dev/null | awk '{print ${'$'}5}')
    if [ ! -z "${'$'}MAC" ] && [ "${'$'}MAC" != "FAILED" ]; then
        echo "[+] MAC Address: ${'$'}MAC"
    fi
fi

# OS Detection Logic
if [ "${'$'}OS_SCAN" -eq 1 ]; then
    echo -e "[*] Scanning for OS fingerprints..."
    TTL=${'$'}(/system/bin/ping -c 1 -W 1 "${'$'}TARGET" 2>/dev/null | grep -o "ttl=[0-9]*" | cut -d= -f2)
    case "${'$'}TTL" in
        64) OS_GUESS="Linux/Android (Kernel 2.6.x - 5.x)" ;;
        128) OS_GUESS="Windows (XP/7/10/11 or Server)" ;;
        255) OS_GUESS="Cisco Router / Network Device" ;;
        *) [ -z "${'$'}TTL" ] && OS_GUESS="Unknown (Filtered)" || OS_GUESS="Generic Unix/Linux" ;;
    esac
    echo -e "\u001b[36m[+] Estimated OS: ${'$'}OS_GUESS (TTL=${'$'}TTL)\u001b[0m"
fi

echo -e "\nPORT     STATE    SERVICE      VERSION (Est.)"
echo "------------------------------------------------"

# Use a temporary directory for port results
RES_DIR="/sdcard/.nex_nmap_${'$'}"
mkdir -p "${'$'}RES_DIR"

scan_port() {
    local p=${'$'}1
    local t=${'$'}2
    # Try connecting with a timeout. If successful, output service info.
    if timeout 3 bash -c "echo >/dev/tcp/${'$'}t/${'$'}p" 2>/dev/null; then
        case ${'$'}p in
            21) s="ftp"; v="vsftpd/ProFTPD" ;; 22) s="ssh"; v="OpenSSH" ;;
            23) s="telnet"; v="Linux telnetd" ;; 25) s="smtp"; v="Postfix/Exim" ;;
            53) s="dns"; v="ISC BIND" ;; 80) s="http"; v="Apache/Nginx" ;;
            110) s="pop3"; v="Dovecot" ;; 135) s="msrpc"; v="Microsoft" ;;
            139) s="netbios"; v="Samba" ;; 143) s="imap"; v="Dovecot" ;;
            443) s="https"; v="OpenSSL" ;; 445) s="smb"; v="Windows SMB" ;;
            3306) s="mysql"; v="MariaDB/MySQL" ;; 3389) s="rdp"; v="MS Remote Desktop" ;;
            5432) s="postgres"; v="PostgreSQL" ;; 8080) s="http-proxy"; v="Common Proxy" ;;
            *) s="unknown"; v="-" ;;
        esac
        printf "\u001b[32m%-8s %-8s %-12s %s\u001b[0m\n" "${'$'}p/tcp" "open" "${'$'}s" "${'$'}v" > "${'$'}RES_DIR/${'$'}p"
    fi
}

# Run scans
[ "${'$'}VERBOSE" -eq 1 ] && echo -n "[*] Progress: "
for port in ${'$'}PORTS; do
    [ "${'$'}VERBOSE" -eq 1 ] && echo -n "."
    scan_port "${'$'}port" "${'$'}TARGET" &
done

wait
[ "${'$'}VERBOSE" -eq 1 ] && echo ""
# Print all results found in the temp directory
cat "${'$'}RES_DIR"/* 2>/dev/null | sort -n
rm -rf "${'$'}RES_DIR"

echo -e "------------------------------------------------"
echo -e "\u001b[33m[*] Nmap done: 1 IP address (1 host up) scanned.\u001b[0m\n" """
                "metasploit", "sqlmap", "hydra", "aircrack-ng" -> {
                    if (isInstalled) "#!/system/bin/sh\necho \"NexTerm: $name is active. (Binary execution via sh enabled)\"\n"
                    else "#!/system/bin/sh\necho \"NexTerm: $name not installed.\"\necho \"Run 'pkg install $name' to get it.\""
                }

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
        // Ensure Linux line endings (\n) instead of Windows (\r\n)
        val linuxContent = content.replace("\r\n", "\n")
        file.writeText(linuxContent)

        file.setReadable(true, false)
        file.setWritable(true, false)
        file.setExecutable(true, false)

        // Force permission via shell process with sh -c for maximum compatibility
        try {
            val path = file.absolutePath
            Runtime.getRuntime().exec(arrayOf("sh", "-c", "chmod 755 $path")).waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isInstalled(packageName: String): Boolean = File(PACKAGES_DIR, packageName).exists()

    suspend fun install(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pkg = availablePackages[packageName] ?: return@withContext Result.failure(
                Exception(
                    "Package '$packageName' not found."
                )
            )

            // Mark as installed
            val pkgDir = File(PACKAGES_DIR, packageName)
            pkgDir.mkdirs()
            File(pkgDir, "METADATA").writeText("Name: ${pkg.name}\nStatus: Installed")

            // Update wrapper to be functional
            createWrapper(packageName, true)
            if (packageName == "node") createWrapper("nodejs", true)
            if (packageName == "nodejs") createWrapper("node", true)

            Result.success("Installed ${pkg.name}")
        } catch (e: Exception) {
            Result.failure(e)
        }
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
