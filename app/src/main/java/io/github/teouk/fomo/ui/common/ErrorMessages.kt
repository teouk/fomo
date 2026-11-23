package io.github.teouk.fomo.ui.common

import androidx.annotation.StringRes
import io.github.teouk.fomo.R
import io.github.teouk.fomo.data.repository.DataError

@StringRes
fun DataError.messageRes(): Int = when (this) {
    DataError.OFFLINE -> R.string.error_offline
    DataError.INVALID_API_KEY -> R.string.error_invalid_key
    DataError.RATE_LIMITED -> R.string.error_rate_limited
    DataError.NOT_FOUND -> R.string.error_not_found
    DataError.SERVER -> R.string.error_server
}
