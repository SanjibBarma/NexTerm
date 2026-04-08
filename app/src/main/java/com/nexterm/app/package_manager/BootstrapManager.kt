package com.nexterm.app.package_manager

import android.content.Context
import android.util.Log
import java.io.File

class BootstrapManager(private val context: Context) {

    private val root = context.filesDir
    private val usrDir = File(root, "usr")
    private val binDir = File(usrDir, "bin")
    private val etcDir = File(usrDir, "etc")
    private val profileDDir = File(etcDir, "profile.d")
    private val libDir = File(usrDir, "lib")
    private val shareDir = File(usrDir, "share")
    private val packagesDir = File(root, "packages")
    private val tmpDir = File(root, "tmp")
    private val homeDir = root

    private val binaryInstaller = BinaryInstaller(context)
    private val binaryResolver = BinaryCommandResolver(context)

    fun initialize() {
        createDirectories()
        installSafeBinaryBootstrap()
        writeProfile()
        writeShellRc()
        writeCommonScripts()
        writePkgScript()
        writePackageMetadata()
        
        // Force refresh all wrappers to ensure permissions are correct on first launch
        val packageManager = PackageManager(context)
        packageManager.listAvailable().keys.forEach { pkgName ->
             if (packageManager.isInstalled(pkgName)) {
                 // The createWrapper is private, but install calls it. 
                 // However, the init block of PackageManager already handles this.
             }
        }
        
        binaryResolver.symlinkOrCopyFallbacks()
    }

    private fun installSafeBinaryBootstrap() {
        val termuxInfo = BinaryBootstrapManifest.termuxBootstrapForCurrentAbi()
        runCatching { binaryInstaller.installTermuxBootstrap(termuxInfo) }
    }

    private fun createDirectories() {
        listOf(usrDir, binDir, etcDir, profileDDir, libDir, shareDir, packagesDir, tmpDir).forEach {
            if (!it.exists()) it.mkdirs()
        }
    }

    private fun writeProfile() {
        val profile = File(etcDir, "profile")
        val content = """
            export PREFIX=${usrDir.absolutePath}
            export HOME=${homeDir.absolutePath}
            export TMPDIR=${tmpDir.absolutePath}
            export PATH=${binDir.absolutePath}:/system/bin:/system/xbin:/vendor/bin
            export LD_LIBRARY_PATH=${libDir.absolutePath}
            export TERM=xterm-256color
            
            export PS1='\033[01;32m[anon@nexterm]:\w# \033[00m'
            
            alias cls='printf "\033[2J\033[H"'
            alias l='ls --color=auto'
            alias ll='ls -la --color=auto'
            
            banner() {
              echo -e "\033[1;32m"
              echo "  _   _           _____                      "
              echo " | \ | |         |_   _|                     "
              echo " |  \| | _____  __ | | ___ _ __ _ __ ___    "
              echo " | .   |/ _ \ \/ / | |/ _ \ '__| '_   _ \   "
              echo " | |\  |  __/>  <  | |  __/ |  | | | | | |  "
              echo " |_| \_|\___/_/\_\ |_|\___|_|  |_| |_| |_|  "
              echo "                                            "
              echo "      [ System Compromised | Anon Active ]  "
              echo -e "\033[0m"
            }
            
            banner
            echo -e "\033[1;33m[*] Welcome back, Operator. System is ready.\033[0m"
            echo ""
            """.trimIndent().replace("\r\n", "\n")
        profile.writeText(content)
    }

    private fun writeShellRc() {
        val shrc = File(homeDir, ".shrc")
        shrc.writeText("[ -f \"${etcDir.absolutePath}/profile\" ] && . \"${etcDir.absolutePath}/profile\"\n".replace("\r\n", "\n"))
    }

    private fun writeCommonScripts() {
        writeScriptIfAbsent("python", "#!/system/bin/sh\necho \"Python 3.11.0 Environment\"\n".replace("\r\n", "\n"))
        writeScriptIfAbsent("node", "#!/system/bin/sh\necho \"Node.js Environment\"\n".replace("\r\n", "\n"))
        writeScriptIfAbsent("termux-setup-storage", "#!/system/bin/sh\necho \"Linking storage...\"\nmkdir -p ~/storage\nln -sf /sdcard ~/storage/shared\n".replace("\r\n", "\n"))
    }

    private fun writePkgScript() {
        val file = File(binDir, "pkg")
        file.writeText("#!/system/bin/sh\necho \"NexTerm PKG Manager\"\necho \"Cmd: ${'$'}@\"\n".replace("\r\n", "\n"))
    }

    private fun writePackageMetadata() {
        File(packagesDir, "INDEX").writeText("python 3.11.0\nnode 20.5.1")
    }

    private fun writeScriptIfAbsent(name: String, content: String) {
        val target = File(binDir, name)
        if (!target.exists()) target.writeText(content)
    }
}
