package com.innaval.ingresosnamao.core.common.result

import kotlinx.coroutines.CancellationException

sealed interface AppError {
    data object Network : AppError
    data object Unauthorized : AppError
    data class Http(val code: Int, val message: String?) : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/** Converts thrown exceptions into [AppResult.Failure]; cancellation is rethrown. */
suspend inline fun <T> runCatchingApp(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    AppResult.Failure(AppError.Unknown(e))
}
