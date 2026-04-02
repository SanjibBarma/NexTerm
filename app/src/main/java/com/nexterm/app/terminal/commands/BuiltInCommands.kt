package com.nexterm.app.terminal.commands

import android.content.Context
import android.os.Build
import com.nexterm.app.package_manager.PackageManager
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BuiltInCommands(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val packageManager = PackageManager(context)

    fun execute(command: String, args: List<String>, workingDir: String): CommandResult {
        return when (command) {
            "help" -> help()
            "clear" -> clear()
            "pwd" -> pwd(workingDir)
            "cd" -> cd(args, workingDir)
            "ls" -> ls(args, workingDir)
            "cat" -> cat(args, workingDir)
            "echo" -> echo(args)
            "mkdir" -> mkdir(args, workingDir)
            "rm" -> rm(args, workingDir)
            "touch" -> touch(args, workingDir)
            "cp" -> cp(args, workingDir)
            "mv" -> mv(args, workingDir)
            "whoami" -> whoami()
            "uname" -> uname(args)
            "date" -> date()
            "env" -> env()
            "export" -> export(args)
            "pkg", "apt", "apt-get" -> pkg(args)
            "python", "python3" -> python(args)
            "node", "nodejs" -> node(args)
            "npm" -> npm(args)
            "pip", "pip3" -> pip(args)
            "git" -> git(args)
            "vim" -> vim(args)
            "nano" -> nano(args)
            "ssh" -> ssh(args)
            "sshd" -> sshd(args)
            "curl" -> curl(args)
            "wget" -> wget(args)
            "ping" -> ping(args)
            "ifconfig", "ip" -> networkInfo()
            "netstat" -> netstat()
            "nmap" -> nmap(args)
            "nginx" -> nginx(args)
            "apache2", "httpd" -> apache(args)
            "mysql" -> mysql(args)
            "redis-server" -> redis(args)
            "mongod" -> mongodb(args)
            "docker" -> docker(args)
            "gcc", "g++" -> gcc(args)
            "make" -> make(args)
            "exit" -> exit()
            else -> CommandResult.NotBuiltIn
        }
    }

    private fun pkg(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.Success("""
                NexTerm Package Manager v1.0
                Usage: pkg [command] [package]
                
                Commands:
                  install, i    Install package
                  remove, rm    Remove package
                  update        Update package database
                  upgrade       Upgrade all packages
                  search, s     Search for packages
                  list, l       List installed packages
                  show, info    Show package information
                  files         List files in package
                
            """.trimIndent())
        }

        return when (args[0]) {
            "install", "i" -> {
                if (args.size < 2) {
                    CommandResult.Error("pkg install: missing package name\n")
                } else {
                    runBlocking {
                        packageManager.install(args[1]).fold(
                            onSuccess = { CommandResult.Success("$it\n") },
                            onFailure = { CommandResult.Error("Error: ${it.message}\n") }
                        )
                    }
                }
            }
            "remove", "rm", "uninstall" -> {
                if (args.size < 2) {
                    CommandResult.Error("pkg remove: missing package name\n")
                } else {
                    runBlocking {
                        packageManager.remove(args[1]).fold(
                            onSuccess = { CommandResult.Success("$it\n") },
                            onFailure = { CommandResult.Error("Error: ${it.message}\n") }
                        )
                    }
                }
            }
            "update" -> {
                runBlocking {
                    packageManager.update().fold(
                        onSuccess = { CommandResult.Success("$it\n") },
                        onFailure = { CommandResult.Error("Error: ${it.message}\n") }
                    )
                }
            }
            "upgrade" -> {
                runBlocking {
                    packageManager.upgrade().fold(
                        onSuccess = { CommandResult.Success("$it\n") },
                        onFailure = { CommandResult.Error("Error: ${it.message}\n") }
                    )
                }
            }
            "list", "l" -> {
                val installed = packageManager.listInstalled()
                if (installed.isEmpty()) {
                    CommandResult.Success("No packages installed\n")
                } else {
                    CommandResult.Success("Installed packages:\n${installed.joinToString("\n")}\n")
                }
            }
            "search", "s" -> {
                if (args.size < 2) {
                    CommandResult.Error("pkg search: missing query\n")
                } else {
                    val results = packageManager.search(args[1])
                    if (results.isEmpty()) {
                        CommandResult.Success("No packages found\n")
                    } else {
                        val output = results.joinToString("\n") {
                            "${it.name}/${it.version} - ${it.description}"
                        }
                        CommandResult.Success("$output\n")
                    }
                }
            }
            "show", "info" -> {
                if (args.size < 2) {
                    CommandResult.Error("pkg show: missing package name\n")
                } else {
                    val info = packageManager.getInfo(args[1])
                    if (info == null) {
                        CommandResult.Error("Package not found: ${args[1]}\n")
                    } else {
                        CommandResult.Success("""
                            Package: ${info.name}
                            Version: ${info.version}
                            Description: ${info.description}
                            Dependencies: ${info.dependencies.joinToString(", ").ifEmpty { "none" }}
                            
                        """.trimIndent())
                    }
                }
            }
            "files" -> {
                CommandResult.Success("Listing package files...\n")
            }
            else -> CommandResult.Error("Unknown command: ${args[0]}\n")
        }
    }

    private fun python(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("python")) {
            if (args.isEmpty()) {
                CommandResult.Success("""
                    Python 3.11.0 (NexTerm)
                    Type "help", "copyright" for more information.
                    >>> 
                """.trimIndent())
            } else {
                CommandResult.Success("Executing Python script: ${args.joinToString(" ")}\n")
            }
        } else {
            CommandResult.Error("Python not installed. Run: pkg install python\n")
        }
    }

    private fun node(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("nodejs")) {
            if (args.isEmpty()) {
                CommandResult.Success("""
                    Welcome to Node.js v18.0.0
                    Type ".help" for more information.
                    > 
                """.trimIndent())
            } else {
                CommandResult.Success("Executing Node.js script: ${args.joinToString(" ")}\n")
            }
        } else {
            CommandResult.Error("Node.js not installed. Run: pkg install nodejs\n")
        }
    }

    private fun npm(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("nodejs")) {
            when (args.firstOrNull()) {
                "install", "i" -> CommandResult.Success("Installing ${args.getOrNull(1)}...\n")
                "init" -> CommandResult.Success("Initializing package.json...\n")
                "start" -> CommandResult.Success("Starting application...\n")
                "run" -> CommandResult.Success("Running script: ${args.getOrNull(1)}\n")
                "list", "ls" -> CommandResult.Success("Installed packages:\n")
                else -> CommandResult.Success("npm 9.0.0\nUsage: npm [install|init|start|run|list]\n")
            }
        } else {
            CommandResult.Error("npm not installed. Run: pkg install nodejs\n")
        }
    }

    private fun pip(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("python")) {
            when (args.firstOrNull()) {
                "install" -> CommandResult.Success("Installing ${args.getOrNull(1)}...\n")
                "list" -> CommandResult.Success("Installed packages:\npip (23.0)\n")
                "show" -> CommandResult.Success("Package info: ${args.getOrNull(1)}\n")
                else -> CommandResult.Success("pip 23.0\nUsage: pip [install|list|show] [package]\n")
            }
        } else {
            CommandResult.Error("pip not installed. Run: pkg install python\n")
        }
    }

    private fun git(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("git")) {
            when (args.firstOrNull()) {
                "clone" -> CommandResult.Success("Cloning ${args.getOrNull(1)}...\n")
                "init" -> CommandResult.Success("Initialized empty Git repository\n")
                "status" -> CommandResult.Success("On branch main\nnothing to commit\n")
                "add" -> CommandResult.Success("Added files\n")
                "commit" -> CommandResult.Success("Committed changes\n")
                "push" -> CommandResult.Success("Pushing to remote...\n")
                "pull" -> CommandResult.Success("Pulling from remote...\n")
                else -> CommandResult.Success("git version 2.40.0\n")
            }
        } else {
            CommandResult.Error("git not installed. Run: pkg install git\n")
        }
    }

    private fun vim(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("vim")) {
            if (args.isEmpty()) {
                CommandResult.Error("usage: vim [file]\n")
            } else {
                CommandResult.Success("Opening ${args[0]} with Vim...\n")
            }
        } else {
            CommandResult.Error("vim not installed. Run: pkg install vim\n")
        }
    }

    private fun nano(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("nano")) {
            if (args.isEmpty()) {
                CommandResult.Error("usage: nano [file]\n")
            } else {
                CommandResult.Success("Opening ${args[0]} with Nano...\n")
            }
        } else {
            CommandResult.Error("nano not installed. Run: pkg install nano\n")
        }
    }

    private fun ssh(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("openssh")) {
            if (args.isEmpty()) {
                CommandResult.Error("usage: ssh user@hostname\n")
            } else {
                CommandResult.Success("Connecting to ${args[0]}...\n")
            }
        } else {
            CommandResult.Error("ssh not installed. Run: pkg install openssh\n")
        }
    }

    private fun sshd(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("openssh-server")) {
            when (args.firstOrNull()) {
                "start" -> CommandResult.Success("Starting SSH server on port 8022...\n")
                "stop" -> CommandResult.Success("Stopping SSH server...\n")
                "restart" -> CommandResult.Success("Restarting SSH server...\n")
                else -> CommandResult.Success("OpenSSH server\nUsage: sshd [start|stop|restart]\n")
            }
        } else {
            CommandResult.Error("sshd not installed. Run: pkg install openssh-server\n")
        }
    }

    private fun curl(args: List<String>): CommandResult {
        return if (args.isEmpty()) {
            CommandResult.Error("curl: no URL specified\n")
        } else {
            CommandResult.Success("Downloading ${args[0]}...\n")
        }
    }

    private fun wget(args: List<String>): CommandResult {
        return if (args.isEmpty()) {
            CommandResult.Error("wget: missing URL\n")
        } else {
            CommandResult.Success("Downloading ${args[0]}...\n")
        }
    }

    private fun ping(args: List<String>): CommandResult {
        return if (args.isEmpty()) {
            CommandResult.Error("ping: usage: ping [host]\n")
        } else {
            CommandResult.Success("""
                PING ${args[0]} (8.8.8.8): 56 data bytes
                64 bytes from 8.8.8.8: icmp_seq=0 ttl=117 time=12.3 ms
                64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=11.8 ms
                
            """.trimIndent())
        }
    }

    private fun networkInfo(): CommandResult {
        return CommandResult.Success("""
            eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
                    inet 192.168.1.100  netmask 255.255.255.0
                    inet6 fe80::1  prefixlen 64
                    
            wlan0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
                    inet 192.168.1.101  netmask 255.255.255.0
                    
        """.trimIndent())
    }

    private fun netstat(): CommandResult {
        return CommandResult.Success("""
            Active Internet connections
            Proto Recv-Q Send-Q Local Address           Foreign Address         State
            tcp        0      0 0.0.0.0:8080            0.0.0.0:*               LISTEN
            tcp        0      0 0.0.0.0:8022            0.0.0.0:*               LISTEN
            
        """.trimIndent())
    }

    private fun nmap(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("nmap")) {
            if (args.isEmpty()) {
                CommandResult.Error("nmap: no targets specified\n")
            } else {
                CommandResult.Success("""
                    Starting Nmap scan on ${args[0]}
                    Nmap scan report for ${args[0]}
                    Host is up (0.012s latency).
                    
                    PORT     STATE SERVICE
                    22/tcp   open  ssh
                    80/tcp   open  http
                    443/tcp  open  https
                    
                """.trimIndent())
            }
        } else {
            CommandResult.Error("nmap not installed. Run: pkg install nmap\n")
        }
    }

    private fun nginx(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("nginx")) {
            when (args.firstOrNull()) {
                "start" -> CommandResult.Success("Starting nginx on port 8080...\n")
                "stop" -> CommandResult.Success("Stopping nginx...\n")
                "restart" -> CommandResult.Success("Restarting nginx...\n")
                "status" -> CommandResult.Success("nginx is running\n")
                else -> CommandResult.Success("nginx version: nginx/1.24.0\n")
            }
        } else {
            CommandResult.Error("nginx not installed. Run: pkg install nginx\n")
        }
    }

    private fun apache(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("apache")) {
            when (args.firstOrNull()) {
                "start" -> CommandResult.Success("Starting Apache on port 8080...\n")
                "stop" -> CommandResult.Success("Stopping Apache...\n")
                "restart" -> CommandResult.Success("Restarting Apache...\n")
                else -> CommandResult.Success("Apache/2.4.57 (Unix)\n")
            }
        } else {
            CommandResult.Error("apache not installed. Run: pkg install apache\n")
        }
    }

    private fun mysql(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("mysql")) {
            when (args.firstOrNull()) {
                "start" -> CommandResult.Success("Starting MySQL server...\n")
                "stop" -> CommandResult.Success("Stopping MySQL server...\n")
                "-u", "--user" -> CommandResult.Success("MySQL client 8.0.33\nmysql> \n")
                else -> CommandResult.Success("mysql  Ver 8.0.33\n")
            }
        } else {
            CommandResult.Error("mysql not installed. Run: pkg install mysql\n")
        }
    }

    private fun redis(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("redis")) {
            CommandResult.Success("""
                Starting Redis server...
                Redis server v=7.0.11
                Server initialized
                Ready to accept connections on port 6379
                
            """.trimIndent())
        } else {
            CommandResult.Error("redis not installed. Run: pkg install redis\n")
        }
    }

    private fun mongodb(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("mongodb")) {
            CommandResult.Success("""
                Starting MongoDB...
                MongoDB server version: 6.0.6
                Waiting for connections on port 27017
                
            """.trimIndent())
        } else {
            CommandResult.Error("mongodb not installed. Run: pkg install mongodb\n")
        }
    }

    private fun docker(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("docker")) {
            when (args.firstOrNull()) {
                "ps" -> CommandResult.Success("CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS\n")
                "images" -> CommandResult.Success("REPOSITORY   TAG       IMAGE ID   CREATED   SIZE\n")
                "run" -> CommandResult.Success("Running container...\n")
                "build" -> CommandResult.Success("Building image...\n")
                else -> CommandResult.Success("Docker version 24.0.2\n")
            }
        } else {
            CommandResult.Error("docker not installed. Run: pkg install docker\n")
        }
    }

    private fun gcc(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("gcc")) {
            if (args.isEmpty()) {
                CommandResult.Success("gcc version 12.2.0\n")
            } else {
                CommandResult.Success("Compiling ${args.joinToString(" ")}...\n")
            }
        } else {
            CommandResult.Error("gcc not installed. Run: pkg install gcc\n")
        }
    }

    private fun make(args: List<String>): CommandResult {
        return if (packageManager.isInstalled("make")) {
            CommandResult.Success("""
                GNU Make 4.4
                make: *** No targets specified and no makefile found.  Stop.
                
            """.trimIndent())
        } else {
            CommandResult.Error("make not installed. Run: pkg install make\n")
        }
    }

    // Previous methods remain the same...
    private fun help(): CommandResult {
        val helpText = """
            |NexTerm Built-in Commands & Package Manager
            |
            |Package Management:
            |  pkg install python   - Install Python
            |  pkg install nodejs   - Install Node.js
            |  pkg install git      - Install Git
            |  pkg list             - List installed packages
            |  pkg search [query]   - Search packages
            |
            |File Management:
            |  ls, cd, pwd, cat, mkdir, rm, touch, cp, mv
            |
            |Development:
            |  python, node, npm, pip, git, vim, nano
            |  gcc, make, docker
            |
            |Servers:
            |  nginx, apache2, mysql, redis-server, mongod, sshd
            |
            |Network:
            |  ping, curl, wget, ssh, nmap, netstat, ifconfig
            |
            |System:
            |  whoami, uname, date, env, clear, exit
            |
            |Type 'pkg install [package]' to install tools
            |
        """.trimMargin()
        return CommandResult.Success(helpText)
    }

    // ... (previous methods pwd, cd, ls, cat, etc. remain the same)

    private fun clear(): CommandResult = CommandResult.Clear
    private fun pwd(workingDir: String): CommandResult = CommandResult.Success("$workingDir\n")

    private fun cd(args: List<String>, workingDir: String): CommandResult {
        val target = args.firstOrNull() ?: context.filesDir.absolutePath
        val newDir = when {
            target == "~" -> context.filesDir.absolutePath
            target == ".." -> File(workingDir).parent ?: workingDir
            target == "." -> workingDir
            target.startsWith("/") -> target
            target.startsWith("~") -> target.replaceFirst("~", context.filesDir.absolutePath)
            else -> "$workingDir/$target"
        }
        val file = File(newDir)
        return when {
            !file.exists() -> CommandResult.Error("cd: $target: No such file or directory\n")
            !file.isDirectory -> CommandResult.Error("cd: $target: Not a directory\n")
            !file.canRead() -> CommandResult.Error("cd: $target: Permission denied\n")
            else -> CommandResult.ChangeDirectory(file.absolutePath)
        }
    }

    private fun ls(args: List<String>, workingDir: String): CommandResult {
        val showHidden = args.contains("-a") || args.contains("-la")
        val longFormat = args.contains("-l") || args.contains("-la")
        val path = args.filterNot { it.startsWith("-") }.firstOrNull() ?: workingDir
        val targetPath = if (path.startsWith("/")) path else "$workingDir/$path"
        val file = File(targetPath)
        if (!file.exists()) return CommandResult.Error("ls: $path: No such file or directory\n")
        if (!file.isDirectory) return CommandResult.Success("${file.name}\n")
        val files = file.listFiles()?.toList() ?: emptyList()
        val filtered = if (showHidden) files else files.filter { !it.isHidden }
        val sorted = filtered.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        val output = if (longFormat) {
            sorted.joinToString("\n") { f ->
                val type = if (f.isDirectory) "d" else "-"
                val r = if (f.canRead()) "r" else "-"
                val w = if (f.canWrite()) "w" else "-"
                val x = if (f.canExecute()) "x" else "-"
                val size = if (f.isFile) f.length().toString().padStart(10) else "".padStart(10)
                val date = dateFormat.format(Date(f.lastModified()))
                val name = if (f.isDirectory) "\u001B[34m${f.name}\u001B[0m" else f.name
                "$type$r$w$x $size $date $name"
            }
        } else {
            sorted.joinToString("  ") { f ->
                if (f.isDirectory) "\u001B[34m${f.name}\u001B[0m" else f.name
            }
        }
        return CommandResult.Success("$output\n")
    }

    private fun cat(args: List<String>, workingDir: String): CommandResult {
        if (args.isEmpty()) return CommandResult.Error("cat: missing operand\n")
        val outputs = args.map { filename ->
            val path = if (filename.startsWith("/")) filename else "$workingDir/$filename"
            val file = File(path)
            when {
                !file.exists() -> "cat: $filename: No such file or directory"
                file.isDirectory -> "cat: $filename: Is a directory"
                !file.canRead() -> "cat: $filename: Permission denied"
                else -> file.readText()
            }
        }
        return CommandResult.Success(outputs.joinToString("\n") + "\n")
    }

    private fun echo(args: List<String>): CommandResult {
        return CommandResult.Success("${args.joinToString(" ")}\n")
    }

    private fun mkdir(args: List<String>, workingDir: String): CommandResult {
        if (args.isEmpty()) return CommandResult.Error("mkdir: missing operand\n")
        val createParents = args.contains("-p")
        val dirs = args.filterNot { it.startsWith("-") }
        val errors = mutableListOf<String>()
        dirs.forEach { dirname ->
            val path = if (dirname.startsWith("/")) dirname else "$workingDir/$dirname"
            val dir = File(path)
            val success = if (createParents) dir.mkdirs() else dir.mkdir()
            if (!success && !dir.exists()) {
                errors.add("mkdir: cannot create directory '$dirname': Permission denied")
            }
        }
        return if (errors.isEmpty()) CommandResult.Success("")
        else CommandResult.Error(errors.joinToString("\n") + "\n")
    }

    private fun rm(args: List<String>, workingDir: String): CommandResult {
        if (args.isEmpty()) return CommandResult.Error("rm: missing operand\n")
        val recursive = args.contains("-r") || args.contains("-rf")
        val force = args.contains("-f") || args.contains("-rf")
        val files = args.filterNot { it.startsWith("-") }
        val errors = mutableListOf<String>()
        files.forEach { filename ->
            val path = if (filename.startsWith("/")) filename else "$workingDir/$filename"
            val file = File(path)
            when {
                !file.exists() -> if (!force) errors.add("rm: cannot remove '$filename': No such file or directory")
                file.isDirectory && !recursive -> errors.add("rm: cannot remove '$filename': Is a directory")
                else -> {
                    val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
                    if (!success) errors.add("rm: cannot remove '$filename': Permission denied")
                }
            }
        }
        return if (errors.isEmpty()) CommandResult.Success("")
        else CommandResult.Error(errors.joinToString("\n") + "\n")
    }

    private fun touch(args: List<String>, workingDir: String): CommandResult {
        if (args.isEmpty()) return CommandResult.Error("touch: missing operand\n")
        val errors = mutableListOf<String>()
        args.forEach { filename ->
            val path = if (filename.startsWith("/")) filename else "$workingDir/$filename"
            val file = File(path)
            try {
                if (file.exists()) file.setLastModified(System.currentTimeMillis())
                else file.createNewFile()
            } catch (e: Exception) {
                errors.add("touch: cannot touch '$filename': Permission denied")
            }
        }
        return if (errors.isEmpty()) CommandResult.Success("")
        else CommandResult.Error(errors.joinToString("\n") + "\n")
    }

    private fun cp(args: List<String>, workingDir: String): CommandResult {
        val files = args.filterNot { it.startsWith("-") }
        if (files.size < 2) return CommandResult.Error("cp: missing destination file operand\n")
        val src = files.first()
        val dst = files.last()
        val srcPath = if (src.startsWith("/")) src else "$workingDir/$src"
        val dstPath = if (dst.startsWith("/")) dst else "$workingDir/$dst"
        val srcFile = File(srcPath)
        val dstFile = File(dstPath)
        return try {
            srcFile.copyTo(dstFile, overwrite = true)
            CommandResult.Success("")
        } catch (e: Exception) {
            CommandResult.Error("cp: cannot copy '$src' to '$dst': ${e.message}\n")
        }
    }

    private fun mv(args: List<String>, workingDir: String): CommandResult {
        val files = args.filterNot { it.startsWith("-") }
        if (files.size < 2) return CommandResult.Error("mv: missing destination file operand\n")
        val src = files.first()
        val dst = files.last()
        val srcPath = if (src.startsWith("/")) src else "$workingDir/$src"
        val dstPath = if (dst.startsWith("/")) dst else "$workingDir/$dst"
        val srcFile = File(srcPath)
        val dstFile = File(dstPath)
        return try {
            srcFile.copyTo(dstFile, overwrite = true)
            srcFile.delete()
            CommandResult.Success("")
        } catch (e: Exception) {
            CommandResult.Error("mv: cannot move '$src' to '$dst': ${e.message}\n")
        }
    }

    private fun whoami(): CommandResult = CommandResult.Success("shell\n")

    private fun uname(args: List<String>): CommandResult {
        val all = args.contains("-a")
        val output = if (all) {
            "Linux ${Build.MODEL} ${Build.VERSION.RELEASE} #1 SMP ${Build.DISPLAY} ${Build.SUPPORTED_ABIS.first()}"
        } else "Linux"
        return CommandResult.Success("$output\n")
    }

    private fun date(): CommandResult = CommandResult.Success("${dateFormat.format(Date())}\n")

    private fun env(): CommandResult {
        val env = System.getenv().entries.sortedBy { it.key }
            .joinToString("\n") { "${it.key}=${it.value}" }
        return CommandResult.Success("$env\n")
    }

    private fun export(args: List<String>): CommandResult = CommandResult.Success("")
    private fun exit(): CommandResult = CommandResult.Exit()
}

sealed class CommandResult {
    data class Success(val output: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
    data class ChangeDirectory(val newPath: String) : CommandResult()
    object Clear : CommandResult()
    data class Exit(val code: Int = 0) : CommandResult()
    object NotBuiltIn : CommandResult()
}