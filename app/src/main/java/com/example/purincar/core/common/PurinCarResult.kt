// The loading, success or error wrapper every data request returns.
package com.example.purincar.core.common

import androidx.compose.runtime.Immutable

sealed interface PurinCarResult<out T> {
    data object Loading : PurinCarResult<Nothing>

    @Immutable
    data class Success<out T>(val data: T) : PurinCarResult<T>

    @Immutable
    data class Error(val message: String) : PurinCarResult<Nothing>
}

// Transforms the data in a successful result and passes loading and errors through unchanged.
inline fun <T, R> PurinCarResult<T>.map(transform: (T) -> R): PurinCarResult<R> =
    when (this) {
        is PurinCarResult.Loading -> PurinCarResult.Loading
        is PurinCarResult.Error -> this
        is PurinCarResult.Success -> PurinCarResult.Success(transform(data))
    }
