package com.nexterm.app.package_manager

data class SafePackageInfo(
    val name: String,
    val version: String,
    val description: String,
    val category: String
)

object SafePackageRegistry {

    val packages = listOf(
        SafePackageInfo("python", "3.11", "Python runtime wrapper", "development"),
        SafePackageInfo("nodejs", "18", "Node.js runtime wrapper", "development"),
        SafePackageInfo("git", "2.40", "Git wrapper", "development"),
        SafePackageInfo("vim", "9.0", "Editor launcher wrapper", "editor"),
        SafePackageInfo("nano", "7.0", "Editor launcher wrapper", "editor"),
        SafePackageInfo("curl", "8.0", "HTTP request wrapper", "network"),
        SafePackageInfo("wget", "1.21", "Downloader wrapper", "network"),
        SafePackageInfo("php", "8.2", "PHP runtime wrapper", "development"),
        SafePackageInfo("ruby", "3.2", "Ruby runtime wrapper", "development"),
        SafePackageInfo("go", "1.20", "Go runtime wrapper", "development"),
        SafePackageInfo("gcc", "12.2", "Compiler wrapper", "development"),
        SafePackageInfo("make", "4.4", "Build tool wrapper", "development"),
        SafePackageInfo("nginx", "1.24", "HTTP server helper wrapper", "server"),
        SafePackageInfo("mysql", "8.0", "Database client wrapper", "database"),
        SafePackageInfo("postgresql", "15", "Database client wrapper", "database"),
        SafePackageInfo("redis", "7.0", "Redis helper wrapper", "database"),
        SafePackageInfo("mongodb", "6.0", "MongoDB helper wrapper", "database"),
        SafePackageInfo("tmux", "3.3", "Terminal multiplexer placeholder", "terminal"),
        SafePackageInfo("htop", "3.2", "System monitor placeholder", "system"),
        SafePackageInfo("ffmpeg", "6.0", "Media tool placeholder", "media")
    )

    fun all(): List<SafePackageInfo> = packages

    fun find(name: String): SafePackageInfo? =
        packages.find { it.name.equals(name.trim(), ignoreCase = true) }

    fun search(query: String): List<SafePackageInfo> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        return packages.filter {
            it.name.contains(q, ignoreCase = true) ||
                    it.description.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true)
        }
    }
}