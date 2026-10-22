package com.livingrelation.markdownnotes.core.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NoteIo {
    private val utf8: Charset = StandardCharsets.UTF_8

    const val BUNDLED_NOTES_BASE_URI = "file:///android_asset/notes"

    fun persistReadPermission(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    fun imageBaseUri(uri: Uri?): String? {
        if (uri == null) return BUNDLED_NOTES_BASE_URI
        if (!uri.scheme.equals("file", ignoreCase = true)) return null
        val parent = uri.path?.let(::File)?.parentFile ?: return null
        return Uri.fromFile(parent).toString()
    }

    suspend fun readText(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val text =
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(utf8)
                    } ?: return@withContext Result.failure(FileIoException(FileError.NotFound))
                Result.success(text)
            } catch (throwable: Throwable) {
                Result.failure(toFileIoException(throwable))
            }
        }

    suspend fun resolveDisplayName(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            val fromQuery = queryDisplayName(context, uri)
            if (!fromQuery.isNullOrBlank()) return@withContext fromQuery
            uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: "Untitled.md"
        }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
        return null
    }

    fun isUriAccessible(context: Context, uri: Uri): Boolean {
        val hasPersistedRead =
            context.contentResolver.persistedUriPermissions.any { permission ->
                permission.uri == uri && permission.isReadPermission
            }
        if (hasPersistedRead) return true
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: SecurityException) {
            false
        } catch (_: FileNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun toFileIoException(throwable: Throwable): FileIoException {
        if (throwable is FileIoException) return throwable
        val error =
            when (throwable) {
                is SecurityException -> FileError.AccessDenied
                is FileNotFoundException -> FileError.NotFound
                is java.nio.charset.MalformedInputException,
                is java.nio.charset.UnmappableCharacterException,
                -> FileError.EncodingError
                else ->
                    FileError.ProviderError(
                        throwable.message ?: throwable.javaClass.simpleName,
                    )
            }
        return FileIoException(error)
    }
}
