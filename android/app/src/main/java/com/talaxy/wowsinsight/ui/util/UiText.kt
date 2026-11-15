package com.talaxy.wowsinsight.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.talaxy.wowsinsight.R
import com.talaxy.wowsinsight.data.remote.ApiException

/** A string that is either a resource with arguments or a server message. */
sealed interface UiText {
    data class Res(
        @param:StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class Raw(val value: String) : UiText
}

@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Res -> stringResource(id, *args.toTypedArray())
    is UiText.Raw -> value
}

/** Resolves outside composition, e.g. from a coroutine that shows a snackbar. */
fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Res -> context.getString(id, *args.toTypedArray())
    is UiText.Raw -> value
}

/** Maps an [ApiException] (or anything else) to copy the UI can show. */
fun Throwable.toUiText(): UiText = when (this) {
    is ApiException.InvalidUrl -> UiText.Res(R.string.error_invalid_url)
    is ApiException.Http -> UiText.Res(R.string.error_http_status, listOf(code.toString()))
    is ApiException.NotFound -> UiText.Res(R.string.error_not_found)
    is ApiException.Api -> if (!serverMessage.isNullOrBlank()) {
        UiText.Raw(serverMessage)
    } else {
        UiText.Res(R.string.error_api, listOf(code.toString()))
    }

    is ApiException.MissingData -> UiText.Res(R.string.error_missing_data)
    is ApiException.Decoding -> UiText.Res(
        R.string.error_decoding,
        listOf(detail.ifBlank { message.orEmpty() }),
    )

    is ApiException.Network -> if (!detail.isNullOrBlank()) {
        UiText.Raw(detail)
    } else {
        UiText.Res(R.string.error_unknown)
    }

    else -> UiText.Res(R.string.error_unknown)
}
