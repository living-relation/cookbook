package com.livingrelation.markdownnotes.core.files

sealed class FileError {
    data object AccessDenied : FileError()

    data object NotFound : FileError()

    data class ProviderError(val detail: String?) : FileError()

    data object EncodingError : FileError()
}

class FileIoException(val error: FileError) : Exception(error.toString())
