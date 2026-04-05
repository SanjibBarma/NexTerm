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
        Log.e("BootstrapManager", "initialize() called")
        createDirectories()
        installSafeBinaryBootstrap()
        writeProfile()
        writeShellRc()
        writeCommonScripts()
        writePkgScript()
        writePackageMetadata()
        binaryResolver.symlinkOrCopyFallbacks()
        Log.e("BootstrapManager", "initialize() finished")
    }

    private fun installSafeBinaryBootstrap() {
        val manifest = BinaryBootstrapManifest.manifestForCurrentAbi()
        Log.e("BootstrapManager", "installSafeBinaryBootstrap abi=${manifest.abi} version=${manifest.version}")
        runCatching {
            binaryInstaller.installOrUpdateIfNeeded(manifest)
        }.onSuccess {
            Log.e("BootstrapManager", "binary bootstrap installedOrUpdated=$it")
        }.onFailure {
            Log.e("BootstrapManager", "binary bootstrap failed: ${it.message}", it)
        }
    }

    private fun createDirectories() {
        listOf(
            usrDir,
            binDir,
            etcDir,
            profileDDir,
            libDir,
            shareDir,
            packagesDir,
            tmpDir
        ).forEach {
            if (!it.exists()) {
                val created = it.mkdirs()
                Log.e("BootstrapManager", "mkdir ${it.absolutePath} = $created")
            } else {
                Log.e("BootstrapManager", "exists ${it.absolutePath}")
            }
        }
    }

    private fun writeProfile() {
        val profile = File(etcDir, "profile")
        profile.writeText(
            """
            export PREFIX=${usrDir.absolutePath}
            export HOME=${homeDir.absolutePath}
            export TMPDIR=${tmpDir.absolutePath}
            export PATH=${binDir.absolutePath}:/system/bin:/system/xbin:/vendor/bin
            export LD_LIBRARY_PATH=${libDir.absolutePath}
            export TERM=xterm-256color
            export COLORTERM=truecolor
            export LANG=en_US.UTF-8
            export LC_ALL=en_US.UTF-8
            export EDITOR=nano
            export VISUAL=nano
            export PAGER=cat
            export PS1='nexterm$ '
            
            l() { ls "${'$'}@"; }
            la() { ls -a "${'$'}@"; }
            ll() { ls -l "${'$'}@"; }
            cls() { printf '\033[2J\033[H'; }

            help() {
              echo "NexTerm Help"
              echo "-----------------------------"
              echo "Shell: /system/bin/sh (PTY-backed)"
              echo ""
              echo "Core commands:"
              echo "  ls, cat, cp, mv, rm, mkdir, touch, pwd, echo"
              echo ""
              echo "Shortcuts:"
              echo "  l, la, ll, cls"
              echo ""
              echo "Helpers:"
              echo "  help, pkg, which2, neofetch"
              echo ""
            }
            
            which2() {
              if [ -z "${'$'}1" ]; then
                echo "usage: which2 command"
                return 1
              fi
              OLD_IFS="${'$'}IFS"
              IFS=":"
              for p in ${'$'}PATH; do
                if [ -x "${'$'}p/${'$'}1" ]; then
                  echo "${'$'}p/${'$'}1"
                  IFS="${'$'}OLD_IFS"
                  return 0
                fi
              done
              IFS="${'$'}OLD_IFS"
              return 1
            }

            neofetch() {
              echo ""
              echo "NexTerm"
              echo "OS: Android $(getprop ro.build.version.release)"
              echo "Device: $(getprop ro.product.model)"
              echo "Kernel: $(uname -r)"
              echo "Shell: /system/bin/sh"
              echo "Prefix: ${usrDir.absolutePath}"
              echo ""
            }

            pkg() {
              case "${'$'}1" in
                ""|help|-h|--help)
                  echo "NexTerm Safe Package Registry"
                  echo "Usage:"
                  echo "  pkg list"
                  echo "  pkg search [query]"
                  echo "  pkg show [name]"
                  ;;
                list)
                  cat "${packagesDir.absolutePath}/INDEX"
                  ;;
                search)
                  if [ -z "${'$'}2" ]; then
                    echo "pkg search: missing query"
                    return 1
                  fi
                  grep "${'$'}2" "${packagesDir.absolutePath}/INDEX"
                  ;;
                show)
                  if [ -z "${'$'}2" ]; then
                    echo "pkg show: missing package name"
                    return 1
                  fi
                  if [ -f "${packagesDir.absolutePath}/${'$'}2/METADATA" ]; then
                    cat "${packagesDir.absolutePath}/${'$'}2/METADATA"
                  else
                    echo "Package not found: ${'$'}2"
                    return 1
                  fi
                  ;;
                *)
                  echo "Unknown subcommand: ${'$'}1"
                  return 1
                  ;;
              esac
            }

            if [ -d "${profileDDir.absolutePath}" ]; then
              for f in "${profileDDir.absolutePath}"/*.sh; do
                [ -f "${'$'}f" ] && . "${'$'}f"
              done
            fi
            """.trimIndent()
        )
        Log.e("BootstrapManager", "profile written: ${profile.absolutePath}")
    }

    private fun writeShellRc() {
        val shrc = File(homeDir, ".shrc")
        shrc.writeText(
            """
            [ -f "${etcDir.absolutePath}/profile" ] && . "${etcDir.absolutePath}/profile"
            """.trimIndent()
        )
        Log.e("BootstrapManager", ".shrc written: ${shrc.absolutePath}")
    }

    private fun writeCommonScripts() {
        writeScriptIfAbsent(
            "grep",
            """
            #!/system/bin/sh
            if [ ${'$'}# -lt 2 ]; then
              echo "usage: grep pattern file"
              exit 1
            fi
            pattern="${'$'}1"
            file="${'$'}2"
            if [ ! -f "${'$'}file" ]; then
              echo "grep: ${'$'}file: No such file"
              exit 1
            fi
            while IFS= read -r line; do
              case "${'$'}line" in
                *"${'$'}pattern"*) echo "${'$'}line" ;;
              esac
            done < "${'$'}file"
            """.trimIndent()
        )

        writeRuntimeWrapperIfAbsent("python", "Python 3.11 wrapper", "python")
        writeRuntimeWrapperIfAbsent("node", "Node.js 18 wrapper", "node")
        writeRuntimeWrapperIfAbsent("nodejs", "Node.js 18 wrapper", "nodejs")
        writeRuntimeWrapperIfAbsent("git", "Git 2.40 wrapper", "git")
        writeRuntimeWrapperIfAbsent("vim", "Vim launcher wrapper", "vim")
        writeRuntimeWrapperIfAbsent("nano", "Nano launcher wrapper", "nano")
        writeRuntimeWrapperIfAbsent("php", "PHP 8.2 wrapper", "php")
        writeRuntimeWrapperIfAbsent("ruby", "Ruby 3.2 wrapper", "ruby")
        writeRuntimeWrapperIfAbsent("go", "Go 1.20 wrapper", "go")
        writeRuntimeWrapperIfAbsent("gcc", "GCC 12.2 wrapper", "gcc")
        writeRuntimeWrapperIfAbsent("make", "GNU Make 4.4 wrapper", "make")
        writeRuntimeWrapperIfAbsent("curl", "Curl 8.0 wrapper", "curl")
        writeRuntimeWrapperIfAbsent("wget", "Wget 1.21 wrapper", "wget")
    }

    private fun writePkgScript() {
        val file = File(binDir, "pkg")
        file.writeText(
            """
            #!/system/bin/sh
            echo "Use shell function 'pkg' from sourced profile."
            """.trimIndent()
        )
        file.setReadable(true, false)
        file.setWritable(true, true)
        file.setExecutable(true, false)
        Log.e("BootstrapManager", "pkg stub written: ${file.absolutePath}")
    }

    private fun writePackageMetadata() {
        val index = File(packagesDir, "INDEX")
        index.writeText(
            SafePackageRegistry.all().joinToString("\n") {
                "${it.name.padEnd(14)} ${it.version.padEnd(8)} ${it.category.padEnd(12)} ${it.description}"
            } + "\n"
        )
        Log.e("BootstrapManager", "INDEX written: ${index.absolutePath}")

        SafePackageRegistry.all().forEach { pkg ->
            val dir = File(packagesDir, pkg.name)
            if (!dir.exists()) dir.mkdirs()

            File(dir, "METADATA").writeText(
                """
                Name: ${pkg.name}
                Version: ${pkg.version}
                Category: ${pkg.category}
                Description: ${pkg.description}
                Type: safe-registry-wrapper-or-real-binary
                Installed: available-by-default
                """.trimIndent()
            )
            Log.e("BootstrapManager", "METADATA written for ${pkg.name}")
        }
    }

    private fun writeRuntimeWrapperIfAbsent(
        commandName: String,
        displayName: String,
        logicalName: String
    ) {
        val target = File(binDir, commandName)
        if (target.exists()) {
            Log.e("BootstrapManager", "wrapper exists: $commandName")
            return
        }

        writeScript(
            commandName,
            """
            #!/system/bin/sh
            echo "$displayName"
            echo "NexTerm bootstrap command wrapper"
            echo ""
            echo "Command: $logicalName"
            echo "Arguments: ${'$'}@"
            echo ""
            echo "No real embedded binary is installed for '$logicalName' yet."
            """.trimIndent()
        )
    }

    private fun writeScriptIfAbsent(name: String, content: String) {
        val target = File(binDir, name)
        if (!target.exists()) {
            writeScript(name, content)
        } else {
            Log.e("BootstrapManager", "script exists: $name")
        }
    }

    private fun writeScript(name: String, content: String) {
        val file = File(binDir, name)
        file.writeText(content)
        file.setReadable(true, false)
        file.setWritable(true, true)
        file.setExecutable(true, false)
        Log.e("BootstrapManager", "script written: ${file.absolutePath}")
    }
}