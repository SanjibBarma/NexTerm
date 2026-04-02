package com.nexterm.app.data.repository

import android.content.Context
import com.nexterm.app.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

interface FileRepository {
    suspend fun listFiles(path: String): Result<List<FileItem>>
    suspend fun createFile(path: String, name: String): Result<File>
    suspend fun createDirectory(path: String, name: String): Result<File>
    suspend fun deleteFile(path: String): Result<Boolean>
    suspend fun renameFile(oldPath: String, newName: String): Result<File>
    suspend fun copyFile(sourcePath: String, destPath: String): Result<File>
    suspend fun moveFile(sourcePath: String, destPath: String): Result<File>
    suspend fun readFile(path: String): Result<String>
    suspend fun writeFile(path: String, content: String): Result<Boolean>
    suspend fun getFileInfo(path: String): Result<FileItem>
    fun getHomeDirectory(): String
    fun getStorageDirectory(): String
}

class FileRepositoryImpl(private val context: Context) : FileRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override suspend fun listFiles(path: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val directory = File(path)
            if (!directory.exists()) {
                return@withContext Result.failure(Exception("Directory does not exist"))
            }
            if (!directory.isDirectory) {
                return@withContext Result.failure(Exception("Path is not a directory"))
            }

            val files = directory.listFiles()?.map { file ->
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0,
                    lastModified = dateFormat.format(Date(file.lastModified())),
                    permissions = getPermissions(file),
                    isHidden = file.isHidden,
                    isReadable = file.canRead(),
                    isWritable = file.canWrite(),
                    isExecutable = file.canExecute()
                )
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: emptyList()

            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFile(path: String, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = File(path, name)
            if (file.createNewFile()) {
                Result.success(file)
            } else {
                Result.failure(Exception("Failed to create file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createDirectory(path: String, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path, name)
            if (dir.mkdirs()) {
                Result.success(dir)
            } else {
                Result.failure(Exception("Failed to create directory"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            val deleted = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameFile(oldPath: String, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            val newFile = File(oldFile.parent, newName)
            if (oldFile.renameTo(newFile)) {
                Result.success(newFile)
            } else {
                Result.failure(Exception("Failed to rename file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun copyFile(sourcePath: String, destPath: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val dest = File(destPath)
            source.copyTo(dest, overwrite = true)
            Result.success(dest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveFile(sourcePath: String, destPath: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val dest = File(destPath)
            source.copyTo(dest, overwrite = true)
            source.delete()
            Result.success(dest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readFile(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeFile(path: String, content: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.writeText(content)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFileInfo(path: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File does not exist"))
            }
            Result.success(
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else calculateDirectorySize(file),
                    lastModified = dateFormat.format(Date(file.lastModified())),
                    permissions = getPermissions(file),
                    isHidden = file.isHidden,
                    isReadable = file.canRead(),
                    isWritable = file.canWrite(),
                    isExecutable = file.canExecute()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getHomeDirectory(): String = context.filesDir.absolutePath

    override fun getStorageDirectory(): String =
        context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath

    private fun getPermissions(file: File): String {
        val r = if (file.canRead()) "r" else "-"
        val w = if (file.canWrite()) "w" else "-"
        val x = if (file.canExecute()) "x" else "-"
        return "$r$w$x"
    }

    private fun calculateDirectorySize(directory: File): Long {
        var size: Long = 0
        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }
}